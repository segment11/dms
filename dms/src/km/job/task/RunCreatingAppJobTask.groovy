package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.AppDTO
import model.AppJobDTO
import plugin.BasePlugin
import server.scheduler.processor.CreateProcessor

@CompileStatic
@Slf4j
class RunCreatingAppJobTask extends KmJobTask {
    final AppDTO app

    RunCreatingAppJobTask(KmJob kmJob, AppDTO app) {
        this.app = app
        this.job = kmJob
        this.step = new JobStep('run_creating_app_job', 1)
    }

    @Override
    JobResult doTask() {
        assert app

        def job = BasePlugin.creatingAppJob(app)

        log.warn('start application create job, job id: {}', job.id)
        try {
            new CreateProcessor().process(job, app, [])
            new AppJobDTO(id: job.id, status: AppJobDTO.Status.done, updatedDate: new Date()).update()
            log.warn('start application create job done, job id: {}', job.id)
            return JobResult.ok('start application done')
        } catch (Exception e) {
            log.error('start application create job error', e)
            return JobResult.fail('start application error')
        }
    }
}
