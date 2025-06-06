package rm

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.DynConfigDTO
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

    static void updateDataDir(String dataDir) {
        def one = new DynConfigDTO(name: 'rm.data.dir').one()
        if (one) {
            new DynConfigDTO(id: one.id, vv: dataDir, updatedDate: new Date()).update()
        } else {
            new DynConfigDTO(name: 'rm.data.dir', vv: dataDir, updatedDate: new Date()).add()
        }
    }

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
