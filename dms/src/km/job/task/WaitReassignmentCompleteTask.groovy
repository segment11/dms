package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO

@CompileStatic
@Slf4j
class WaitReassignmentCompleteTask extends KmJobTask {
    final KmServiceDTO kmService
    int tryCount = 0

    WaitReassignmentCompleteTask(KmJob kmJob) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.step = new JobStep('wait_reassignment_complete', 4)
    }

    @Override
    JobResult doTask() {
        JobResult.ok('wait reassignment complete done')
    }
}
