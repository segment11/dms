package server.scheduler

import com.segment.common.job.NamedThreadFactory
import groovy.transform.CompileStatic

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@CompileStatic
class OneThreadExecutor extends ThreadPoolExecutor {
    OneThreadExecutor(String threadName) {
        super(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue(1), new NamedThreadFactory(threadName), new AbortPolicy())
    }
}
