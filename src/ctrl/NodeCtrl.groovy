package ctrl

import agent.Agent
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import common.ContainerHelper
import common.Utils
import model.AppDTO
import model.NodeDTO
import org.segment.web.handler.ChainHandler
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.scheduler.Guardian

def h = ChainHandler.instance

h.group('/node') {
    h.get('/tag/update') { req, resp ->
        def id = req.param('id')
        def tags = req.param('tags')
        assert id
        new NodeDTO(id: id as int, tags: tags ?: '').update()
        [flag: true]
    }.get('/info/one') { req, resp ->
        def nodeIp = req.param('nodeIp')
        assert nodeIp
        InMemoryAllContainerManager.instance.getNodeInfo(nodeIp) ?: [:]
    }.get('/info/list') { req, resp ->
        def r = InMemoryAllContainerManager.instance.allNodeInfo
        def dat = Utils.getNodeAliveCheckLastDate(3)
        r.each { k, v ->
            def d = InMemoryAllContainerManager.instance.getHeartBeatDate(k)
            v.isOk = d > dat
        }
        r
    }.get('/list') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId
        def nodeList = new NodeDTO(clusterId: clusterId as int).
                queryFields('id,ip,tags,updated_date,agent_version').loadList() as List<NodeDTO>
        nodeList.sort { a, b ->
            Utils.compareIp(a.ip, b.ip)
        }
        def instance = InMemoryAllContainerManager.instance
        def r = instance.allNodeInfo
        def dat = Utils.getNodeAliveCheckLastDate(3)
        nodeList.findAll { r[it.ip] }.collect {
            def info = r[it.ip]
            List<JSONObject> cpuPercList = info.cpuPercList ?: []
            List<JSONObject> fsUsageList = info.fsUsageList ?: []
            JSONObject mem = info.mem ?: new JSONObject([total: 0, free: 0, usedPercent: 0])
            double cpuUsedPercent = cpuPercList ? (cpuPercList.sum { JSONObject x ->
                x.getDouble('sys') + x.getDouble('user')
            } / cpuPercList.size()) : 0
            [id               : it.id,
             nodeIp           : it.ip,
             agentVersion     : it.agentVersion,
             updatedDate      : it.updatedDate,
             isOk             : instance.getHeartBeatDate(it.ip) > dat,
             isLiveCheckOk    : info.getBoolean('isLiveCheckOk'),
             isMetricGetOk    : info.getBoolean('isMetricGetOk'),
             tags             : it.tags,
             tagList          : it.tags ? it.tags.split(',') : [],
             fsUsageList      : fsUsageList,
             cpuVCore         : cpuPercList.size(),
             cpuIdle          : cpuPercList.sum { JSONObject x -> x.getDouble('idle') },
             cpuSys           : cpuPercList.sum { JSONObject x -> x.getDouble('sys') },
             cpuUser          : cpuPercList.sum { JSONObject x -> x.getDouble('user') },
             cpuUsedPercent   : (cpuUsedPercent * 100).round(4),
             memoryTotalMB    : mem.getDouble('total').round(2),
             memoryFreeMB     : mem.getDouble('free').round(2),
             memoryUsedPercent: mem.getDouble('usedPercent').round(4)]
        }
    }.get('/call') { req, resp ->
        def uri = req.param('uri')
        def nodeIp = req.param('nodeIp')
        assert nodeIp && uri

        def params = [:]
        req.raw().getParameterNames().each {
            params[it] = req.param(it)
        }
        AgentCaller.instance.get(nodeIp, uri, params)
    }.get('/metric/queue') { req, resp ->
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
                def appOne = new AppDTO(id: appId).queryFields('monitor_conf').one() as AppDTO
                if (!appOne.monitorConf) {
                    resp.halt(500, 'this app is not config monitor')
                }
            }
        }

        resp.end AgentCaller.instance.get(nodeIp, '/dmc/metric/queue', p)
    }.get('/metric/gauge/name/list') { req, resp ->
        def nodeIp = req.param('nodeIp')
        def containerId = req.param('containerId')
        assert nodeIp && containerId

        def appId = InMemoryAllContainerManager.instance.getAppIpByContainerId(containerId)

        def p = [:]
        p.appId = appId
        def appOne = new AppDTO(id: appId).queryFields('monitor_conf').one() as AppDTO
        if (!appOne.monitorConf) {
            resp.halt(500, 'this app is not config monitor')
        }

        resp.end AgentCaller.instance.get(nodeIp, '/dmc/metric/gauge/name/list', p)
    }
}

h.group('/api') {
    h.group('/hb') {
        h.post('/node') { req, resp ->
            def x = JSON.parseObject(req.body())
            def nodeIp = x.getString('nodeIp')
            def agentVersion = x.getString('version')
            def clusterId = x.getInteger('clusterId')

            def authToken = req.header(Agent.AUTH_TOKEN_HEADER)
            InMemoryAllContainerManager.instance.addAuthToken(nodeIp, authToken)
            InMemoryAllContainerManager.instance.addNodeInfo(nodeIp, x)
            def old = new NodeDTO(ip: nodeIp).queryFields('id').one() as NodeDTO
            if (old) {
                old.updatedDate = new Date()
                old.agentVersion = agentVersion
                old.clusterId = clusterId
                old.update()
            } else {
                new NodeDTO(ip: nodeIp, clusterId: clusterId, agentVersion: agentVersion, updatedDate: new Date()).add()
            }
            Guardian.instance.addAgentHeartBeatNodeIp(nodeIp)
            'ok'
        }.post('/container') { req, resp ->
            def x = JSON.parseObject(req.body())
            def nodeIp = x.getString('nodeIp')
            def nodeIpDockerHost = x.getString('nodeIpDockerHost')
            def clusterId = x.getInteger('clusterId')
            List<JSONObject> list = x.getJSONArray('containers')
            for (one in list) {
                ContainerHelper.resetAppId(one)
                one.put(ContainerHelper.KEY_CLUSTER_ID, clusterId)
                one.put(ContainerHelper.KEY_NODE_IP, nodeIp)
                one.put(ContainerHelper.KEY_NODE_IP_DOCKER_HOST, nodeIpDockerHost)
            }
            InMemoryAllContainerManager.instance.addContainers(nodeIp, list)
            'ok'
        }
    }
}
