package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
import plugin.BasePlugin
import plugin.PluginManager

@CompileStatic
@Slf4j
class RedisShakePlugin extends BasePlugin {
    @Override
    String name() {
        'redis-shake'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    final String tplName = 'redis-shake.toml.tpl'

    private void initImageConfig() {
        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/redis/RedisShakeTomlTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        // sync or scan
        tplParams.addParam('type', 'sync', 'string')
        tplParams.addParam('srcAddress', 'localhost:6379', 'string')
        tplParams.addParam('srcUsername', '', 'string')
        tplParams.addParam('srcPassword', '', 'string')
        tplParams.addParam('targetType', 'standalone', 'string')
        tplParams.addParam('targetAddress', 'localhost:6379', 'string')
        tplParams.addParam('targetUsername', '', 'string')
        tplParams.addParam('targetPassword', '', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/redis-shake.toml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }
    }

    @Override
    String group() {
        'montplex'
    }

    @Override
    String image() {
        'redis-shake'
    }

    @Override
    String version() {
        '4.4.0'
    }
}
