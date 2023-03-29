package server.lock

import groovy.transform.CompileStatic

@CompileStatic
interface OneLock {
    void setLockKey(String lockKey)

    boolean lock()

    void unlock()

    boolean exe(Closure closure)
}
