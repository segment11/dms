package plugin.demo2

import com.alibaba.fastjson.JSON
import com.github.kevinsawicki.http.HttpRequest
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.*
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import plugin.callback.ConfigFileReloaded
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class PrometheusPlugin extends BasePlugin implements ConfigFileReloaded {
    @Override
    String name() {
        'prometheus'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    final String tplName = 'prometheus.yml.tpl'
    final String tplNameRedisExporter = 'prometheus.redis.exporter.yml.tpl'

    private void initImageConfig() {
        addEnvIfNotExists('DATA_DIR', 'DATA_DIR', '--storage.tsdb.path, default /prometheus')
        addPortIfNotExists('9090', 9090)

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/prometheus/PrometheusYmlTpl.groovy'
        String content = new File(tplFilePath).text

        String tplFilePathRedisExporter = PluginManager.pluginsResourceDirPath() + '/prometheus/PrometheusRedisExporterYmlTpl.groovy'
        String contentRedisExporter = new File(tplFilePathRedisExporter).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('intervalSecondsGlobal', '15', 'int')

        TplParamsConf tplParams2 = new TplParamsConf()
        tplParams2.addParam('intervalSecondsGlobal', '15', 'int')
        tplParams2.addParam('eachContainerChargeRedisServerCount', '100', 'int')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/prometheus/prometheus.yml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        def two = new ImageTplDTO(imageName: imageName, name: tplNameRedisExporter).queryFields('id').one()
        if (!two) {
            new ImageTplDTO(
                    name: tplNameRedisExporter,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/prometheus/prometheus.yml',
                    content: contentRedisExporter,
                    isParentDirMount: false,
                    params: tplParams2
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/prometheus', '--storage.tsdb.path, default /prometheus')
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def envOne = conf.conf.envList.find { it.key == 'DATA_DIR' }

                List<String> cmdArgs = []
                cmdArgs << '--config.file=/etc/prometheus/prometheus.yml'
                cmdArgs << '--storage.tsdb.path=' + (envOne ? envOne.value.toString() : '/prometheus')
                cmdArgs << '--web.console.libraries=/usr/share/prometheus/console_libraries'
                cmdArgs << '--web.console.templates=/usr/share/prometheus/consoles'
                cmdArgs << '--web.enable-lifecycle'

//                cmdArgs << '--enable-feature=remote-write-receiver'
//                cmdArgs << '--query.lookback-delta=2m'

                conf.conf.cmd = JSON.toJSONString(cmdArgs)
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'prometheus cmd reloadable generate'
            }

            @Override
            String imageName() {
                PrometheusPlugin.this.imageName()
            }
        }
    }

    @Override
    String group() {
        'prom'
    }

    @Override
    String image() {
        'prometheus'
    }

    String configTplName = tplName
    String nodeDir

    @Override
    AppDTO demoApp(AppDTO app) {
        initAppBase(app)

        def conf = app.conf
        conf.tag = 'v2.25.0'

        if (conf.memMB == 0) {
            // set default
            conf.memMB = 512
            conf.memReservationMB = conf.memMB
            conf.cpuShares = 512
        }

        if (!nodeDir) {
            nodeDir = '/data/openobserve'
        }

        conf.dirVolumeList << new DirVolumeMount(
                dir: nodeDir, dist: '/prometheus', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/prometheus'))

        conf.fileVolumeList << new FileVolumeMount(
                isReloadInterval: true,
                paramList: [new KVPair<String>('intervalSecondsGlobal', '15')],
                dist: '/etc/prometheus/prometheus.yml',
                imageTplId: getImageTplIdByName(configTplName))

        if (configTplName == tplNameRedisExporter) {
            conf.fileVolumeList[0].paramList << new KVPair<String>('eachContainerChargeRedisServerCount', '100')
        }

        if (!conf.portList) {
            conf.portList << new PortMapping(privatePort: 9090, publicPort: 9090)
        }

        app
    }

    @Override
    void reloaded(AppDTO app, ContainerInfo x, List<String> changedDistList) {
        String url = "http://${x.nodeIp}:${x.publicPort(9090)}/-/reload"
        def code = HttpRequest.post(url).code()
        log.info 'refresh prometheus reload - {}', code
    }
}
