package common

import groovy.transform.CompileStatic

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class NamedThreadFactory implements ThreadFactory {
    @Override
    Thread newThread(Runnable runnable) {
        def t = new Thread(group, runnable, namePrefix + threadNumber.getAndIncrement(), 0)
        if (t.isDaemon()) {
            t.daemon = false
        }
        if (t.priority != Thread.NORM_PRIORITY) {
            t.priority = Thread.NORM_PRIORITY
        }
        t
    }

    NamedThreadFactory(String namePrefix) {
        def s = System.getSecurityManager()
        group = (s != null) ? s.threadGroup : Thread.currentThread().threadGroup
        this.namePrefix = namePrefix + '-' + poolNumber.getAndIncrement() + '-thread-'
    }

    private static final AtomicInteger poolNumber = new AtomicInteger(1)
    private static final AtomicInteger threadNumber = new AtomicInteger(1)
    private final ThreadGroup group
    private final String namePrefix
}
