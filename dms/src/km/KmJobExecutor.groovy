package km

import com.segment.common.job.NamedThreadFactory
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import plugin.BasePlugin
import server.scheduler.processor.CreateProcessor

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@CompileStatic
@Singleton
@Slf4j
class KmJobExecutor {
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.runtime.availableProcessors(),
            new NamedThreadFactory('km-job-'))

    void execute(Runnable runnable) {
        executor.execute(runnable)
    }

    int runCreatingAppJob(AppDTO app) {
        def job = BasePlugin.creatingAppJob(app)

        log.warn('start application create job, job id: {}', job.id)
        execute {
            try {
                new CreateProcessor().process(job, app, [])
                new AppJobDTO(id: job.id, status: AppJobDTO.Status.done, updatedDate: new Date()).update()
                log.warn('start application create job done, job id: {}', job.id)
            } catch (Exception e) {
                log.error('start application create job error', e)
            }
        }

        job.id
    }

    void cleanUp() {
        log.warn('clean up km job executor')
        executor.shutdown()
    }
}
