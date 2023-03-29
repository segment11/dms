package server.lock

import common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@CompileStatic
@Slf4j
class LocalOneLock implements OneLock {
    String lockKey

    boolean debug = Conf.instance.isOn('localOneLock.debug')

    // will not oom, only app id, user name, frontend id, may 100-1000 lock key
    static ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>()

    ReentrantLock localLock

    boolean lock() {
        localLock = locks[lockKey]
        if (localLock == null) {
            def newLock = new ReentrantLock()
            def oldLock = locks.putIfAbsent(lockKey, newLock)
            if (oldLock) {
                if (debug) {
                    log.info 'use old lock - {}', lockKey
                }
                localLock = oldLock
            } else {
                if (debug) {
                    log.info 'create new lock - {}', lockKey
                }
                localLock = newLock
            }
        } else {
            if (debug) {
                log.info 'use old lock - {}', lockKey
            }
        }
        try {
            localLock.tryLock()
        } catch (Exception e) {
            if (debug) {
                log.error('try lock error ' + lockKey, e)
            }
            false
        }
    }

    void unlock() {
        localLock.unlock()
        if (debug) {
            log.info 'unlock - {}', lockKey
        }
    }

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
