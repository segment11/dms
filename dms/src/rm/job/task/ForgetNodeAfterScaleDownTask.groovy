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
class ForgetNodeAfterScaleDownTask extends RmJobTask {
    final RmServiceDTO rmService

    final ClusterSlotsDetail.Shard oldShard

    ForgetNodeAfterScaleDownTask(RmJob rmJob, ClusterSlotsDetail.Shard oldShard) {
        this.rmService = rmJob.rmService
        this.oldShard = oldShard

        this.job = rmJob
        this.step = new JobStep('forget_node_after_scale_down', 0)
    }

    @Override
    JobResult doTask() {
        assert rmService

        def firstShard = rmService.clusterSlotsDetail.shards.first()
        def primaryNode = firstShard.primary()
        rmService.connectAndExe(primaryNode) { jedis ->
            for (node in oldShard.nodes) {
                def r = jedis.clusterForget(node.nodeId())
                log.warn 'forget node: {}, result: {}', node.nodeId(), r
            }
        }

        rmService.clusterSlotsDetail.shards.remove(oldShard)

        new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
        log.warn 'update cluster nodes ok'

        JobResult.ok('forget node ok')
    }
}
