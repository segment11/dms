package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
import plugin.BasePlugin
import plugin.PluginManager

@CompileStatic
@Slf4j
@Deprecated
class FilebeatPlugin extends BasePlugin {
    @Override
    String name() {
        'filebeat'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        final String tplName = 'filebeat.yml.tpl'
        final String tplName2 = 'filebeat.app.yml.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/filebeat/FilebeatYmlTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/filebeat/FilebeatAppYmlTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('esHost', 'localhost', 'string')
        tplParams.addParam('esPort', '4080', 'int')
        tplParams.addParam('adminPassword', 'Complexpass#123', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/filebeat/filebeat.yml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }
        def one2 = new ImageTplDTO(imageName: imageName, name: tplName2).queryFields('id').one()
        if (!one2) {
            new ImageTplDTO(
                    name: tplName2,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/filebeat/conf.d/dyn-reloadable-apps.yml',
                    content: content2,
                    isParentDirMount: false,
                    params: new TplParamsConf()
            ).add()
        }

        addNodeVolumeForUpdate('var-log-dir', '/var/log', 'host log dir, eg. /var/log')
        addNodeVolumeForUpdate('opt-log-dir', '/opt/log', 'host log dir, eg. /opt/log')
    }

    @Override
    String group() {
        'key232323'
    }

    @Override
    String image() {
        'filebeat'
    }
}
