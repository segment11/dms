package rm

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import metric.SimpleGauge
import model.*
import plugin.BasePlugin
import server.AgentCaller
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class RedisManager {
    static final int CLUSTER_ID = 1

    // settings
    static final String DEFAULT_DATA_DIR = '/data/redis_manager'

    static final String DEFAULT_BACKUP_DATA_DIR = '/data/redis_manager_backup'

    static String dataDir() {
        def one = new DynConfigDTO(name: 'redis_manager.data.dir').one()
        return one?.vv ?: DEFAULT_DATA_DIR
    }

    static void updateDataDir(String dataDir) {
        def one = new DynConfigDTO(name: 'redis_manager.data.dir').one()
        if (one) {
            new DynConfigDTO(id: one.id, vv: dataDir, updatedDate: new Date()).update()
        } else {
            new DynConfigDTO(name: 'redis_manager.data.dir', vv: dataDir, updatedDate: new Date()).add()
        }
    }

    static String backupDataDir() {
        def one = new DynConfigDTO(name: 'redis_manager.backup.data.dir').one()
        return one?.vv ?: DEFAULT_BACKUP_DATA_DIR
    }

    static void updateBackupDataDir(String backupDataDir) {
        def one = new DynConfigDTO(name: 'redis_manager.backup.data.dir').one()
        if (one) {
            new DynConfigDTO(id: one.id, vv: backupDataDir, updatedDate: new Date()).update()
        } else {
            new DynConfigDTO(name: 'redis_manager.backup.data.dir', vv: backupDataDir, updatedDate: new Date()).add()
        }
    }

    static boolean changeOneNodeForTestFlag() {
        def one = new DynConfigDTO(name: 'redis_manager.one.node.for.test').one()
        if (one) {
            def isOldFalse = one.vv == 'false'
            new DynConfigDTO(id: one.id, vv: isOldFalse ? 'true' : 'false', updatedDate: new Date()).update()
            return isOldFalse
        } else {
            new DynConfigDTO(name: 'redis_manager.one.node.for.test', vv: 'true', updatedDate: new Date()).add()
            return true
        }
    }

    static boolean isOnlyOneNodeForTest() {
        def one = new DynConfigDTO(name: 'redis_manager.one.node.for.test').one()
        one && one.vv == 'true'
    }

    static int preferRegistryId() {
        BasePlugin.addRegistryIfNotExist('docker.1ms.run', 'https://docker.1ms.run')
    }

    // metrics
    static final SimpleGauge globalGauge = new SimpleGauge('Redis Manager', 'Redis Manager Metrics.', ['cluster_id'])

    static {
        globalGauge.register()
    }

    static void initMetricCollector() {
        var labelValues = List.of(CLUSTER_ID.toString())

        globalGauge.addRawGetter(() -> {
            def map = new HashMap<String, SimpleGauge.ValueWithLabelValues>()

            def instance = InMemoryAllContainerManager.instance

            def sentinelServiceList = new RmSentinelServiceDTO(status: RmSentinelServiceDTO.Status.running).list()
            def sentinelClusterCount = sentinelServiceList.size()
            def sentinelClusterStateOkCount = 0
            for (one in sentinelServiceList) {
                def runningContainerList = instance.getRunningContainerList(CLUSTER_ID, one.appId)
                if (runningContainerList.size() == one.replicas) {
                    sentinelClusterStateOkCount++
                }
            }
            map.rm_sentinel_cluster_count = new SimpleGauge.ValueWithLabelValues((double) sentinelClusterCount, labelValues)
            map.rm_sentinel_cluster_state_ok_count = new SimpleGauge.ValueWithLabelValues((double) sentinelClusterStateOkCount, labelValues)

            def serviceList = new RmServiceDTO(status: RmServiceDTO.Status.running).list()
            def serviceSentinelStandaloneCount = 0
            def serviceSentinelStandaloneStateOkCount = 0
            def serviceSentinelModeCount = 0
            def serviceSentinelModeStateOkCount = 0
            def serverClusterModeCount = 0
            def serverClusterModeStateOkCount = 0
            for (one in serviceList) {
                def jobResult = one.checkNodes()
                if (one.mode == RmServiceDTO.Mode.standalone) {
                    serviceSentinelStandaloneCount++
                    if (jobResult) {
                        serviceSentinelStandaloneStateOkCount++
                    }
                } else if (one.mode == RmServiceDTO.Mode.sentinel) {
                    serviceSentinelModeCount++
                    if (jobResult) {
                        serviceSentinelModeStateOkCount++
                    }
                } else {
                    serverClusterModeCount++
                    if (jobResult) {
                        serverClusterModeStateOkCount++
                    }
                }
            }

            map.rm_service_sentinel_standalone_count = new SimpleGauge.ValueWithLabelValues((double) serviceSentinelStandaloneCount, labelValues)
            map.rm_service_sentinel_standalone_state_ok_count = new SimpleGauge.ValueWithLabelValues((double) serviceSentinelStandaloneStateOkCount, labelValues)
            map.rm_service_sentinel_mode_count = new SimpleGauge.ValueWithLabelValues((double) serviceSentinelModeCount, labelValues)
            map.rm_service_sentinel_mode_state_ok_count = new SimpleGauge.ValueWithLabelValues((double) serviceSentinelModeStateOkCount, labelValues)
            map.rm_service_cluster_mode_count = new SimpleGauge.ValueWithLabelValues((double) serverClusterModeCount, labelValues)
            map.rm_service_cluster_mode_state_ok_count = new SimpleGauge.ValueWithLabelValues((double) serverClusterModeStateOkCount, labelValues)

            map
        })
    }

    // for password encode/decode
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

    static final int ONE_INSTANCE_MAX_MEMORY_MB = 64 * 1024

    static final int ONE_SHARD_MAX_REPLICAS = 4

    static final int ONE_CLUSTER_MAX_SHARDS = 32

    // containers management
    static void stopContainers(int appId) {
        def appOne = new AppDTO(id: appId).queryFields('id,status').one()
        if (!appOne) {
            return
        }
        if (appOne.status == AppDTO.Status.auto) {
            // update to manual
            log.warn('update app status to manual, app id: {}', appOne.id)
            new AppDTO(id: appOne.id, status: AppDTO.Status.manual, updatedDate: new Date()).update()
        }

        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, appId)
        containerList.each { x ->
            if (x.running()) {
                log.warn('stop running container: {}', x.name())

                def p = [id: x.id]
                p.isRemoveAfterStop = '1'
                p.readTimeout = 30 * 1000

                def stopR = AgentCaller.instance.agentScriptExe(CLUSTER_ID, x.nodeIp, 'container stop', p)
                log.info 'done stop and remove container - {} - {}, result: {}', x.id, x.appId(), stopR
            }
        }
    }

    static void deleteDmsApp(int appId) {
        def appJobList = new AppJobDTO(appId: appId).queryFields('id').list()
        if (appJobList) {
            new AppJobLogDTO().whereIn('job_id', appJobList.collect { it.id }).deleteAll()
            new AppJobDTO(appId: appId).deleteAll()
        }
    }
}
