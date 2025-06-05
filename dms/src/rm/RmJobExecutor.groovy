package rm

import com.segment.common.job.NamedThreadFactory
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

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

    void cleanUp() {
        log.warn('clean up rm job executor')
        executor.shutdown()
    }
}
