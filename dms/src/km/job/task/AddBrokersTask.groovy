package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.AppDTO
import model.AppJobDTO
import model.KmServiceDTO
import plugin.BasePlugin
import server.scheduler.processor.CreateProcessor

@CompileStatic
@Slf4j
class AddBrokersTask extends KmJobTask {
    final KmServiceDTO kmService
    int addCount

    AddBrokersTask(KmJob kmJob, int addCount) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.addCount = addCount
        this.step = new JobStep('add_brokers', 0)
    }

    @Override
    JobResult doTask() {
        def app = new AppDTO(id: kmService.appId).one()
        if (!app) return JobResult.fail('app not found')

        def conf = app.conf
        def oldContainerNumber = conf.containerNumber
        def newContainerNumber = oldContainerNumber + addCount
        conf.containerNumber = newContainerNumber

        def newApp = new AppDTO()
        newApp.id = app.id
        newApp.clusterId = app.clusterId
        newApp.namespaceId = app.namespaceId
        newApp.name = app.name
        newApp.status = AppDTO.Status.auto
        newApp.conf = conf
        newApp.updatedDate = new Date()

        new AppDTO(id: app.id, conf: conf, updatedDate: new Date()).update()

        def job = BasePlugin.creatingAppJob(newApp)
        try {
            new CreateProcessor().process(job, newApp, [])
            new AppJobDTO(id: job.id, status: AppJobDTO.Status.done, updatedDate: new Date()).update()

            new KmServiceDTO(id: kmService.id, brokers: newContainerNumber, updatedDate: new Date()).update()
            kmService.brokers = newContainerNumber

            JobResult.ok('added ' + addCount + ' brokers, total: ' + newContainerNumber)
        } catch (Exception e) {
            log.error('add brokers error', e)
            JobResult.fail('add brokers error: ' + e.message)
        }
    }
}
