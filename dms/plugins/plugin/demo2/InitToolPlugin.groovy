package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ClusterDTO
import model.NamespaceDTO
import model.NodeDTO
import org.segment.web.handler.ChainHandler
import plugin.BasePlugin

@CompileStatic
@Slf4j
class InitToolPlugin extends BasePlugin {
    @Override
    String group() {
        'key232323'
    }

    @Override
    String image() {
        'dms'
    }

    @Override
    void init() {
        initApi()
    }

    private void initApi() {
        def h = ChainHandler.instance

        h.get('/init-all') { req, resp ->
            def clusterIdStr = req.param('clusterId')
            def clusterSecret = req.param('clusterSecret')
            assert clusterIdStr && clusterSecret

            int clusterId = clusterIdStr as int

            def cluster = new ClusterDTO(id: clusterId).one()
            if (!cluster) {
                resp.halt(404, 'cluster not found')
                return
            }

            if (cluster.secret != clusterSecret) {
                resp.halt(403, 'cluster secret not match')
                return
            }

            // check node
            def nodeList = new NodeDTO(clusterId: clusterId).loadList()
            if (!nodeList) {
                resp.halt(404, 'node not found')
                return
            }

            def nodeIpList = nodeList.collect { it.ip }

            int namespaceId
            def ns = new NamespaceDTO(clusterId: clusterId, name: 'monitor').one()
            if (!ns) {
                namespaceId = new NamespaceDTO(clusterId: clusterId as int, name: 'monitor').add()
            } else {
                namespaceId = ns.id
            }

            List<String> logs = []

            def appPrometheus = new PrometheusPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            appPrometheus.add()
            logs << 'done add prometheus'
            log.info logs[-1]

            def appNodeExporter = new NodeExporterPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            appNodeExporter.add()
            logs << 'done add node exporter'
            log.info logs[-1]

            def appGrafana = new GrafanaPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            appGrafana.add()
            logs << 'done add grafana'
            log.info logs[-1]

            def appZo = new ZincObservePlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            appZo.add()
            logs << 'done add zinc observe'
            log.info logs[-1]

            def appVector = new VectorPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            appVector.add()
            logs << 'done add vector'
            log.info logs[-1]

            [logs: logs]
        }
    }
}
