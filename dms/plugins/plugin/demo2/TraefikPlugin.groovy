package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.*
import plugin.BasePlugin
import plugin.PluginManager

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
}
