package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.*
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

    @Override
    AppDTO demoApp(AppDTO app) {
        app.name = image()

        def conf = app.conf
        conf.group = group()
        conf.image = image()
        conf.tag = 'latest-alpine'

        conf.memMB = 64
        conf.cpuShare = 128

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/opt/log', dist: '/opt/log', mode: 'ro',
                nodeVolumeId: getNodeVolumeIdByDir('/opt/log'))
        conf.dirVolumeList << new DirVolumeMount(
                dir: '/var/log', dist: '/var/log', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/var/log'))
        conf.dirVolumeList << new DirVolumeMount(
                dir: '/var/run/docker.sock', dist: '/var/run/docker.sock', mode: 'ro',
                nodeVolumeId: getNodeVolumeIdByDir('/var/run/docker.sock'))

        conf.fileVolumeList << new FileVolumeMount(
                paramList: [new KVPair<String>('zincobserveAppName', 'zincobserve')],
                dist: '/etc/vector/vector.toml',
                imageTplId: getImageTplIdByName('vector.toml.tpl'))

        conf.portList << new PortMapping(privatePort: 8686, publicPort: 8686)

        conf.dependAppIdList << new AppDTO(name: 'zincobserve').queryFields('id').one().id

        app
    }
}
