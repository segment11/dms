package plugin.demo2

import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ha.JedisPoolHolder
import model.AppDTO
import model.ImageEnvDTO
import model.ImagePortDTO
import model.ImageTplDTO
import model.json.TplParamsConf
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

@CompileStatic
@Slf4j
/**
 * this only support redis master slave + sentinel
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
        final String tplNameUseTemplate = 'redis.template.conf.tpl'
        final String tplName2 = 'redis.conf.single.node.tpl'
        final String tplName3 = 'sentinel.conf.tpl'

        String tplFilePath = PluginManager.pluginsResourceDirPath() + '/redis/RedisConfTpl.groovy'
        String tplFilePathUseTemplate = PluginManager.pluginsResourceDirPath() + '/redis/RedisConfUseTemplateTpl.groovy'
        String tplFilePath2 = PluginManager.pluginsResourceDirPath() + '/redis/RedisConfSingleNodeTpl.groovy'
        String tplFilePath3 = PluginManager.pluginsResourceDirPath() + '/redis/SentinelConfTpl.groovy'
        String content = new File(tplFilePath).text
        String contentUseTemplate = new File(tplFilePathUseTemplate).text
        String content2 = new File(tplFilePath2).text
        String content3 = new File(tplFilePath3).text

        TplParamsConf tplParams = new TplParamsConf()
        tplParams.addParam('port', '6379', 'int')
        tplParams.addParam('dataDir', '/data/redis', 'string')
        tplParams.addParam('password', '123456', 'string')
        tplParams.addParam('isSingleNode', 'false', 'string')
        tplParams.addParam('isMasterSlave', 'true', 'string')
        tplParams.addParam('sentinelAppName', 'sentinel', 'string')
        tplParams.addParam('customParameters', 'cluster-enabled no', 'string')

        TplParamsConf tplParamsUseTemplate = new TplParamsConf()
        tplParams.addParam('port', '6379', 'int')
        tplParams.addParam('dataDir', '/data/redis', 'string')
        tplParams.addParam('password', '123456', 'string')
        tplParams.addParam('maxmemoryMB', '1024', 'int')
        tplParams.addParam('isSingleNode', 'false', 'string')
        tplParams.addParam('isMasterSlave', 'true', 'string')
        tplParams.addParam('sentinelAppName', 'sentinel', 'string')
        tplParams.addParam('isCluster', 'false', 'string')
        tplParams.addParam('configTemplateId', '0', 'int')

        TplParamsConf tplParams2 = new TplParamsConf()
        tplParams2.addParam('port', '6379', 'int')
        tplParams2.addParam('dataDir', '/data/redis', 'string')
        tplParams2.addParam('password', '123456', 'string')
        tplParams2.addParam('customParameters', 'cluster-enabled no', 'string')

        TplParamsConf tplParams3 = new TplParamsConf()
        tplParams3.addParam('port', '26379', 'int')
        tplParams2.addParam('dataDir', '/data/sentinel', 'string')
        tplParams3.addParam('password', '123456', 'string')
        tplParams3.addParam('isSingleNode', 'false', 'string')
        tplParams3.addParam('downAfterMs', '30000', 'int')
        tplParams3.addParam('failoverTimeout', '180000', 'int')

        def imageName = imageName()

        def one = new ImageTplDTO(imageName: imageName, name: tplName).queryFields('id').one()
        if (!one) {
            new ImageTplDTO(name: tplName,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/redis/redis.conf',
                    content: content,
                    isParentDirMount: false,
                    params: tplParams).add()
        }

        def oneUseTemplate = new ImageTplDTO(imageName: imageName, name: tplNameUseTemplate).queryFields('id').one()
        if (!oneUseTemplate) {
            new ImageTplDTO(name: tplNameUseTemplate,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/redis/redis.conf',
                    content: contentUseTemplate,
                    isParentDirMount: false,
                    params: tplParamsUseTemplate).add()
        }

        def one2 = new ImageTplDTO(imageName: imageName, name: tplName2).queryFields('id').one()
        if (!one2) {
            new ImageTplDTO(name: tplName2,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
                    mountDist: '/etc/redis/redis.conf',
                    content: content2,
                    isParentDirMount: false,
                    params: tplParams2).add()
        }

        def one3 = new ImageTplDTO(imageName: imageName, name: tplName3).queryFields('id').one()
        if (!one3) {
            new ImageTplDTO(name: tplName3,
                    imageName: imageName,
                    tplType: ImageTplDTO.TplType.mount,
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
                                log.warn 'port {} is already used, app id: {}, app name: {}, check app id: {}, check app name: {}',
                                        checkPort, otherApp.id, otherApp.name, conf.app.id, conf.app.name
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

                def containerNumber = conf.conf.containerNumber

                List<String> list = []
                conf.conf.dirVolumeList.collect { it.dir }.each { nodeDir ->
                    String nodeDirTmp
                    if (nodeDir.contains('${appId}')) {
                        nodeDirTmp = nodeDir.replace('${appId}', conf.appId.toString())
                    } else {
                        nodeDirTmp = nodeDir
                    }

                    containerNumber.times { instanceIndex ->
                        list << (nodeDirTmp.replace('${instanceIndex}', instanceIndex.toString()) + '/instance_' + instanceIndex)
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
            private boolean monitorRedisServersIfMasterNameNotExist(CreateContainerConf conf, AppDTO app) {
                // for sentinel server scale up or restart but config changed
                def confOne = app.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                def isMasterSlave = confOne.paramValue('isMasterSlave') == 'true'
                if (!isMasterSlave) {
                    log.info 'it is not master slave mode, skip init master slave'
                    return true
                }

                def sentinelAppName = confOne.paramValue('sentinelAppName')
                if (sentinelAppName != conf.app.name) {
                    log.info 'redis app {} is charge by another sentinel app {}, not this sentinel app {}, skip', app.name, sentinelAppName, conf.app.name
                }

                def redisPort = confOne.paramValue('port') as int
                def redisPassword = confOne.paramValue('password') as String
                def isSingleNode = confOne.paramValue('isSingleNode') == 'true'

                def instance = InMemoryAllContainerManager.instance
                def runningContainerList = instance.getRunningContainerList(app.clusterId, app.id)
                def primaryX = runningContainerList.find { x ->
                    def thisInstanceRedisPort = redisPort + (isSingleNode ? x.instanceIndex() : 0)
                    def jedisPool = JedisPoolHolder.instance.create(x.nodeIp, thisInstanceRedisPort, redisPassword)
                    'master' == JedisPoolHolder.exe(jedisPool) { jedis ->
                        jedis.role()[0] as String
                    }
                }

                if (!primaryX) {
                    log.warn 'no master found for redis app {}, skip', app.name
                    return true
                }

                def primaryRedisNodeIp = primaryX.nodeIp
                def primaryRedisPort = redisPort + (isSingleNode ? primaryX.instanceIndex() : 0)

                // monitor config params
                def masterName = 'redis-app-' + app.id
                def quorum = (conf.conf.containerNumber / 2).intValue() + 1

                def sentinelConfOne = conf.app.conf.fileVolumeList.find {
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

                def thisInstanceSentinelPort = sentinelPort + (isSentinelSingleNode ? conf.instanceIndex : 0)
                def jedisPoolSentinel = JedisPoolHolder.instance.create(conf.nodeIp, thisInstanceSentinelPort, sentinelPassword)
                JedisPoolHolder.instance.exe(jedisPoolSentinel) { jedis ->
                    def infoSentinel = jedis.info('sentinel')
                    if (infoSentinel.contains(masterName + ',')) {
                        log.info 'sentinel monitor already added, instance index: {}, master name: {}', conf.instanceIndex, masterName
                    } else {
                        def r = jedis.sentinelMonitor(masterName, primaryRedisNodeIp, primaryRedisPort, quorum)
                        log.info 'sentinel monitor, instance index: {}, master name: {}, target: {}, quorum: {} result: {}',
                                conf.instanceIndex, masterName, primaryRedisNodeIp + ':' + primaryRedisPort, quorum, r
                        def r2 = jedis.sentinelSet(masterName, sentinelParams)
                        log.info 'sentinel set, instance index: {}, master name: {}, params: {}, result: {}',
                                conf.instanceIndex, masterName, sentinelParams, r2
                    }
                }

                true
            }

            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                // if use agent proxy, dms server can not connect agent directly
                def clusterOne = InMemoryCacheSupport.instance.oneCluster(conf.clusterId)
                if (clusterOne.globalEnvConf.proxyInfoList) {
                    log.warn 'this cluster is using agent proxy, skip'
                    return true
                }

                def sentinelConfThisOne = conf.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
                def isSentinel = sentinelConfThisOne != null
                if (isSentinel) {
                    def redisAppList = InMemoryCacheSupport.instance.appList.findAll {
                        it.id != conf.appId && canUseTo(it.conf.group, it.conf.image)
                    }
                    for (app in redisAppList) {
                        monitorRedisServersIfMasterNameNotExist(conf, app)
                    }
                    return true
                }

                def confOne = conf.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
                def isMasterSlave = confOne.paramValue('isMasterSlave') == 'true'
                if (!isMasterSlave) {
                    log.info 'it is not master slave mode, skip init master slave'
                    return true
                }

                def redisPort = confOne.paramValue('port') as int
                def redisPassword = confOne.paramValue('password') as String
                def isSingleNode = confOne.paramValue('isSingleNode') == 'true'

                def primaryRedisNodeIp = conf.nodeIpList[0]
                def primaryRedisPort = redisPort

                def thisInstanceRedisNodeIp = conf.nodeIp
                def thisInstanceRedisPort = redisPort + (isSingleNode ? conf.instanceIndex : 0)

                // get master address from sentinel or just use first node ip and port as primary
                Set<String> multiMasterAddressSet = []
                Set<String> replicaAddressSet = []
                String masterAddress = null
                def sentinelAppName = confOne.paramValue('sentinelAppName')
                if (sentinelAppName) {
                    def sentinelAppOne = InMemoryCacheSupport.instance.appList.find {
                        it.clusterId == conf.clusterId && it.name == sentinelAppName
                    }
                    if (!sentinelAppOne) {
                        log.warn 'sentinel application not exists, {}', sentinelAppName
                        return false
                    }

                    // monitor config params
                    def masterName = 'redis-app-' + conf.appId
                    def quorum = (conf.conf.containerNumber / 2).intValue() + 1

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
                    def instance = InMemoryAllContainerManager.instance
                    def runningContainerList = instance.getRunningContainerList(sentinelAppOne.clusterId, sentinelAppOne.id)
                    for (x in runningContainerList) {
                        def thisInstanceSentinelPort = sentinelPort + (isSentinelSingleNode ? x.instanceIndex() : 0)
                        def jedisPoolSentinel = JedisPoolHolder.instance.create(x.nodeIp, thisInstanceSentinelPort, sentinelPassword)
                        JedisPoolHolder.instance.exe(jedisPoolSentinel) { jedis ->
                            def infoSentinel = jedis.info('sentinel')
                            if (infoSentinel.contains(masterName + ',')) {
                                log.info 'sentinel monitor already added, instance index: {}, master name: {}', x.instanceIndex(), masterName
                                def rList = jedis.sentinelGetMasterAddrByName(masterName)
                                if (rList) {
                                    def masterAddressAlreadyAdded = rList.join(':')
                                    multiMasterAddressSet << masterAddressAlreadyAdded
                                    if (!masterAddress) {
                                        masterAddress = masterAddressAlreadyAdded
                                    }
                                }

                                for (sr in jedis.sentinelReplicas(masterName)) {
                                    def ip = sr.ip
                                    def port = sr.port as int
                                    def replicaAddress = ip + ':' + port
                                    replicaAddressSet << replicaAddress
                                }
                            } else {
                                if (!masterAddress) {
                                    masterAddress = primaryRedisNodeIp + ':' + primaryRedisPort
                                }
                                multiMasterAddressSet << masterAddress

                                def r = jedis.sentinelMonitor(masterName, primaryRedisNodeIp, primaryRedisPort, quorum)
                                log.info 'sentinel monitor, instance index: {}, master name: {}, target: {}, quorum: {} result: {}',
                                        x.instanceIndex(), masterName, primaryRedisNodeIp + ':' + primaryRedisPort, quorum, r
                                def r2 = jedis.sentinelSet(masterName, sentinelParams)
                                log.info 'sentinel set, instance index: {}, master name: {}, params: {}, result: {}',
                                        x.instanceIndex(), masterName, sentinelParams, r2
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
                    masterNodeIp = primaryRedisNodeIp
                    masterPort = primaryRedisPort
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

                def jedisPool = JedisPoolHolder.instance.create(thisInstanceRedisNodeIp, thisInstanceRedisPort, redisPassword)
                def isOk = JedisPoolHolder.instance.exe(jedisPool) { jedis ->
                    def infoReplication = jedis.info('replication')
                    def lines = infoReplication.readLines()
                    if (lines.find { it.contains('role:slave') }) {
                        log.warn 'it is slave, node ip: {}, port: {}', thisInstanceRedisNodeIp, thisInstanceRedisPort
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
                    if (masterNodeIp == thisInstanceRedisNodeIp && masterPort == thisInstanceRedisPort) {
                        log.info 'master address is self, skip init master slave'
                        return true
                    }

                    // if sentinel replicas address already added, skip
                    if (replicaAddressSet.contains(thisInstanceRedisNodeIp + ':' + thisInstanceRedisPort)) {
                        log.info 'replica address already added, skip init master slave, sentinel will do it'
                        return true
                    }

                    // check if master has slaves
                    def lineConnectedSlaves = lines.find { it.contains('connected_slaves:') }
                    if (lineConnectedSlaves) {
                        def num = lineConnectedSlaves.replace('connected_slaves:', '') as int
                        if (num > 0) {
                            log.warn 'it is master, but has slaves, check data after replica of, node ip: {}, port: {}', thisInstanceRedisNodeIp, thisInstanceRedisPort
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
                def instance = InMemoryAllContainerManager.instance
                if (sentinelConfOne) {
                    def sentinelPort = sentinelConfOne.paramValue('port') as int
                    def sentinelPassword = sentinelConfOne.paramValue('password') as String
                    def isSentinelSingleNode = 'true' == sentinelConfOne.paramValue('isSingleNode')

                    // check all master status
                    def runningContainerList = instance.getRunningContainerList(0, app.id)
                    for (x in runningContainerList) {
                        def thisInstanceSentinelPort = sentinelPort + (isSentinelSingleNode ? x.instanceIndex() : 0)
                        def jedisPoolSentinel = JedisPoolHolder.instance.create(x.nodeIp, thisInstanceSentinelPort, sentinelPassword)
                        def isMastersAllOk = JedisPoolHolder.instance.exe(jedisPoolSentinel) { jedis ->
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
                    return true
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

                def containerList = instance.getContainerList(0, app.id)
                def roleList = containerList.collect {
                    if (!it.running()) {
                        return 'unknown'
                    }

                    def redisPort = confOne.paramValue('port') as int
                    def redisPassword = confOne.paramValue('password') as String
                    def isSingleNode = confOne.paramValue('isSingleNode') == 'true'

                    def thisInstanceRedisPort = redisPort + (isSingleNode ? it.instanceIndex() : 0)
                    def jedisPool = JedisPoolHolder.instance.create(it.nodeIp, thisInstanceRedisPort, redisPassword)
                    JedisPoolHolder.instance.exe(jedisPool) { jedis ->
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

    @Override
    String registry() {
        'https://docker.1ms.run'
    }

    @Override
    String group() {
        'library'
    }

    @Override
    String image() {
        'redis'
    }

    @Override
    boolean canUseTo(String group, String image) {
        if ('library' == group && 'redis' == image) {
            return true
        }
        if ('library' == group && 'valkey' == image) {
            return true
        }
        if ('montplex' == group && 'engula' == image) {
            return true
        }

        false
    }
}
