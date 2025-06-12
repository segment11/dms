package ctrl.redis

import com.segment.common.Conf
import model.AppDTO
import model.NamespaceDTO
import model.json.AppConf
import model.json.KVPair
import model.json.PortMapping
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import plugin.BasePlugin
import plugin.demo2.*
import rm.RedisManager
import rm.RmJobExecutor
import server.InMemoryAllContainerManager
import transfer.ContainerInfo

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis/metric') {
    h.get('/init-exporters') { req, resp ->
        def targetNodeIp = req.param('targetNodeIp')
        assert targetNodeIp

        def instance = InMemoryAllContainerManager.instance
        def nodeInfo = instance.getNodeInfo(targetNodeIp)
        if (!nodeInfo) {
            resp.halt(404, 'node not exists')
        }
        nodeInfo.checkIfOk(new Date())
        if (!nodeInfo.isOk) {
            resp.halt(409, 'node heart beat not ok')
        }

        def namespaceIdMetric = NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'metric')

        // prometheus application
        final String prometheusAppName = 'prometheus'
        // check if already exists
        def existsOne = new AppDTO(clusterId: RedisManager.CLUSTER_ID, name: prometheusAppName).one()
        if (existsOne) {
            log.warn('prometheus already exists {}', prometheusAppName)
            resp.halt(409, 'prometheus already exists')
        }

        List<String> targetNodeIpList = [targetNodeIp]
        def prometheusApp = BasePlugin.tplApp(RedisManager.CLUSTER_ID, namespaceIdMetric, targetNodeIpList) { conf ->
            conf.memMB = 1024
            conf.cpuFixed = 2.0d

            conf.registryId = RedisManager.preferRegistryId()

            // 9090 conflict, use 19090, for local test
            conf.networkMode = 'bridge'
            conf.portList.clear()
            conf.portList << new PortMapping(privatePort: 9090, publicPort: 19090)
        }

        def prometheusPlugin = new PrometheusPlugin()
        prometheusPlugin.configTplName = prometheusPlugin.tplNameRedisExporter
        prometheusPlugin.nodeDir = RedisManager.dataDir() + '/prometheus'
        prometheusPlugin.demoApp(prometheusApp)
        prometheusApp.name = prometheusAppName
        // config file need reload
        prometheusApp.status = AppDTO.Status.auto.val
        def prometheusAppId = InitToolPlugin.addAppIfNotExists(prometheusApp)
        log.warn 'created prometheus app {}', prometheusAppId
        def prometheusJobId = RmJobExecutor.instance.runCreatingAppJob(prometheusApp)
        log.warn 'created prometheus job {}', prometheusJobId

        // redis exporter application
        final String redisExporterAppName = 'redis_exporter'
        // check if already exists
        def existsOne2 = new AppDTO(clusterId: RedisManager.CLUSTER_ID, name: redisExporterAppName).one()
        if (existsOne2) {
            log.warn('redis exporter already exists {}', redisExporterAppName)
            resp.halt(409, 'redis exporter already exists')
        }

        def app = new AppDTO()
        app.clusterId = RedisManager.CLUSTER_ID
        app.namespaceId = namespaceIdMetric
        app.name = redisExporterAppName
        // not auto first
        app.status = AppDTO.Status.manual.val
        app.updatedDate = new Date()

        def conf = new AppConf()
        app.conf = conf

        conf.containerNumber = 1
        conf.registryId = RedisManager.preferRegistryId()
        conf.group = 'oliver006'
        conf.image = 'redis_exporter'
        conf.tag = 'latest'
        conf.memMB = 128
        conf.memReservationMB = conf.memMB
        conf.cpuFixed = 0.1
        conf.user = '59000:59000'

        def c = Conf.instance
        def isSingleNode = c.isOn('rm.isSingleNodeTest')

        conf.isLimitNode = isSingleNode

        def envValue = "redis://127.0.0.1:6379".toString()

        conf.envList << new KVPair<String>('REDIS_ADDR', envValue)

        final int exporterPort = 9121

        if (isSingleNode) {
            conf.envList << new KVPair<String>('REDIS_EXPORTER_WEB_LISTEN_ADDRESS', '0.0.0.0:${' + exporterPort + '+instanceIndex}')
            conf.envList << new KVPair<String>(ContainerInfo.ENV_KEY_PUBLIC_PORT + exporterPort, '${' + exporterPort + '+instanceIndex}')
        } else {
            conf.envList << new KVPair<String>('REDIS_EXPORTER_WEB_LISTEN_ADDRESS', "0.0.0.0:${exporterPort}".toString())
            conf.envList << new KVPair<String>(ContainerInfo.ENV_KEY_PUBLIC_PORT + exporterPort, exporterPort.toString())
        }

        conf.networkMode = 'host'
        conf.portList << new PortMapping(privatePort: exporterPort, publicPort: exporterPort)

        // add application to dms
        int appId = app.add()
        app.id = appId
        log.info 'done create redis exporter application, app id: {}', appId

        def redisExporterJobId = RmJobExecutor.instance.runCreatingAppJob(app)
        log.warn 'created redis exporter job {}', redisExporterJobId

        [flag: true, prometheusJobId: prometheusJobId, redisExporterJobId: redisExporterJobId]
    }

    h.get('/init-log-collectors') { req, resp ->
        def targetNodeIp = req.param('targetNodeIp')
        assert targetNodeIp

        def instance = InMemoryAllContainerManager.instance
        def nodeInfo = instance.getNodeInfo(targetNodeIp)
        if (!nodeInfo) {
            resp.halt(404, 'node not exists')
        }
        nodeInfo.checkIfOk(new Date())
        if (!nodeInfo.isOk) {
            resp.halt(409, 'node heart beat not ok')
        }

        def namespaceIdMetric = NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'metric')

        // openobserve application
        final String openobserveAppName = 'openobserve'
        def existsOne = new AppDTO(clusterId: RedisManager.CLUSTER_ID, name: openobserveAppName).one()
        if (existsOne) {
            log.warn('openobserve already exists {}', openobserveAppName)
            resp.halt(409, 'openobserve already exists')
        }

        List<String> targetNodeIpList = [targetNodeIp]
        def ooApp = BasePlugin.tplApp(RedisManager.CLUSTER_ID, namespaceIdMetric, targetNodeIpList) { conf ->
            // use plugin define registry url
            conf.registryId = 0

            conf.memMB = 1024
            conf.cpuFixed = 2.0d
        }

        def ooPlugin = new OpenobservePlugin()
        ooPlugin.nodeDir = RedisManager.dataDir() + '/openobserve'
        ooPlugin.addNodeVolumeForUpdate('data-dir-only-for-redis-manager', ooPlugin.nodeDir,
                'need mount to /data/openobserve same value as env ZO_DATA_DIR')
        ooPlugin.demoApp(ooApp)
        ooApp.name = openobserveAppName
        ooApp.status = AppDTO.Status.manual.val
        def ooAppId = InitToolPlugin.addAppIfNotExists(ooApp)
        log.warn 'created openobserve app {}', ooAppId
        def ooJobId = RmJobExecutor.instance.runCreatingAppJob(ooApp)
        log.warn 'created openobserve job {}', ooJobId

        // vector application
        final String vectorAppName = 'vector'
        def existsOne1 = new AppDTO(clusterId: RedisManager.CLUSTER_ID, name: vectorAppName).one()
        if (existsOne1) {
            log.warn('vector already exists {}', vectorAppName)

            // check if container number = all nodes size
            def hbOkNodeInfList = instance.getHbOkNodeInfoList(RedisManager.CLUSTER_ID)
            if (hbOkNodeInfList.size() > existsOne.conf.containerNumber) {
                log.warn 'vector application container need scale up from {} to {}', existsOne.conf.containerNumber, hbOkNodeInfList.size()
                existsOne.conf.containerNumber = hbOkNodeInfList.size()
                new AppDTO(id: existsOne.id, conf: existsOne.conf, updatedDate: new Date()).update()
                resp.halt(409, 'vector application container need scale up, please wait')
            }

            resp.halt(409, 'vector already exists')
        }

        def vectorApp = BasePlugin.tplApp(RedisManager.CLUSTER_ID, namespaceIdMetric, []) { conf ->
            conf.memMB = 256
            conf.cpuFixed = 0.1d

            conf.registryId = RedisManager.preferRegistryId()
        }

        def vectorPlugin = new VectorPlugin()
        vectorPlugin.demoApp(vectorApp)
        vectorApp.name = vectorAppName
        vectorApp.status = AppDTO.Status.manual.val
        def vectorAppId = InitToolPlugin.addAppIfNotExists(vectorApp)
        log.warn 'created vector app {}', vectorAppId
        def vectorJobId = RmJobExecutor.instance.runCreatingAppJob(vectorApp)
        log.warn 'created vector job {}', vectorJobId

        [flag: true, ooJobId: ooJobId, vectorJobId: vectorJobId]
    }

    h.get('/init-node-exporters') { req, resp ->
        // node exporter application
        final String nodeExporterAppName = 'node-exporter'
        def existsOne = new AppDTO(clusterId: RedisManager.CLUSTER_ID, name: nodeExporterAppName).one()
        if (existsOne) {
            log.warn('node exporter already exists {}', nodeExporterAppName)

            // check if container number = all nodes size
            def hbOkNodeInfList = InMemoryAllContainerManager.instance.getHbOkNodeInfoList(RedisManager.CLUSTER_ID)
            if (hbOkNodeInfList.size() > existsOne.conf.containerNumber) {
                log.warn 'node exporter application container need scale up from {} to {}', existsOne.conf.containerNumber, hbOkNodeInfList.size()
                existsOne.conf.containerNumber = hbOkNodeInfList.size()
                new AppDTO(id: existsOne.id, conf: existsOne.conf, updatedDate: new Date()).update()
                resp.halt(409, 'node exporter application container need scale up, please wait')
            }

            resp.halt(409, 'node exporter already exists')
        }

        def namespaceIdMetric = NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'metric')

        def nodeExporterApp = BasePlugin.tplApp(RedisManager.CLUSTER_ID, namespaceIdMetric, [])

        def nodeExporterPlugin = new NodeExporterPlugin()
        nodeExporterPlugin.demoApp(nodeExporterApp)
        nodeExporterApp.name = nodeExporterAppName
        nodeExporterApp.status = AppDTO.Status.manual.val
        def nodeExporterAppId = InitToolPlugin.addAppIfNotExists(nodeExporterApp)
        log.warn 'created node exporter app {}', nodeExporterAppId
        def nodeExporterJobId = RmJobExecutor.instance.runCreatingAppJob(nodeExporterApp)
        log.warn 'created node exporter job {}', nodeExporterJobId

        [flag: true, nodeExporterJobId: nodeExporterJobId]
    }
}
