package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import rm.job.RmJob
import rm.job.RmJobTask

@CompileStatic
@Slf4j
class WaitClusterStateAfterScaleTask extends RmJobTask {
    final RmServiceDTO rmService

    WaitClusterStateAfterScaleTask(RmJob rmJob) {
        this.rmService = rmJob.rmService

        this.job = rmJob
        this.step = new JobStep('wait_cluster_state_after_scale', 0)
    }

    int tryCount = 0

    @Override
    JobResult doTask() {
        assert rmService

        def jobResult = rmService.checkNodes()
        if (!jobResult.isOk) {
            Thread.sleep(1000 * 5)
            tryCount++

            if (tryCount > 10) {
                return jobResult
            } else {
                return doTask()
            }
        } else {
            // update status to running
            rmService.updateStatus(RmServiceDTO.Status.running, 'wait_cluster_state_after_scale, check cluster nodes and slots ok')
        }

        jobResult
    }
}
