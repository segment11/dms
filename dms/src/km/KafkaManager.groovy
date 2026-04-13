package km

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import metric.SimpleGauge
import model.AppDTO
import model.DynConfigDTO
import model.KmServiceDTO
import plugin.BasePlugin
import server.AgentCaller
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class KafkaManager {
    static final int CLUSTER_ID = 1
    static final int ONE_CLUSTER_MAX_BROKERS = 32
    static final int MAX_PARTITIONS_PER_TOPIC = 256

    static final String DEFAULT_DATA_DIR = '/data/kafka_manager'

    static int preferRegistryId() {
        BasePlugin.addRegistryIfNotExist('docker.1ms.run', 'https://docker.1ms.run')
    }

    static String dataDir() {
        def one = new DynConfigDTO(name: 'kafka_manager.data.dir').one()
        return one?.vv ?: DEFAULT_DATA_DIR
    }

    static final SimpleGauge globalGauge = new SimpleGauge('Kafka Manager', 'Kafka Manager Metrics.', ['cluster_id'])

    static { globalGauge.register() }

    static void init() {
        initMetricCollector()
    }

    static void initMetricCollector() {
        def labelValues = List.of(CLUSTER_ID.toString())
        globalGauge.addRawGetter(() -> {
            def map = new HashMap<String, SimpleGauge.ValueWithLabelValues>()
            def serviceList = new KmServiceDTO(status: KmServiceDTO.Status.running).list()
            def clusterCount = 0
            def brokerTotalCount = 0
            for (one in serviceList) {
                clusterCount++
                brokerTotalCount += one.brokers
            }
            map.km_cluster_count = new SimpleGauge.ValueWithLabelValues((double) clusterCount, labelValues)
            map.km_broker_count = new SimpleGauge.ValueWithLabelValues((double) brokerTotalCount, labelValues)
            map
        })
    }

    static String decode(String content) {
        char[] chars = content.toCharArray()
        char[] x = new char[chars.length]
        chars.eachWithIndex { char c, int i ->
            def diff = i % 2 == 0 ? 1 : 2
            x[i] = (c - diff) as char
        }
        new String(x)
    }

    static String encode(String content) {
        char[] chars = content.toCharArray()
        char[] x = new char[chars.length]
        chars.eachWithIndex { char c, int i ->
            def diff = i % 2 == 0 ? 1 : 2
            x[i] = (c + diff) as char
        }
        new String(x)
    }

    static void stopContainers(int appId) {
        def appOne = new AppDTO(id: appId).queryFields('id,status').one()
        if (!appOne) {
            return
        }
        if (appOne.status == AppDTO.Status.auto) {
            log.warn('update app status to manual, app id: {}', appOne.id)
            new AppDTO(id: appOne.id, status: AppDTO.Status.manual, updatedDate: new Date()).update()
        }
        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getContainerList(CLUSTER_ID, appId)
        containerList.each { x ->
            if (x.running()) {
                log.warn('stop running container: {}', x.name())
                def p = [id: x.id]
                p.isRemoveAfterStop = '1'
                p.readTimeout = 30 * 1000
                AgentCaller.instance.agentScriptExe(CLUSTER_ID, x.nodeIp, 'container stop', p)
            }
        }
    }
}
