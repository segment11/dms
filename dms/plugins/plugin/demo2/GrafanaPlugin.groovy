package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
import plugin.BasePlugin
import plugin.PluginManager

@CompileStatic
@Slf4j
class GrafanaPlugin extends BasePlugin {
    @Override
    String name() {
        'grafana'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        addEnvIfNotExists('GF_SECURITY_ADMIN_USER', 'GF_SECURITY_ADMIN_USER')
        addEnvIfNotExists('GF_SECURITY_ADMIN_PASSWORD', 'GF_SECURITY_ADMIN_PASSWORD')

        addPortIfNotExists('3000', 3000)

        final String tplName = 'grafana.ini.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/grafana/GrafanaIniTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/grafana/grafana.ini',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/var/lib/grafana')
    }

    @Override
    String group() {
        'grafana'
    }

    @Override
    String image() {
        'grafana-oss'
    }
}
