import auth.User
import com.segment.common.Conf
import com.segment.common.Utils
import com.segment.common.job.leader.LeaderFlagHolder
import common.Const
import ha.JedisPoolHolder
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.dialect.MySQLDialect
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
import server.lock.CuratorFrameworkClientHolder
import server.scheduler.Guardian
import server.scheduler.processor.CreateProcessor
import support.DefaultLocalH2DataSourceCreator
import support.FirstClusterCreate
import support.JobBatchDone
import support.LocalGroovyScriptLoader

import java.sql.Types

def log = LoggerFactory.getLogger(this.getClass())

// project work directory set
def c = Conf.instance.resetWorkDir().load()
log.info c.toString()

def projectDir = new File(c.projectPath('/'))
projectDir.eachFile {
    if (it.name.endsWith('.jar') && it.name.startsWith('dms_server')) {
        c.on('server.runtime.jar')
        log.info 'running in jar'
    }
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
def ds = DefaultLocalH2DataSourceCreator.create()
def d = new D(ds, new MySQLDialect())

boolean isPG = c.getString('db.driver', '').contains('postgre')

// check if need create table first
String queryTableNameSql = isPG ?
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
        : "SELECT table_name FROM information_schema.tables"
def tableNameList = d.query(queryTableNameSql, String).collect { it.toUpperCase() }
if (!tableNameList.contains('CLUSTER')) {
    new File(c.projectPath('/init_h2.sql')).text.split(';').each {
        def ddl = it.trim()
        if (!ddl) {
            return
        }
        try {
            d.exe(ddl)
        } catch (Exception e) {
            log.error('execute ddl error, ddl: ' + ddl, e)
        }
    }
}

// load script for dms agent
LocalGroovyScriptLoader.loadWhenFirstStart(c.isOn('agent.isScriptUpdateForce'))
// done undone jobs
JobBatchDone.doneJobWhenFirstStart()
// create first cluster
FirstClusterCreate.create()

D.classTypeBySqlType[Types.TINYINT] = Integer
D.classTypeBySqlType[Types.SMALLINT] = Integer

def leaderFlagHolder = LeaderFlagHolder.instance
if (c.get('leader.etcdAddr')) {
    leaderFlagHolder.init()
    leaderFlagHolder.doJob()
    leaderFlagHolder.start()
} else {
    // if not etcd addr given, default is leader
    leaderFlagHolder.isLeader = true
}

// agent send container or node info to this manager
def containerManager = InMemoryAllContainerManager.instance
containerManager.init()
containerManager.start()

def curatorClientHolder = CuratorFrameworkClientHolder.instance
curatorClientHolder.init()

PluginManager.instance.loadDemo()

def guardian = Guardian.instance
guardian.interval = c.getInt('guardian.interval.seconds', 5)
guardian.start()

def cacheSupport = InMemoryCacheSupport.instance
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
log.info 'metrics server started - {}', Const.METRICS_HTTP_LISTEN_PORT

def stopCl = {
    metricsServer.close()
    server.stop()
    cacheSupport.stop()
    guardian.stop()
    CreateProcessor.executorService.shutdown()
    curatorClientHolder.close()
    containerManager.stop()
    leaderFlagHolder.stop()
    ZkClientHolder.instance.close()
    JedisPoolHolder.instance.close()
    Ds.disconnectAll()
}

Runtime.addShutdownHook stopCl

ChainHandler.instance.get('/manage/stop/all') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    Thread.start {
        Thread.sleep(1000)
        stopCl.call()
    }
    resp.end 'stopped'
}