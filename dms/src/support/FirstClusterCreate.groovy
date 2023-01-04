package support

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ClusterDTO
import model.json.GlobalEnvConf

@CompileStatic
@Slf4j
class FirstClusterCreate {
    static void create() {
        def clusterOne = new ClusterDTO().queryFields('id,name').
                where('1=1').one()
        if (!clusterOne) {
            new ClusterDTO(id: 1, name: 'Cluster For Test', secret: '1', isInGuard: false,
                    globalEnvConf: new GlobalEnvConf()).add()
            log.info 'done create first cluster for test'
        } else {
            log.info 'there is already a cluster define {}', clusterOne.name
        }
    }
}