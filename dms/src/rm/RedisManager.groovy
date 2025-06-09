package rm

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.DynConfigDTO
import plugin.BasePlugin
import server.AgentCaller
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class RedisManager {
    // settings
    static final String DEFAULT_DATA_DIR = '/data/rm'

    static String dataDir() {
        def one = new DynConfigDTO(name: 'rm.data.dir').one()
        return one?.vv ?: DEFAULT_DATA_DIR
    }

    static int preferRegistryId() {
        BasePlugin.addRegistryIfNotExist('docker.1ms.run', 'https://docker.1ms.run')
    }

    static void updateDataDir(String dataDir) {
        def one = new DynConfigDTO(name: 'rm.data.dir').one()
        if (one) {
            new DynConfigDTO(id: one.id, vv: dataDir, updatedDate: new Date()).update()
        } else {
            new DynConfigDTO(name: 'rm.data.dir', vv: dataDir, updatedDate: new Date()).add()
        }
    }

    static final int ONE_SHARD_MAX_REPLICAS = 10

    static final int ONE_CLUSTER_MAX_SHARDS = 128

    // containers management
    static void stopContainers(int appId) {
        def appOne = new AppDTO(id: appId).queryFields('id,status').one()
        if (!appOne) {
            return
        }
        if (appOne.status == AppDTO.Status.auto.val) {
            // update to manual
            log.warn('update app status to manual, app id: {}', appOne.id)
            new AppDTO(id: appOne.id, status: AppDTO.Status.manual.val, updatedDate: new Date()).update()
        }

        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getContainerList(appOne.clusterId, appId)
        containerList.each { x ->
            if (x.running()) {
                log.warn('stop running container: {}', x.name())

                def p = [id: x.id]
                p.isRemoveAfterStop = '1'
                p.readTimeout = 30 * 1000

                def stopR = AgentCaller.instance.agentScriptExe(appOne.clusterId, x.nodeIp, 'container stop', p)
                log.info 'done stop and remove container - {} - {}, result: {}', x.id, x.appId(), stopR
            }
        }
    }
}
