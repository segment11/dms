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
class RemoveBrokersTask extends KmJobTask {
    final KmServiceDTO kmService

    RemoveBrokersTask(KmJob kmJob) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.step = new JobStep('remove_brokers', 5)
    }

    @Override
    JobResult doTask() {
        JobResult.ok('remove brokers done')
    }
}
