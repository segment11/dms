package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
import plugin.BasePlugin
import plugin.PluginManager

@CompileStatic
@Slf4j
class DnsmasqPlugin extends BasePlugin {
    @Override
    String name() {
        'dnsmasq'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        addEnvIfNotExists('HTTP_USER', 'HTTP_USER')
        addEnvIfNotExists('HTTP_PASS', 'HTTP_PASS')

        '53,8080'.split(',').each {
            addPortIfNotExists(it.toString(), it as int)
        }

        final String tplName = 'dnsmasq.consul.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/dnsmasq/DnsmasqConsulConfTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('port', '6363', 'int')
        tplParams.addParam('defaultServer', '119.29.29.29,182.254.116.116', 'string')
        tplParams.addParam('consulAppName', 'consul', 'string')
        tplParams.addParam('domain', 'dms', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/dnsmasq.conf',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }
    }

    @Override
    String group() {
        'jpillora'
    }

    @Override
    String image() {
        'dnsmasq'
    }
}
