package plugin

import com.segment.common.Conf
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

    synchronized void add(Plugin plugin) {
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
        loadPlugin('plugin.demo2.EtcdPlugin', false)
        loadPlugin('plugin.demo2.GrafanaPlugin', false)
        loadPlugin('plugin.demo2.LokiPlugin', false)
        loadPlugin('plugin.demo2.MySQLPlugin', false)
        loadPlugin('plugin.demo2.N9ePlugin', false)
        loadPlugin('plugin.demo2.NodeExporterPlugin', false)
        loadPlugin('plugin.demo2.PatroniPlugin', false)
        loadPlugin('plugin.demo2.PmaPlugin', false)
        loadPlugin('plugin.demo2.PrometheusPlugin', false)
        loadPlugin('plugin.demo2.RedisPlugin', false)
        loadPlugin('plugin.demo2.RedisShakePlugin', false)
        loadPlugin('plugin.demo2.TraefikPlugin', false)
        loadPlugin('plugin.demo2.VectorPlugin', false)
        loadPlugin('plugin.demo2.OpenobservePlugin', false)
        loadPlugin('plugin.demo2.ZookeeperPlugin', false)
    }

    void loadPlugin(String className, boolean isInClasspath = false) {
        if (isInClasspath) {
            def plugin = Class.forName(className).getDeclaredConstructor().newInstance() as Plugin
            add(plugin)
        } else {
            final String dir = pluginsDirPath()
            def filePath = dir + '/' + className.replaceAll(/\./, '/') + '.groovy'
            def file = new File(filePath)
            if (!file.exists()) {
                throw new PluginException('plugin file not found: ' + filePath)
            }

            def gcl = CachedGroovyClassLoader.instance.gcl
            def plugin = gcl.parseClass(file).getDeclaredConstructor().newInstance() as Plugin
            add(plugin)
        }
    }
}
