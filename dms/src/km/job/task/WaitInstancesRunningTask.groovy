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

    int tryCount = 0

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def instance = InMemoryAllContainerManager.instance
        def runningContainerList = instance.getRunningContainerList(KafkaManager.CLUSTER_ID, kmService.appId)
        if (!runningContainerList) {
            Thread.sleep(10 * 1000)
            tryCount++

            if (tryCount > 10) {
                return JobResult.fail('no containers found for app id: ' + kmService.appId)
            } else {
                return doTask()
            }
        }

        def runningNumber = runningContainerList.size()
        log.info 'running containers number: {}, app id: {}', runningNumber, kmService.appId
        if (runningNumber == kmService.brokers) {
            return JobResult.ok('running containers number: ' + runningNumber)
        }

        Thread.sleep(10 * 1000)
        tryCount++

        if (tryCount > 10) {
            return JobResult.fail('running containers number: ' + runningNumber + ', expect: ' + kmService.brokers)
        } else {
            return doTask()
        }
    }
}
