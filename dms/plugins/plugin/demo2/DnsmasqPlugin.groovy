package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.FileVolumeMount
import model.json.KVPair
import model.json.PortMapping
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
        tplParams.addParam('port', '53', 'int')
        tplParams.addParam('defaultServer', '119.29.29.29,182.254.116.116', 'string')
        tplParams.addParam('consulAppName', 'consul', 'string')
        tplParams.addParam('domain', 'consul', 'string')

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

    @Override
    AppDTO demoApp(AppDTO app) {
        app.name = image()

        def conf = app.conf
        conf.group = group()
        conf.image = image()

        conf.containerNumber = conf.targetNodeIpList.size()

        List<KVPair<String>> paramList = []
        paramList << new KVPair<String>(key: 'port', value: '53')
        paramList << new KVPair<String>(key: 'defaultServer', value: '119.29.29.29,182.254.116.116')
        paramList << new KVPair<String>(key: 'consulAppName', value: 'consul')
        paramList << new KVPair<String>(key: 'domain', value: 'consul')

        conf.fileVolumeList << new FileVolumeMount(
                paramList: paramList,
                dist: '/etc/dnsmasq.conf',
                imageTplId: getImageTplIdByName('dnsmasq.consul.conf.tpl'))

        conf.portList << new PortMapping(privatePort: 53, publicPort: 53)
        conf.portList << new PortMapping(privatePort: 8080, publicPort: 8080)

        conf.dependAppIdList << new AppDTO(name: 'consul').queryFields('id').one().id

        app
    }
}
