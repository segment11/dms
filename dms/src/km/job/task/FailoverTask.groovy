package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.KafkaManager
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import model.json.BrokerDetail
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.segment.d.json.DefaultJsonTransformer
import server.AgentCaller
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class FailoverTask extends KmJobTask {
    final KmServiceDTO kmService

    FailoverTask(KmJob kmJob) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.step = new JobStep('failover', 0)
    }

    @Override
    JobResult doTask() {
        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString, new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            def controllerPath = '/controller'
            if (client.checkExists().forPath(controllerPath) == null) {
                return JobResult.fail('no controller broker found')
            }

            def data = new String(client.getData().forPath(controllerPath), 'UTF-8')
            def json = new DefaultJsonTransformer().read(data, Map.class)
            def controllerBrokerId = json['brokerid'] as int

            def brokerDetail = kmService.brokerDetail
            if (!brokerDetail?.brokers) {
                return JobResult.fail('no broker detail')
            }

            def controllerNode = brokerDetail.findByBrokerId(controllerBrokerId)
            if (!controllerNode) {
                return JobResult.fail('controller broker not found in broker detail: ' + controllerBrokerId)
            }

            def instance = InMemoryAllContainerManager.instance
            def containerList = instance.getContainerList(KafkaManager.CLUSTER_ID, kmService.appId)
            def controllerContainer = containerList.find { x ->
                def containerIp = x.nodeIp
                controllerNode.ip == containerIp
            }

            if (controllerContainer?.running()) {
                log.warn 'stopping controller broker: id={}, ip={}', controllerBrokerId, controllerNode.ip
                def p = [id: controllerContainer.id, readTimeout: 30 * 1000]
                AgentCaller.instance.agentScriptExe(KafkaManager.CLUSTER_ID, controllerContainer.nodeIp, 'container stop', p)

                Thread.sleep(5 * 1000)

                int maxRetries = 10
                for (int i = 0; i < maxRetries; i++) {
                    def newData = client.checkExists().forPath(controllerPath) != null ?
                            new String(client.getData().forPath(controllerPath), 'UTF-8') : ''
                    if (!newData || newData != data) {
                        return JobResult.ok('failover complete, new controller elected')
                    }
                    Thread.sleep(3 * 1000)
                }
            }

            JobResult.ok('failover skipped or completed')
        } catch (Exception e) {
            log.error('failover error', e)
            JobResult.fail('failover error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
