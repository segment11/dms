package plugin.demo2

import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ha.JedisPoolHolder
import model.AppDTO
import model.ImageEnvDTO
import model.ImagePortDTO
import model.ImageTplDTO
import model.json.*
import model.server.CreateContainerConf
import plugin.BasePlugin
import plugin.PluginManager
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.checker.HealthChecker
import server.scheduler.checker.HealthCheckerHolder
import server.scheduler.processor.JobStepKeeper
import transfer.ContainerInfo

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
        ['REDIS_ADDR', 'REDIS_PASSWORD', 'REDIS_EXPORTER_WEB_LISTEN_ADDRESS', 'X-env-public-port9121'].each {
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
        tplParams.addParam('sentinelAppName', 'sentinel', 'string')
        tplParams.addParam('customParameters', 'cluster-enabled no', 'string')

        TplParamsConf tplParams2 = new TplParamsConf()
        tplParams2.addParam('port', '6379', 'int')
        tplParams2.addParam('dataDir', '/data/redis', 'string')
        tplParams2.addParam('password', '123456', 'string')
        tplParams2.addParam('customParameters', 'cluster-enabled no', 'string')

        TplParamsConf tplParams3 = new TplParamsConf()
        tplParams3.addParam('port', '26379', 'int')
        tplParams3.addParam('password', '123456', 'string')
        tplParams3.addParam('isSingleNode', 'false', 'string')
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
                    params: tplParams2).add()
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

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
//                if ('host' != conf.conf.networkMode) {
//                    return true
//                }

                def conf0 = conf.conf
                def confOne = conf0.fileVolumeList.find {
                    it.dist.contains('/sentinel') || it.dist == '/etc/redis/redis.conf'
                }
                def checkPort = confOne.paramValue('port') as int

                for (otherApp in InMemoryCacheSupport.instance.appList) {
                    // exclude self
                    if (otherApp.id == conf.appId) {
                        continue
                    }

                    // just check redis
                    def conf1 = otherApp.conf
                    if (conf1.group == conf0.group && conf1.image == conf0.image) {
                        def otherConfOne = conf1.fileVolumeList.find {
                            it.dist.contains('/sentinel') || it.dist == '/etc/redis/redis.conf'
                        }
                        if (otherConfOne) {
                            def otherPort = otherConfOne.paramValue('port') as int
                            if (otherPort == checkPort) {
                                log.warn 'port {} is already used, app id: {}, app name: {}', checkPort, otherApp.id, otherApp.name
                                return false
                            }
                        }
                    }
                }

//                if (!Utils.isPortListenAvailable(checkPort, conf.nodeIp)) {
//                    log.warn 'port {} is not available, node ip: {}', checkPort, conf.nodeIp
//                    return false
//                }

                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.before
            }

            @Override
            String name() {
                'redis port conflict check'
            }

            @Override
            String imageName() {
                RedisPlugin.this.imageName()
            }
        }

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
                    isSingleNode = sentinelConfOne.paramValue('isSingleNode') == 'true'
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
                    log.info 'it is redis sentinel, skip init master slave'
                    return true
                }

                // if use agent proxy, dms server can not connect agent directly
                def clusterOne = InMemoryCacheSupport.instance.oneCluster(conf.clusterId)
                if (clusterOne.globalEnvConf.proxyInfoList) {
                    log.warn 'this cluster is using agent proxy, skip'
                    return true
                }

                def confOne = conf.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                def isMasterSlave = confOne.paramValue('isMasterSlave') == 'true'
                if (!isMasterSlave) {
                    log.info 'it is not master slave mode, skip init master slave'
                    return true
                }

                def primaryNodeIp = conf.nodeIpList[0]
                def redisPort = confOne.paramValue('port') as int
                def primaryPort = redisPort
                def redisPassword = confOne.paramValue('password') as String

                def thisInstanceNodeIp = conf.nodeIp
                def tplOne = new ImageTplDTO(id: confOne.imageTplId).one()
                def isSingleNode = tplOne.name.contains('single.node')
                def thisInstanceRedisPort = redisPort + (isSingleNode ? conf.instanceIndex : 0)

                // get master address from sentinel or just use first node ip and port
                Set<String> multiMasterAddressSet = []
                Set<String> replicaAddressSet = []
                String masterAddress
                def sentinelAppName = confOne.paramValue('sentinelAppName')
                if (sentinelAppName) {
                    def sentinelAppOne = InMemoryCacheSupport.instance.appList.find {
                        it.clusterId == conf.clusterId && it.name == sentinelAppName
                    }
                    if (!sentinelAppOne) {
                        log.warn 'sentinel application not exists, {}', sentinelAppName
                        return false
                    }

                    def masterName = 'redis-app-' + conf.appId
                    def quorum = (conf.nodeIpList.size() / 2).intValue() + 1

                    def sentinelConfOne = sentinelAppOne.conf.fileVolumeList.find {
                        it.dist.contains('/sentinel')
                    }

                    Map<String, String> sentinelParams = [:]
                    sentinelParams['down-after-milliseconds'] = sentinelConfOne.paramValue('downAfterMs').toString()
                    sentinelParams['failover-timeout'] = sentinelConfOne.paramValue('failoverTimeout').toString()
                    sentinelParams['parallel-syncs'] = '1'
                    if (redisPassword) {
                        sentinelParams['auth-pass'] = redisPassword
                    }

                    def sentinelPort = sentinelConfOne.paramValue('port') as int
                    def sentinelPassword = sentinelConfOne.paramValue('password') as String
                    def isSentinelSingleNode = 'true' == sentinelConfOne.paramValue('isSingleNode')

                    // add to sentinel
                    def containerList = InMemoryAllContainerManager.instance.getContainerList(0, sentinelAppOne.id)
                    for (x in containerList) {
                        if (!x.running()) {
                            continue
                        }

                        def finalSentinelPort = sentinelPort + (isSentinelSingleNode ? x.instanceIndex() : 0)
                        def jedisPool = JedisPoolHolder.instance.create(x.nodeIp, finalSentinelPort, sentinelPassword)
                        JedisPoolHolder.instance.useRedisPool(jedisPool) { jedis ->
                            def infoSentinel = jedis.info('sentinel')
                            if (infoSentinel.contains(masterName + ',')) {
                                log.info 'sentinel monitor already added, instance index: {}, master name: {}', x.instanceIndex(), masterName
                                def masterAddressAlreadyAdded = jedis.sentinelGetMasterAddrByName(masterName).join(':')
                                multiMasterAddressSet << masterAddressAlreadyAdded
                                if (!masterAddress) {
                                    masterAddress = masterAddressAlreadyAdded
                                }

                                for (sr in jedis.sentinelReplicas(masterName)) {
                                    def ip = sr.ip
                                    def port = sr.port as int
                                    def replicaAddress = ip + ':' + port
                                    replicaAddressSet << replicaAddress
                                }
                            } else {
                                if (!masterAddress) {
                                    masterAddress = primaryNodeIp + ':' + primaryPort
                                }
                                multiMasterAddressSet << masterAddress

                                def r = jedis.sentinelMonitor(masterName, primaryNodeIp, primaryPort, quorum)
                                log.info 'sentinel monitor, instance index: {}, master name: {}, result: {}',
                                        x.instanceIndex(), masterName, r
                                def r2 = jedis.sentinelSet(masterName, sentinelParams)
                                log.info 'sentinel set, instance index: {}, master name: {}, result: {}, params: {}',
                                        x.instanceIndex(), masterName, r2, sentinelParams
                            }
                        }
                    }
                }

                if (multiMasterAddressSet.size() > 1) {
                    log.warn 'multi master address, master address list: {}, will use first one: {}', multiMasterAddressSet, masterAddress
                }

                String masterNodeIp
                int masterPort
                if (!masterAddress) {
                    masterNodeIp = primaryNodeIp
                    masterPort = primaryPort
                } else {
                    def arr = masterAddress.split(':')
                    masterNodeIp = arr[0]
                    masterPort = arr[1] as int
                }

                // check if master is ready
                if (Utils.isPortListenAvailable(masterPort, masterNodeIp)) {
                    log.warn 'master node {}:{} is not ready, skip init master slave', masterNodeIp, masterPort
                    return true
                }

                def jedisPool = JedisPoolHolder.instance.create(thisInstanceNodeIp, thisInstanceRedisPort, redisPassword)
                def isOk = JedisPoolHolder.instance.useRedisPool(jedisPool) { jedis ->
                    def infoReplication = jedis.info('replication')
                    def lines = infoReplication.readLines()
                    if (lines.find { it.contains('role:slave') }) {
                        log.warn 'it is slave, node ip: {}, port: {}', thisInstanceNodeIp, thisInstanceRedisPort
                        // master may be not target master, so check it
                        def lineMasterHost = lines.find { it.contains('master_host:') }
                        def thisInstanceMasterHost = lineMasterHost.replace('master_host:', '').trim()
                        def lineMasterPort = lines.find { it.contains('master_port:') }
                        def thisInstanceMasterPort = lineMasterPort.replace('master_port:', '').trim() as int

                        if (thisInstanceMasterHost == masterNodeIp && thisInstanceMasterPort == masterPort) {
                            log.info 'master host and port is same, skip'
                            return true
                        } else {
                            log.warn 'master host or port is not same, slave of target master'
                            def r = jedis.replicaof(masterNodeIp, masterPort)
                            log.info 'replica of {}:{}, result: {}', masterNodeIp, masterPort, r
                            return 'OK' == r
                        }
                    }

                    // role:master
                    // check if master is self
                    if (masterNodeIp == thisInstanceNodeIp && masterPort == thisInstanceRedisPort) {
                        log.info 'master address is self, skip init master slave'
                        return true
                    }

                    // if sentinel replicas address already added, skip
                    if (replicaAddressSet.contains(thisInstanceNodeIp + ':' + thisInstanceRedisPort)) {
                        log.info 'replica address already added, skip init master slave, sentinel will do it'
                        return true
                    }

                    // check if master has slaves
                    def lineConnectedSlaves = lines.find { it.contains('connected_slaves:') }
                    if (lineConnectedSlaves) {
                        def num = lineConnectedSlaves.replace('connected_slaves:', '') as int
                        if (num > 0) {
                            log.warn 'it is master, but has slaves, check data after replica of, node ip: {}, port: {}', thisInstanceNodeIp, thisInstanceRedisPort
                            log.warn 'slaves will change to new master address, master node ip: {}, port: {}', masterNodeIp, masterPort
                        }
                    }

                    def r = jedis.replicaof(masterNodeIp, masterPort)
                    log.info 'replica of {}:{}, result: {}', masterNodeIp, masterPort, r
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
        }

        HealthCheckerHolder.instance.add new HealthChecker() {
            @Override
            String name() {
                'redis master slave status check'
            }

            @Override
            String imageName() {
                RedisPlugin.this.imageName()
            }

            @Override
            boolean check(AppDTO app) {
                def sentinelConfOne = app.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
                if (sentinelConfOne) {
                    def isSentinelSingleNode = 'true' == sentinelConfOne.paramValue('isSingleNode')
                    def sentinelPort = sentinelConfOne.paramValue('port') as int
                    def sentinelPassword = sentinelConfOne.paramValue('password') as String

                    // check all master status
                    def containerList = InMemoryAllContainerManager.instance.getContainerList(0, app.id)
                    for (x in containerList) {
                        if (!x.running()) {
                            continue
                        }

                        def finalSentinelPort = sentinelPort + (isSentinelSingleNode ? x.instanceIndex() : 0)
                        def jedisPool = JedisPoolHolder.instance.create(x.nodeIp, finalSentinelPort, sentinelPassword)
                        def isMastersAllOk = JedisPoolHolder.instance.useRedisPool(jedisPool) { jedis ->
                            def infoSentinel = jedis.info('sentinel')
                            def lines = infoSentinel.readLines()
                            def masterLines = lines.findAll { it.contains('redis-app-') }
                            if (!masterLines) {
                                return true
                            }
                            return masterLines.every {
                                def arr = it.split(',')
                                for (str in arr) {
                                    def arr2 = str.split('=')
                                    if (arr2[0] == 'status' && arr2[1] != 'ok') {
                                        log.warn 'master status is not ok, master name: {}, status: {}, sentinel node ip: {}', arr[0], arr2[1], x.nodeIp
                                        return false
                                    }
                                }
                                true
                            }
                        }

                        if (!isMastersAllOk) {
                            return false
                        }
                    }
                }

                def confOne = app.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                if (!confOne) {
                    return true
                }

                def isMasterSlave = confOne.paramValue('isMasterSlave') == 'true'
                if (!isMasterSlave) {
                    log.info 'it is not master slave mode, skip health check'
                    return true
                }

                def containerList = InMemoryAllContainerManager.instance.getContainerList(0, app.id)
                def roleList = containerList.collect {
                    if (!it.running()) {
                        return 'unknown'
                    }

                    def redisPort = confOne.paramValue('port') as int
                    def redisPassword = confOne.paramValue('password') as String

                    def tplOne = new ImageTplDTO(id: confOne.imageTplId).one()
                    def isSingleNode = tplOne.name.contains('single.node')
                    def finalPort = redisPort + (isSingleNode ? it.instanceIndex() : 0)

                    def jedisPool = JedisPoolHolder.instance.create(it.nodeIp, finalPort, redisPassword)
                    JedisPoolHolder.instance.useRedisPool(jedisPool) { jedis ->
                        String role
                        def infoReplication = jedis.info('replication')
                        def lines = infoReplication.readLines()
                        if (lines.find { it.contains('role:slave') }) {
                            role = 'slave'
                        } else if (lines.find { it.contains('role:master') }) {
                            role = 'master'
                        } else {
                            role = 'unknown'
                        }
                        role
                    } as String
                }

                if (roleList.any { it == 'unknown' }) {
                    log.warn 'unknown role, app id: {}, role list: {}', app.id, roleList
                    return false
                }

                if (roleList.count { it == 'master' } != 1) {
                    log.warn 'master count is not 1, app id: {}, role list: {}', app.id, roleList
                    return false
                }

                def expectSlaveCount = containerList.size() - 1
                if (roleList.count { it == 'slave' } != expectSlaveCount) {
                    log.warn 'slave count is not {}, app id: {}, role list: {}', expectSlaveCount, app.id, roleList
                    return false
                }

                return true
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
                    log.info 'it is redis sentinel, skip init exporter'
                    return true
                }

                // only last container create exporter application
                if (cc.instanceIndex != cc.conf.containerNumber - 1) {
                    return
                }

                def confOne = cc.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                def redisPort = confOne.paramValue('port') as int
                def redisPassword = confOne.paramValue('password') as String

                def tplOne = new ImageTplDTO(id: confOne.imageTplId).one()
                def isSingleNode = tplOne.name.contains('single.node')

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

                String envValue
                if (isSingleNode) {
                    // ${nodeIp} is a placeholder, will be replaced by real ip
                    envValue = "redis://\${nodeIp}:\${${redisPort} + instanceIndex}".toString()
                } else {
                    envValue = "redis://\${nodeIp}:${redisPort}".toString()
                }

                conf.envList << new KVPair<String>('REDIS_ADDR', envValue)
                conf.envList << new KVPair<String>('REDIS_PASSWORD', redisPassword)

                final int exporterPort = 9121
                def exporterPublicPort = exporterPort + (redisPort - 6379)

                if (isSingleNode) {
                    conf.envList << new KVPair<String>('REDIS_EXPORTER_WEB_LISTEN_ADDRESS', '0.0.0.0:${' + exporterPublicPort + '+instanceIndex}')
                    conf.envList << new KVPair<String>(ContainerInfo.ENV_KEY_PUBLIC_PORT + exporterPort, '${' + exporterPublicPort + '+instanceIndex}')
                } else {
                    conf.envList << new KVPair<String>('REDIS_EXPORTER_WEB_LISTEN_ADDRESS', "0.0.0.0:${exporterPublicPort}".toString())
                    conf.envList << new KVPair<String>(ContainerInfo.ENV_KEY_PUBLIC_PORT + exporterPort, exporterPublicPort.toString())
                }

                conf.networkMode = 'host'
                conf.portList << new PortMapping(privatePort: exporterPort, publicPort: exporterPublicPort)

                // monitor
                def monitorConf = new MonitorConf()
                app.monitorConf = monitorConf
                monitorConf.port = exporterPort
                monitorConf.isHttpRequest = true
                monitorConf.httpRequestUri = '/metrics'

                // add application to dms
                int appId = app.add() as int
                app.id = appId
                log.info 'done create related exporter application, app id: {}', appId

                // create dms job
                createAppCreateJob(appId, conf)
                true
            }

            @Override
            Checker.Type type() {
                Checker.Type.after
            }

            @Override
            String name() {
                'redis create exporter application'
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
