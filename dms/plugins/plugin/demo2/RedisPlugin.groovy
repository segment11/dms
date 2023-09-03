package plugin.demo2

import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ha.JedisPoolHolder
import model.*
import model.json.*
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
/**
 * this only support redis master slave + sentinel
 * use segment_kvrocks_controller to manage redis cluster
 * https://github.com/segment11/segment_kvrocks_controller
 */
class RedisPlugin extends BasePlugin {
    @Override
    String name() {
        'redis'
    }

    @Override
    void init() {
        super.init()

        initImageConfig()
        initChecker()
        initExporter()
    }

    private void initImageConfig() {
        // exporter env
        def exporterImageName = 'oliver006/redis_exporter'
        ['REDIS_ADDR', 'REDIS_PASSWORD', 'REDIS_EXPORTER_WEB_LISTEN_ADDRESS'].each {
            def one = new ImageEnvDTO(imageName: exporterImageName, env: it).one()
            if (!one) {
                new ImageEnvDTO(imageName: exporterImageName, name: it, env: it).add()
            }
        }
        // exporter port
        [9121].each {
            def two = new ImagePortDTO(imageName: exporterImageName, port: it).one()
            if (!two) {
                new ImagePortDTO(imageName: exporterImageName, name: it.toString(), port: it).add()
            }
        }

        addPortIfNotExists('6379', 6379)
        addPortIfNotExists('26379', 26379)

        final String tplName = 'redis.conf.tpl'
        final String tplName2 = 'redis.conf.single.node.tpl'
        final String tplName3 = 'sentinel.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/redis/RedisConfTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/redis/RedisConfSingleNodeTpl.groovy'
        String tplFilePath3 = PluginManager.pluginsResourceDirPath() + '/redis/SentinelConfTpl.groovy'
        String content = new File(tplFilePath).text
        String content2 = new File(tplFilePath2).text
        String content3 = new File(tplFilePath3).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('port', '6379', 'int')
        tplParams.addParam('dataDir', '/data/redis', 'string')
        tplParams.addParam('password', '123456', 'string')
        tplParams.addParam('isMasterSlave', 'true', 'string')
        tplParams.addParam('customParameters', 'cluster-enabled no', 'string')

        TplParamsConf tplParams3 = new TplParamsConf()
        tplParams3.addParam('port', '26379', 'int')
        tplParams3.addParam('password', '123456', 'string')
        tplParams3.addParam('isSingleNode', 'false', 'string')
        tplParams3.addParam('redisAppNames', 'redis', 'string')
        tplParams3.addParam('downAfterMs', '30000', 'int')
        tplParams3.addParam('failoverTimeout', '180000', 'int')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/redis/redis.conf',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams).add()
        }

        def one2 = new ImageTplDTO(imageName: imageName, name: tplName2).queryFields('id').one()
        if (!one2) {
            new ImageTplDTO(name: tplName2,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/etc/redis/redis.conf',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams).add()
        }

        def one3 = new ImageTplDTO(imageName: imageName, name: tplName3).queryFields('id').one()
        if (!one3) {
            new ImageTplDTO(name: tplName3,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount.name(),
                    mountDist: '/data/sentinel/${appId}_${instanceIndex}.conf',
                    content: content3,
                    isParentDirMount: true,
                    params: tplParams3).add()
        }

        addNodeVolumeForUpdate('data-dir', '/data/redis')
        addNodeVolumeForUpdate('sentinel-data-dir', '/data/sentinel', '-v /data/sentinel:/data/sentinel')
    }

    private static int getPublicPort(AppConf conf) {
        def port = getParamOneValue(conf, 'port') as int

        int publicPort = port
        if ('host' != conf.networkMode) {
            def pm = conf.portList.find { it.privatePort == port }
            if (pm) {
                publicPort = pm.publicPort
            }
        }
        publicPort
    }

    private static String getParamOneValue(AppConf conf, String key) {
        def mountFileOne = conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
        def paramOne = mountFileOne.paramList.find { it.key == key }
        paramOne.value
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {

            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def sentinelConfOne = conf.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
                def isSentinel = sentinelConfOne != null
                if (isSentinel) {
                    conf.conf.cmd = '[ "sh", "-c", "redis-server /data/sentinel/' + conf.appId + '_' + conf.instanceIndex + '.conf --sentinel" ]'
                } else {
                    conf.conf.cmd = '[ "sh", "-c", "redis-server /etc/redis/redis.conf" ]'
                }

                // check if single node
                boolean isSingleNode
                if (isSentinel) {
                    isSingleNode = sentinelConfOne.paramList.find { it.key == 'isSingleNode' }.value == 'true'
                } else {
                    def confOne = conf.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                    if (confOne) {
                        def tplOne = new ImageTplDTO(id: confOne.imageTplId).one()
                        isSingleNode = tplOne.name.contains('single.node')
                    } else {
                        isSingleNode = false
                    }
                }

                if (!isSingleNode) {
                    return true
                }

                def containerNumber = conf.conf.containerNumber

                List<String> list = []
                conf.conf.dirVolumeList.collect { it.dir }.each { nodeDir ->
                    containerNumber.times { instanceIndex ->
                        list << (nodeDir + '/instance_' + instanceIndex)
                    }
                }
                def dir = list.join(',')
                log.warn 'ready mkdir dirs: {}', dir
                AgentCaller.instance.agentScriptExe(conf.app.clusterId, conf.nodeIp, 'mk dir', [dir: dir])
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'redis single node create sub data dir'
            }

            @Override
            String imageName() {
                RedisPlugin.this.imageName()
            }
        }

        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def sentinelConfThisOne = conf.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
                def isSentinel = sentinelConfThisOne != null
                if (isSentinel) {
                    log.info 'skip redis sentinel init'
                    return true
                }

                def confOne = conf.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                def isMasterSlave = confOne.paramList.find { it.key == 'isMasterSlave' }.value == 'true'
                if (!isMasterSlave) {
                    log.info 'skip redis master slave init as not master slave'
                    return true
                }

                // check if there is a sentinel and do failover
                def sentinelAppList = InMemoryCacheSupport.instance.appList.findAll {
                    (it.conf.group + '/' + it.conf.image == RedisPlugin.this.imageName()) &&
                            it.conf.fileVolumeList.any { fv -> fv.dist.contains('/sentinel') }
                }
                if (sentinelAppList.any {
                    def sentinelConfOne = it.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
                    def redisAppNames = sentinelConfOne.paramList.find { it.key == 'redisAppNames' }.value
                    redisAppNames.split(',').contains(conf.app.name)
                }) {
                    log.info 'skip redis master slave init as there is a sentinel'
                    return true
                }

                // only first container is master
                if (conf.instanceIndex == 0) {
                    log.info 'skip redis master slave init as is the first container'
                    return true
                }

                // check if master is ready
                // if use agent proxy, dms server can not connect agent directly
                def clusterOne = InMemoryCacheSupport.instance.oneCluster(conf.clusterId)
                if (clusterOne.globalEnvConf.proxyNodeIp) {
                    log.warn 'skip redis master slave init as use agent proxy'
                    return true
                }

                def masterNodeIp = conf.nodeIpList[0]
                def port = confOne.paramList.find { it.key == 'port' }.value as int
                if (Utils.isPortListenAvailable(port, masterNodeIp)) {
                    log.warn 'master node {}:{} is not ready, skip redis master slave init', masterNodeIp, port
                    return true
                }

                def tplOne = new ImageTplDTO(id: confOne.imageTplId).one()
                def isSingleNode = tplOne.name.contains('single.node')

                def password = confOne.paramList.find { it.key == 'password' }.value
                def jedisPool = JedisPoolHolder.instance.create(conf.nodeIp, port + (isSingleNode ? conf.instanceIndex : 0), password)
                def isOk = JedisPoolHolder.instance.useRedisPool(jedisPool) { jedis ->
                    def r = jedis.replicaof(masterNodeIp, port)
                    log.info 'redis master slave init result: {}', r
                    'OK' == r
                } as boolean

                isOk
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'redis master slave init'
            }

            @Override
            String imageName() {
                RedisPlugin.this.imageName()
            }

            @Override
            String script(CreateContainerConf conf) {
                return null
            }
        }
    }

    private void initExporter() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf cc, JobStepKeeper keeper) {
                def sentinelConfThisOne = cc.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
                def isSentinel = sentinelConfThisOne != null
                if (isSentinel) {
                    log.info 'skip create exporter as is redis sentinel'
                    return true
                }

                // only last container create exporter application
                if (cc.instanceIndex != cc.conf.containerNumber - 1) {
                    return
                }
                def publicPort = getPublicPort(cc.conf)
                def password = getParamOneValue(cc.conf, 'password')

                def app = new AppDTO()
                app.name = cc.appId + '_' + cc.app.name + '_exporter'

                // check if database name duplicated
                def existsOne = new AppDTO(name: app.name).one()
                if (existsOne) {
                    log.warn('this exporter application name already exists {}', app.name)
                    if (existsOne.conf.containerNumber != cc.conf.containerNumber) {
                        existsOne.conf.containerNumber = cc.conf.containerNumber
                        existsOne.conf.targetNodeIpList = cc.conf.targetNodeIpList

                        new AppDTO(id: existsOne.id, conf: existsOne.conf).update()
                        log.info 'update exporter application container number - {}', existsOne.id
                    }
                    return true
                }

                app.clusterId = cc.app.clusterId
                app.namespaceId = cc.app.namespaceId
                // not auto first
                app.status = AppDTO.Status.manual.val

                def conf = new AppConf()
                app.conf = conf

                def rawConf = cc.app.conf
                // one redis instance -> one redis_exporter application
                conf.containerNumber = cc.conf.containerNumber
                conf.targetNodeIpList = cc.conf.targetNodeIpList
                conf.registryId = rawConf.registryId
                conf.group = 'oliver006'
                conf.image = 'redis_exporter'
                conf.tag = 'latest'
                conf.memMB = 64
                conf.cpuFixed = 0.1
                conf.user = '59000:59000'

                // check if single node
                boolean isSingleNode
                def confOne = cc.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                if (confOne) {
                    def tplOne = new ImageTplDTO(id: confOne.imageTplId).one()
                    isSingleNode = tplOne.name.contains('single.node')
                } else {
                    isSingleNode = false
                }

                String envValue
                if (isSingleNode) {
                    // ${nodeIp} is a placeholder, will be replaced by real ip
                    envValue = "redis://\${nodeIp}:\${${publicPort} + instanceIndex}".toString()
                } else {
                    envValue = "redis://\${nodeIp}:${publicPort}".toString()
                }

                conf.envList << new KVPair<String>('REDIS_ADDR', envValue)
                conf.envList << new KVPair<String>('REDIS_PASSWORD', password)

                final int exporterPort = 9121
                def exporterPublicPort = exporterPort + (6379 - publicPort)
                conf.envList << new KVPair<String>('REDIS_EXPORTER_WEB_LISTEN_ADDRESS', '0.0.0.0:${' + exporterPublicPort + '+instanceIndex}')

                conf.networkMode = 'host'
                if (isSingleNode) {
                    conf.portList << new PortMapping(privatePort: exporterPort, publicPort: exporterPublicPort)
                } else {
                    conf.portList << new PortMapping(privatePort: exporterPort, publicPort: exporterPublicPort)
                }

                // monitor
                def monitorConf = new MonitorConf()
                app.monitorConf = monitorConf
                monitorConf.port = exporterPort
                monitorConf.isHttpRequest = true
                monitorConf.httpRequestUri = '/metrics'

                // add application to dms
                int appId = app.add() as int
                app.id = appId
                log.info 'done create related exporter application {}', appId

                // create dms job
                List<Integer> needRunInstanceIndexList = []
                (0..<conf.containerNumber).each {
                    needRunInstanceIndexList << it
                }
                def job = new AppJobDTO(appId: appId,
                        failNum: 0,
                        status: AppJobDTO.Status.created.val,
                        jobType: AppJobDTO.JobType.create.val,
                        createdDate: new Date(),
                        updatedDate: new Date()).
                        needRunInstanceIndexList(needRunInstanceIndexList)
                int jobId = job.add()
                job.id = jobId

                // set auto so dms can handle this job
                new AppDTO(id: appId, status: AppDTO.Status.auto.val).update()
                log.info 'done create related exporter application start job {}', jobId
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'Redis create exporter application'
            }

            @Override
            String imageName() {
                RedisPlugin.this.imageName()
            }
        }
    }

    @Override
    String group() {
        'library'
    }

    @Override
    String image() {
        'redis'
    }
}
