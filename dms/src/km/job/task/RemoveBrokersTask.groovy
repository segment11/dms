package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.KafkaManager
import km.job.KmJob
import km.job.KmJobTask
import model.AppDTO
import model.KmServiceDTO
import model.json.BrokerDetail
import server.AgentCaller
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class RemoveBrokersTask extends KmJobTask {
    final KmServiceDTO kmService

    RemoveBrokersTask(KmJob kmJob) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.step = new JobStep('remove_brokers', 5)
    }

    @Override
    JobResult doTask() {
        def params = job.params
        def brokerCount = params?.getInt('brokerCount', 0)
        if (!brokerCount) return JobResult.fail('brokerCount param missing')

        def removeBrokerIdsStr = params?.get('removeBrokerIds') as String
        if (!removeBrokerIdsStr) return JobResult.fail('removeBrokerIds param missing')

        def removeBrokerIdSet = removeBrokerIdsStr.split(',').collect { it as int }.toSet()

        def app = new AppDTO(id: kmService.appId).one()
        if (!app) return JobResult.fail('app not found')

        def brokerDetail = kmService.brokerDetail
        if (!brokerDetail?.brokers) return JobResult.fail('no broker detail')

        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getRunningContainerList(KafkaManager.CLUSTER_ID, kmService.appId)

        containerList.each { x ->
            def idx = x.instanceIndex()
            def matchedBroker = brokerDetail.brokers.find { it.brokerIndex == idx }
            if (matchedBroker && matchedBroker.brokerId in removeBrokerIdSet) {
                if (x.running()) {
                    log.warn 'stopping broker container: brokerId={}, instanceIndex={}, ip={}', matchedBroker.brokerId, idx, x.nodeIp
                    def p = [id: x.id, isRemoveAfterStop: '1', readTimeout: 30 * 1000]
                    AgentCaller.instance.agentScriptExe(KafkaManager.CLUSTER_ID, x.nodeIp, 'container stop', p)
                }
            }
        }

        def oldContainerNumber = app.conf.containerNumber
        def newContainerNumber = oldContainerNumber - brokerCount

        app.conf.containerNumber = newContainerNumber
        new AppDTO(id: app.id, conf: app.conf, updatedDate: new Date()).update()

        new KmServiceDTO(id: kmService.id, brokers: newContainerNumber, updatedDate: new Date()).update()
        kmService.brokers = newContainerNumber

        JobResult.ok('removed ' + brokerCount + ' brokers, remaining: ' + newContainerNumber)
    }
}
