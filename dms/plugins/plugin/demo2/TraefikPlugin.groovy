package plugin.demo2

import ex.JobProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.*
import plugin.BasePlugin
import plugin.PluginManager
import plugin.callback.Observer
import plugin.model.Menu
import server.scheduler.processor.ContainerRunResult
import server.scheduler.processor.JobStepKeeper
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class TraefikPlugin extends BasePlugin implements Observer {
    @Override
    String name() {
        'traefik'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initWhoamiImageConfig()
    }

    private void initImageConfig() {
        '80,8080'.split(',').each {
            addPortIfNotExists(it.toString(), it as int)
        }

        final String tplName = 'traefik.yml.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/traefik/TraefikYmlTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('logLevel', 'info', 'string')
        tplParams.addParam('logDir', '/var/log/traefik', 'string')
        tplParams.addParam('serverPort', '80', 'int')
        tplParams.addParam('dashboardPort', '8080', 'int')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/traefik/traefik.yml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('log-dir', '/var/log/traefik')
    }

    private void initWhoamiImageConfig() {
        def imageName = 'traefik/whoami'
        ['WHOAMI_PORT_NUMBER', 'WHOAMI_NAME'].each { envName ->
            def one = new ImageEnvDTO(imageName: imageName, env: envName).one()
            if (!one) {
                new ImageEnvDTO(imageName: imageName, name: envName, env: envName).add()
            }
        }
    }

    @Override
    String group() {
        'library'
    }

    @Override
    String image() {
        'traefik'
    }

    @Override
    AppDTO demoApp(AppDTO app) {
        initAppBase(app)

        def conf = app.conf
        conf.tag = 'v3.0'

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/var/log/traefik', dist: '/var/log/traefik', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/var/log/traefik'))

        List<KVPair<String>> paramList = []
        paramList << new KVPair<String>('logLevel', 'info')
        paramList << new KVPair<String>('logDir', '/var/log/traefik')
        paramList << new KVPair<String>('serverPort', '80')
        paramList << new KVPair<String>('dashboardPort', '8080')

        conf.fileVolumeList << new FileVolumeMount(
                paramList: paramList,
                dist: '/etc/traefik/traefik.yml',
                imageTplId: getImageTplIdByName('traefik.yml.tpl'))

        conf.portList << new PortMapping(privatePort: 80, publicPort: 80)
        conf.portList << new PortMapping(privatePort: 8080, publicPort: 8080)

        app
    }

    @Override
    List<Menu> menus() {
        List<Menu> menus = []

        menus << new Menu(title: 'Traefik', icon: 'icon-cloud', children: [
                new Menu(title: 'Overview', module: 'gateway', page: 'overview', icon: 'icon-dashboard'),
                new Menu(title: 'List', module: 'gateway', page: 'cluster', icon: 'icon-list')
        ])

        menus
    }

    static final int DEFAULT_WEIGHT = 10

    boolean addServerUrlToRouter(int appId, GatewayConf c, ContainerRunResult result, int weight) {
        def serverUrl = "http://${result.nodeIp}:${result.port}".toString()

        def gwCluster = new GwClusterDTO(id: c.clusterId).one()
        if (!gwCluster) {
            log.warn 'no gateway cluster found, app id: {}', appId
            return false
        }

        def gwRouter = new GwRouterDTO(id: c.routerId).one()
        if (!gwRouter) {
            log.warn 'no gateway router found, app id: {}, router id: {}', appId, c.routerId
            return false
        }

        boolean needUpdate = false

        // health check refresh
        def healthCheck = gwRouter.service.loadBalancer.healthCheck
        if (healthCheck == null) {
            healthCheck = new GwService.HealthCheck()
            needUpdate = true
        }

        if (healthCheck.path != c.healthCheckPath ||
                healthCheck.interval != c.healthCheckIntervalSeconds ||
                healthCheck.timeout != c.healthCheckTimeoutSeconds) {
            healthCheck.path = c.healthCheckPath
            healthCheck.interval = c.healthCheckIntervalSeconds
            healthCheck.timeout = c.healthCheckTimeoutSeconds

            needUpdate = true
        }

        List<GwLoadBalancer.ServerUrl> serverUrlList = gwRouter.service.loadBalancer.serverUrlList ?: new ArrayList<GwLoadBalancer.ServerUrl>()
        if (!serverUrlList.find { it.url == serverUrl }) {
            serverUrlList << new GwLoadBalancer.ServerUrl(url: serverUrl, weight: weight)
            needUpdate = true
        }

        if (needUpdate) {
            gwRouter.service.loadBalancer.serverUrlList = serverUrlList
            new GwRouterDTO(id: gwRouter.id, service: gwRouter.service, updatedDate: new Date()).update()
        }

        true
    }

    boolean removeServerUrlFromRouter(int appId, GatewayConf c, String nodeIp, int port) {
        def serverUrl = "http://${nodeIp}:${port}".toString()

        def gwCluster = new GwClusterDTO(id: c.clusterId).one()
        if (!gwCluster) {
            log.warn 'no gateway cluster found, app id: {}', appId
            return false
        }

        def gwRouter = new GwRouterDTO(id: c.routerId).one()
        if (!gwRouter) {
            log.warn 'no gateway router found, app id: {}, router id: {}', appId, c.routerId
            return false
        }

        List<GwLoadBalancer.ServerUrl> serverUrlList = gwRouter.service.loadBalancer.serverUrlList
        if (!serverUrlList) {
            return true
        }

        def isRemoved = serverUrlList.removeIf { it.url == serverUrl }
        if (!isRemoved) {
            return true
        }

        new GwRouterDTO(id: gwRouter.id, service: gwRouter.service, updatedDate: new Date()).update()

        true
    }

    boolean refreshServerUrlListToRouter(int appId, GatewayConf c, List<GwLoadBalancer.ServerUrl> serverUrlList) {
        // need do nothing
        // because container state is delay posted by dms agent
        // traefik loadbalancer has health check, is correct, dms server is not

        def gwCluster = new GwClusterDTO(id: c.clusterId).one()
        if (!gwCluster) {
            log.warn 'no gateway cluster found, app id: {}', appId
            return false
        }

        def gwRouter = new GwRouterDTO(id: c.routerId).one()
        if (!gwRouter) {
            log.warn 'no gateway router found, app id: {}, router id: {}', appId, c.routerId
            return false
        }

        List<GwLoadBalancer.ServerUrl> serverUrlListOld = gwRouter.service.loadBalancer.serverUrlList
        // already config server url list
        if (serverUrlListOld) {
            return true
        }

        // health check refresh
        def healthCheck = gwRouter.service.loadBalancer.healthCheck
        if (healthCheck == null) {
            healthCheck = new GwService.HealthCheck()
        }

        healthCheck.path = c.healthCheckPath
        healthCheck.interval = c.healthCheckIntervalSeconds
        healthCheck.timeout = c.healthCheckTimeoutSeconds

        gwRouter.service.loadBalancer.healthCheck = healthCheck
        gwRouter.service.loadBalancer.serverUrlList = serverUrlList
        new GwRouterDTO(id: gwRouter.id, service: gwRouter.service, updatedDate: new Date()).update()

        true
    }

    @Override
    void afterContainerRun(AppDTO app, int instanceIndex, ContainerRunResult result) {
        def c = app.gatewayConf
        if (!c) {
            return
        }

        def abConf = app.abConf
        int weight = abConf && instanceIndex < abConf.containerNumber ? abConf.weight : DEFAULT_WEIGHT

        result.port = result.containerConfig.publicPort(c.containerPrivatePort)

        def isAddOk = addServerUrlToRouter(app.id, c, result, weight)
        String message = "add to cluster ${c.clusterId} router ${c.routerId} ${result.nodeIp}:${result.port}".toString()
        result.keeper.next(JobStepKeeper.Step.addToGateway, 'add real server to gateway', message, isAddOk)
    }

    @Override
    void beforeContainerStop(AppDTO app, ContainerInfo x, JobStepKeeper keeper) {
        def c = app.gatewayConf
        if (!c) {
            return
        }

        def publicPort = x.publicPort(c.containerPrivatePort)
        if (!publicPort) {
            throw new JobProcessException('no public port get for ' + app.name)
        }

        def isRemoveOk = removeServerUrlFromRouter(app.id, c, x.nodeIp, publicPort)
        String message = "remove from cluster ${c.clusterId} router ${c.routerId} ${x.nodeIp}:${publicPort}".toString()
        keeper.next(JobStepKeeper.Step.removeFromGateway, 'remove real server from gateway',
                message, isRemoveOk)
    }

    @Override
    void afterContainerStopped(AppDTO app, ContainerInfo x, boolean flag) {

    }

    @Override
    void refresh(AppDTO app, List<ContainerInfo> runningContainerList) {
        def c = app.gatewayConf
        if (!c) {
            return
        }

        def abConf = app.abConf

        List<GwLoadBalancer.ServerUrl> serverUrlList = []
        for (x in runningContainerList) {
            def publicPort = x.publicPort(c.containerPrivatePort)
            if (!publicPort) {
                throw new JobProcessException('no public port get for ' + app.name)
            }

            int weight = abConf && x.instanceIndex() < abConf.containerNumber ? abConf.weight : DEFAULT_WEIGHT
            serverUrlList << new GwLoadBalancer.ServerUrl(url: "http://${x.nodeIp}:${publicPort}".toString(), weight: weight)
        }

        refreshServerUrlListToRouter(app.id, c, serverUrlList)
    }
}
