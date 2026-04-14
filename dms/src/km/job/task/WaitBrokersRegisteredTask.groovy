package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.CuratorPoolHolder
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import model.json.BrokerDetail

@CompileStatic
@Slf4j
class WaitBrokersRegisteredTask extends KmJobTask {
    WaitBrokersRegisteredTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('wait_brokers_registered', 3)
    }

    private static final ObjectMapper mapper = new ObjectMapper()
    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        int maxRetries = 20

        def client = CuratorPoolHolder.instance.create(connectionString)
        try {

            for (int i = 0; i <= maxRetries; i++) {
                def brokersPath = '/brokers/ids'

                if (client.checkExists().forPath(brokersPath) == null) {
                    if (i == maxRetries) {
                        return JobResult.fail('brokers path not found in zookeeper')
                    }
                    Thread.sleep(5 * 1000)
                    continue
                }

                def brokerIds = client.getChildren().forPath(brokersPath)
                if (!brokerIds || brokerIds.size() < kmService.brokers) {
                    if (i == maxRetries) {
                        return JobResult.fail('registered brokers: ' + (brokerIds ? brokerIds.size() : 0) +
                                ', expect: ' + kmService.brokers)
                    }
                    Thread.sleep(5 * 1000)
                    continue
                }

                def brokerDetail = new BrokerDetail()
                brokerIds.eachWithIndex { idStr, int idx ->
                    def brokerId = Integer.parseInt(idStr)
                    def data = client.getData().forPath(brokersPath + '/' + idStr)
                    def json = mapper.readValue(new String(data, 'UTF-8'), Map) as Map<String, Object>
                    def host = json['host'] as String
                    def port = json['port'] as Integer

                    def node = new BrokerDetail.BrokerNode()
                    node.brokerId = brokerId
                    node.brokerIndex = brokerId
                    node.ip = host
                    node.port = port ?: kmService.port
                    brokerDetail.brokers << node
                }

                kmService.brokerDetail = brokerDetail
                new KmServiceDTO(id: kmService.id, brokerDetail: brokerDetail, updatedDate: new Date()).update()

                return JobResult.ok('brokers registered: ' + brokerIds.size())
            }

            JobResult.fail('unexpected state')
        } catch (Exception e) {
            log.error('wait brokers registered error', e)
            return JobResult.fail('wait brokers registered error: ' + e.message)
        }
    }
}
