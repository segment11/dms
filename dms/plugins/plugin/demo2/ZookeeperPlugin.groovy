package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
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

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/zookeeper/ZooCfgTpl.groovy'
        String content = new File(tplFilePath).text

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
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/zookeeper/conf/zoo.cfg',
                    content: content,
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
                def myidFilePath = hostDirPath + '/myid'

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

            @Override
            String script(CreateContainerConf conf) {
                null
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
}
