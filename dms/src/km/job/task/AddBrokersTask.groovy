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
class AddBrokersTask extends KmJobTask {
    final KmServiceDTO kmService
    int addCount

    AddBrokersTask(KmJob kmJob, int addCount) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.addCount = addCount
        this.step = new JobStep('add_brokers', 0)
    }

    @Override
    JobResult doTask() {
        JobResult.ok('add brokers done, count: ' + addCount)
    }
}
