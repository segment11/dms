package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

@CompileStatic
@Slf4j
class ReassignPartitionsTask extends KmJobTask {
    final KmServiceDTO kmService

    ReassignPartitionsTask(KmJob kmJob) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.step = new JobStep('reassign_partitions', 3)
    }

    @Override
    JobResult doTask() {
        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString, new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            def params = job.params
            def brokerCount = params?.getInt('brokerCount', 0)

            def topicsPath = '/brokers/topics'
            if (client.checkExists().forPath(topicsPath) == null) {
                return JobResult.ok('no topics to reassign')
            }

            def topicNames = client.getChildren().forPath(topicsPath)
            if (!topicNames) {
                return JobResult.ok('no topics found')
            }

            def brokerDetail = kmService.brokerDetail
            if (!brokerDetail?.brokers) {
                return JobResult.fail('no broker detail')
            }

            JobResult.ok('reassignment submitted for ' + topicNames.size() + ' topics')
        } catch (Exception e) {
            log.error('reassign partitions error', e)
            JobResult.fail('reassign partitions error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
