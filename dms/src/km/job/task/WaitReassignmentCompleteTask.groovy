package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.CuratorPoolHolder
import km.job.KmJobTask
import model.KmServiceDTO

@CompileStatic
@Slf4j
class WaitReassignmentCompleteTask extends KmJobTask {
    final KmServiceDTO kmService

    WaitReassignmentCompleteTask(KmJob kmJob) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.step = new JobStep('wait_reassignment_complete', 4)
    }

    @Override
    JobResult doTask() {
        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {

            int maxRetries = 20
            for (int i = 0; i <= maxRetries; i++) {
                def reassignPath = '/admin/reassign_partitions'
                if (client.checkExists().forPath(reassignPath) == null) {
                    return JobResult.ok('no reassignment in progress')
                }

                def data = client.getData().forPath(reassignPath)
                if (data == null || data.length == 0 || new String(data, 'UTF-8') == '{}') {
                    return JobResult.ok('reassignment complete')
                }

                if (i == maxRetries) {
                    return JobResult.fail('reassignment timeout')
                }

                Thread.sleep(5 * 1000)
            }

            JobResult.ok('reassignment complete')
        } catch (Exception e) {
            log.error('wait reassignment error', e)
            JobResult.fail('wait reassignment error: ' + e.message)
        }
    }
}
