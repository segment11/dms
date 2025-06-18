package ctrl.redis

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.kevinsawicki.http.HttpRequest
import model.AppDTO
import model.NamespaceDTO
import model.RmServiceDTO
import model.json.AppConf
import model.json.ExtendParams
import model.json.KVPair
import model.json.PortMapping
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import plugin.BasePlugin
import plugin.demo2.*
import redis.clients.jedis.HostAndPort
import rm.RedisManager
import rm.RmJobExecutor
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import transfer.ContainerInfo

import java.text.SimpleDateFormat

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
        prometheusApp.status = AppDTO.Status.auto
        prometheusApp.extendParams = new ExtendParams()
        prometheusApp.extendParams.put('for_redis_manager', '1')
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
        app.status = AppDTO.Status.manual
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

        def isSingleNode = RedisManager.isOnlyOneNodeForTest()

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
        ooApp.status = AppDTO.Status.manual
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
        vectorApp.status = AppDTO.Status.manual
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
        nodeExporterApp.status = AppDTO.Status.manual
        def nodeExporterAppId = InitToolPlugin.addAppIfNotExists(nodeExporterApp)
        log.warn 'created node exporter app {}', nodeExporterAppId
        def nodeExporterJobId = RmJobExecutor.instance.runCreatingAppJob(nodeExporterApp)
        log.warn 'created node exporter job {}', nodeExporterJobId

        [flag: true, nodeExporterJobId: nodeExporterJobId]
    }

    h.get('/query') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int
        def timeRange = req.param('timeRange') ?: '1h'
        Map<String, String> steps = [:]
        steps['5m'] = '15s'
        steps['1h'] = '1m'
        steps['3h'] = '5m'
        steps['1d'] = '20m'
        steps['7d'] = '1h'
        steps['30d'] = '6h'
        def step = steps[timeRange]
        if (!step) {
            resp.halt(400, 'time range not support')
        }

        // js: (new Date().time / 1000) as int
        def start = req.param('start')
        def end = req.param('end')

        if (!start && !end) {
            // now
            def endSeconds = (System.currentTimeMillis() / 1000).intValue()
            int fromMinute
            if (timeRange.endsWith('m')) {
                fromMinute = timeRange[0..-2] as int
            } else if (timeRange.endsWith('h')) {
                fromMinute = timeRange[0..-2] as int * 60
            } else if (timeRange.endsWith('d')) {
                fromMinute = timeRange[0..-2] as int * 60 * 24
            } else {
                fromMinute = 60 * 24
//                resp.halt(400, 'time range not support')
            }
            start = (endSeconds - fromMinute * 60).toString()
            end = endSeconds.toString()
        }

        def prometheusApp = InMemoryCacheSupport.instance.appList.find { app ->
            app.conf.image == 'prometheus' && app.extendParams && app.extendParams.get('for_redis_manager') == '1'
        }
        if (!prometheusApp) {
            resp.halt(404, 'prometheus app not found, need "Init Exporters / Prometheus" first')
        }

        def instance = InMemoryAllContainerManager.instance
        def prometheusContainerList = instance.getRunningContainerList(prometheusApp.clusterId, prometheusApp.id)
        if (!prometheusContainerList) {
            resp.halt(409, 'prometheus no running instances')
        }

        def prometheusX = prometheusContainerList[0]
        def nodeIp = prometheusX.nodeIp
        def port = prometheusX.publicPort(9090)

        final String uriPrefix = '/api/v1/query_range'
        def url = "http://${nodeIp}:${port}${uriPrefix}".toString()

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        def runningContainerList = one.runningContainerList()
        if (!runningContainerList) {
            resp.halt(409, 'no running instances')
        }

        List<HostAndPort> hostAndPortList = runningContainerList.collect { x ->
            new HostAndPort(x.nodeIp, one.listenPort(x))
        }
        def instancesFilter = hostAndPortList.collect {
            "redis://${it.host}:${it.port}"
        }.join('|')

        def nameList = '''
redis_memory_used_bytes
redis_connected_clients
redis_keyspace_hits_total
redis_keyspace_misses_total
'''.readLines().collect { it.trim() }.findAll { it }
        def namesFilter = nameList.join('|')

        def queryQl = '{__name__=~"' + namesFilter + '", instance=~"' + instancesFilter + '"}'

        final int connectTimeout = 500
        final int readTimeout = 2000

        def body = HttpRequest.post(url).form([query: queryQl, start: start, end: end, step: step]).
                connectTimeout(connectTimeout).readTimeout(readTimeout).body()

        def obj = JSON.parseObject(body)
        if ('success' != obj.getString('status')) {
            log.warn 'query failed, queryQL: {}, body: {}', queryQl, body
            resp.halt(500, 'query failed')
        }

        List<Map> list = []
        def result = obj.getJSONObject('data').getJSONArray('result')
        for (item in result) {
            /*
    {
        "metric" : {
           "__name__" : "redis_memory_used_bytes",
           "job" : "***",
           "instance" : "localhost:9090",
        },
        "values" : [[
           1435781430.781, "1024"
        ]]
     }
    */
            def jo = item as JSONObject
            list << resultItemToMap(jo)
        }

        def qpsQl = 'rate(redis_commands_processed_total{instance=~"' + instancesFilter + '"}[1m])'
        list.addAll queryOneLabel(url, connectTimeout, readTimeout, start, end, step, qpsQl, 'redis_commands_processed_qps')

        [map: list.groupBy { it.metricName }]
    }
}

static Map resultItemToMap(JSONObject jo, String givenMetricName = null) {
    def sf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')

    def metric = jo.getJSONObject('metric')
    def metricName = givenMetricName ?: metric.getString('__name__')
    def metricInstance = metric.getString('instance')
    def values = jo.getJSONArray('values')

    [metricName    : metricName,
     metricInstance: metricInstance,
     xData         : values.collect { value ->
         def ll = value as List
         def millis = ll[0] as double * 1000
         sf.format(new Date(millis as long))
     },
     data          : values.collect { value ->
         def ll = value as List
         (ll[1] as double).round(2)
     }]
}

static List<Map> queryOneLabel(String url, int connectTimeout, int readTimeout, String start, String end, String step,
                               String queryQl, String givenMetricName = null) {
    def log = LoggerFactory.getLogger(MetricCtrl.class)

    def body = HttpRequest.post(url).form([query: queryQl, start: start, end: end, step: step]).
            connectTimeout(connectTimeout).readTimeout(readTimeout).body()

    def obj = JSON.parseObject(body)
    if ('success' != obj.getString('status')) {
        log.warn 'query failed, query: {}, body: {}', queryQl, body
        return []
    }


    List<Map> list = []
    def result = obj.getJSONObject('data').getJSONArray('result')
    for (item in result) {
        def jo = item as JSONObject
        list << resultItemToMap(jo, givenMetricName)
    }
    list
}
