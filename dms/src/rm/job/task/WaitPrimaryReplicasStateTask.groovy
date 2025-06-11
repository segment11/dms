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
class WaitPrimaryReplicasStateTask extends RmJobTask {
    final RmServiceDTO rmService

    WaitPrimaryReplicasStateTask(RmJob rmJob) {
        this.rmService = rmJob.rmService

        this.job = rmJob
        this.step = new JobStep('wait_primary_replicas_state', 0)
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
        }

        jobResult
    }
}
