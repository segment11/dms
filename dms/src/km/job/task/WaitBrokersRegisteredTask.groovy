package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import model.json.BrokerDetail
import org.apache.curator.framework.CuratorFrameworkFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.curator.retry.ExponentialBackoffRetry

@CompileStatic
@Slf4j
class WaitBrokersRegisteredTask extends KmJobTask {
    WaitBrokersRegisteredTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('wait_brokers_registered', 3)
    }

    int tryCount = 0

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString,
                new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            def brokersPath = '/brokers/ids'
            if (client.checkExists().forPath(brokersPath) == null) {
                Thread.sleep(5 * 1000)
                tryCount++

                if (tryCount > 20) {
                    return JobResult.fail('brokers path not found in zookeeper')
                } else {
                    return doTask()
                }
            }

            def brokerIds = client.getChildren().forPath(brokersPath)
            if (!brokerIds || brokerIds.size() < kmService.brokers) {
                Thread.sleep(5 * 1000)
                tryCount++

                if (tryCount > 20) {
                    return JobResult.fail('registered brokers: ' + (brokerIds ? brokerIds.size() : 0) +
                            ', expect: ' + kmService.brokers)
                } else {
                    return doTask()
                }
            }

            def brokerDetail = new BrokerDetail()
            brokerIds.each { idStr ->
                def brokerId = Integer.parseInt(idStr)
                def data = client.getData().forPath(brokersPath + '/' + idStr)
                def mapper = new ObjectMapper()
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                def json = mapper.readValue(new String(data, 'UTF-8'), Map) as Map<String, Object>
                def host = json['host'] as String
                def port = json['port'] as Integer

                def node = new BrokerDetail.BrokerNode()
                node.brokerId = brokerId
                node.ip = host
                node.port = port ?: kmService.port
                brokerDetail.brokers << node
            }

            kmService.brokerDetail = brokerDetail
            new KmServiceDTO(id: kmService.id, brokerDetail: brokerDetail, updatedDate: new Date()).update()

            return JobResult.ok('brokers registered: ' + brokerIds.size())
        } catch (Exception e) {
            log.error('wait brokers registered error', e)
            return JobResult.fail('wait brokers registered error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
