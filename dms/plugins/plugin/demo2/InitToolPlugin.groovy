package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.GatewayConf
import model.json.GwService
import org.segment.web.handler.ChainHandler
import plugin.BasePlugin

@CompileStatic
@Slf4j
class InitToolPlugin {
    static int addAppIfNotExists(AppDTO app) {
        def one = new AppDTO(name: app.name, clusterId: app.clusterId).queryFields('id').one()
        if (one) {
            log.warn 'app already exists, skip add, app name: {}', app.name
            return one.id
        } else {
            def id = app.add()
            app.id = id
            log.info 'add app success, app name: {}', app.name
            return id
        }
    }

    private static void addGwRouterIfNotExists(int gwClusterId, int clusterId, int appId, String appName, int privatePort) {
        def router = new GwRouterDTO(clusterId: gwClusterId, name: appName)
        def one = router.one()
        if (one) {
            log.warn 'gw router already exists, skip add, app name: {}', appName
            return
        }

        router.priority = 10
        router.service = new GwService()
        // todo, refer ClusterDnsAnswerHandler
        router.rule = 'Host(`' + "app_${appId}.cluster_${clusterId}" + '`)'
        def routerId = router.add()

        // update app gateway config
        def gatewayConf = new GatewayConf(clusterId: gwClusterId, routerId: routerId, containerPrivatePort: privatePort)
        new AppDTO(id: appId, gatewayConf: gatewayConf).update()
    }

    private static void initApi() {
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
            def nodeList = new NodeDTO(clusterId: clusterId).list()
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

            def appPrometheus = new PrometheusPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            def prometheusAppId = addAppIfNotExists(appPrometheus)

            def appNodeExporter = new NodeExporterPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appNodeExporter)

            def lokiPrometheus = new LokiPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            def lokiAppId = addAppIfNotExists(lokiPrometheus)

            def appGrafana = new GrafanaPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            def grafanaAppId = addAppIfNotExists(appGrafana)

            def appOo = new OpenobservePlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            def ooAppId = addAppIfNotExists(appOo)

            def appVector = new VectorPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appVector)

            def appZk = new ZookeeperPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appZk)

            def appTraefik = new TraefikPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            def traefikAppId = addAppIfNotExists(appTraefik)

            def appEtcd = new EtcdPlugin().demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appEtcd)

            cluster.globalEnvConf.dnsInfo.nameservers = nodeIpList[0]
            new ClusterDTO(id: clusterId, globalEnvConf: cluster.globalEnvConf, isInGuard: true).update()

            // add traefik frontend
            def gw = new GwClusterDTO()
            gw.appId = traefikAppId
            gw.name = 'traefik'
            gw.serverUrl = 'http://' + nodeIpList[0]
            gw.serverPort = 80
            gw.dashboardPort = 81
            gw.updatedDate = new Date()
            def gwClusterId = gw.add()

            addGwRouterIfNotExists(gwClusterId, clusterId, prometheusAppId, 'prometheus', 9090)
            addGwRouterIfNotExists(gwClusterId, clusterId, lokiAppId, 'loki', 3100)
            addGwRouterIfNotExists(gwClusterId, clusterId, grafanaAppId, 'grafana', 3000)
            addGwRouterIfNotExists(gwClusterId, clusterId, ooAppId, 'openobserve', 5080)

            'ok'
        }
    }
}
