package server.lock

import com.segment.common.Conf
import com.segment.common.job.lock.OneLock
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.locks.InterProcessMutex

import java.util.concurrent.TimeUnit

@CompileStatic
@Slf4j
class RemoteOneLock implements OneLock {
    String lockKey

    boolean debug = Conf.instance.isOn('remoteOneLock.debug')

    int acquireTrySeconds = Conf.instance.getInt('remoteOneLock.acquireTrySeconds', 10)

    CuratorFramework client = CuratorFrameworkClientHolder.instance.client

    InterProcessMutex lock

    @Override
    boolean lock() {
        if (!client) {
            throw new IllegalStateException('curator client not set')
        }
        if (lock) {
            throw new IllegalStateException('lock already set')
        }

        lock = new InterProcessMutex(client, lockKey)
        try {
            lock.acquire(acquireTrySeconds, TimeUnit.SECONDS)
        } catch (Exception e) {
            if (debug) {
                log.error 'try lock error ' + lockKey, e
            }
            return false
        }
    }

    @Override
    void unlock() {
        if (!lock) {
            throw new IllegalStateException('lock not set')
        }
        lock.release()

        if (debug) {
            log.info 'unlock - {}', lockKey
        }
    }

    @Override
    boolean exe(Closure closure) {
        def r = lock()
        if (r) {
            try {
                closure.call()
            } finally {
                unlock()
            }
        } else {
            if (debug) {
                log.info 'get lock fail - {}', lockKey
            }
        }
        r
    }
}
