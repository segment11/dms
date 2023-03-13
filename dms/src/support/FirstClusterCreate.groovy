package support

import common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ClusterDTO
import model.NamespaceDTO
import model.json.GlobalEnvConf

@CompileStatic
@Slf4j
class FirstClusterCreate {
    static void create() {
        def clusterOne = new ClusterDTO().queryFields('id,name').
                noWhere().one()
        if (!clusterOne) {
            def localIpFilterPre = Conf.instance.getString('localIpFilterPre', '192.')
            new ClusterDTO(id: 1, name: 'Cluster For Test', secret: '1', isInGuard: false,
                    globalEnvConf: new GlobalEnvConf(localIpFilterPre: localIpFilterPre)).add()
            log.info 'done create first cluster for test'

            def ns = new NamespaceDTO(clusterId: 1).one()
            if (!ns) {
                new NamespaceDTO(clusterId: 1, name: 'demo').add()
            }
        } else {
            log.info 'there is already a cluster define {}', clusterOne.name
        }
    }
}
