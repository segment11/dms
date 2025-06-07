package plugin

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.AppConf
import org.apache.commons.io.FileUtils
import org.segment.web.common.CachedGroovyClassLoader
import plugin.model.Menu

import java.util.function.Consumer

@CompileStatic
@Slf4j
abstract class BasePlugin implements Plugin {
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

    static AppDTO tplApp(int clusterId, int namespaceId, List<String> targetNodeIpList, Consumer<AppConf> confUpdater = null) {
        def app = new AppDTO()
        app.clusterId = clusterId
        app.namespaceId = namespaceId
        app.status = AppDTO.Status.auto.val
        app.updatedDate = new Date()

        def conf = new AppConf()
        // default
        conf.memMB = 256
        conf.cpuFixed = 0.2
        conf.cpuShares = 0

        conf.registryId = getRegistryIdByUrl('https://docker.io')
        conf.group = 'library'
        conf.tag = 'latest'

        conf.containerNumber = 1
        conf.targetNodeIpList = targetNodeIpList
        conf.isLimitNode = targetNodeIpList.size() < 3

        conf.networkMode = 'host'

        if (confUpdater) {
            confUpdater.accept(conf)
        }

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

    static String getParamValueFromTpl(AppConf conf, String dist, String key) {
        def mountFileOne = conf.fileVolumeList.find { it.dist == dist }
        if (!mountFileOne) {
            log.warn 'not found mount file - {}', dist
            return null
        }
        def paramOne = mountFileOne.paramList.find { it.key == key }
        paramOne?.value
    }

    static String getEnvValue(AppConf conf, String key) {
        def envOne = conf.envList.find { it.key == key }
        envOne?.value?.toString()
    }

    static AppJobDTO creatingAppJob(AppDTO app) {
        List<Integer> needRunInstanceIndexList = []
        (0..<app.conf.containerNumber).each {
            needRunInstanceIndexList << it
        }

        def job = new AppJobDTO(
                appId: app.id,
                failNum: 0,
                status: AppJobDTO.Status.created.val,
                jobType: AppJobDTO.JobType.create.val,
                createdDate: new Date(),
                updatedDate: new Date()).
                needRunInstanceIndexList(needRunInstanceIndexList)
        int jobId = job.add()
        job.id = jobId

        job
    }

    static int delayRunCreatingAppJob(AppDTO app) {
        def job = creatingAppJob(app)

        // set auto so dms can handle this job
        new AppDTO(id: app.id, status: AppDTO.Status.auto.val).update()
        log.info 'done create related application job, job id: {}', job.id
        job.id
    }

    String imageName() {
        group() + '/' + image()
    }

    @Override
    void init() {
        log.info 'init plugin - {}', name()

        def registryUrl = registry()
        addRegistryIfNotExist(registryUrl.replace('https://', ''), registryUrl)

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

    protected static void evalCtrl(File f) {
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
