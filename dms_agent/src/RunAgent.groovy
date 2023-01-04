import agent.Agent
import agent.script.ScriptHolder
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.jaxrs.JerseyDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import common.Conf
import common.Const
import common.Utils
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.segment.web.RouteRefreshLoader
import org.segment.web.RouteServer
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())

// project work directory set
def c = Conf.instance.resetWorkDir().load()
log.info c.toString()
def srcDirPath = c.projectPath('/src')

if (!Utils.isPortListenAvailable(Const.AGENT_HTTP_LISTEN_PORT, Utils.localIp())) {
    log.info 'dms agent already running'
    return
}

// agent
def agent = Agent.instance
agent.init()

c.params.findAll { it.key.startsWith('docker.') }.each { k, v ->
    System.setProperty(k['docker.'.length()..1], v)
}

if (c.isOn('collectDockerDaemon')) {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
    DockerHttpClient httpClient = new JerseyDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .maxTotalConnections(5)
            .connectTimeout(200)
            .build()
    agent.docker = DockerClientImpl.getInstance(config, httpClient)
}
agent.start()

// script holder
def scriptHolder = ScriptHolder.instance
scriptHolder.doJob()
scriptHolder.start()

// groovy class loader init
def loader = CachedGroovyClassLoader.instance
loader.init(agent.class.classLoader, srcDirPath)

// chain filter uri prefix set
ChainHandler.instance.context('/dmc')

def localIp = Utils.localIp()

// create jetty server, load route define interval using cached groovy class loader
def server = RouteServer.instance
server.loader = RouteRefreshLoader.create(loader.gcl).addClasspath(srcDirPath).
        addDir(c.projectPath('/src/agent/ctrl')).jarLoad(c.isOn('server.runtime.jar'))
server.isStartMetricServer = false
server.start(Const.AGENT_HTTP_LISTEN_PORT, localIp)

// prometheus metrics
DefaultExports.initialize()
def metricsServer = new HTTPServer(localIp, Const.METRICS_AGENT_HTTP_LISTEN_PORT, true)

Utils.stopWhenConsoleQuit {
    metricsServer.stop()
    scriptHolder.stop()
    agent.stop()
    server.stop()
}