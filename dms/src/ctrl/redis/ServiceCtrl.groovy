package ctrl.redis

import com.segment.common.Conf
import com.segment.common.Utils
import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import model.*
import model.json.*
import org.apache.commons.beanutils.BeanUtils
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import plugin.BasePlugin
import rm.RedisManager
import rm.RmJobExecutor
import rm.SlotBalancer
import rm.job.RmJob
import rm.job.RmJobTypes
import rm.job.task.MeetNodesSetSlotsTask
import rm.job.task.WaitClusterStateTask
import rm.job.task.WaitInstancesRunningTask
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

final int clusterId = 1

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
                resp.halt(500, 'mode must be standalone, sentinel or cluster')
            }
            dto.where('mode = ?', mode)
        }

        def status = req.param('status')
        if (status) {
            if (!(status in ['creating', 'running', 'deleted', 'unhealthy'])) {
                resp.halt(500, 'status must be creating, running, deleted or unhealthy')
            }
            dto.where('status = ?', status)
        }

        def pager = dto.listPager(pageNum, pageSize)

        def instance = InMemoryAllContainerManager.instance
        def cached = InMemoryCacheSupport.instance
        if (pager.list) {
            pager.list.each { one ->
                if (one.mode == RmServiceDTO.Mode.standalone || one.mode == RmServiceDTO.Mode.sentinel) {
                    def appOne = cached.oneApp(one.appId)
                    if (!appOne) {
                        return
                    }

                    def containerList = instance.getContainerList(clusterId, one.appId)
                    def runningNumber = containerList.findAll { x ->
                        x.running()
                    }.size()

                    if (one.status == RmServiceDTO.Status.running) {
                        if (one.replicas != runningNumber) {
                            one.status = RmServiceDTO.Status.unhealthy
                        }
                    } else if (one.status == RmServiceDTO.Status.creating) {
                        if (one.replicas == runningNumber) {
                            one.status = RmServiceDTO.Status.running
                            new RmServiceDTO(id: one.id, status: RmServiceDTO.Status.running).update()
                        }
                    }
                } else {
                    // cluster mode
                    def checkTask = new WaitClusterStateTask(new RmJob(rmService: one))
                    def jobResult = checkTask.doTask()

                    if (one.status == RmServiceDTO.Status.running) {
                        if (!jobResult.isOk) {
                            one.status = RmServiceDTO.Status.unhealthy
                            one.extendParams.put('statusMessage', jobResult.message)
                        } else {
                            one.extendParams.put('statusMessage', 'check cluster state ok')
                        }
                    } else if (one.status == RmServiceDTO.Status.creating) {
                        if (!jobResult.isOk) {
                            one.extendParams.put('statusMessage', jobResult.message)
                        } else {
                            one.status = RmServiceDTO.Status.running
                            one.extendParams.put('statusMessage', 'check cluster state ok')
                        }
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
            resp.halt(500, 'service not found')
        }

        // todo
        [one: one]
    }

    h.post('/add') { req, resp ->
        def one = req.bodyAs(RmServiceDTO)
        assert one.name && one.mode && one.engineType && one.engineVersion && one.port && one.replicas && one.shards

        // check name
        def existOne = new RmServiceDTO(name: one.name).queryFields('id').one()
        if (existOne) {
            resp.halt(500, 'name already exists')
        }

        def namespaceId = NamespaceDTO.createIfNotExist(clusterId, 'redis')

        // check node ready
        def instance = InMemoryAllContainerManager.instance
        def hbOkNodeList = instance.hbOkNodeList(clusterId, 'ip,tags')
        if (one.nodeTags) {
            def matchNodeList = hbOkNodeList.findAll { node ->
                node.tags && node.tags.any { tag ->
                    tag in one.nodeTags
                }
            }
            if (matchNodeList.size() < one.shards * one.replicas) {
                resp.halt(500, 'not enough node ready, for tags: ' + one.nodeTags)
            }
        } else {
            if (hbOkNodeList.size() < one.replicas && !Conf.instance.isOn('rm.isSingleNodeTest')) {
                resp.halt(500, 'not enough node ready')
            }
        }

        boolean isSentinelMode = one.mode == RmServiceDTO.Mode.sentinel
        def isClusterMode = one.mode == RmServiceDTO.Mode.cluster

        String sentinelAppName
        if (isSentinelMode) {
            def sentinelServiceOne = new RmSentinelServiceDTO(id: one.sentinelServiceId).one()
            if (!sentinelServiceOne) {
                resp.halt(500, 'sentinel service not found')
            }
            if (sentinelServiceOne.status != RmSentinelServiceDTO.Status.running) {
                resp.halt(500, 'sentinel service not running')
            }

            sentinelAppName = 'rm_sentinel_' + sentinelServiceOne.name
        }

        // create app
        def app = new AppDTO()
        app.clusterId = clusterId
        app.namespaceId = namespaceId
        app.name = 'rm_' + one.name
        app.status = AppDTO.Status.manual.val
        app.updatedDate = new Date()

        def conf = new AppConf()
        conf.containerNumber = one.replicas
        conf.registryId = BasePlugin.addRegistryIfNotExist('docker.1ms.run', 'https://docker.1ms.run')

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
        conf.memMB = extendParams.getInt('memMB', 1024)
        conf.memReservationMB = conf.memMB
        conf.cpuFixed = extendParams.getDouble('cpuFixed', 1)

        conf.networkMode = 'host'
        conf.portList << new PortMapping(privatePort: one.port, publicPort: one.port)

        conf.targetNodeTagList = []
        if (one.nodeTags) {
            one.nodeTags.each {
                conf.targetNodeTagList << it
            }
        }

        def c = Conf.instance
        conf.isLimitNode = c.isOn('rm.isSingleNodeTest')

        final String dataDir = RedisManager.dataDir()
        def serviceDataDir = dataDir + '/' + one.engineType + '_data_' + Utils.uuid() + '_${appId}_${instanceIndex}'
        def nodeVolumeId = new NodeVolumeDTO(imageName: conf.imageName(), name: 'for service ' + one.name, dir: serviceDataDir,
                clusterId: clusterId, des: 'data dir for service').add()
        def dirOne = new DirVolumeMount(
                dir: serviceDataDir, dist: '/data/redis', mode: 'rw',
                nodeVolumeId: nodeVolumeId)
        conf.dirVolumeList << dirOne

        // config tpl use redis template
        def tplOne = new ImageTplDTO(imageName: 'library/redis', name: 'redis.template.conf.tpl').one()
        def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content, dist: '/etc/redis/redis.conf')
        mountOne.isParentDirMount = false

        mountOne.paramList << new KVPair<String>('isSingleNode', c.isOn('rm.isSingleNodeTest').toString())
        mountOne.paramList << new KVPair<String>('port', '' + one.port)
        mountOne.paramList << new KVPair<String>('dataDir', '/data/redis')
        mountOne.paramList << new KVPair<String>('password', one.pass ?: '')
        mountOne.paramList << new KVPair<String>('isMasterSlave', isSentinelMode ? 'true' : 'false')
        mountOne.paramList << new KVPair<String>('isCluster', isClusterMode ? 'true' : 'false')
        if (sentinelAppName) {
            mountOne.paramList << new KVPair<String>('sentinelAppName', sentinelAppName)
        }
        mountOne.paramList << new KVPair<String>('configTemplateId', '' + one.configTemplateId)
        conf.fileVolumeList << mountOne

        app.conf = conf

        if (!isClusterMode) {
            // only create one application
            def appId = app.add()
            app.id = appId

            one.appId = appId
            one.status = RmServiceDTO.Status.creating
            one.createdDate = new Date()
            one.updatedDate = new Date()

            def id = one.add()

            RmJobExecutor.instance.runCreatingAppJob(app)

            return [id: id]
        }

        // cluster mode
        List<AppDTO> appListByShard = []
        one.clusterSlotsDetail = new ClusterSlotsDetail()
        for (i in 0..<one.shards) {
            def appShard = new AppDTO()
            // shadow copy properties from app
            BeanUtils.copyProperties(appShard, app)
            // rename
            appShard.name = app.name + '_shard_' + i

            def portForThisShard = one.port + i * 10

            appShard.conf.fileVolumeList.clear()
            def copyOne = mountOne.copy()
            copyOne.paramList.find { it.key == 'port' }.value = '' + portForThisShard
            appShard.conf.fileVolumeList << copyOne

            appShard.conf.portList.clear()
            appShard.conf.portList << new PortMapping(privatePort: portForThisShard, publicPort: portForThisShard)

            def appShardId = appShard.add()
            appShard.id = appShardId
            appListByShard << appShard

            // calc slots range
            def pager = SlotBalancer.splitAvg(one.shards, i + 1)
            def shard = new ClusterSlotsDetail.Shard(shardIndex: i, appId: appShardId)
            shard.multiSlotRange.addSingle(pager.start, pager.end - 1)

            one.clusterSlotsDetail.shards << shard
        }

        one.status = RmServiceDTO.Status.creating
        one.createdDate = new Date()
        one.updatedDate = new Date()

        def id = one.add()

        appListByShard.each {
            RmJobExecutor.instance.runCreatingAppJob(it)
        }

        def rmJob = new RmJob()
        rmJob.rmService = one
        rmJob.type = RmJobTypes.CLUSTER_CREATE
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmServiceId', id.toString())
        // sub tasks
        for (i in 0..<one.shards) {
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

    h.delete('/delete') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int
        assert id > 0

        // check if exists
        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(500, 'not exists')
        }

        if (one.status == RmServiceDTO.Status.deleted) {
            resp.halt(500, 'already deleted')
        }

        if (one.status == RmServiceDTO.Status.creating) {
            resp.halt(500, 'creating, please wait')
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
