package ctrl.redis

import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import model.NamespaceDTO
import model.RmServiceDTO
import model.cluster.SlotRange
import model.job.RmJobDTO
import model.job.RmTaskLogDTO
import model.json.ClusterSlotsDetail
import model.json.PrimaryReplicasDetail
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import rm.BackupManager
import rm.RedisManager
import rm.RmJobExecutor
import rm.job.RmJob
import rm.job.RmJobTypes
import rm.job.task.CopyFromTask
import rm.job.task.MeetNodesWhenScaleUpTask
import rm.job.task.MigrateSlotsTask

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis/job') {
    h.get('/list') { req, resp ->
        def rmServiceIdStr = req.param('rmServiceId')
        assert rmServiceIdStr
        def rmServiceId = rmServiceIdStr as int

        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def dto = new RmJobDTO(busiId: rmServiceId)
        def pager = dto.listPager(pageNum, pageSize)

        pager
    }

    h.get('/task/list') { req, resp ->
        def jobIdStr = req.param('jobId')
        assert jobIdStr
        def jobId = jobIdStr as int

        def list = new RmTaskLogDTO(jobId: jobId).list()
        [list: list]
    }

    h.post('/service/copy-from') { req, resp ->
        Map map = req.bodyAs()
        def fromId = map.fromId as int
        def id = map.id as int
        def type = map.type as String

        if (id == fromId) {
            resp.halt(409, 'service cannot be itself')
        }

        // redis-shake only support sync or scan
        if (type !in ['sync', 'scan']) {
            resp.halt(409, 'type must be sync or scan')
        }

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        def fromOne = new RmServiceDTO(id: fromId).one()
        if (!fromOne) {
            resp.halt(404, 'copy from service not found')
        }

        def checkResult = one.checkNodes()
        if (!checkResult.isOk) {
            resp.halt(409, checkResult.message)
        }

        def checkResult2 = fromOne.checkNodes()
        if (!checkResult2.isOk) {
            resp.halt(409, checkResult2.message)
        }

        def runningContainerList = one.runningContainerList()
        def fromRunningContainerList = fromOne.runningContainerList()
        if (!runningContainerList || !fromRunningContainerList) {
            resp.halt(409, 'service not running')
        }

        // redis shake params
        String targetType
        String targetAddress
//        String targetUsername
        def targetPassword = one.pass
        if (one.mode == RmServiceDTO.Mode.cluster) {
            targetType = 'cluster'
            def node = one.clusterSlotsDetail.shards.find { shard ->
                shard.shardIndex == 0
            }.nodes.find { n ->
                n.isPrimary
            }
            targetAddress = node.ip + ':' + node.port
        } else if (one.mode == RmServiceDTO.Mode.sentinel) {
            targetType = 'standalone'
            def node = one.primaryReplicasDetail.nodes.find { n ->
                n.isPrimary
            }
            targetAddress = node.ip + ':' + node.port
        } else {
            targetType = 'standalone'
            def x = runningContainerList[0]
            targetAddress = x.nodeIp + ':' + one.port
        }

        // when cluster, need copy from multi-shards
        List<Tuple3<String, String, String>> srcParamsGroupList = []
        if (fromOne.mode == RmServiceDTO.Mode.cluster) {
            fromOne.clusterSlotsDetail.shards.each { shard ->
                def node = shard.nodes.find { n ->
                    n.isPrimary
                }
                srcParamsGroupList << new Tuple3(
                        fromOne.id + 'shard' + shard.shardIndex + '->' + one.id,
                        node.ip + ':' + node.port,
                        fromOne.pass,
                )
            }
        } else if (fromOne.mode == RmServiceDTO.Mode.sentinel) {
            def node = fromOne.primaryReplicasDetail.nodes.find { n ->
                n.isPrimary
            }
            srcParamsGroupList << new Tuple3(
                    fromOne.id + 'shard->' + one.id,
                    node.ip + ':' + node.port,
                    fromOne.pass,
            )
        } else {
            def fromX = fromRunningContainerList[0]
            srcParamsGroupList << new Tuple3(
                    fromOne.id + 'shard->' + one.id,
                    fromX.nodeIp + ':' + fromOne.port,
                    fromOne.pass,
            )
        }

        NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'redis-shake')

        def rmJob = new RmJob()
        rmJob.rmService = one
        rmJob.type = RmJobTypes.COPY_FROM
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmServiceId', id.toString())
        rmJob.params.put('fromServiceId', fromId.toString())

        for (srcParamsGroup in srcParamsGroupList) {
            rmJob.taskList << new CopyFromTask(rmJob, srcParamsGroup.v1, type,
                    srcParamsGroup.v2, srcParamsGroup.v3, targetType, targetAddress, targetPassword)
        }

        rmJob.createdDate = new Date()
        rmJob.updatedDate = new Date()
        rmJob.save()

        RmJobExecutor.instance.execute {
            rmJob.run()
        }

        [flag: true]
    }

    // for debug
    h.get('/service/status/update') { req, resp ->
        def idStr = req.param('id')
        def status = req.param('status')
        assert idStr && status
        def id = idStr as int

        def s = RmServiceDTO.Status.valueOf(status)

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        new RmServiceDTO(id: id, status: s).update()
        [flag: true]
    }

    h.get('/service/primary-replicas-detail/update') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        assert one.mode == RmServiceDTO.Mode.sentinel

        def runningContainerList = one.runningContainerList()
        one.primaryReplicasDetail.nodes.clear()
        runningContainerList.each { x ->
            def node = new PrimaryReplicasDetail.Node()
            node.ip = x.nodeIp
            node.port = one.listenPort(x)
            node.replicaIndex = x.instanceIndex()
            node.isPrimary = 'master' == one.connectAndExe(x) { jedis ->
                jedis.role()[0] as String
            }

            one.primaryReplicasDetail.nodes << node
        }

        new RmServiceDTO(id: id, primaryReplicasDetail: one.primaryReplicasDetail,
                replicas: runningContainerList.size(),
                status: RmServiceDTO.Status.running).update()
        [flag: true]
    }

    h.get('/service/cluster-slots-detail/update') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        assert one.mode == RmServiceDTO.Mode.cluster

        def runningContainerList = one.runningContainerList()
        Set<Integer> runningAppIdList = []

        one.clusterSlotsDetail.shards.each { shard -> shard.nodes.clear() }
        runningContainerList.each { x ->
            def appId = x.appId()
            def shard = one.clusterSlotsDetail.shards.find { shard -> shard.appId == appId }

            def node = new ClusterSlotsDetail.Node()
            node.ip = x.nodeIp
            node.port = one.listenPort(x)
            node.replicaIndex = x.instanceIndex()
            node.isPrimary = 'master' == one.connectAndExe(x) { jedis ->
                jedis.role()[0] as String
            }

            shard.nodes << node

            runningAppIdList << appId
        }

        one.clusterSlotsDetail.shards.removeIf { shard -> shard.appId !in runningAppIdList }

        new RmServiceDTO(id: id, clusterSlotsDetail: one.clusterSlotsDetail,
                shards: one.clusterSlotsDetail.shards.size(),
                replicas: (runningContainerList.size() / one.clusterSlotsDetail.shards.size()).intValue(),
                status: RmServiceDTO.Status.running).update()
        [flag: true]
    }

    h.get('/service/cluster-scale-up/redo-meet-nodes') { req, resp ->
        def idStr = req.param('id')
        def shardIndexes = req.param('shardIndexes')
        assert idStr && shardIndexes
        def id = idStr as int

        def shardIndexList = shardIndexes.split(',').collect { it as int }

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        assert one.mode == RmServiceDTO.Mode.cluster

        def newShardList = one.clusterSlotsDetail.shards.findAll { shard ->
            shard.shardIndex in shardIndexList
        }
        def task = new MeetNodesWhenScaleUpTask(new RmJob(rmService: one), newShardList)
        task.doTask()

        [flag: true]
    }

    h.get('/service/backup-log/remove-old-list') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        BackupManager.instance.removeOldBackupLogs(one.id, one.backupPolicy)
        [flag: true]
    }

    h.get('/service/cluster-scale-up/redo-migrate-slots') { req, resp ->
        def idStr = req.param('id')
        def fromShardIndexStr = req.param('fromShardIndex')
        def toShardIndexStr = req.param('toShardIndex')
        assert idStr && fromShardIndexStr && toShardIndexStr
        def id = idStr as int
        def fromShardIndex = fromShardIndexStr as int
        def toShardIndex = toShardIndexStr as int

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        assert one.mode == RmServiceDTO.Mode.cluster

        def fromShard = one.clusterSlotsDetail.shards.find { shard ->
            shard.shardIndex == fromShardIndex
        }
        def toShard = one.clusterSlotsDetail.shards.find { shard ->
            shard.shardIndex == toShardIndex
        }

        def slotRange = fromShard.multiSlotRange.list[0]
        def halfSlotNumber = (slotRange.totalNumber() / 2).intValue()

        def halfSlotRange = new SlotRange()
        halfSlotRange.begin = slotRange.begin + halfSlotNumber
        halfSlotRange.end = slotRange.end

        def task = new MigrateSlotsTask(new RmJob(rmService: one), fromShard, toShard, halfSlotRange)
        task.doTask()

        [flag: true]
    }
}