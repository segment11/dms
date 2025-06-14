package ctrl.redis

import model.RmServiceDTO
import model.job.RmJobDTO
import model.json.ClusterSlotsDetail
import model.json.PrimaryReplicasDetail
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

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

    h.get('/service/status/update') { req, resp ->
        def idStr = req.param('id')
        def status = req.param('status')
        assert idStr && status
        def id = idStr as int

        def s = RmServiceDTO.Status.valueOf(status)

        def one = new RmServiceDTO(id: id).one()
        assert one

        new RmServiceDTO(id: id, status: s).update()
        [flag: true]
    }

    h.get('/service/primary-replicas-detail/update') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new RmServiceDTO(id: id).one()
        assert one

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
        assert one

        assert one.mode == RmServiceDTO.Mode.cluster

        def runningContainerList = one.runningContainerList()
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
        }

        new RmServiceDTO(id: id, clusterSlotsDetail: one.clusterSlotsDetail,
                shards: one.clusterSlotsDetail.shards.size(),
                replicas: (runningContainerList.size() / one.clusterSlotsDetail.shards.size()).intValue(),
                status: RmServiceDTO.Status.running).update()
        [flag: true]
    }
}