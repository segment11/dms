package common

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@CompileStatic
@Slf4j
abstract class IntervalJob {
    private ScheduledExecutorService scheduler

    abstract String name()

    abstract void doJob()

    long interval = 10

    long intervalCount = 0

    boolean isDelayRelative = false

    void stop() {
        if (scheduler) {
            scheduler.shutdown()
            log.info 'stop interval job - {}', name()
        }
    }

    void start() {
        def now = new Date()
        int sec = now.seconds
        long delaySeconds = isDelayRelative ? (interval - (sec % interval)) : interval

        // do not be same
        def finalDelaySeconds = new Random().nextInt(delaySeconds.intValue())

        def threadName = name().replaceAll(' ', '_')
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(threadName))
        scheduler.scheduleWithFixedDelay({
            intervalCount++
            try {
                if (intervalCount % 10 == 0) {
                    log.info 'interval count - {} for {}', intervalCount, name()
                }
                doJob()
            } catch (Exception e) {
                log.error('do interval job error - ' + name(), e)
            }
        }, finalDelaySeconds, interval, TimeUnit.SECONDS)
        log.info 'start interval job - {}', name()
    }
}
