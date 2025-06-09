package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.json.DirVolumeMount
import model.json.KVPair
import model.json.PortMapping
import plugin.BasePlugin

@CompileStatic
@Slf4j
class OpenobservePlugin extends BasePlugin {
    @Override
    String name() {
        'openobserve'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        addEnvIfNotExists('TZ', 'TZ', 'eg. Asia/Shanghai')
        addEnvIfNotExists('ZO_DATA_DIR', 'ZO_DATA_DIR', 'default, /data/openobserve')
        addEnvIfNotExists('ZO_ROOT_USER_EMAIL', 'ZO_ROOT_USER_EMAIL')
        addEnvIfNotExists('ZO_ROOT_USER_PASSWORD', 'ZO_ROOT_USER_PASSWORD')

        addPortIfNotExists('5080', 5080)

        addNodeVolumeForUpdate('data-dir', '/data/openobserve',
                'need mount to /data/openobserve same value as env ZO_DATA_DIR')
    }

    @Override
    String registry() {
        'https://public.ecr.aws'
    }

    @Override
    String group() {
        'zinclabs'
    }

    @Override
    String image() {
        'openobserve'
    }

    String nodeDir

    @Override
    AppDTO demoApp(AppDTO app) {
        initAppBase(app)

        def conf = app.conf
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
                dir: nodeDir, dist: '/data/openobserve', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir(nodeDir))

        conf.portList << new PortMapping(privatePort: 5080, publicPort: 5080)

        conf.envList << new KVPair<String>('ZO_ROOT_USER_EMAIL', 'admin@163.com')
        conf.envList << new KVPair<String>('ZO_ROOT_USER_PASSWORD', 'admin@pass')

        app
    }
}
