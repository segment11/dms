package plugin

import common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.web.common.CachedGroovyClassLoader

@CompileStatic
@Singleton
@Slf4j
class PluginManager {

    static String pluginsDirPath() {
        Conf.instance.projectPath('/plugins')
    }

    static String pluginsResourceDirPath() {
        Conf.instance.projectPath('/plugins_resources')
    }

    List<Plugin> pluginList = []

    private synchronized void add(Plugin plugin) {
        def name = plugin.name()
        def old = pluginList.find { it.name() == name }
        if (old) {
            pluginList.remove(old)
            log.warn 'plugin {} already exists, will be replaced', name
        }

        plugin.init()
        pluginList << plugin
    }

    void loadDemo() {
        loadPlugin('plugin.demo.ConsulPlugin', true)
        loadPlugin('plugin.demo2.DnsmasqPlugin', false)
        loadPlugin('plugin.demo2.ZookeeperPlugin', false)
        loadPlugin('plugin.demo2.PrometheusPlugin', false)
        loadPlugin('plugin.demo2.ZincSearchPlugin', false)
        loadPlugin('plugin.demo2.FilebeatPlugin', false)
        loadPlugin('plugin.demo2.EtcdPlugin', false)
        loadPlugin('plugin.demo2.MySQLPlugin', false)
    }

    void loadPlugin(String className, boolean isInClasspath = false) {
        if (isInClasspath) {
            def plugin = Class.forName(className).newInstance() as Plugin
            add(plugin)
        } else {
            final String dir = pluginsDirPath()
            def filePath = dir + '/' + className.replaceAll(/\./, '/') + '.groovy'
            def file = new File(filePath)
            if (!file.exists()) {
                throw new PluginException('plugin file not found: ' + filePath)
            }

            def gcl = CachedGroovyClassLoader.instance.gcl
            def plugin = gcl.parseClass(file).newInstance() as Plugin
            add(plugin)
        }
    }
}
