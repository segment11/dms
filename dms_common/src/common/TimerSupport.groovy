package common

import groovy.transform.CompileStatic
import io.prometheus.client.Summary

@CompileStatic
class TimerSupport {
    static Summary requestTimeSummary = Summary.build().name('request_time').
            help('cluster server http request time cost').
            labelNames('uri').
            quantile(0.5.doubleValue(), 0.05.doubleValue()).
            quantile(0.95.doubleValue(), 0.01.doubleValue()).register()

    private static ThreadLocal<Summary.Timer> requestTimer = new ThreadLocal<>()

    static void startUriHandle(String uri) {
        if (requestTimer.get() != null) {
            requestTimer.remove()
        }
        def timer = requestTimeSummary.labels(uri).startTimer()
        requestTimer.set(timer)
    }

    static void stopUriHandle() {
        def timer = requestTimer.get()
        if (timer != null) {
            timer.observeDuration()
            requestTimer.remove()
        }
    }
}
