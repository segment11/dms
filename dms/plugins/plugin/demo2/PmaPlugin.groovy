package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import plugin.BasePlugin

@CompileStatic
@Slf4j
class PmaPlugin extends BasePlugin {
    @Override
    String name() {
        // php my admin
        'pma'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
    }

    private void initImageConfig() {
        addEnvIfNotExists('PMA_HOST', 'PMA_HOST')

        addPortIfNotExists('80', 80)
    }

    @Override
    String group() {
        'phpmyadmin'
    }

    @Override
    String image() {
        'phpmyadmin'
    }
}
