package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import plugin.BasePlugin
import rm.job.RmJob
import rm.job.RmJobTask
import server.scheduler.processor.CreateProcessor

@CompileStatic
@Slf4j
class RunCreatingAppJobTask extends RmJobTask {
    final int shardIndex

    final AppDTO app

    RunCreatingAppJobTask(RmJob rmJob, int shardIndex, AppDTO app) {
        this.shardIndex = shardIndex
        this.app = app

        this.job = rmJob
        this.step = new JobStep('run_creating_app_job_for_shard_' + shardIndex, shardIndex)
    }

    @Override
    JobResult doTask() {
        assert app

        def job = BasePlugin.creatingAppJob(app)

        log.warn('start application create job, job id: {}', job.id)
        try {
            new CreateProcessor().process(job, app, [])
            new AppJobDTO(id: job.id, status: AppJobDTO.Status.done.val, updatedDate: new Date()).update()
            log.warn('start application create job done, job id: {}', job.id)
            return JobResult.ok('start application done')
        } catch (Exception e) {
            log.error('start application create job error', e)
            return JobResult.fail('start application error')
        }
    }
}
