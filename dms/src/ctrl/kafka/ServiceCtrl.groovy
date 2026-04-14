package ctrl.kafka

import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import km.CuratorPoolHolder
import km.KafkaManager
import km.KmJobExecutor
import km.job.KmJob
import km.job.KmJobTypes
import km.job.task.*
import model.*
import model.json.*
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import server.InMemoryAllContainerManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/service') {
    h.get('/simple-list') { req, resp ->
        def list = new KmServiceDTO(status: KmServiceDTO.Status.running).
                queryFields('id,name').
                list()

        [list: list.collect {
            return [
                    id  : it.id,
                    name: it.name,
            ]
        }]
    }

    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def dto = new KmServiceDTO().noWhere()

        def keyword = req.param('keyword')
        dto.where(keyword as boolean, '(name like ?)', '%' + keyword + '%')

        def mode = req.param('mode')
        if (mode) {
            if (!(mode in ['standalone', 'cluster'])) {
                resp.halt(409, 'mode must be standalone or cluster')
            }
            dto.where('mode = ?', mode)
        }

        def status = req.param('status')
        if (status) {
            if (!(status in ['creating', 'running', 'deleted', 'unhealthy', 'scaling_up', 'scaling_down', 'stopped'])) {
                resp.halt(409, 'invalid status')
            }
            dto.where('status = ?', status)
        }

        def pager = dto.listPager(pageNum, pageSize)

        pager
    }

    h.get('/one') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        def r = [:]
        def ext = [:]
        r.ext = ext

        ext.clusterId = KafkaManager.CLUSTER_ID
        ext.namespaceId = NamespaceDTO.createIfNotExist(KafkaManager.CLUSTER_ID, 'kafka')

        r.one = one

        if (one.status.canChangeToRunningWhenInstancesRunningOk()) {
            def instance = InMemoryAllContainerManager.instance
            def runningContainerList = instance.getRunningContainerList(KafkaManager.CLUSTER_ID, one.appId)
            if (runningContainerList.size() == one.brokers) {
                one.status = KmServiceDTO.Status.running
                new KmServiceDTO(id: one.id, status: KmServiceDTO.Status.running).update()
            }
        }

        def configTemplateOne = new KmConfigTemplateDTO(id: one.configTemplateId).queryFields('name').one()
        if (configTemplateOne) {
            ext.configTemplateName = configTemplateOne.name
        }

        List<Map> nodes = []
        r.nodes = nodes

        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getContainerList(KafkaManager.CLUSTER_ID, one.appId)
        containerList.each { x ->
            nodes << [brokerIndex: x.instanceIndex(), ip: x.nodeIp, port: one.port, running: x.running()]
        }

        if (one.brokerDetail && one.brokerDetail.brokers) {
            ext.brokerDetail = one.brokerDetail
        }

        r
    }

    h.post('/add') { req, resp ->
        def body = req.bodyAs(Map)

        def name = body.name as String
        def mode = body.mode as String
        def zkConnectString = body.zkConnectString as String
        def kafkaVersion = body.kafkaVersion as String
        def configTemplateId = body.configTemplateId ? (body.configTemplateId as int) : null
        def port = body.port ? (body.port as int) : 9092
        def brokers = body.brokers ? (body.brokers as int) : 1
        def heapMb = body.heapMb ? (body.heapMb as int) : 1024
        def des = body.des as String
        def defaultReplicationFactor = body.defaultReplicationFactor ? (body.defaultReplicationFactor as int) : 1
        def defaultPartitions = body.defaultPartitions ? (body.defaultPartitions as int) : 8
        def nodeTags = body.nodeTags as String[]
        def nodeTagsByBrokerIndex = body.nodeTagsByBrokerIndex as String[]

        assert name && mode && zkConnectString && kafkaVersion

        def existOne = new KmServiceDTO(name: name).queryFields('id').one()
        if (existOne) {
            resp.halt(409, 'name already exists')
        }

        if (!(mode in ['standalone', 'cluster'])) {
            resp.halt(409, 'mode must be standalone or cluster')
        }

        if (!zkConnectString) {
            resp.halt(409, 'zkConnectString is required')
        }

        int minBrokers = mode == 'standalone' ? 1 : 3
        if (brokers < minBrokers) {
            resp.halt(409, 'brokers must be at least ' + minBrokers + ' for ' + mode + ' mode')
        }

        if (brokers > KafkaManager.ONE_CLUSTER_MAX_BROKERS) {
            resp.halt(409, 'max brokers is ' + KafkaManager.ONE_CLUSTER_MAX_BROKERS)
        }

        def zkChroot = '/kafka/' + name

        def namespaceId = NamespaceDTO.createIfNotExist(KafkaManager.CLUSTER_ID, 'kafka')

        def app = new AppDTO()
        app.clusterId = KafkaManager.CLUSTER_ID
        app.namespaceId = namespaceId
        app.name = 'km_' + name
        app.status = AppDTO.Status.auto
        app.updatedDate = new Date()

        def conf = new AppConf()
        conf.containerNumber = brokers
        conf.registryId = KafkaManager.preferRegistryId()
        conf.group = 'bitnami'
        conf.image = 'kafka'
        conf.tag = kafkaVersion
        conf.memReservationMB = heapMb
        conf.memMB = (heapMb * 2).intValue()
        conf.cpuFixed = 1.0
        conf.networkMode = 'host'
        conf.portList << new PortMapping(privatePort: port, publicPort: port)

        conf.targetNodeTagList = []
        if (nodeTags) {
            nodeTags.each {
                conf.targetNodeTagList << it
            }
        }
        conf.targetNodeTagListByInstanceIndex = []
        if (nodeTagsByBrokerIndex) {
            nodeTagsByBrokerIndex.each {
                conf.targetNodeTagListByInstanceIndex << it
            }
        }

        final String dataDir = '/data'
        def serviceDataDir = dataDir + '/kafka_data_app_' + '_${appId}_${instanceIndex}'
        def nodeVolumeId = new NodeVolumeDTO(imageName: 'bitnami/kafka', name: 'for service ' + name, dir: serviceDataDir,
                clusterId: KafkaManager.CLUSTER_ID, des: 'data dir for kafka service').add()
        def dirOne = new DirVolumeMount(
                dir: serviceDataDir, dist: '/kafka/logs', mode: 'rw',
                nodeVolumeId: nodeVolumeId)
        conf.dirVolumeList << dirOne

        def tplName = configTemplateId ? 'server.properties.template.tpl' : 'server.properties.tpl'
        def tplOne = new ImageTplDTO(imageName: 'bitnami/kafka', name: tplName).one()
        if (!tplOne) {
            resp.halt(409, 'KafkaPlugin not initialized — image templates not found')
        }
        def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content, dist: '/opt/bitnami/kafka/config/server.properties')
        mountOne.isParentDirMount = false
        mountOne.paramList << new KVPair<String>('brokerId', '${instanceIndex}')
        mountOne.paramList << new KVPair<String>('port', '' + port)
        mountOne.paramList << new KVPair<String>('dataDir', '/kafka/logs')
        mountOne.paramList << new KVPair<String>('zkConnectString', zkConnectString)
        mountOne.paramList << new KVPair<String>('zkChroot', zkChroot)
        mountOne.paramList << new KVPair<String>('defaultPartitions', '' + defaultPartitions)
        mountOne.paramList << new KVPair<String>('defaultReplicationFactor', '' + defaultReplicationFactor)
        mountOne.paramList << new KVPair<String>('brokerCount', '' + brokers)
        mountOne.paramList << new KVPair<String>('heapMb', heapMb.toString())
        if (configTemplateId) {
            mountOne.paramList << new KVPair<String>('configTemplateId', '' + configTemplateId)
        }
        conf.fileVolumeList << mountOne

        app.conf = conf

        def appId = app.add()
        app.id = appId

        def one = new KmServiceDTO()
        one.name = name
        one.des = des
        one.mode = mode == 'standalone' ? KmServiceDTO.Mode.standalone : KmServiceDTO.Mode.cluster
        one.kafkaVersion = kafkaVersion
        one.configTemplateId = configTemplateId
        one.zkConnectString = zkConnectString
        one.zkChroot = zkChroot
        one.appId = appId
        one.port = port
        one.brokers = brokers
        one.heapMb = heapMb
        one.defaultReplicationFactor = defaultReplicationFactor
        one.defaultPartitions = defaultPartitions
        one.nodeTags = nodeTags
        one.nodeTagsByBrokerIndex = nodeTagsByBrokerIndex
        one.status = KmServiceDTO.Status.creating
        one.extendParams = new ExtendParams()
        one.createdDate = new Date()
        one.updatedDate = new Date()

        def id = one.add()
        one.id = id

        app.extendParams = new ExtendParams()
        app.extendParams.put('kmServiceId', id.toString())
        new AppDTO(id: app.id, extendParams: app.extendParams).update()

        def kmJob = new KmJob()
        kmJob.kmService = one
        kmJob.type = mode == 'standalone' ? KmJobTypes.STANDALONE_CREATE : KmJobTypes.CLUSTER_CREATE
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmServiceId', id.toString())

        kmJob.taskList << new ValidateZookeeperTask(kmJob)
        kmJob.taskList << new RunCreatingAppJobTask(kmJob, app)
        kmJob.taskList << new WaitInstancesRunningTask(kmJob)
        kmJob.taskList << new WaitBrokersRegisteredTask(kmJob)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: id]
    }

    h.post('/delete') { req, resp ->
        def body = req.bodyAs(Map)
        def idStr = body.id as String
        assert idStr
        def id = idStr as int
        assert id > 0

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        KafkaManager.stopContainers(one.appId)

        if (one.zkConnectString && one.zkChroot && km.job.task.ValidateZookeeperTask.isValidChroot(one.zkChroot)) {
            try {
                def connectionString = one.zkConnectString + one.zkChroot
                def client = CuratorPoolHolder.instance.create(connectionString)
                if (client.checkExists().forPath('/') != null) {
                    client.delete().deletingChildrenIfNeeded().forPath('/')
                }
            } catch (Exception e) {
                log.warn('delete zk chroot error: {}', e.message)
            }
        }

        new KmServiceDTO(id: id, status: KmServiceDTO.Status.deleted, updatedDate: new Date()).update()

        [id: id]
    }

    h.post('/scale-up') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int
        def brokerCount = body.brokerCount as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        if (brokerCount <= 0) {
            resp.halt(409, 'brokerCount must be positive')
        }

        def newBrokers = one.brokers + brokerCount
        if (newBrokers > KafkaManager.ONE_CLUSTER_MAX_BROKERS) {
            resp.halt(409, 'max brokers is ' + KafkaManager.ONE_CLUSTER_MAX_BROKERS)
        }

        new KmServiceDTO(id: id, status: KmServiceDTO.Status.scaling_up, updatedDate: new Date()).update()
        log.warn 'scale up service, name: {}, old brokers: {}, adding: {}', one.name, one.brokers, brokerCount

        one.status = KmServiceDTO.Status.scaling_up

        def kmJob = new KmJob()
        kmJob.kmService = one
        kmJob.type = KmJobTypes.BROKER_SCALE_UP
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmServiceId', id.toString())
        kmJob.params.put('brokerCount', brokerCount.toString())

        kmJob.taskList << new AddBrokersTask(kmJob, brokerCount)
        kmJob.taskList << new WaitInstancesRunningTask(kmJob)
        kmJob.taskList << new WaitBrokersRegisteredTask(kmJob)
        kmJob.taskList << new ReassignPartitionsTask(kmJob)
        kmJob.taskList << new WaitReassignmentCompleteTask(kmJob)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: id]
    }

    h.post('/scale-down') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int
        def brokerCount = body.brokerCount as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        if (brokerCount <= 0) {
            resp.halt(409, 'brokerCount must be positive')
        }

        int minBrokers = one.mode == KmServiceDTO.Mode.standalone ? 1 : 3
        def newBrokers = one.brokers - brokerCount
        if (newBrokers < minBrokers) {
            resp.halt(409, 'min brokers is ' + minBrokers + ' for ' + one.mode + ' mode')
        }

        new KmServiceDTO(id: id, status: KmServiceDTO.Status.scaling_down, updatedDate: new Date()).update()
        log.warn 'scale down service, name: {}, old brokers: {}, removing: {}', one.name, one.brokers, brokerCount

        one.status = KmServiceDTO.Status.scaling_down

        int[] removeBrokerIds
        if (one.brokerDetail?.brokers) {
            def sorted = one.brokerDetail.brokers.sort { a, b -> b.brokerId <=> a.brokerId }
            removeBrokerIds = sorted[0..<Math.min(brokerCount, sorted.size())].collect { it.brokerId } as int[]
        } else {
            removeBrokerIds = ((one.brokers - brokerCount)..<one.brokers) as int[]
        }

        def kmJob = new KmJob()
        kmJob.kmService = one
        kmJob.type = KmJobTypes.BROKER_SCALE_DOWN
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmServiceId', id.toString())
        kmJob.params.put('brokerCount', brokerCount.toString())

        kmJob.params.put('removeBrokerIds', removeBrokerIds.toList().join(','))

        kmJob.taskList << new ReassignPartitionsTask(kmJob)
        kmJob.taskList << new WaitReassignmentCompleteTask(kmJob)
        kmJob.taskList << new RemoveBrokersTask(kmJob)
        kmJob.taskList << new DecommissionBrokerTask(kmJob, removeBrokerIds)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: id]
    }

    h.post('/failover') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        if (one.mode == KmServiceDTO.Mode.standalone) {
            resp.halt(409, 'failover not supported for standalone mode')
        }

        def kmJob = new KmJob()
        kmJob.kmService = one
        kmJob.type = KmJobTypes.FAILOVER
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmServiceId', id.toString())

        kmJob.taskList << new FailoverTask(kmJob)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: id]
    }

    h.post('/preferred-replica-election') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        if (one.mode == KmServiceDTO.Mode.standalone) {
            resp.halt(409, 'preferred replica election not supported for standalone mode')
        }

        def kmJob = new KmJob()
        kmJob.kmService = one
        kmJob.type = KmJobTypes.PREFERRED_REPLICA_ELECTION
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmServiceId', id.toString())

        kmJob.taskList << new PreferredReplicaElectionTask(kmJob)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: id]
    }

    h.post('/update-config') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int
        def configOverrides = body.configOverrides as Map

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        if (!configOverrides) {
            resp.halt(409, 'configOverrides is required')
        }

        def brokerDetail = one.brokerDetail
        if (!brokerDetail?.brokers) {
            resp.halt(409, 'no broker detail found')
        }

        def connectionString = one.zkConnectString + one.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {
            brokerDetail.brokers.each { broker ->
                def configPath = '/config/brokers/' + broker.brokerId
                Map existingConfig = [:]
                if (client.checkExists().forPath(configPath) != null) {
                    def existingData = new String(client.getData().forPath(configPath), 'UTF-8')
                    def existingJson = com.alibaba.fastjson.JSON.parseObject(existingData, Map.class)
                    existingConfig = (existingJson['config'] as Map) ?: [:]
                }
                existingConfig.putAll(configOverrides)
                def configData = [version: 1, config: existingConfig]
                def configJson = com.alibaba.fastjson.JSON.toJSONString(configData)

                if (client.checkExists().forPath(configPath) != null) {
                    client.setData().forPath(configPath, configJson.getBytes('UTF-8'))
                } else {
                    client.create().creatingParentsIfNeeded().forPath(configPath, configJson.getBytes('UTF-8'))
                }
            }

            if (!one.configOverrides) {
                one.configOverrides = new ExtendParams()
            }
            configOverrides.each { k, v -> one.configOverrides.put(k as String, v) }
            new KmServiceDTO(id: id, configOverrides: one.configOverrides, updatedDate: new Date()).update()

            [id: id]
        } catch (Exception e) {
            log.error('update config error', e)
            resp.halt(500, 'update config error: ' + e.message)
        }
    }

    h.post('/stop') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        KafkaManager.stopContainers(one.appId)
        new KmServiceDTO(id: id, status: KmServiceDTO.Status.stopped, updatedDate: new Date()).update()

        [id: id]
    }

    h.post('/start') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.stopped) {
            resp.halt(409, 'service must be stopped')
        }

        def app = new AppDTO(id: one.appId).one()
        if (!app) {
            resp.halt(404, 'app not found')
        }

        new AppDTO(id: app.id, status: AppDTO.Status.auto, updatedDate: new Date()).update()
        new KmServiceDTO(id: id, status: KmServiceDTO.Status.creating, updatedDate: new Date()).update()

        [id: id]
    }
}
