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
class DecommissionBrokerTask extends KmJobTask {
    final KmServiceDTO kmService
    int[] removeBrokerIds

    DecommissionBrokerTask(KmJob kmJob, int[] removeBrokerIds) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.removeBrokerIds = removeBrokerIds
        this.step = new JobStep('decommission_broker', 2)
    }

    @Override
    JobResult doTask() {
        JobResult.ok('decommission broker done, ids: ' + removeBrokerIds)
    }
}
