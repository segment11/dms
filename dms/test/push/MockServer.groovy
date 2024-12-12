package push

import com.segment.common.Conf
import org.segment.web.RouteRefreshLoader
import org.segment.web.RouteServer
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())

// project work directory set
String[] x = super.binding.getProperty('args') as String[]
def c = Conf.instance.resetWorkDir(true).load().loadArgs(x)
log.info c.toString()

// chain filter uri prefix set
ChainHandler.instance.context('/dms')

def srcDirPath = c.projectPath('/src')

// groovy class loader init
def loader = CachedGroovyClassLoader.instance
loader.init(c.class.classLoader, srcDirPath)

def server = RouteServer.instance
server.loader = RouteRefreshLoader.create(loader.gcl).addClasspath(srcDirPath).
        addDir(c.projectPath('/src/ctrl/push')).jarLoad(c.isOn('server.runtime.jar'))

server.start(5040, '0.0.0.0')

def stopCl = {
    server.stop()
}
Runtime.addShutdownHook stopCl
