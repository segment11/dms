package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
import plugin.BasePlugin
import plugin.PluginManager

@CompileStatic
@Slf4j
class EtcdPlugin extends BasePlugin {
    @Override
    String name() {
        'etcd'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        '2379,2380'.split(',').each {
            addPortIfNotExists(it.toString(), it as int)
        }

        final String tplName = 'etcd.yml.tpl'
        final String tplName2 = 'etcd.yml.single.node.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/etcd/EtcdYmlTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/etcd/EtcdYmlSingleNodeTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('enableV2', 'true', 'string')
        tplParams.addParam('dataDir', '/data/etcd', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etcd/etcd.yml',
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
                    mountDist: '/etcd/etcd.yml',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('/data/etcd', '/data/etcd')
    }

    @Override
    String group() {
        'key232323'
    }

    @Override
    String image() {
        'etcd'
    }
}
