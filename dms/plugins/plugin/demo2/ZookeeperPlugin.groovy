package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.json.*
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.AgentCaller
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class ZookeeperPlugin extends BasePlugin {
    @Override
    String name() {
        'zookeeper'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    private void initImageConfig() {
        addEnvIfNotExists('JVMFLAGS', 'JVMFLAGS')

        '2181,2888,3888,7000'.split(',').each {
            addPortIfNotExists(it.toString(), it as int)
        }

        final String tplName = 'zoo.cfg.tpl'
        final String tplName2 = 'zoo.cfg.single.node.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/zookeeper/ZooCfgTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/zookeeper/ZooCfgSingleNodeTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('tickTime', '2000', 'int')
        tplParams.addParam('initLimit', '5', 'int')
        tplParams.addParam('syncLimit', '2', 'int')
        tplParams.addParam('dataDir', '/data/zookeeper', 'string')
        tplParams.addParam('isMetricsExport', 'false', 'string')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/zookeeper/conf/zoo.cfg',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        def one2 = new ImageTplDTO(imageName: imageName, name: tplName2).queryFields('id').one()
        if (!one2) {
            new ImageTplDTO(
                    name: tplName2,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/zookeeper/conf/zoo.cfg',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/data/zookeeper')
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                final String tplName = 'zoo.cfg.tpl'
                def mountFileOne = conf.conf.fileVolumeList.find { it.dist == '/zookeeper/conf/zoo.cfg' }
                def param = mountFileOne.paramList.find { it.key == 'dataDir' }

                def mountDirOne = conf.conf.dirVolumeList.find { it.dist == param.value }
                def hostDirPath = mountDirOne.dir
                def myidFilePath = conf.conf.isLimitNode ? (hostDirPath + '/instance_' + conf.instanceIndex + '/myid')
                        : hostDirPath + '/myid'

                AgentCaller.instance.agentScriptExe(conf.clusterId, conf.nodeIp, 'write file content',
                        [filePath: myidFilePath, fileContent: conf.instanceIndex.toString()])
                log.info 'done write myid file: {}, myid: {}, nodeIp: {}', myidFilePath, conf.instanceIndex, conf.nodeIp

                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'zookeeper myid file generate'
            }

            @Override
            String imageName() {
                ZookeeperPlugin.this.imageName()
            }
        }
    }

    @Override
    String group() {
        'key232323'
    }

    @Override
    String image() {
        'zookeeper'
    }

    @Override
    AppDTO demoApp(AppDTO app) {
        initAppBase(app)

        def conf = app.conf
        conf.tag = '3.6.4'

        conf.containerNumber = 3

        conf.dirVolumeList << new DirVolumeMount(
                dir: '/data/zookeeper', dist: '/data/zookeeper', mode: 'rw',
                nodeVolumeId: getNodeVolumeIdByDir('/data/zookeeper'))

        List<KVPair<String>> paramList = []
        paramList << new KVPair<String>('tickTime', '2000')
        paramList << new KVPair<String>('initLimit', '5')
        paramList << new KVPair<String>('syncLimit', '2')
        paramList << new KVPair<String>('dataDir', '/data/zookeeper')
        paramList << new KVPair<String>('isMetricsExport', 'false')

        if (conf.isLimitNode) {
            conf.fileVolumeList << new FileVolumeMount(
                    paramList: paramList,
                    dist: '/zookeeper/conf/zoo.cfg',
                    imageTplId: getImageTplIdByName('zoo.cfg.single.node.tpl'))
        } else {
            conf.fileVolumeList << new FileVolumeMount(
                    paramList: paramList,
                    dist: '/zookeeper/conf/zoo.cfg',
                    imageTplId: getImageTplIdByName('zoo.cfg.tpl'))
        }
        conf.portList << new PortMapping(privatePort: 2181, publicPort: 2181)

        app
    }
}
