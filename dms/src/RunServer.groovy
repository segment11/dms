import common.Conf
import common.Const
import common.Utils
import ha.JedisPoolHolder
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.MySQLDialect
import org.segment.web.RouteRefreshLoader
import org.segment.web.RouteServer
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import plugin.PluginManager
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.gateway.ZkClientHolder
import server.scheduler.Guardian
import support.DefaultLocalH2DataSourceCreator
import support.FirstClusterCreate
import support.JobBatchDone
import support.LocalGroovyScriptLoader

import java.sql.Types

def log = LoggerFactory.getLogger(this.getClass())

// project work directory set
def c = Conf.instance.resetWorkDir().load()
log.info c.toString()

if (new File(c.projectPath('/dms_server-1.0.jar')).exists()) {
    c.on('server.runtime.jar')
    log.info 'running in jar'
}

def srcDirPath = c.projectPath('/src')
def resourceDirPath = c.projectPath('/resources')

def agentCaller = AgentCaller.instance
agentCaller.connectTimeout = c.getInt('agent.caller.connectTimeout', 500)
agentCaller.readTimeout = c.getInt('agent.caller.readTimeout', 2000)

// groovy class loader init
def loader = CachedGroovyClassLoader.instance
loader.init(Guardian.instance.class.classLoader, srcDirPath + ':' + resourceDirPath)

// chain filter uri prefix set
ChainHandler.instance.context('/dms')

// DB
def ds = new DefaultLocalH2DataSourceCreator().create()
def d = new D(ds, new MySQLDialect())

boolean isPG = c.getString('db.driver', '').contains('postgre')

// check if need create table first
String queryTableNameSql = isPG ?
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
        : "show tables"
def tableNameList = d.query(queryTableNameSql, String).collect { it.toUpperCase() }
if (!tableNameList.contains('CLUSTER')) {
    new File(c.projectPath('/init_h2.sql')).text.split(';').each {
        try {
            d.exe(it.toString())
        } catch (Exception e) {
            log.error('create table fail', e)
        }
    }
}
// load script for dms agent
LocalGroovyScriptLoader.loadWhenFirstStart()
// done undone jobs
JobBatchDone.doneJobWhenFirstStart()
// create first cluster
FirstClusterCreate.create()

D.classTypeBySqlType[Types.TINYINT] = Integer
D.classTypeBySqlType[Types.SMALLINT] = Integer

// agent send container or node info to this manager
def manager = InMemoryAllContainerManager.instance
manager.init()
manager.start()

PluginManager.instance.loadDemo()

def guardian = Guardian.instance
guardian.interval = c.getInt('guardian.intervalSeconds', 10)
guardian.start()

def cacheSupport = InMemoryCacheSupport.instance
cacheSupport.init()
cacheSupport.start()

def localIp = Utils.localIp()

// create jetty server, load route define interval using cached groovy class loader
def server = RouteServer.instance
server.loader = RouteRefreshLoader.create(loader.gcl).addClasspath(srcDirPath).addClasspath(resourceDirPath).
        addDir(c.projectPath('/src/ctrl')).jarLoad(c.isOn('server.runtime.jar'))
server.webRoot = c.projectPath('/www')
server.start(Const.SERVER_HTTP_LISTEN_PORT, localIp)

// prometheus metrics
DefaultExports.initialize()
def metricsServer = new HTTPServer(localIp, Const.METRICS_HTTP_LISTEN_PORT, true)

Utils.stopWhenConsoleQuit {
    metricsServer.stop()
    server.stop()
    cacheSupport.stop()
    guardian.stop()
    manager.stop()
    ZkClientHolder.instance.close()
    JedisPoolHolder.instance.close()
    Ds.disconnectAll()
}