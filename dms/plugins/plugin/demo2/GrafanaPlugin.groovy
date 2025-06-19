package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.json.DirVolumeMount
import model.json.FileVolumeMount
import model.json.PortMapping
import plugin.BasePlugin

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
        initAppBase(app)

        def conf = app.conf
        conf.memMB = 256
        conf.memReservationMB = conf.memMB
        conf.cpuFixed = 0.2

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/var/lib/grafana', dist: '/var/lib/grafana', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/var/lib/grafana'))

        conf.portList << new PortMapping(privatePort: 3000, publicPort: 3000)

        app
    }
}
