package plugin

import common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Singleton
@Slf4j
class PluginManager {
    List<Plugin> pluginList = []

    void loadDemo() {
        loadPlugin('plugin.demo.Consul', true)
    }

    void loadPlugin(String className, boolean isInClasspath = false) {
        if (isInClasspath) {
            def plugin = Class.forName(className).newInstance() as Plugin
            def name = plugin.name()
            def old = pluginList.find { it.name() == name }
            if (old) {
                pluginList.remove(old)
                log.warn 'plugin {} already exists, will be replaced', name
            }

            plugin.init()
            pluginList << plugin
        } else {
            final String dir = Conf.instance.projectPath('/plugins')
            def filePath = dir + '/' + className.replaceAll(/\./, '/')
            def file = new File(filePath)
            if (!file.exists()) {
                throw new PluginException('plugin file not found: ' + filePath)
            }
            // todo: load plugin
        }
    }
}
