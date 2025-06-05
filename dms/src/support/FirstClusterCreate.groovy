package support

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ClusterDTO
import model.NamespaceDTO
import model.json.GlobalEnvConf

@CompileStatic
@Slf4j
class FirstClusterCreate {
    static void create() {
        final int clusterId = 1

        def clusterOne = new ClusterDTO().queryFields('id,name').
                noWhere().one()
        if (!clusterOne) {
            def localIpFilterPre = Conf.instance.getString('localIpFilterPre', '192.')
            new ClusterDTO(id: clusterId, name: 'Cluster For Test', secret: '1', isInGuard: false,
                    globalEnvConf: new GlobalEnvConf(sameVpcNodeIpPrefix: localIpFilterPre)).add()
            log.info 'done create first cluster for test'

            NamespaceDTO.createIfNotExist(clusterId, 'demo')
        } else {
            log.info 'there is already a cluster define {}', clusterOne.name
        }
    }
}
