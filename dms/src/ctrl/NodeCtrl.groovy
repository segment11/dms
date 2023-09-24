package ctrl

import auth.User
import com.segment.common.Conf
import com.segment.common.Utils
import common.AgentConf
import common.Const
import common.Event
import deploy.DeploySupport
import deploy.InitAgentEnvSupport
import model.AppDTO
import model.NodeDTO
import model.NodeKeyPairDTO
import org.segment.web.handler.ChainHandler
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import transfer.NodeInfo
import transfer.X

def h = ChainHandler.instance

h.group('/node') {
    h.get('/tag/update') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        def tags = req.param('tags')
        assert id
        new NodeDTO(id: id as int, tags: tags ?: '').update()
        [flag: true]
    }.post('/key-pair/reset-root-pass') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        Map params = req.bodyAs()
        // kp id
        String id = params.id
        assert id
        def kp = new NodeKeyPairDTO(id: id as int).one()
        assert kp

        if (!kp.pass && !kp.keyPrivate) {
            resp.halt(500, 'Node key pair password or key private required!')
        }

        Event.builder().type(Event.Type.cluster).reason('reset root password').
                result(kp.ip).build().log('cluster id: ' + kp.clusterId).toDto().add()

        def clusterOne = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
        def proxyNodeIp = clusterOne.globalEnvConf.proxyNodeIp
        def support = new InitAgentEnvSupport(kp)
        if (!proxyNodeIp || proxyNodeIp == kp.ip) {
            boolean isDone = support.resetRootPassword()
            return [flag   : isDone,
                    steps  : support.getSteps(),
                    message: 'Please view log for detail']
        } else {
            def r = AgentCaller.instance.doSshResetRootPassword(kp)
            return [flag   : r.getBoolean('flag').booleanValue(),
                    steps  : r.getJSONArray('steps'),
                    message: 'Please view log for detail']
        }
    }.post('/key-pair/init') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        Map params = req.bodyAs()

        String pass = params.pass
        String rootPass = params.rootPass
        String keyPrivate = params.keyPrivate

        assert pass || keyPrivate

        // node id, not key pair node id
        String id = params.id

        String ip
        int clusterId
        if (!id) {
            ip = params.ip
            def clusterIdStr = params.clusterId
            assert ip && clusterIdStr
            clusterId = clusterIdStr as int
        } else {
            def one = new NodeDTO(id: id as int).queryFields('cluster_id,ip').one()
            assert one
            ip = one.ip
            clusterId = one.clusterId
        }

        String user = params.user ?: 'root'

        def kp = new NodeKeyPairDTO(ip: ip).one()
        if (!kp) {
            String message
            kp = new NodeKeyPairDTO(clusterId: clusterId, ip: ip, sshPort: 22,
                    user: user, pass: pass, rootPass: rootPass)
            if (!keyPrivate) {
                DeploySupport.instance.initPrivateKey(kp)
                message = 'Generate primary key'
            } else {
                kp.keyPrivate = keyPrivate
                message = 'Use given private key'
            }
            kp.add()
            [flag: true, message: message]
        } else {
            boolean isForceUpdatePass = user != kp.user
            if (isForceUpdatePass) {
                String message
                kp.user = user
                kp.pass = pass
                if (!keyPrivate) {
                    DeploySupport.instance.initPrivateKey(kp)
                    message = 'Generate primary key'
                } else {
                    kp.keyPrivate = keyPrivate
                    message = 'Use given private key'
                }
                kp.update()
                [flag: true, message: message]
            } else {
                String message
                kp.user = user
                kp.pass = pass
                kp.rootPass = rootPass
                if (keyPrivate && !kp.keyPrivate) {
                    kp.keyPrivate = keyPrivate
                    message = 'Use given private key'
                } else {
                    message = 'Only update user/password/root password'
                }
                kp.update()
                [flag: true, message: message]
            }
        }
    }.post('/agent/init') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        Map params = req.bodyAs()
        String id = params.id
        assert id

        def kp = new NodeKeyPairDTO(id: id as int).one()
        assert kp

        if (!kp.rootPass) {
            return [flag: false, message: 'Root password need init']
        }

        Event.builder().type(Event.Type.cluster).reason('init agent').
                result(kp.ip).build().log('cluster id: ' + kp.clusterId).toDto().add()

        def clusterOne = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
        def proxyNodeIp = clusterOne.globalEnvConf.proxyNodeIp
        def support = new InitAgentEnvSupport(kp)
        if (!proxyNodeIp || proxyNodeIp == kp.ip) {
            boolean isDone = support.initNodeAgent()
            return [flag   : isDone,
                    steps  : support.getSteps(),
                    message: 'Please view log for detail']
        } else {
            def r = AgentCaller.instance.doSshInitAgent(kp)
            return [flag   : r.getBoolean('flag').booleanValue(),
                    steps  : r.getJSONArray('steps'),
                    message: 'Please view log for detail']
        }
    }.post('/agent/start') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        Map params = req.bodyAs()
        String id = params.id
        assert id

        def kp = new NodeKeyPairDTO(id: id as int).one()
        assert kp

        if (!kp.rootPass) {
            return [flag: false, message: 'Root password need init']
        }

        Event.builder().type(Event.Type.cluster).reason('start agent').
                result(kp.ip).build().log('cluster id: ' + kp.clusterId).toDto().add()

        def clusterOne = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
        def conf = clusterOne.globalEnvConf

        def agentConf = new AgentConf()
        agentConf.clusterId = clusterOne.id
        agentConf.secret = clusterOne.secret
        agentConf.serverHost = conf.serverHost ?: Utils.localIp()
        agentConf.serverPort = conf.serverPort ?: Const.SERVER_HTTP_LISTEN_PORT
        agentConf.localIpFilterPre = conf.localIpFilterPre
        agentConf.agentIntervalSeconds = Conf.instance.getInt('agent.interval.seconds', 5)

        def proxyNodeIp = conf.proxyNodeIp
        def support = new InitAgentEnvSupport(kp)
        if (!proxyNodeIp || proxyNodeIp == kp.ip) {
            support.initAgentConf(agentConf)
            boolean isDone = support.startAgentCmd()
            return [flag   : isDone,
                    steps  : support.getSteps(),
                    message: 'Please view log for detail']
        } else {
            List steps = []
            def copyR = AgentCaller.instance.doSshCopyAgentConf(kp, agentConf)
            def isCopyOk = copyR.getBoolean('flag').booleanValue()
            steps.addAll copyR.getJSONArray('steps')
            if (!isCopyOk) {
                return [flag   : isCopyOk,
                        steps  : steps,
                        message: 'Please view log for detail']
            }
            def startR = AgentCaller.instance.doSshStartAgent(kp)
            def isStartOk = startR.getBoolean('flag').booleanValue()
            steps.addAll startR.getJSONArray('steps')
            return [flag   : isStartOk,
                    steps  : steps,
                    message: 'Please view log for detail']
        }
    }.post('/agent/stop') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        Map params = req.bodyAs()
        String id = params.id
        assert id

        def kp = new NodeKeyPairDTO(id: id as int).one()
        assert kp

        if (!kp.rootPass) {
            return [flag: false, message: 'Root password need init']
        }

        Event.builder().type(Event.Type.cluster).reason('stop agent').
                result(kp.ip).build().log('cluster id: ' + kp.clusterId).toDto().add()

        def clusterOne = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
        def conf = clusterOne.globalEnvConf

        def proxyNodeIp = conf.proxyNodeIp
        def support = new InitAgentEnvSupport(kp)
        if (!proxyNodeIp || proxyNodeIp == kp.ip) {
            boolean isDone = support.stopAgent()
            return [flag   : isDone,
                    steps  : support.getSteps(),
                    message: 'Please view log for detail']
        } else {
            def stopR = AgentCaller.instance.doSshStopAgent(kp)
            def isDone = stopR.getBoolean('flag').booleanValue()
            return [flag   : isDone,
                    steps  : support.getSteps(),
                    message: 'Please view log for detail']
        }
    }.delete('/agent/remove-node') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        assert id
        def kp = new NodeKeyPairDTO(id: id as int).one()
        assert kp

        Event.builder().type(Event.Type.cluster).reason('remove node').
                result(kp.ip).build().log('cluster id: ' + kp.clusterId).toDto().add()

        new NodeKeyPairDTO(id: id as int).delete()

        def node = new NodeDTO(clusterId: kp.clusterId, ip: kp.ip).one()
        if (node) {
            new NodeDTO(id: node.id).delete()
        }

        [flag: true]
    }.get('/list') { req, resp ->
        // :clusterId -> for other handler reuse this handler's code by set request attribute
        def clusterId = req.param('clusterId') ?: req.param(':clusterId')
        assert clusterId
        def nodeList = new NodeDTO(clusterId: clusterId as int).
                queryFields('id,ip,tags,updated_date,agent_version').list()
        nodeList.sort { a, b ->
            common.Utils.compareIp(a.ip, b.ip)
        }

        def keyPairList = new NodeKeyPairDTO(clusterId: clusterId as int).
                queryFields('ip').
                list()

        def instance = InMemoryAllContainerManager.instance
        Map<String, NodeInfo> r = instance.getAllNodeInfo(clusterId as int)

        nodeList.findAll { r[it.ip] }.collect {
            def info = r[it.ip]
            info.checkIfOk(instance.getHeartBeatDate(it.ip))
            [id               : it.id,
             nodeIp           : it.ip,
             agentVersion     : it.agentVersion,
             updatedDate      : it.updatedDate,
             isOk             : info.isOk,
             isLiveCheckOk    : info.isLiveCheckOk,
             tags             : it.tags,
             tagList          : it.tags ? it.tags.split(',') : [],
             instances        : instance.getContainerListByNodeIp(it.ip)?.size(),
             fsUsageList      : info.fileUsageList.sort { a, b -> b.usePercent <=> a.usePercent },
             cpuVCore         : info.cpuPercList.size(),
             cpuIdle          : info.cpuPercList.sum { NodeInfo.CpuPerc x -> x.idle },
             cpuSys           : info.cpuPercList.sum { NodeInfo.CpuPerc x -> x.sys },
             cpuUser          : info.cpuPercList.sum { NodeInfo.CpuPerc x -> x.user },
             cpuUsedPercent   : (info.cpuUsedPercent() * 100).round(4),
             memoryTotalMB    : info.mem.total,
             memoryFreeMB     : info.mem.actualFree,
             memoryUsedMB     : info.mem.actualUsed,
             memoryUsedPercent: info.mem.usedPercent,
             haveKeyPair      : keyPairList.find { kp -> kp.ip == it.ip } != null
            ]
        }
    }.get('/call') { req, resp ->
        // for debug
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def uri = req.param('uri')
        def clusterId = req.param('clusterId')
        def nodeIp = req.param('nodeIp')
        assert clusterId && nodeIp && uri

        def params = [:]
        req.raw().getParameterNames().each { String key ->
            params[key] = req.param(key)
        }
        AgentCaller.instance.get(clusterId as int, nodeIp, uri, params)
    }.get('/metric/queue') { req, resp ->
        def clusterId = req.param('clusterId')
        def nodeIp = req.param('nodeIp')
        def type = req.param('type')
        def queueType = req.param('queueType')
        def containerId = req.param('containerId')
        def gaugeName = req.param('gaugeName')
        assert nodeIp && type

        def p = [type: type, queueType: queueType, containerId: containerId, gaugeName: gaugeName]
        if (containerId) {
            def appId = InMemoryAllContainerManager.instance.getAppIpByContainerId(containerId)
            p.appId = appId
            if ('container' == queueType) {
                def appOne = new AppDTO(id: appId).queryFields('monitor_conf').one()
                if (!appOne.monitorConf) {
                    resp.halt(500, 'this app is not config monitor')
                }
            }
        }

        resp.end AgentCaller.instance.get(clusterId as int, nodeIp, '/dmc/metric/queue', p)
    }.get('/metric/gauge/name/list') { req, resp ->
        def clusterId = req.param('clusterId')
        def nodeIp = req.param('nodeIp')
        def containerId = req.param('containerId')
        assert nodeIp && containerId

        def appId = InMemoryAllContainerManager.instance.getAppIpByContainerId(containerId)

        def p = [:]
        p.appId = appId
        def appOne = new AppDTO(id: appId).queryFields('monitor_conf').one()
        if (!appOne.monitorConf) {
            resp.halt(500, 'this app is not config monitor')
        }

        resp.end AgentCaller.instance.get(clusterId as int, nodeIp, '/dmc/metric/gauge/name/list', p)
    }
}

h.group('/api') {
    h.group('/hb') {
        h.post('/node') { req, resp ->
            def authToken = req.header(Const.AUTH_TOKEN_HEADER)

            NodeInfo info = req.bodyAs(NodeInfo)
            info.hbTime = new Date()
            def nodeIp = info.nodeIp

            InMemoryAllContainerManager.instance.addAuthToken(nodeIp, authToken)
            InMemoryAllContainerManager.instance.addNodeInfo(nodeIp, info)
            def old = new NodeDTO(ip: nodeIp).queryFields('id').one()
            if (old) {
                old.updatedDate = info.hbTime
                old.agentVersion = info.version
                old.clusterId = info.clusterId
                old.update()
            } else {
                new NodeDTO(ip: nodeIp, clusterId: info.clusterId, agentVersion: info.version, updatedDate: info.hbTime).add()
            }

            def clusterOne = InMemoryCacheSupport.instance.oneCluster(info.clusterId)
            [envList: clusterOne.globalEnvConf.envList, dnsServer: clusterOne.globalEnvConf.dnsServer]
        }.post('/container') { req, resp ->
            X x = req.bodyAs(X)
            InMemoryAllContainerManager.instance.addContainers(x.clusterId, x.nodeIp, x.containers)
            'ok'
        }
    }
}
