package plugin.demo2

import com.alibaba.fastjson.JSON
import com.github.kevinsawicki.http.HttpRequest
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.TplParamsConf
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.InMemoryAllContainerManager
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.checker.HealthChecker
import server.scheduler.checker.HealthCheckerHolder
import server.scheduler.processor.JobStepKeeper
import transfer.ContainerInfo

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Slf4j
class PrometheusPlugin extends BasePlugin {
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

    private void initImageConfig() {
        addEnvIfNotExists('DATA_DIR', 'DATA_DIR', '--storage.tsdb.path, default /prometheus')
        addPortIfNotExists('9090', 9090)

        final String tplName = 'prometheus.yml.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/prometheus/PrometheusYmlTpl.groovy'
        String content = new File(tplFilePath).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('intervalSecondsGlobal', '15', 'int')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/prometheus/prometheus.yml',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
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

            @Override
            String script(CreateContainerConf conf) {
                null
            }
        }

        HealthCheckerHolder.instance.add new HealthChecker() {

            private AtomicInteger count = new AtomicInteger(0)

            @Override
            String name() {
                'prometheus reload'
            }

            @Override
            String imageName() {
                PrometheusPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO app) {
                def c = count.incrementAndGet()
                if (c % 2 != 0) {
                    return true
                }

                List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(app.clusterId, app.id)
                if (!containerList) {
                    return true
                }
                containerList.each { x ->
                    String url = "http://${x.nodeIp}:${x.publicPort(9090)}/-/reload"
                    log.info 'refresh prometheus reload - {}', HttpRequest.post(url).code()
                }
                true
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
}
