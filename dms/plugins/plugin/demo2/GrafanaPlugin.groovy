package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.DirVolumeMount
import model.json.FileVolumeMount
import model.json.PortMapping
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

    @Override
    AppDTO demoApp(AppDTO app) {
        app.name = image()

        def conf = app.conf
        conf.group = group()
        conf.image = image()
        conf.tag = '8.2.6'

        conf.memMB = 256
        conf.cpuFixed = 0.2

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/var/lib/grafana', dist: '/var/lib/grafana', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/var/lib/grafana'))

        conf.fileVolumeList << new FileVolumeMount(
                dist: '/etc/grafana/grafana.ini',
                imageTplId: getImageTplIdByName('grafana.ini.tpl'))
        conf.portList << new PortMapping(privatePort: 3000, publicPort: 3000)

        app
    }
}
