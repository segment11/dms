package spi

import auth.DefaultLoginService
import auth.LoginService
import groovy.transform.CompileStatic
import server.OneLock
import server.hpa.DefaultScaleStrategy
import server.hpa.ScaleStrategy
import server.scheduler.Guardian

@CompileStatic
class SpiSupport {
    static ClassLoader cl = Guardian.instance.class.classLoader

    static OneLock createLock() {
        def list = ServiceLoader.load(OneLock, cl)
        list[0]
    }

    static LoginService createLoginService() {
        ServiceLoader.load(LoginService, cl).find { it.class.name.startsWith('vendor') } as LoginService
                ?: new DefaultLoginService()
    }

    static ScaleStrategy createScaleStrategy() {
        ServiceLoader.load(ScaleStrategy, cl).find { it.class.name.startsWith('vendor') } as ScaleStrategy
                ?: new DefaultScaleStrategy()
    }
}
