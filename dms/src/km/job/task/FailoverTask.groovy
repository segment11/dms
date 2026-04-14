package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.KafkaManager
import km.job.KmJob
import km.CuratorPoolHolder
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import model.json.BrokerDetail
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
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {

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
                x.nodeIp == controllerNode.ip && x.instanceIndex() == controllerNode.brokerIndex
            }

            if (!controllerContainer) {
                controllerContainer = containerList.find { x ->
                    x.nodeIp == controllerNode.ip
                }
            }

            if (!controllerContainer) {
                return JobResult.fail('controller container not found for broker id: ' + controllerBrokerId)
            }

            if (!controllerContainer.running()) {
                return JobResult.ok('controller broker already stopped')
            }

            log.warn 'stopping controller broker: id={}, ip={}', controllerBrokerId, controllerNode.ip
            def stopP = [id: controllerContainer.id, readTimeout: 30 * 1000]
            AgentCaller.instance.agentScriptExe(KafkaManager.CLUSTER_ID, controllerContainer.nodeIp, 'container stop', stopP)

            Thread.sleep(5 * 1000)

            int maxRetries = 15
            boolean newElected = false
            for (int i = 0; i < maxRetries; i++) {
                def exists = client.checkExists().forPath(controllerPath)
                if (exists != null) {
                    def newData = new String(client.getData().forPath(controllerPath), 'UTF-8')
                    def newJson = new DefaultJsonTransformer().read(newData, Map.class)
                    def newBrokerId = newJson['brokerid'] as int
                    if (newBrokerId != controllerBrokerId) {
                        newElected = true
                        break
                    }
                }
                Thread.sleep(3 * 1000)
            }

            if (!newElected) {
                return JobResult.fail('new controller election timeout')
            }

            Thread.sleep(3 * 1000)
            def startP = [id: controllerContainer.id, readTimeout: 30 * 1000]
            AgentCaller.instance.agentScriptExe(KafkaManager.CLUSTER_ID, controllerContainer.nodeIp, 'container start', startP)
            log.warn 'restarted controller broker container, id={}', controllerContainer.id

            Thread.sleep(5 * 1000)

            def newControllerData = client.checkExists().forPath(controllerPath) != null ?
                    new String(client.getData().forPath(controllerPath), 'UTF-8') : null
            if (newControllerData) {
                def newControllerJson = new DefaultJsonTransformer().read(newControllerData, Map.class)
                def newControllerBrokerId = newControllerJson['brokerid'] as int

                brokerDetail.brokers.each { b ->
                    b.isController = (b.brokerId == newControllerBrokerId)
                }
                new KmServiceDTO(id: kmService.id, brokerDetail: brokerDetail, updatedDate: new Date()).update()
            }

            JobResult.ok('failover complete, controller restarted and broker detail updated')
        } catch (Exception e) {
            log.error('failover error', e)
            JobResult.fail('failover error: ' + e.message)
        }
    }
}
