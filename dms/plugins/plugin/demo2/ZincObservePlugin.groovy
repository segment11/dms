package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import plugin.BasePlugin

@CompileStatic
@Slf4j
class ZincObservePlugin extends BasePlugin {
    @Override
    String name() {
        'zincobserve'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        addEnvIfNotExists('TZ', 'TZ', 'eg. Asia/Shanghai')
        addEnvIfNotExists('ZO_DATA_DIR', 'ZO_DATA_DIR', 'default, /data')
        addEnvIfNotExists('ZO_ROOT_USER_EMAIL', 'ZO_ROOT_USER_EMAIL')
        addEnvIfNotExists('ZO_ROOT_USER_PASSWORD', 'ZO_ROOT_USER_PASSWORD')

        addPortIfNotExists('5080', 5080)

        addNodeVolumeForUpdate('data-dir', '/data/zinc-observe',
                'need mount to /data same value as env ZO_DATA_DIR')
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
        'zincobserve'
    }
}
