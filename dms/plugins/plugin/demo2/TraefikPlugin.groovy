package plugin.demo2


import ex.JobProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.*
import plugin.BasePlugin
import plugin.PluginManager
import plugin.callback.Observer
import plugin.model.Menu
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.gateway.GatewayOperator
import server.scheduler.checker.HealthChecker
import server.scheduler.checker.HealthCheckerHolder
import server.scheduler.processor.ContainerRunResult
import server.scheduler.processor.JobStepKeeper
import transfer.ContainerInfo
import transfer.ContainerInspectInfo

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
        initHealthChecker()
    }

    private void initImageConfig() {
        '80,81'.split(',').each {
            addPortIfNotExists(it.toString(), it as int)
        }

        final String tplName = 'traefik.toml.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/traefik/TraefikTomlTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('logLevel', 'info', 'string')
        tplParams.addParam('logDir', '/var/log/traefik', 'string')
        tplParams.addParam('serverPort', '80', 'int')
        tplParams.addParam('dashboardPort', '81', 'int')
        tplParams.addParam('prefix', 'traefik', 'string')
        tplParams.addParam('zkAppName', 'zk', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/traefik/traefik.toml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('log-dir', '/var/log/traefik')
    }

    private void initHealthChecker() {
        HealthCheckerHolder.instance.add new HealthChecker() {
            @Override
            String name() {
                'traefik backend server list check'
            }

            @Override
            String imageName() {
                TraefikPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO xx) {
                for (oneApp in InMemoryCacheSupport.instance.appList) {
                    def gatewayConf = oneApp.gatewayConf
                    if (!gatewayConf) {
                        continue
                    }

                    def containerList = InMemoryAllContainerManager.instance.getContainerList(oneApp.clusterId, oneApp.id)
                    def runningContainerList = containerList.findAll { x -> x.running() }

                    // may be there is another job not finished
                    if (oneApp.conf.containerNumber != runningContainerList.size()) {
                        continue
                    }

                    // check gateway
                    def operator = GatewayOperator.create(oneApp.id, gatewayConf)
                    List<String> runningServerUrlList = runningContainerList.findAll { x ->
                        def p = [id: x.id]
                        def r = AgentCaller.instance.agentScriptExeAs(oneApp.clusterId, x.nodeIp,
                                'container inspect', ContainerInspectInfo, p)
                        r.state.running
                    }.collect { x ->
                        def publicPort = x.publicPort(gatewayConf.containerPrivatePort)
                        GatewayOperator.scheme(x.nodeIp, publicPort)
                    }
                    List<String> backendServerUrlList = operator.getBackendServerUrlListFromApi()
                    // gateway container not running yet
                    if (backendServerUrlList == null) {
                        continue
                    }

                    (backendServerUrlList - runningServerUrlList).each {
                        operator.removeBackend(it)
                    }
                    (runningServerUrlList - backendServerUrlList).each {
                        operator.addBackend(it, false)
                    }
                }

                true
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
        app.name = image()

        def conf = app.conf
        conf.group = group()
        conf.image = image()
        conf.tag = 'v1.7.34-alpine'

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/var/log/traefik', dist: '/var/log/traefik', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/var/log/traefik'))

        List<KVPair<String>> paramList = []
        paramList << new KVPair<String>(key: 'logLevel', value: 'info')
        paramList << new KVPair<String>(key: 'logDir', value: '/var/log/traefik')
        paramList << new KVPair<String>(key: 'serverPort', value: '80')
        paramList << new KVPair<String>(key: 'dashboardPort', value: '81')
        paramList << new KVPair<String>(key: 'prefix', value: 'traefik')
        paramList << new KVPair<String>(key: 'zkAppName', value: 'zookeeper')

        conf.fileVolumeList << new FileVolumeMount(
                paramList: paramList,
                dist: '/etc/traefik/traefik.toml',
                imageTplId: getImageTplIdByName('traefik.toml.tpl'))

        conf.portList << new PortMapping(privatePort: 80, publicPort: 80)
        conf.portList << new PortMapping(privatePort: 81, publicPort: 81)

        conf.dependAppIdList << new AppDTO(name: 'zookeeper').queryFields('id').one().id

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

    @Override
    void afterContainerRun(AppDTO app, int instanceIndex, ContainerRunResult result) {
        def c = app.gatewayConf
        if (!c) {
            return
        }

        def abConf = app.abConf
        int w = abConf && instanceIndex < abConf.containerNumber ? abConf.weight :
                GatewayOperator.DEFAULT_WEIGHT

        result.port = result.containerConfig.publicPort(c.containerPrivatePort)

        def isAddOk = GatewayOperator.create(app.id, c).addBackend(result, w)
        String message = "add to cluster ${c.clusterId} frontend ${c.frontendId} ${result.nodeIp}:${result.port}".toString()
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

        def isRemoveOk = GatewayOperator.create(app.id, c).removeBackend(x.nodeIp, publicPort)
        String message = "remove from cluster ${c.clusterId} frontend ${c.frontendId} ${x.nodeIp}:${publicPort}".toString()
        keeper.next(JobStepKeeper.Step.removeFromGateway, 'remove real server from gateway',
                message, isRemoveOk)
    }

    @Override
    void afterContainerStopped(AppDTO app, ContainerInfo x, boolean flag) {

    }

    @Override
    void refresh(AppDTO app, List<ContainerInfo> runningContainerList) {

    }
}
