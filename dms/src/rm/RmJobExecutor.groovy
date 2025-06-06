package rm

import com.segment.common.job.NamedThreadFactory
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import server.scheduler.processor.CreateProcessor

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@CompileStatic
@Singleton
@Slf4j
class RmJobExecutor {
    private final ExecutorService executor = Executors.newFixedThreadPool(10, new NamedThreadFactory('rm-job-'))

    void execute(Runnable runnable) {
        executor.execute(runnable)
    }

    void runCreatingAppJob(AppDTO app) {
        List<Integer> needRunInstanceIndexList = []
        (0..<app.conf.containerNumber).each {
            needRunInstanceIndexList << it
        }

        def job = new AppJobDTO(
                appId: app.id,
                failNum: 0,
                status: AppJobDTO.Status.created.val,
                jobType: AppJobDTO.JobType.create.val,
                createdDate: new Date(),
                updatedDate: new Date()).
                needRunInstanceIndexList(needRunInstanceIndexList)
        int jobId = job.add()
        job.id = jobId

        log.warn('start application create job, job id: {}', jobId)
        execute {
            try {
                new CreateProcessor().process(job, app, [])
                new AppJobDTO(id: job.id, status: AppJobDTO.Status.done.val, updatedDate: new Date()).update()
                log.warn('start application create job done, job id: {}', jobId)
            } catch (Exception e) {
                log.error('start application create job error', e)
            }
        }
    }

    void cleanUp() {
        log.warn('clean up rm job executor')
        executor.shutdown()
    }
}
