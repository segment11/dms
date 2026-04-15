package plugin.demo2

import com.segment.common.job.chain.JobResult
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.CuratorPoolHolder
import model.AppDTO
import model.ImageTplDTO
import model.KmServiceDTO
import model.json.TplParamsConf
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.AgentCaller
import server.InMemoryCacheSupport
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.checker.HealthChecker
import server.scheduler.checker.HealthCheckerHolder
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
        initHealthChecker()
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
        tplParams.addParam('zkConnectString', 'localhost:2181', 'string')
        tplParams.addParam('zkChroot', '', 'string')
        tplParams.addParam('defaultPartitions', '3', 'int')
        tplParams.addParam('defaultReplicationFactor', '1', 'int')
        tplParams.addParam('brokerCount', '1', 'int')

        TplParamsConf tplParamsUseTemplate = new TplParamsConf()
        tplParamsUseTemplate.addParam('port', '9092', 'int')
        tplParamsUseTemplate.addParam('dataDir', '/data/kafka/data', 'string')
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
                def kafkaConfOne = conf.conf.fileVolumeList.find {
                    it.dist.contains('/opt/bitnami/kafka/config')
                }
                if (kafkaConfOne) {
                    def checkPort = kafkaConfOne.paramValue('port') as int

                    def apps = InMemoryCacheSupport.instance.appList
                    for (otherApp in apps) {
                        if (otherApp.id == conf.appId) continue
                        def otherConf = otherApp.conf
                        if (otherConf.group == 'bitnami' && otherConf.image == 'kafka') {
                            def otherConfOne = otherConf.fileVolumeList.find {
                                it.dist.contains('/opt/bitnami/kafka/config')
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

    private void initHealthChecker() {
        HealthCheckerHolder.instance.add new HealthChecker() {
            @Override
            String name() {
                'kafka broker health check'
            }

            @Override
            String imageName() {
                KafkaPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO app) {
                def kmServiceId = app.extendParams?.get('kmServiceId') as String
                if (!kmServiceId) {
                    log.warn 'kafka health check: kmServiceId not found for app id: {}', app.id
                    return true
                }

                def kmService = new KmServiceDTO(id: kmServiceId as int).one()
                if (!kmService) {
                    log.warn 'kafka health check: kmService not found for id: {}', kmServiceId
                    return true
                }

                def connectionString = kmService.zkConnectString + kmService.zkChroot
                def client = CuratorPoolHolder.instance.create(connectionString)
                try {
                    def brokersPath = '/brokers/ids'
                    if (client.checkExists().forPath(brokersPath) == null) {
                        log.warn 'kafka health check: brokers path not found in zk for kmService: {}', kmService.name
                        return false
                    }

                    def brokerIds = client.getChildren().forPath(brokersPath)
                    if (!brokerIds || brokerIds.size() < kmService.brokers) {
                        log.warn 'kafka health check: registered brokers: {}, expect: {} for kmService: {}',
                                brokerIds ? brokerIds.size() : 0, kmService.brokers, kmService.name
                        return false
                    }

                    log.info 'kafka health check ok: registered brokers: {} for kmService: {}', brokerIds.size(), kmService.name
                    return true
                } catch (Exception e) {
                    log.error 'kafka health check error for kmService: {}', kmService.name, e
                    return false
                }
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
