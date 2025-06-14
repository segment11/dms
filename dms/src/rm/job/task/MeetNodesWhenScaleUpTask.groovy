package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import model.json.ClusterSlotsDetail
import rm.RedisManager
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class MeetNodesWhenScaleUpTask extends RmJobTask {
    final RmServiceDTO rmService

    // already add to service one cluster slots detail's shards
    final List<ClusterSlotsDetail.Shard> newShardList

    MeetNodesWhenScaleUpTask(RmJob rmJob, List<ClusterSlotsDetail.Shard> newShardList) {
        this.rmService = rmJob.rmService
        this.newShardList = newShardList

        this.job = rmJob
        this.step = new JobStep('meet_nodes_when_scale_up', 0)
    }

    @Override
    JobResult doTask() {
        assert rmService

        List<ContainerInfo> allContainerList = []
        def instance = InMemoryAllContainerManager.instance

        List<Integer> oldShardAppIdList = []

        for (shard in rmService.clusterSlotsDetail.shards) {
            def appId = shard.appId

            def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, appId)

            def runningNumber = runningContainerList.size()
            if (runningNumber != rmService.replicas) {
                return JobResult.fail('running containers number: ' + runningNumber + ', expect: ' + rmService.replicas)
            }

            allContainerList.addAll(runningContainerList)

            if (shard !in newShardList) {
                oldShardAppIdList << appId
            }
        }

        // only cluster meet from primary node, only one is ok, but all primary nodes meet new node be better
        for (shard in rmService.clusterSlotsDetail.shards) {
            def appId = shard.appId
            def containerList = allContainerList.findAll { x -> x.appId() == appId }

            def primaryX = containerList.find { x -> x.instanceIndex() == shard.primary().replicaIndex }
            rmService.connectAndExe(primaryX) { jedis ->
                for (x2 in allContainerList.findAll { xx -> xx.appId() !in oldShardAppIdList && xx.instanceIndex() == 0 }) {
                    def listenPort2 = rmService.listenPort(x2)
                    def r = jedis.clusterMeet(x2.nodeIp, listenPort2)
                    log.warn 'meet node, new node: {}, old node: {}, shard index:{}, app id: {}, result: {}',
                            x2.nodeIp + ':' + listenPort2,
                            primaryX.nodeIp + ':' + rmService.listenPort(primaryX),
                            shard.shardIndex,
                            appId,
                            r
                }
            }
        }

        log.warn('meet nodes when scale up done, wait 5 seconds')
        Thread.sleep(1000 * 5)

        // replica of for new created shards
        for (newShard in newShardList) {
            def appId = newShard.appId
            def containerList = allContainerList.findAll { x -> x.appId() == appId }

            for (x in containerList) {
                // skip primary
                if (x.instanceIndex() == 0) {
                    continue
                }

                def xPrimary = containerList.find { xx -> xx.instanceIndex() == 0 }
                def primaryNodeId = rmService.connectAndExe(xPrimary) { jedis ->
                    jedis.clusterMyId()
                }

                rmService.connectAndExe(x) { jedis ->
                    def r = jedis.clusterReplicate(primaryNodeId)
                    log.warn('replicate node: {}, host: {}, port: {}, result: {}', primaryNodeId, x.nodeIp, rmService.listenPort(x), r)
                }
            }

            containerList.each { x ->
                def node = new ClusterSlotsDetail.Node()
                node.ip = x.nodeIp
                node.port = rmService.listenPort(x)
                node.shardIndex = newShard.shardIndex
                node.replicaIndex = x.instanceIndex()
                // when first created, the first replica is primary
                node.isPrimary = node.replicaIndex == 0
                newShard.nodes << node
            }
        }

        // update after nodes updated
        new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
        log.warn 'update cluster nodes ok'

        log.warn('replica of done')

        return JobResult.ok('meet nodes when scale up ok')
    }
}
