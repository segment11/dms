package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.KafkaManager
import km.job.KmJob
import km.job.KmJobTask
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class WaitInstancesRunningTask extends KmJobTask {
    WaitInstancesRunningTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('wait_instances_running', 2)
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def instance = InMemoryAllContainerManager.instance
        int maxRetries = 10

        for (int i = 0; i <= maxRetries; i++) {
            def runningContainerList = instance.getRunningContainerList(KafkaManager.CLUSTER_ID, kmService.appId)
            if (runningContainerList) {
                def runningNumber = runningContainerList.size()
                log.info 'running containers number: {}, app id: {}', runningNumber, kmService.appId
                if (runningNumber == kmService.brokers) {
                    return JobResult.ok('running containers number: ' + runningNumber)
                }
            }

            if (i == maxRetries) {
                def running = runningContainerList ? runningContainerList.size() : 0
                return JobResult.fail('running containers number: ' + running + ', expect: ' + kmService.brokers)
            }

            Thread.sleep(10 * 1000)
        }

        JobResult.fail('unexpected state')
    }
}
