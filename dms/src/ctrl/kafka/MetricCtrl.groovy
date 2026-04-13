package ctrl.kafka

import km.KafkaManager
import km.KmJobExecutor
import model.AppDTO
import model.NamespaceDTO
import model.json.AppConf
import model.json.ExtendParams
import model.json.KVPair
import model.json.PortMapping
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import plugin.BasePlugin
import plugin.demo2.InitToolPlugin
import plugin.demo2.PrometheusPlugin
import server.InMemoryAllContainerManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/metric') {
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

        def namespaceIdMetric = NamespaceDTO.createIfNotExist(KafkaManager.CLUSTER_ID, 'metric')

        final String prometheusAppName = 'km_prometheus'
        def existsOne = new AppDTO(clusterId: KafkaManager.CLUSTER_ID, name: prometheusAppName).one()
        if (existsOne) {
            log.warn('prometheus already exists {}', prometheusAppName)
            resp.halt(409, 'prometheus already exists')
        }

        List<String> targetNodeIpList = [targetNodeIp]
        def prometheusApp = BasePlugin.tplApp(KafkaManager.CLUSTER_ID, namespaceIdMetric, targetNodeIpList) { conf ->
            conf.memMB = 1024
            conf.cpuFixed = 2.0d
            conf.networkMode = 'host'
            conf.portList.clear()
            conf.portList << new PortMapping(privatePort: 9090, publicPort: 9090)
        }

        def prometheusPlugin = new PrometheusPlugin()
        prometheusPlugin.demoApp(prometheusApp)
        prometheusApp.name = prometheusAppName
        prometheusApp.status = AppDTO.Status.auto
        prometheusApp.extendParams = new ExtendParams()
        prometheusApp.extendParams.put('for_kafka_manager', '1')
        def prometheusAppId = InitToolPlugin.addAppIfNotExists(prometheusApp)
        log.warn 'created prometheus app {}', prometheusAppId
        def prometheusJobId = KmJobExecutor.instance.runCreatingAppJob(prometheusApp)
        log.warn 'created prometheus job {}', prometheusJobId

        final String kafkaExporterAppName = 'km_kafka_exporter'
        def existsOne2 = new AppDTO(clusterId: KafkaManager.CLUSTER_ID, name: kafkaExporterAppName).one()
        if (existsOne2) {
            log.warn('kafka exporter already exists {}', kafkaExporterAppName)
            resp.halt(409, 'kafka exporter already exists')
        }

        def app = new AppDTO()
        app.clusterId = KafkaManager.CLUSTER_ID
        app.namespaceId = namespaceIdMetric
        app.name = kafkaExporterAppName
        app.status = AppDTO.Status.manual
        app.updatedDate = new Date()

        def conf = new AppConf()
        app.conf = conf

        conf.envList << new KVPair<String>('KAFKA_SERVER', 'broker1:9092')
        conf.group = 'danielqsj'
        conf.image = 'kafka-exporter'
        conf.tag = 'latest'
        conf.memMB = 128
        conf.memReservationMB = conf.memMB
        conf.cpuFixed = 0.1
        conf.networkMode = 'host'

        final int exporterPort = 9308
        conf.portList << new PortMapping(privatePort: exporterPort, publicPort: exporterPort)

        int appId = app.add()
        app.id = appId
        log.info 'done create kafka exporter application, app id: {}', appId

        def kafkaExporterJobId = KmJobExecutor.instance.runCreatingAppJob(app)
        log.warn 'created kafka exporter job {}', kafkaExporterJobId

        [flag: true, prometheusJobId: prometheusJobId, kafkaExporterJobId: kafkaExporterJobId]
    }
}
