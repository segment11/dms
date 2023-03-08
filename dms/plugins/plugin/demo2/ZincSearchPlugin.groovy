package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import plugin.BasePlugin

@CompileStatic
@Slf4j
class ZincSearchPlugin extends BasePlugin {
    @Override
    String name() {
        'zincsearch'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        addEnvIfNotExists('TZ', 'TZ', 'eg. Asia/Shanghai')
        addEnvIfNotExists('DATA_PATH', 'DATA_PATH')
        addEnvIfNotExists('ZINC_FIRST_ADMIN_USER', 'ZINC_FIRST_ADMIN_USER')
        addEnvIfNotExists('ZINC_FIRST_ADMIN_PASSWORD', 'ZINC_FIRST_ADMIN_PASSWORD')
        addEnvIfNotExists('ZINC_PROMETHEUS_ENABLE', 'ZINC_PROMETHEUS_ENABLE')

        addPortIfNotExists('4080', 4080)

        addNodeVolumeForUpdate('data-dir', '/data/zinc-search')
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
        'zinc'
    }
}
