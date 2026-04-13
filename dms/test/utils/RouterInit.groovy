package utils

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.handler.ChainHandler
import server.scheduler.Guardian

@CompileStatic
@Slf4j
class RouterInit {
    static void init(Class clz) {
        def c = Conf.instance
        c.resetWorkDir(true)
        def loader = CachedGroovyClassLoader.instance
        loader.init(Guardian.instance.class.classLoader, c.projectPath('/src'))

        ChainHandler.instance.context('/dms')

        def script = clz.getDeclaredConstructor().newInstance() as Script
        script.run()
    }
}
