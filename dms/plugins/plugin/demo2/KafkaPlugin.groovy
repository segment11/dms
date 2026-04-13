package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.ImageTplDTO
import model.json.TplParamsConf
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.AgentCaller
import server.InMemoryCacheSupport
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class KafkaPlugin extends BasePlugin {
    @Override
    String name() {
        'kafka'
    }

    @Override
    String registry() {
        'https://docker.1ms.run'
    }

    @Override
    String group() {
        'bitnami'
    }

    @Override
    String image() {
        'kafka'
    }

    @Override
    String tag() {
        '2.8.2'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
    }

    private void initImageConfig() {
        addPortIfNotExists('9092', 9092)

        addEnvIfNotExists('KAFKA_BROKER_ID', 'KAFKA_BROKER_ID')
        addEnvIfNotExists('KAFKA_ZOOKEEPER_CONNECT', 'KAFKA_ZOOKEEPER_CONNECT')
        addEnvIfNotExists('KAFKA_LISTENERS', 'KAFKA_LISTENERS')
        addEnvIfNotExists('KAFKA_ADVERTISED_LISTENERS', 'KAFKA_ADVERTISED_LISTENERS')
        addEnvIfNotExists('KAFKA_HEAP_OPTS', 'KAFKA_HEAP_OPTS')

        final String tplName = 'server.properties.tpl'
        final String tplNameUseTemplate = 'server.properties.template.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/kafka/ServerPropertiesTpl.groovy'
        String tplFilePathUseTemplate = PluginManager.pluginsResourceDirPath() + '/kafka/ServerPropertiesUseTemplateTpl.groovy'
        String content = new File(tplFilePath).text
        String contentUseTemplate = new File(tplFilePathUseTemplate).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('port', '9092', 'int')
        tplParams.addParam('dataDir', '/data/kafka/data', 'string')
        tplParams.addParam('brokerId', '0', 'int')
        tplParams.addParam('zkConnectString', 'localhost:2181', 'string')
        tplParams.addParam('zkChroot', '', 'string')
        tplParams.addParam('defaultPartitions', '3', 'int')
        tplParams.addParam('defaultReplicationFactor', '1', 'int')
        tplParams.addParam('brokerCount', '1', 'int')

        TplParamsConf tplParamsUseTemplate = new TplParamsConf()
        tplParamsUseTemplate.addParam('port', '9092', 'int')
        tplParamsUseTemplate.addParam('dataDir', '/data/kafka/data', 'string')
        tplParamsUseTemplate.addParam('brokerId', '0', 'int')
        tplParamsUseTemplate.addParam('zkConnectString', 'localhost:2181', 'string')
        tplParamsUseTemplate.addParam('zkChroot', '', 'string')
        tplParamsUseTemplate.addParam('defaultPartitions', '3', 'int')
        tplParamsUseTemplate.addParam('defaultReplicationFactor', '1', 'int')
        tplParamsUseTemplate.addParam('brokerCount', '1', 'int')
        tplParamsUseTemplate.addParam('configTemplateId', '0', 'int')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(
                    name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/opt/bitnami/kafka/config/server.properties',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams
            ).add()
        }

        def oneUseTemplate = new ImageTplDTO(imageName: imageName, name: tplNameUseTemplate).queryFields('id').one()
        if (!oneUseTemplate) {
            new ImageTplDTO(
                    name: tplNameUseTemplate,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/opt/bitnami/kafka/config/server.properties',
                    content: contentUseTemplate,
                    isParentDirMount: false,
                    params: tplParamsUseTemplate
            ).add()
        }

        addNodeVolumeForUpdate('data-dir', '/data/kafka/data')
        addNodeVolumeForUpdate('logs-dir', '/data/kafka/logs')
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def confOne = conf.conf.fileVolumeList.find {
                    it.dist == '/opt/bitnami/kafka/config/server.properties'
                }
                if (confOne) {
                    def checkPort = confOne.paramValue('port') as int

                    def apps = InMemoryCacheSupport.instance.appList
                    for (otherApp in apps) {
                        if (otherApp.id == conf.appId) continue
                        def otherConf = otherApp.conf
                        if (otherConf.group == 'bitnami' && otherConf.image == 'kafka') {
                            def otherConfOne = otherConf.fileVolumeList.find {
                                it.dist == '/opt/bitnami/kafka/config/server.properties'
                            }
                            if (otherConfOne) {
                                def otherPort = otherConfOne.paramValue('port') as int
                                if (otherPort == checkPort) {
                                    log.warn 'kafka port conflict, port: {}, app: {}', checkPort, otherApp.name
                                    return false
                                }
                            }
                        }
                    }
                }

                List<String> dirList = []
                conf.conf.dirVolumeList.collect { it.dir }.each { nodeDir ->
                    String nodeDirTmp = nodeDir.replace('${appId}', conf.appId.toString())
                    conf.conf.containerNumber.times { instanceIndex ->
                        dirList << (nodeDirTmp.replace('${instanceIndex}', instanceIndex.toString()))
                    }
                }
                if (dirList) {
                    AgentCaller.instance.agentScriptExe(conf.app.clusterId, conf.nodeIp, 'mk dir', [dir: dirList.join(',')])
                }

                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'kafka port conflict check and dir create'
            }

            @Override
            String imageName() {
                KafkaPlugin.this.imageName()
            }
        }
    }

    @Override
    boolean canUseTo(String group, String image) {
        if ('bitnami' == group && 'kafka' == image) {
            return true
        }
        if ('confluentinc' == group && 'cp-kafka' == image) {
            return true
        }

        false
    }
}
