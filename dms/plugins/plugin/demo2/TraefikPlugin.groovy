package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
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
}
