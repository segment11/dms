package ctrl.redis

import model.RmServiceDTO
import model.cluster.SlotRange
import model.job.RmJobDTO
import model.job.RmTaskLogDTO
import model.json.ClusterSlotsDetail
import model.json.PrimaryReplicasDetail
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import rm.job.RmJob
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