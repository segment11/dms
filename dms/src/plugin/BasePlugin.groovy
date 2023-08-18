package plugin

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.AppConf
import org.apache.commons.io.FileUtils
import org.segment.d.D
import org.segment.web.common.CachedGroovyClassLoader
import plugin.model.Menu

@CompileStatic
@Slf4j
abstract class BasePlugin implements Plugin {
    private D d

    void setD(D d) {
        this.d = d
    }

    protected void addEnvIfNotExists(String name, String envName, String des = null) {
        def imageName = imageName()
        def one = new ImageEnvDTO(imageName: imageName, env: envName).one()
        if (!one) {
            new ImageEnvDTO(imageName: imageName, name: name, env: envName, des: des).add()
        }
    }

    protected void addPortIfNotExists(String name, int port, String des = null) {
        def imageName = imageName()
        def one = new ImagePortDTO(imageName: imageName, port: port).one()
        if (!one) {
            new ImagePortDTO(imageName: imageName, name: name, port: port, des: des).add()
        }
    }

    protected void addNodeVolumeForUpdate(String name, String dir, String des = null) {
        def imageName = imageName()
        def one = new NodeVolumeDTO(imageName: imageName, dir: dir).one()
        if (!one) {
            new NodeVolumeDTO(imageName: imageName, name: name, dir: dir, clusterId: 1, des: des).add()
        }
    }

    static int addRegistryIfNotExist(String name, String url) {
        def registryOne = new ImageRegistryDTO(url: url).one()
        if (!registryOne) {
            return new ImageRegistryDTO(name: name, url: url).add()
        } else {
            return registryOne.id
        }
    }

    static AppDTO tplApp(int clusterId, int namespaceId, List<String> targetNodeIpList) {
        def app = new AppDTO()
        app.clusterId = clusterId
        app.namespaceId = namespaceId
        app.status = AppDTO.Status.auto.val
        app.updatedDate = new Date()

        def conf = new AppConf()
        // default
        conf.memMB = 256
        conf.cpuFixed = 0.2

        conf.registryId = getRegistryIdByUrl('https://docker.io')
        conf.group = 'library'
        conf.tag = 'latest'

        conf.containerNumber = 1
        conf.targetNodeIpList = targetNodeIpList
        conf.isLimitNode = targetNodeIpList.size() < 3

        conf.networkMode = 'host'

        app.conf = conf
        app
    }

    static int getRegistryIdByUrl(String url) {
        def registryOne = new ImageRegistryDTO(url: url).one()
        if (!registryOne) {
            return 0
        } else {
            return registryOne.id
        }
    }

    int getNodeVolumeIdByDir(String dir) {
        def one = new NodeVolumeDTO(imageName: imageName(), dir: dir).one()
        if (!one) {
            return 0
        } else {
            return one.id
        }
    }

    int getImageTplIdByName(String name) {
        def one = new ImageTplDTO(imageName: imageName(), name: name).one()
        if (!one) {
            return 0
        } else {
            return one.id
        }
    }

    String imageName() {
        group() + '/' + image()
    }

    @Override
    void init() {
        log.info 'init plugin - {}', name()

        def registryUrl = registry()
        def one = new ImageRegistryDTO(url: registryUrl).one()
        if (!one) {
            new ImageRegistryDTO(name: registryUrl, url: registryUrl).add()
        }

        initWww()
        initCtrl()
    }

    protected void initWww() {
        def pluginResourceDirPath = PluginManager.pluginsResourceDirPath() + '/' + name()
        def wwwPagesDir = new File(pluginResourceDirPath + '/www/pages')
        if (wwwPagesDir.exists()) {
            def wwwDir = new File(Conf.instance.projectPath('/www/admin/pages'))
            wwwPagesDir.listFiles().each { f ->
                FileUtils.copyDirectory(f, new File(wwwDir, f.name))
                log.info 'copy www page dir - {}', f.name
            }
        }
    }

    protected void initCtrl() {
        def ctrlPluginDir = new File(PluginManager.pluginsDirPath() + '/ctrl/' + name())
        if (!ctrlPluginDir.exists() || !ctrlPluginDir.isDirectory()) {
            return
        }

        def isServerRuntimeJar = Conf.instance.isOn('server.runtime.jar')
        if (!isServerRuntimeJar) {
            def ctrlDestDir = new File(Conf.instance.projectPath('/src/ctrl/' + name()))
            if (!ctrlDestDir.exists()) {
                ctrlDestDir.mkdir()
            }

            // need rename to avoid compile conflict in dev mode
            ctrlPluginDir.listFiles().each { f ->
                // only one level dir
                if (f.isDirectory()) {
                    return
                }

                FileUtils.copyFile(f, new File(ctrlDestDir, 'Copy' + f.name))
            }
            return
        }

        ctrlPluginDir.listFiles().each { f ->
            if (f.isDirectory()) {
                f.eachFileRecurse { ff ->
                    if (ff.name.endsWith(CachedGroovyClassLoader.GROOVY_FILE_EXT)) {
                        evalCtrl(ff)
                    }
                }
            } else {
                if (f.name.endsWith(CachedGroovyClassLoader.GROOVY_FILE_EXT)) {
                    evalCtrl(f)
                }
            }
        }
    }

    protected void evalCtrl(File f) {
        try {
            def clz = CachedGroovyClassLoader.instance.gcl.parseClass(f)
            def script = clz.getDeclaredConstructor().newInstance()
            if (script instanceof Script) {
                script.run()
            } else {
                log.warn 'ctrl plugin file must be a groovy script - {}', f.name
            }
        } catch (Exception e) {
            log.error 'eval ctrl plugin file error - {}', f.name, e
        }
    }

    @Override
    void destroy() {
        log.info 'destroy plugin - {}', name()
    }

    @Override
    String name() {
        this.getClass().simpleName
    }

    @Override
    String version() {
        '1.0.0'
    }

    @Override
    String registry() {
        'https://docker.io'
    }

    @Override
    String group() {
        'library'
    }

    @Override
    String image() {
        throw new RuntimeException('not implement')
    }

    @Override
    String tag() {
        'latest'
    }

    @Override
    Map<String, String> expressions() {
        HashMap<String, String> r = [:]

        def dirPath = PluginManager.pluginsResourceDirPath() +
                "/${name().replaceAll(' ', '_')}/expression".toString()
        def dir = new File(dirPath)
        if (dir.exists()) {
            for (file in dir.listFiles()) {
                r[file.name] = file.text
            }
        }
        r
    }

    private Date loadTimeInner = new Date()

    @Override
    Date loadTime() {
        loadTimeInner
    }

    @Override
    AppDTO demoApp(AppDTO app) {
        app
    }

    @Override
    List<Menu> menus() {
        return null
    }
}
