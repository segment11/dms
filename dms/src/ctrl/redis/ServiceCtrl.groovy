package ctrl.redis

import com.alibaba.fastjson.JSON
import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import model.*
import model.cluster.SlotRange
import model.json.*
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import redis.clients.jedis.HostAndPort
import rm.RedisManager
import rm.RmJobExecutor
import rm.SlotBalancer
import rm.job.RmJob
import rm.job.RmJobTypes
import rm.job.task.*
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis/service') {
    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def dto = new RmServiceDTO().noWhere()

        def keyword = req.param('keyword')
        dto.where(keyword as boolean, '(name like ?)', '%' + keyword + '%')

        def mode = req.param('mode')
        if (mode) {
            if (!(mode in ['standalone', 'sentinel', 'cluster'])) {
                resp.halt(409, 'mode must be standalone, sentinel or cluster')
            }
            dto.where('mode = ?', mode)
        }

        def status = req.param('status')
        if (status) {
            if (!(status in ['creating', 'running', 'deleted', 'unhealthy'])) {
                resp.halt(409, 'status must be creating, running, deleted or unhealthy')
            }
            dto.where('status = ?', status)
        }

        def pager = dto.listPager(pageNum, pageSize)

        if (pager.list) {
            pager.list.each { one ->
                def runningContainerList = one.runningContainerList()
                if (runningContainerList.size() == one.shards * one.replicas) {
                    if (one.status.canChangeToRunningWhenInstancesRunningOk()) {
                        one.status = RmServiceDTO.Status.running
                        new RmServiceDTO(id: one.id, status: RmServiceDTO.Status.running).update()
                    }
                } else {
                    if (one.status == RmServiceDTO.Status.running) {
                        one.status = RmServiceDTO.Status.unhealthy
                        one.extendParams.put('statusMessage', 'running instances not ready')
                    }
                }
            }
        }

        pager
    }

    h.get('/one') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        def r = [:]
        def ext = [:]
        r.ext = ext

        ext.clusterId = RedisManager.CLUSTER_ID
        ext.namespaceId = NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'redis')

        r.one = one

        if (one.status == RmServiceDTO.Status.running) {
            r.checkResult = one.checkNodes()
        }

        def configTemplateOne = new RmConfigTemplateDTO(id: one.configTemplateId).queryFields('name').one()
        assert configTemplateOne
        ext.configTemplateName = configTemplateOne.name

        List<Map> nodes = []
        r.nodes = nodes

        if (one.mode == RmServiceDTO.Mode.standalone || one.mode == RmServiceDTO.Mode.sentinel) {
            def appOne = InMemoryCacheSupport.instance.oneApp(one.appId)
            ext.appId = appOne.id
            ext.appName = appOne.name
            ext.appDes = appOne.des
        }

        if (one.mode == RmServiceDTO.Mode.standalone) {
            def instance = InMemoryAllContainerManager.instance
            def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, one.appId)
            containerList.each { x ->
                nodes << [shardIndex: 0, replicaIndex: x.instanceIndex(), ip: x.nodeIp, port: one.listenPort(x), isPrimary: true, running: x.running()]
            }

            if (nodes) {
                def node = nodes[0]
                ext.connectionString = one.pass ? 'redis://****@' + node.ip + ':' + node.port : "redis://" + node.ip + ':' + node.port
            }

        } else if (one.mode == RmServiceDTO.Mode.sentinel) {
            assert one.primaryReplicasDetail && one.primaryReplicasDetail.nodes

            def instance = InMemoryAllContainerManager.instance
            def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, one.appId)

            for (n in one.primaryReplicasDetail.nodes) {
                def x = containerList.find { x ->
                    x.nodeIp == n.ip && one.listenPort(x) == n.port && x.instanceIndex() == n.replicaIndex
                }
                def running = x && x.running()
                nodes << [shardIndex: 0, replicaIndex: n.replicaIndex, ip: n.ip, port: n.port, isPrimary: n.isPrimary, running: running]
            }

            // fix
            if (!one.sentinelAppId) {
                def sentinelServiceOne = new RmSentinelServiceDTO(id: one.sentinelServiceId).one()
                new RmServiceDTO(id: one.id, sentinelAppId: sentinelServiceOne.appId).update()
                one.sentinelAppId = sentinelServiceOne.appId
            }

            def sentinelAppOne = InMemoryCacheSupport.instance.oneApp(one.sentinelAppId)
            def sentinelConfOne = sentinelAppOne.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
            def isSentinelSetPass = sentinelConfOne.paramValue('password') as Boolean
            def masterName = 'redis-app-' + one.appId

            def sentinelContainerList = instance.getContainerList(RedisManager.CLUSTER_ID, one.sentinelAppId)
            if (sentinelContainerList) {
                def inner = sentinelContainerList.collect { x -> x.nodeIp + ':' + one.listenPort(x) }.join(',')
                ext.connectionString = isSentinelSetPass ? 'redis-sentinel://****@' + inner + '/' + masterName : 'redis-sentinel://' + inner + '/' + masterName
            }
        } else {
            assert one.clusterSlotsDetail && one.clusterSlotsDetail.shards

            List<HostAndPort> hostAndPortList = []

            def instance = InMemoryAllContainerManager.instance
            for (shard in one.clusterSlotsDetail.shards) {
                def appOne = InMemoryCacheSupport.instance.oneApp(shard.appId)

                def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, shard.appId)
                for (n in shard.nodes) {
                    def x = containerList.find { x ->
                        x.nodeIp == n.ip && one.listenPort(x) == n.port && x.instanceIndex() == n.replicaIndex
                    }
                    def running = x && x.running()
                    nodes << [shardIndex   : shard.shardIndex, replicaIndex: n.replicaIndex, ip: n.ip, port: n.port, isPrimary: n.isPrimary, running: running,
                              slotRangeList: shard.multiSlotRange.list,
                              appId        : appOne.id, appName: appOne.name, appDes: appOne.des]

                    if (n.isPrimary) {
                        hostAndPortList << new HostAndPort(n.ip, n.port)
                    }
                }
            }

            if (hostAndPortList) {
                ext.connectionString = one.pass ? 'redis-cluster://****@' + hostAndPortList.join(',') : 'redis-cluster://' + hostAndPortList.join(',')
            }
        }

        r
    }

    h.post('/add') { req, resp ->
        def one = req.bodyAs(RmServiceDTO)
        assert one.name && one.mode && one.engineType && one.engineVersion && one.port && one.replicas && one.shards

        // check name
        def existOne = new RmServiceDTO(name: one.name).queryFields('id').one()
        if (existOne) {
            resp.halt(409, 'name already exists')
        }

        // check node ready
        def instance = InMemoryAllContainerManager.instance
        def hbOkNodeList = instance.hbOkNodeList(RedisManager.CLUSTER_ID, 'ip,tags')
        if (one.nodeTags) {
            def matchNodeList = hbOkNodeList.findAll { node ->
                node.tags && node.tags.any { tag ->
                    tag in one.nodeTags
                }
            }
            if (matchNodeList.size() < one.shards * one.replicas) {
                resp.halt(409, 'not enough node ready, for tags: ' + one.nodeTags)
            }
        } else {
            if (hbOkNodeList.size() < one.replicas && !RedisManager.isOnlyOneNodeForTest()) {
                resp.halt(409, 'not enough node ready')
            }
        }

        // check max shards / replicas
        if (one.shards > RedisManager.ONE_CLUSTER_MAX_SHARDS) {
            resp.halt(409, 'max shards is ' + RedisManager.ONE_CLUSTER_MAX_SHARDS)
        }
        if (one.replicas > RedisManager.ONE_SHARD_MAX_REPLICAS) {
            resp.halt(409, 'max replicas is ' + RedisManager.ONE_SHARD_MAX_REPLICAS)
        }

        // check port range conflict
        def runningServiceList = new RmServiceDTO(status: RmServiceDTO.Status.running).queryFields('id,name,mode,shards,replicas,port').list()
        def alreadyUsingPortRangeList = runningServiceList.collect {
            if (it.mode == RmServiceDTO.Mode.standalone || it.mode == RmServiceDTO.Mode.sentinel) {
                return [it.port, it.port + RedisManager.ONE_SHARD_MAX_REPLICAS, it.name]
            } else {
                return [it.port, it.port + RedisManager.ONE_CLUSTER_MAX_SHARDS * RedisManager.ONE_SHARD_MAX_REPLICAS, it.name]
            }
        }
        for (range in alreadyUsingPortRangeList) {
            if (one.port >= (range[0] as int) && one.port < (range[1] as int)) {
                resp.halt(409, 'port conflict, as another service is using it, service: ' +
                        range[2] + ' port: ' + range[0] + '-' + range[1])
            }
        }

        if (one.pass) {
            one.pass = RedisManager.encode(one.pass)
        }

        def isStandaloneMode = one.mode == RmServiceDTO.Mode.standalone
        def isSentinelMode = one.mode == RmServiceDTO.Mode.sentinel
        def isClusterMode = one.mode == RmServiceDTO.Mode.cluster

        String sentinelAppName
        if (isSentinelMode) {
            def sentinelServiceOne = new RmSentinelServiceDTO(id: one.sentinelServiceId).one()
            if (!sentinelServiceOne) {
                resp.halt(404, 'sentinel service not found')
            }
            if (sentinelServiceOne.status != RmSentinelServiceDTO.Status.running) {
                resp.halt(409, 'sentinel service not running')
            }

            sentinelAppName = 'rm_sentinel_' + sentinelServiceOne.name
            one.sentinelAppId = sentinelServiceOne.appId
        }

        // create app
        def namespaceId = NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'redis')

        def app = new AppDTO()
        app.clusterId = RedisManager.CLUSTER_ID
        app.namespaceId = namespaceId
        app.name = 'rm_' + one.name
        // sentinel managed redis servers can be managed by dms
        app.status = (isStandaloneMode || isSentinelMode) ? AppDTO.Status.auto : AppDTO.Status.manual
        app.updatedDate = new Date()

        def conf = new AppConf()
        conf.containerNumber = one.replicas
        conf.registryId = RedisManager.preferRegistryId()

        if (one.engineType == RmServiceDTO.EngineType.redis) {
            conf.group = 'library'
            conf.image = 'redis'
            conf.tag = one.engineVersion
        } else if (one.engineType == RmServiceDTO.EngineType.valkey) {
            conf.group = 'library'
            conf.image = 'valkey'
            conf.tag = one.engineVersion
        } else if (one.engineType == RmServiceDTO.EngineType.engula) {
            conf.group = 'montplex'
            conf.image = 'engula'
            conf.tag = one.engineVersion
        }

        def extendParams = one.extendParams

        // default docker container resource config values
        conf.memReservationMB = extendParams.getInt('memMB', 1024)
        // change here
        conf.memMB = (conf.memReservationMB * 2).intValue()
        conf.cpuFixed = extendParams.getDouble('cpuFixed', 1)

        // set maxmemory the same as container required
        one.maxmemoryMb = conf.memReservationMB
        if (one.maxmemoryMb > RedisManager.ONE_INSTANCE_MAX_MEMORY_MB) {
            resp.halt(409, 'maxmemory MB need less than ' + RedisManager.ONE_INSTANCE_MAX_MEMORY_MB + 'MB')
        }

        conf.networkMode = 'host'
        conf.portList << new PortMapping(privatePort: one.port, publicPort: one.port)

        conf.targetNodeTagList = []
        if (one.nodeTags) {
            one.nodeTags.each {
                conf.targetNodeTagList << it
            }
        }
        conf.targetNodeTagListByInstanceIndex = []
        if (one.nodeTagsByReplicaIndex) {
            one.nodeTagsByReplicaIndex.each {
                conf.targetNodeTagListByInstanceIndex << it
            }
        }

        def isSingleNode = RedisManager.isOnlyOneNodeForTest()
        conf.isLimitNode = isSingleNode

        final String dataDir = RedisManager.dataDir()
        def serviceDataDir = dataDir + '/' + one.engineType + '_data_app_' + '_${appId}_${instanceIndex}'
        def nodeVolumeId = new NodeVolumeDTO(imageName: conf.imageName(), name: 'for service ' + one.name, dir: serviceDataDir,
                clusterId: RedisManager.CLUSTER_ID, des: 'data dir for service').add()
        def dirOne = new DirVolumeMount(
                dir: serviceDataDir, dist: '/data/redis', mode: 'rw',
                nodeVolumeId: nodeVolumeId)
        conf.dirVolumeList << dirOne

        // config tpl use redis template
        def tplOne = new ImageTplDTO(imageName: 'library/redis', name: 'redis.template.conf.tpl').one()
        def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content, dist: '/etc/redis/redis.conf')
        mountOne.isParentDirMount = false

        mountOne.paramList << new KVPair<String>('isSingleNode', isSingleNode.toString())
        mountOne.paramList << new KVPair<String>('port', '' + one.port)
        mountOne.paramList << new KVPair<String>('dataDir', '/data/redis')
        mountOne.paramList << new KVPair<String>('password', one.pass ?: '')
        mountOne.paramList << new KVPair<String>('maxmemoryMB', one.maxmemoryMb.toString())
        mountOne.paramList << new KVPair<String>('isMasterSlave', isSentinelMode ? 'true' : 'false')
        mountOne.paramList << new KVPair<String>('isCluster', isClusterMode ? 'true' : 'false')
        if (sentinelAppName) {
            mountOne.paramList << new KVPair<String>('sentinelAppName', sentinelAppName)
        }
        mountOne.paramList << new KVPair<String>('configTemplateId', '' + one.configTemplateId)
        conf.fileVolumeList << mountOne

        app.conf = conf

//        app.logConf = new LogConf()
//        app.logConf.logFileList << new LogConf.LogFile(pathPattern: dataDir + '/**/redis.log', isMultilineSupport: false)

        if (!isClusterMode) {
            // only create one application
            def appId = app.add()
            app.id = appId

            one.appId = appId
            one.status = RmServiceDTO.Status.creating
            one.createdDate = new Date()
            one.updatedDate = new Date()

            if (one.mode == RmServiceDTO.Mode.sentinel) {
                one.primaryReplicasDetail = new PrimaryReplicasDetail()
            }

            def id = one.add()
            one.id = id

            def rmJob = new RmJob()
            rmJob.rmService = one
            rmJob.type = RmJobTypes.BASE_CREATE
            rmJob.status = JobStatus.created
            rmJob.params = new JobParams()
            rmJob.params.put('rmServiceId', id.toString())
            // add tasks
            rmJob.taskList << new RunCreatingAppJobTask(rmJob, 0, app)
            rmJob.taskList << new WaitInstancesRunningTask(rmJob, 0)
            if (one.mode == RmServiceDTO.Mode.sentinel) {
                rmJob.taskList << new WaitPrimaryReplicasStateTask(rmJob)
            }

            rmJob.createdDate = new Date()
            rmJob.updatedDate = new Date()
            rmJob.save()

            RmJobExecutor.instance.execute {
                rmJob.run()
            }

            return [id: id]
        }

        // cluster mode
        List<AppDTO> appListByShard = []
        one.clusterSlotsDetail = new ClusterSlotsDetail()
        for (shardIndex in 0..<one.shards) {
            def appShard = JSON.parseObject(JSON.toJSONString(app), AppDTO)
            // rename
            appShard.name = 'rm_' + one.name + '_shard_' + shardIndex

            def portForThisShard = one.port + shardIndex * RedisManager.ONE_SHARD_MAX_REPLICAS
            appShard.conf.fileVolumeList[0].paramList.find { it.key == 'port' }.value = '' + portForThisShard

            appShard.conf.portList.clear()
            appShard.conf.portList << new PortMapping(privatePort: portForThisShard, publicPort: portForThisShard)

            def appShardId = appShard.add()
            appShard.id = appShardId
            appListByShard << appShard

            // calc slots range
            def pager = SlotBalancer.splitAvg(one.shards, shardIndex + 1)
            def shard = new ClusterSlotsDetail.Shard(shardIndex: shardIndex, appId: appShardId)
            shard.multiSlotRange.addSingle(pager.start, pager.end - 1)

            one.clusterSlotsDetail.shards << shard
        }

        one.status = RmServiceDTO.Status.creating
        one.createdDate = new Date()
        one.updatedDate = new Date()

        def id = one.add()
        one.id = id

        def rmJob = new RmJob()
        rmJob.rmService = one
        rmJob.type = RmJobTypes.CLUSTER_CREATE
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmServiceId', id.toString())
        // sub tasks
        for (i in 0..<one.shards) {
            rmJob.taskList << new RunCreatingAppJobTask(rmJob, i, appListByShard[i])
            rmJob.taskList << new WaitInstancesRunningTask(rmJob, i)
        }
        rmJob.taskList << new MeetNodesSetSlotsTask(rmJob)
        rmJob.taskList << new WaitClusterStateTask(rmJob)

        rmJob.createdDate = new Date()
        rmJob.updatedDate = new Date()
        rmJob.save()

        RmJobExecutor.instance.execute {
            rmJob.run()
        }

        [id: id]
    }

    h.post('/cluster-scale-up') { req, resp ->
        def map = req.bodyAs(HashMap)
        def id = map.id as int
        def toShards = map.toShards as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not exists')
        }

        if (one.mode != RmServiceDTO.Mode.cluster) {
            resp.halt(409, 'only support cluster mode')
        }

        def oldShards = one.shards
        if (toShards != oldShards * 2) {
            resp.halt(409, 'new shards must be old shards * 2')
        }

        if (toShards > RedisManager.ONE_CLUSTER_MAX_SHARDS) {
            resp.halt(409, 'max shards is ' + RedisManager.ONE_CLUSTER_MAX_SHARDS)
        }

        if (one.status != RmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def checkResult = one.checkClusterNodesAndSlots()
        if (!checkResult.isOk) {
            resp.halt(409, checkResult.message)
        }

        def firstShard = one.clusterSlotsDetail.shards[0]
        def app = new AppDTO(id: firstShard.appId).one()
        if (!app) {
            resp.halt(409, 'first shard app not exists')
        }

        List<ClusterSlotsDetail.Shard> newShardList = []
        for (i in one.shards..<toShards) {
            def shard = new ClusterSlotsDetail.Shard(shardIndex: i)
            newShardList << shard
        }

        List<AppDTO> appListByShard = []
        for (shard in newShardList) {
            def shardIndex = shard.shardIndex

            def appShard = JSON.parseObject(JSON.toJSONString(app), AppDTO)
            appShard.id = null
            // rename
            appShard.name = 'rm_' + one.name + '_shard_' + shardIndex

            def portForThisShard = one.port + shardIndex * RedisManager.ONE_SHARD_MAX_REPLICAS
            appShard.conf.fileVolumeList[0].paramList.find { it.key == 'port' }.value = '' + portForThisShard

            appShard.conf.portList.clear()
            appShard.conf.portList << new PortMapping(privatePort: portForThisShard, publicPort: portForThisShard)

            def appShardId = appShard.add()
            appShard.id = appShardId
            appListByShard << appShard

            shard.appId = appShardId
            one.clusterSlotsDetail.shards << shard
        }

        new RmServiceDTO(id: id, clusterSlotsDetail: one.clusterSlotsDetail, shards: toShards,
                status: RmServiceDTO.Status.scaling_up, updatedDate: new Date()).update()
        log.warn 'scale up service, name: {}, old shards: {}, new shards: {}', one.name, oldShards, toShards

        def rmJob = new RmJob()
        rmJob.rmService = one
        rmJob.type = RmJobTypes.CLUSTER_SCALE
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmServiceId', id.toString())
        // sub tasks
        for (shard in newShardList) {
            rmJob.taskList << new RunCreatingAppJobTask(rmJob, shard.shardIndex, appListByShard[shard.shardIndex - oldShards])
            rmJob.taskList << new WaitInstancesRunningTask(rmJob, shard.shardIndex)
        }
        rmJob.taskList << new MeetNodesWhenScaleUpTask(rmJob, newShardList)
        for (newShard in newShardList) {
            def migrateSlotsFromShard = one.clusterSlotsDetail.shards.find { it.shardIndex == newShard.shardIndex - oldShards }
            migrateSlotsFromShard.multiSlotRange.list.each { slotRange ->
                // skip only one slot
                if (slotRange.totalNumber() == 1) {
                    return
                }

                def halfSlotNumber = (slotRange.totalNumber() / 2).intValue()

                def halfSlotRange = new SlotRange()
                halfSlotRange.begin = slotRange.begin + halfSlotNumber
                halfSlotRange.end = slotRange.end

                def migrateSlotsTask = new MigrateSlotsTask(rmJob, migrateSlotsFromShard, newShard, halfSlotRange)
                rmJob.taskList << migrateSlotsTask
            }
        }
        rmJob.taskList << new WaitClusterStateAfterScaleTask(rmJob)

        rmJob.createdDate = new Date()
        rmJob.updatedDate = new Date()
        rmJob.save()

        RmJobExecutor.instance.execute {
            rmJob.run()
        }

        [id: id]
    }

    h.post('/cluster-scale-down') { req, resp ->
        def map = req.bodyAs(HashMap)
        def id = map.id as int
        def toShards = map.toShards as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not exists')
        }

        if (one.mode != RmServiceDTO.Mode.cluster) {
            resp.halt(409, 'only support cluster mode')
        }

        def oldShards = one.shards
        if (oldShards == 2) {
            resp.halt(409, 'min shards is 2')
        }

        if (toShards != (oldShards / 2).intValue()) {
            resp.halt(409, 'new shards must be old shards / 2')
        }

        if (one.status != RmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def checkResult = one.checkClusterNodesAndSlots()
        if (!checkResult.isOk) {
            resp.halt(409, checkResult.message)
        }

        def firstShard = one.clusterSlotsDetail.shards[0]
        def app = new AppDTO(id: firstShard.appId).one()
        if (!app) {
            resp.halt(409, 'first shard app not exists')
        }

        List<ClusterSlotsDetail.Shard> toRemoveShardList = one.clusterSlotsDetail.shards.findAll { shard ->
            shard.shardIndex >= toShards
        }

        new RmServiceDTO(id: id, shards: toShards,
                status: RmServiceDTO.Status.scaling_down, updatedDate: new Date()).update()
        log.warn 'scale down service, name: {}, old shards: {}, new shards: {}', one.name, oldShards, toShards

        one.shards = toShards

        def rmJob = new RmJob()
        rmJob.rmService = one
        rmJob.type = RmJobTypes.CLUSTER_SCALE
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmServiceId', id.toString())
        // sub tasks
        for (oldShard in toRemoveShardList) {
            def migrateSlotsToShard = one.clusterSlotsDetail.shards.find { it.shardIndex == oldShard.shardIndex - toShards }

            oldShard.multiSlotRange.list.each { slotRange ->
                def migrateSlotsTask = new MigrateSlotsTask(rmJob, oldShard, migrateSlotsToShard, slotRange)
                rmJob.taskList << migrateSlotsTask
            }

            rmJob.taskList << new ForgetNodeAfterScaleDownTask(rmJob, oldShard)
        }
        rmJob.taskList << new WaitClusterStateAfterScaleTask(rmJob)

        rmJob.createdDate = new Date()
        rmJob.updatedDate = new Date()
        rmJob.save()

        RmJobExecutor.instance.execute {
            rmJob.run()
        }

        [id: id]
    }

    h.post('/update-replicas') { req, resp ->
        def map = req.bodyAs(HashMap)
        def id = map.id as int
        def toReplicas = map.toReplicas as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not exists')
        }

        def oldReplicas = one.replicas
        if (oldReplicas == toReplicas) {
            resp.halt(409, 'old replicas is same')
        }

        if (one.status != RmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def checkResult = one.checkNodes()
        if (!checkResult.isOk) {
            resp.halt(409, checkResult.message)
        }

        // check sentinel application state
        if (one.mode == RmServiceDTO.Mode.sentinel) {
            def sentinelAppOne = InMemoryCacheSupport.instance.oneApp(one.sentinelAppId)

            def instance = InMemoryAllContainerManager.instance
            def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, one.sentinelAppId)
            if (runningContainerList.size() != sentinelAppOne.conf.containerNumber) {
                resp.halt(409, 'sentinel application container number is not enough')
            }
        }

        new RmServiceDTO(id: id, replicas: toReplicas,
                status: RmServiceDTO.Status.updating_replicas, updatedDate: new Date()).update()
        log.warn 'update service replicas, name: {}, old replicas: {}, new replicas: {}', one.name, oldReplicas, toReplicas

        one.replicas = toReplicas

        def rmJob = new RmJob()
        rmJob.rmService = one
        rmJob.type = RmJobTypes.REPLICAS_SCALE
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmServiceId', id.toString())

        if (toReplicas > oldReplicas) {
            List<Integer> replicaIndexList = []
            for (i in oldReplicas..<toReplicas) {
                replicaIndexList << i
            }
            rmJob.taskList << new AddReplicasTask(rmJob, replicaIndexList)
        } else {
            List<Integer> replicaIndexList = []
            for (i in toReplicas..<oldReplicas) {
                replicaIndexList << i
            }
            rmJob.taskList << new RemoveReplicasTask(rmJob, replicaIndexList)
        }
        rmJob.taskList << new WaitServiceCheckNodesOkTask(rmJob)

        rmJob.createdDate = new Date()
        rmJob.updatedDate = new Date()
        rmJob.save()

        RmJobExecutor.instance.execute {
            rmJob.run()
        }

        [id: id]
    }

    h.post('/failover') { req, resp ->
        def map = req.bodyAs(HashMap)
        def id = map.id as int
        def shardIndex = map.shardIndex as int
        def replicaIndex = map.replicaIndex as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not exists')
        }

        def oldReplicas = one.replicas
        if (oldReplicas <= replicaIndex) {
            resp.halt(409, 'replicas index is out of range')
        }

        if (one.status != RmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def checkResult = one.checkNodes()
        if (!checkResult.isOk) {
            resp.halt(409, checkResult.message)
        }

        def rmJob = new RmJob()
        rmJob.rmService = one
        rmJob.type = RmJobTypes.FAILOVER
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmServiceId', id.toString())
        rmJob.params.put('shardIndex', shardIndex.toString())
        rmJob.params.put('replicaIndex', replicaIndex.toString())

        rmJob.taskList << new FailoverTask(rmJob, shardIndex, replicaIndex)
        rmJob.taskList << new WaitServiceCheckNodesOkTask(rmJob)

        rmJob.createdDate = new Date()
        rmJob.updatedDate = new Date()
        rmJob.save()

        RmJobExecutor.instance.execute {
            rmJob.run()
        }

        [id: id]
    }

    h.post('/view-pass') { req, resp ->
        def map = req.bodyAs(HashMap)
        def id = map.id as int

        def one = new RmServiceDTO(id: id).queryFields('pass').one()
        if (!one) {
            resp.halt(404, 'service not exists')
        }

        if (!one.pass) {
            resp.halt(404, 'service password is not set')
        }

        [pass: RedisManager.decode(one.pass)]
    }

    h.delete('/delete') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int
        assert id > 0

        // check if exists
        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not exists')
        }

        if (one.status == RmServiceDTO.Status.deleted) {
            resp.halt(409, 'already deleted')
        }

        if (one.status == RmServiceDTO.Status.creating) {
            resp.halt(409, 'creating, please wait')
        }

        if (one.mode == RmServiceDTO.Mode.standalone || one.mode == RmServiceDTO.Mode.sentinel) {
            RedisManager.stopContainers(one.appId)
        } else {
            // cluster mode
            one.clusterSlotsDetail.shards.each {
                RedisManager.stopContainers(it.appId)
            }
        }
        new RmServiceDTO(id: id, status: RmServiceDTO.Status.deleted, updatedDate: new Date()).update()

        [flag: true]
    }
}
