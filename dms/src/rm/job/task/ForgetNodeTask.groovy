package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import model.json.ClusterSlotsDetail
import rm.job.RmJob
import rm.job.RmJobTask

@CompileStatic
@Slf4j
class ForgetNodeTask extends RmJobTask {
    final RmServiceDTO rmService

    final ClusterSlotsDetail.Shard oldShard

    final List<Integer> replicaIndexList

    final boolean isReplicasUpdate

    ForgetNodeTask(RmJob rmJob, ClusterSlotsDetail.Shard oldShard, List<Integer> replicaIndexList, boolean isReplicasUpdate) {
        this.rmService = rmJob.rmService
        this.oldShard = oldShard
        this.replicaIndexList = replicaIndexList
        this.isReplicasUpdate = isReplicasUpdate

        this.job = rmJob
        this.step = new JobStep('forget_node', 0)
    }

    @Override
    JobResult doTask() {
        assert rmService

        def firstShard = rmService.clusterSlotsDetail.shards.first()
        def primaryNode = firstShard.primary()
        rmService.connectAndExe(primaryNode) { jedis ->
            for (node in oldShard.nodes) {
                if (node.replicaIndex !in replicaIndexList) {
                    log.warn 'skip forget node: {}, as replica index not in list: {}', node.nodeId(), replicaIndexList
                    return
                }

                def r = jedis.clusterForget(node.nodeId())
                log.warn 'forget node: {}, result: {}', node.nodeId(), r
            }
        }

        if (isReplicasUpdate) {
            // is replicas remove
            rmService.clusterSlotsDetail.shards.nodes.removeIf { node ->
                replicaIndexList.contains(node.replicaIndex)
            }
        } else {
            // is shards remove
            rmService.clusterSlotsDetail.shards.remove(oldShard)
        }

        new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
        log.warn 'update cluster nodes ok'

        JobResult.ok('forget node ok')
    }
}
