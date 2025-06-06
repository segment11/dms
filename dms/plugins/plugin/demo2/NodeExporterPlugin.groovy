package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.json.DirVolumeMount
import model.json.MonitorConf
import model.json.PortMapping
import model.server.CreateContainerConf
import plugin.BasePlugin
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class NodeExporterPlugin extends BasePlugin {
    @Override
    String name() {
        'node exporter'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    private void initImageConfig() {
        // --path.rootfs=/host
        addPortIfNotExists('9100', 9100)
        addNodeVolumeForUpdate('host-dir', '/', 'dist: /host')
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def oldApp = new AppDTO(id: conf.app.id).queryFields('monitor_conf').one()
                if (!oldApp.monitorConf) {
                    // monitor
                    def monitorConf = new MonitorConf()
                    monitorConf.port = 9100
                    monitorConf.isHttpRequest = true
                    monitorConf.httpRequestUri = '/metrics'

                    new AppDTO(id: conf.app.id, monitorConf: monitorConf).update()
                    log.info 'update monitor conf'
                }
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'update monitor conf'
            }

            @Override
            String imageName() {
                NodeExporterPlugin.this.imageName()
            }
        }
    }

    @Override
    String group() {
        'prom'
    }

    @Override
    String image() {
        'node-exporter'
    }

    @Override
    AppDTO demoApp(AppDTO app) {
        app.name = image()

        def conf = app.conf
        conf.group = group()
        conf.image = image()

        conf.cmd = '--path.rootfs=/host'

        conf.memMB = 128
        conf.memReservationMB = conf.memMB

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/', dist: '/host', mode: 'ro',
                nodeVolumeId: getNodeVolumeIdByDir('/'))

        conf.portList << new PortMapping(privatePort: 9100, publicPort: 9100)

        app
    }
}
