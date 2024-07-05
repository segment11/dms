package plugin.demo2

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.kevinsawicki.http.HttpRequest
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.GwClusterDTO
import model.ImageTplDTO
import model.json.*
import plugin.BasePlugin
import plugin.PluginManager
import plugin.model.Menu
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

@CompileStatic
@Slf4j
class TraefikPlugin extends BasePlugin {
    @Override
    String name() {
        'traefik'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
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
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/traefik/traefik.yml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('log-dir', '/var/log/traefik')
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
        conf.tag = 'v3.0'

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/var/log/traefik', dist: '/var/log/traefik', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/var/log/traefik'))

        List<KVPair<String>> paramList = []
        paramList << new KVPair<String>(key: 'logLevel', value: 'info')
        paramList << new KVPair<String>(key: 'logDir', value: '/var/log/traefik')
        paramList << new KVPair<String>(key: 'serverPort', value: '80')
        paramList << new KVPair<String>(key: 'dashboardPort', value: '8080')

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

    static JSONObject getServicesJsonObjectFromApi(int clusterId) {
        def gwCluster = new GwClusterDTO(id: clusterId).one()
        if (!gwCluster) {
            return null
        }
        def appOne = InMemoryCacheSupport.instance.oneApp(gwCluster.appId)
        if (!appOne) {
            return null
        }

        def runningContainerList = InMemoryAllContainerManager.instance.getContainerList(appOne.clusterId, appOne.id)
        if (!runningContainerList) {
            return null
        }

        def apiUrl = gwCluster.serverUrl + ':' + gwCluster.dashboardPort

        def body = HttpRequest.get(apiUrl + '/api/http/services').connectTimeout(500).readTimeout(1000).body()
        def jo = JSON.parseObject(body)

        // todo
        def services = jo.getJSONObject('services')
        services
    }
}
