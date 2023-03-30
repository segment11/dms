package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
import plugin.BasePlugin
import plugin.PluginManager

@CompileStatic
@Slf4j
class VectorPlugin extends BasePlugin {
    @Override
    String name() {
        'vector'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        // sh -c "vector -c /etc/vector/*.toml -w /etc/vector/*.toml"
        addPortIfNotExists('8686', 8686)

        final String tplName = 'vector.toml.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/vector/VectorTomlTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('zincobserveAppName', 'zincobserve', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/vector/vector.toml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('var-log-dir', '/var/log', 'host log dir, eg. /var/log')
        addNodeVolumeForUpdate('opt-log-dir', '/opt/log', 'host log dir, eg. /opt/log')
        addNodeVolumeForUpdate('docker-sock', '/var/run/docker.sock', '/var/run/docker.sock')
    }

    @Override
    String group() {
        'timberio'
    }

    @Override
    String image() {
        'vector'
    }
}
