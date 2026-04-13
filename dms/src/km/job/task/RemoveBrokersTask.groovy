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

        def app = new AppDTO(id: kmService.appId).one()
        if (!app) return JobResult.fail('app not found')

        def oldContainerNumber = app.conf.containerNumber
        def newContainerNumber = oldContainerNumber - brokerCount

        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getRunningContainerList(KafkaManager.CLUSTER_ID, kmService.appId)
        def toRemove = containerList.findAll { it.instanceIndex() >= newContainerNumber }
        toRemove.each { x ->
            if (x.running()) {
                def p = [id: x.id, isRemoveAfterStop: '1', readTimeout: 30 * 1000]
                AgentCaller.instance.agentScriptExe(KafkaManager.CLUSTER_ID, x.nodeIp, 'container stop', p)
            }
        }

        app.conf.containerNumber = newContainerNumber
        new AppDTO(id: app.id, conf: app.conf, updatedDate: new Date()).update()

        new KmServiceDTO(id: kmService.id, brokers: newContainerNumber, updatedDate: new Date()).update()
        kmService.brokers = newContainerNumber

        JobResult.ok('removed ' + brokerCount + ' brokers, remaining: ' + newContainerNumber)
    }
}
