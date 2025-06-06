package plugin.demo2

import groovy.transform.CompileStatic
import model.ImagePortDTO
import model.ImageTplDTO
import model.json.TplParamsConf
import plugin.BasePlugin
import plugin.PluginManager

@CompileStatic
class N9ePlugin extends BasePlugin {
    @Override
    String name() {
        'n9e'
    }

    @Override
    void init() {
        super.init()

        initImageConfigIbex()
        initImageConfig()
    }

    private void initImageConfigIbex() {
        // /app/ibex server
        def imageName = 'ulric2019/ibex'

        [2090, 10090, 20090].each {
            def one = new ImagePortDTO(imageName: imageName, port: it).one()
            if (!one) {
                new ImagePortDTO(imageName: imageName, name: it.toString(), port: it).add()
            }
        }

        final String tplName = 'ibex.server.conf.tpl'
        final String tplName2 = 'ibex.agentd.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/n9e/IbexServerConfTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/n9e/IbexAgentdConfTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('host', '192.168.1.100', 'string')
        tplParams.addParam('port', '3306', 'int')
        tplParams.addParam('user', 'root', 'string')
        tplParams.addParam('password', 'root1234', 'string')

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/app/etc/server.conf',
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
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/app/etc/agentd.conf',
                    content: content2,
                    isParentDirMount: false
            ).add()
        }
    }

    private void initImageConfig() {
        // cmd /app/n9e
        // cmd /app/n9e-edge --configs /app/etc/edge/
        def imageName = imageName()

        [17000, 19000].each {
            def one = new ImagePortDTO(imageName: imageName, port: it).one()
            if (!one) {
                new ImagePortDTO(imageName: imageName, name: it.toString(), port: it).add()
            }
        }

        final String tplName = 'config.toml.tpl'
        final String tplName2 = 'edge.toml.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/n9e/ConfigTomlTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/n9e/EdgeTomlTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('host', '192.168.1.100', 'string')
        tplParams.addParam('port', '3306', 'int')
        tplParams.addParam('user', 'root', 'string')
        tplParams.addParam('password', 'root1234', 'string')
        tplParams.addParam('signingKey', '5b94a0fd640fe2765af826acfe42d151', 'string')

        TplParamsConf tplParams2 = new TplParamsConf()
        tplParams2.addParam('centerAddr', 'http://192.168.1.100:17000', 'string')
        tplParams2.addParam('authUser', 'user001', 'string')
        tplParams2.addParam('authPass', 'ccc26da7b9aba533cbb263a36c07dcc5', 'string')
        tplParams2.addParam('prometheusAddr', 'http://192.168.1.100:9090', 'string')

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/app/etc/config.toml',
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
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/app/etc/edge/edge.toml',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams2
            ).add()
        }
    }

    @Override
    String group() {
        'key232323'
    }

    @Override
    String image() {
        'n9e'
    }

    @Override
    String version() {
        'v6'
    }
}
