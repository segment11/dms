package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.*
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

    private int addAppIfNotExists(AppDTO app) {
        def one = new AppDTO(name: app.name, clusterId: app.clusterId).queryFields('id').one()
        if (one) {
            log.warn 'app already exists, skip add, app name: {}', app.name
            return one.id
        } else {
            def id = app.add()
            log.info 'add app success, app name: {}', app.name
            return id
        }
    }

    private void addGwFrontendIfNotExists(int gwClusterId, int appId, String appName, int privatePort) {
        def f = new GwFrontendDTO(clusterId: gwClusterId, name: appName)
        def one = f.one()
        if (one) {
            log.warn 'gw frontend already exists, skip add, app name: {}', appName
            return
        }

        f.priority = 10
        f.backend = new GwBackend()
        f.auth = new GwAuth()
        def addedId = f.add()

        def suffix = '.service.dc1.consul'
        def rule = new GwFrontendRuleConf(type: 'Host:', rule: "gw_${gwClusterId}_${addedId}".toString() + suffix)

        def conf = new GwFrontendConf()
        conf.ruleConfList << rule

        new GwFrontendDTO(id: addedId, conf: conf).update()

        // update app gateway config
        def gatewayConf = new GatewayConf(clusterId: gwClusterId, frontendId: addedId, containerPrivatePort: privatePort)
        new AppDTO(id: appId, gatewayConf: gatewayConf).update()
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

            def appPrometheus = new PrometheusPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            def prometheusAppId = addAppIfNotExists(appPrometheus)

            def appNodeExporter = new NodeExporterPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appNodeExporter)

            def appGrafana = new GrafanaPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            def grafanaAppId = addAppIfNotExists(appGrafana)

            def appZo = new ZincObservePlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            def zoAppId = addAppIfNotExists(appZo)

            def appVector = new VectorPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appVector)

            def appZk = new ZookeeperPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appZk)

            def appTraefik = new TraefikPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            def traefikAppId = addAppIfNotExists(appTraefik)

            def appConsul = new ConsulPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            def consulAppId = addAppIfNotExists(appConsul)

            def appDnsmasq = new DnsmasqPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appDnsmasq)

            def appEtcd = new EtcdPlugin().demoApp(tplApp(clusterId, namespaceId, nodeIpList))
            addAppIfNotExists(appEtcd)

            cluster.globalEnvConf.dnsServer = nodeIpList.join(',')
            new ClusterDTO(id: clusterId, globalEnvConf: cluster.globalEnvConf, isInGuard: true).update()

            // add traefik frontend
            def gw = new GwClusterDTO()
            gw.appId = traefikAppId
            gw.name = 'traefik'
            gw.serverUrl = 'http://' + nodeIpList[0]
            gw.serverPort = 80
            gw.dashboardPort = 81
            gw.zkConnectString = nodeIpList[0] + ':2181'
            gw.prefix = 'traefik'
            gw.updatedDate = new Date()
            def gwClusterId = gw.add()

            addGwFrontendIfNotExists(gwClusterId, prometheusAppId, 'prometheus', 9090)
            addGwFrontendIfNotExists(gwClusterId, grafanaAppId, 'grafana', 3000)
            addGwFrontendIfNotExists(gwClusterId, zoAppId, 'zincobserve', 5080)
            addGwFrontendIfNotExists(gwClusterId, consulAppId, 'consul', 8500)

            'ok'
        }
    }
}
