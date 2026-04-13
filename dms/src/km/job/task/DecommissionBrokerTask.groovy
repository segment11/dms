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
class DecommissionBrokerTask extends KmJobTask {
    final KmServiceDTO kmService
    final int[] removeBrokerIds

    DecommissionBrokerTask(KmJob kmJob, int[] removeBrokerIds) {
        this.kmService = kmJob.kmService
        this.removeBrokerIds = removeBrokerIds
        this.job = kmJob
        this.step = new JobStep('decommission_broker', 2)
    }

    @Override
    JobResult doTask() {
        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString, new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            removeBrokerIds.each { brokerId ->
                def brokerPath = '/brokers/ids/' + brokerId
                if (client.checkExists().forPath(brokerPath) != null) {
                    client.delete().forPath(brokerPath)
                    log.warn 'removed broker id {} from zookeeper', brokerId
                }
            }

            if (kmService.brokerDetail?.brokers) {
                def remaining = kmService.brokerDetail.brokers.findAll { !(it.brokerId in removeBrokerIds) }
                kmService.brokerDetail.brokers = remaining
                new KmServiceDTO(id: kmService.id, brokerDetail: kmService.brokerDetail, updatedDate: new Date()).update()
            }

            JobResult.ok('decommissioned brokers: ' + removeBrokerIds.toList())
        } catch (Exception e) {
            log.error('decommission broker error', e)
            JobResult.fail('decommission broker error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
