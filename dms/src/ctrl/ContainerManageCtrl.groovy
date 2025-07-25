package ctrl

import auth.User
import com.alibaba.fastjson.JSONObject
import common.ContainerHelper
import common.Event
import common.Utils
import model.AppDTO
import model.AppJobDTO
import model.ImageTplDTO
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.handler.ChainHandler
import org.segment.web.handler.Req
import org.segment.web.handler.Resp
import server.AgentCaller
import server.ContainerMountFileGenerator
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.scheduler.processor.HostProcessSupport
import spi.SpiSupport
import transfer.ContainerInfo
import transfer.ContainerInspectInfo

import java.math.RoundingMode

def h = ChainHandler.instance

h.group('/container/manage') {
    h.get('/start') { req, resp ->
        checkIfAppJobNotDone(req, resp)
        callAgentScript(req, resp, 'container start')
    }.get('/stop') { req, resp ->
        checkIfAppJobNotDone(req, resp)
        callAgentScript(req, resp, 'container stop')
    }.get('/remove') { req, resp ->
        checkIfAppJobNotDone(req, resp)
        callAgentScript(req, resp, 'container remove')
    }.get('/kill') { req, resp ->
        checkIfAppJobNotDone(req, resp)
        callAgentScript(req, resp, 'container kill')
    }.get('/inspect') { req, resp ->
        callAgentScript(req, resp, 'container inspect')
    }.get('/list') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId
        def appId = req.param('appId')
        def nodeIp = req.param('nodeIp')
        User u = req.attr('user') as User

        def instance = InMemoryAllContainerManager.instance
        List<ContainerInfo> containerList = instance.getContainerList(clusterId as int,
                appId ? appId as int : 0, nodeIp, u) ?: []
        def simpleContainerList = containerList.collect { x ->
            x.simple()
        }

        def appList = new AppDTO().where('cluster_id=?', clusterId as int).
                queryFields('id,name,des,namespace_id,conf').list()
        for (x in simpleContainerList) {
            def appOne = appList.find { it.id == x.appId() }
            if (appOne) {
                x.appName = appOne.name
                x.appDes = appOne.des
                x.namespaceId = appOne.namespaceId
            }
        }

        def groupByApp = simpleContainerList.groupBy { x -> x.appId() }
        def groupByNodeIp = simpleContainerList.groupBy { x -> x.nodeIp }
        List<Map> appCheckOkList = []
        groupByApp.each { k, v ->
            if (k == 0) {
                appCheckOkList << [appId: k, isOk: true]
            } else {
                def appOne = appList.find { it.id == k }
                if (!appOne) {
                    appCheckOkList << [appId: k, isOk: true]
                } else {
                    boolean isOk = v.count { x -> x.running() } == appOne.conf.containerNumber
                    appCheckOkList << [appId: k, isOk: isOk]
                }
            }
        }

        Map<String, Map<Integer, List<Double>>> cpusetCpusMapByNodeIp = [:]
        Map<String, Map<Integer, Double>> cpuUsedPercentMapByNodeIp = [:]

        Map<String, Long> memByNodeIp = [:]
        Map<String, Map<String, Long>> memRssUsedMapByNodeIp = [:]
        Map<String, Map<String, Integer>> memRequiredMapByNodeIp = [:]

        groupByNodeIp.each { nodeIpInner, v ->
            Map<Integer, List<Double>> map = [:]
            cpusetCpusMapByNodeIp[nodeIpInner] = map

            Map<Integer, Double> mapUsedPercent = [:]
            cpuUsedPercentMapByNodeIp[nodeIpInner] = mapUsedPercent

            Map<String, Long> mapMemRssUsed = [:]
            Map<String, Integer> mapRequired = [:]
            memRssUsedMapByNodeIp[nodeIpInner] = mapMemRssUsed
            memRequiredMapByNodeIp[nodeIpInner] = mapRequired

            def nodeInfo = instance.getNodeInfo(nodeIpInner)
            for (i in 0..<nodeInfo.cpuNumber()) {
                List<Double> subList = []
                map[i] = subList

                cpuUsedPercentMapByNodeIp[nodeIpInner][i] = nodeInfo.cpuPercList[i].usedPercent()
            }
            memByNodeIp[nodeIpInner] = nodeInfo.mem.total.longValue()

            for (x in v) {
                if (x.running()) {
                    mapMemRssUsed[x.name()] = ((x.memResident ?: 0) / 1024 / 1024).longValue()
                }

                def appOne = appList.find { it.id == x.appId() }
                if (!appOne) {
                    // should never happen
                    continue
                }

                def conf = appOne.conf
                String cpusetCpus = conf.cpusetCpus ?: '0-' + (nodeInfo.cpuNumber() - 1)
                if (!x.labels) {
                    x.labels = [:]
                }
                def cpusetCpuList = Utils.cpusetCpusToList(cpusetCpus)
                x.labels.cpusetCpus = cpusetCpuList.join(',')
                double vCpuNumber = 0
                if (conf.cpuShares) {
                    vCpuNumber = (conf.cpuShares / 1024).round(2).doubleValue()
                } else if (conf.cpuFixed) {
                    vCpuNumber = conf.cpuFixed
                }
                x.labels.vCpuNumber = vCpuNumber.toString()

                double avgInPer = (vCpuNumber / cpusetCpuList.size()).round(2).doubleValue()
                for (i in cpusetCpuList) {
                    map[i] << avgInPer
                }

                mapRequired[x.name()] = conf.memMB
            }
        }

        [groupByApp               : groupByApp,
         groupByNodeIp            : groupByNodeIp,
         appCheckOkList           : appCheckOkList,
         cpusetCpusMapByNodeIp    : cpusetCpusMapByNodeIp,
         cpuUsedPercentMapByNodeIp: cpuUsedPercentMapByNodeIp,
         memByNodeIp              : memByNodeIp,
         memRssUsedMapByNodeIp    : memRssUsedMapByNodeIp,
         memRequiredMapByNodeIp   : memRequiredMapByNodeIp]
    }.get('/bind/list') { req, resp ->
        def id = req.param('id')
        assert id

        def instance = InMemoryAllContainerManager.instance
        def nodeIp = instance.getNodeIpByContainerId(id)
        if (!nodeIp) {
            resp.halt(500, 'no node ip get')
        }

        def appId = instance.getAppIpByContainerId(id)
        if (!appId) {
            resp.halt(500, 'no app get')
        }

        User u = req.attr('user') as User
        if (!u.isAccessApp(appId)) {
            resp.halt(403, 'not this app manager')
        }

        def clusterId = InMemoryCacheSupport.instance.getClusterIdByAppId(appId)

        def r = AgentCaller.instance.agentScriptExeAs(clusterId, nodeIp, 'container inspect', ContainerInspectInfo, [id: id])
        def mounts = r.mounts

        List<Map> list = []
        if (!mounts) {
            return list
        }

        def hostDirs = mounts.findAll { !it.source.endsWith('.file') }.join(',')
        if (hostDirs) {
            def r2 = AgentCaller.instance.agentScriptExe(clusterId, nodeIp, 'file system dir usage', [dirs: hostDirs])
            mounts.each {
                def hostDir = it.source
                Map item = [hostDir: hostDir, containerDir: it.destination, mode: it.mode, fileType: 'file']

                JSONObject dirUsage = r2.getJSONObject(hostDir)
                if (dirUsage) {
                    long diskUsage = dirUsage.getLong('diskUsage')
                    double diskUsageMB = (diskUsage / 1024 / 1024).setScale(2, RoundingMode.FLOOR)
                    dirUsage.diskUsageMB = diskUsageMB
                    item.fileType = 'dir'
                    item.dirUsage = dirUsage
                }
                list << item
            }
        } else {
            mounts.findAll { it.source.endsWith('.file') }.each {
                list << [hostDir: it.source, containerDir: it.destination, mode: it.mode, fileType: 'file']
            }
        }

        list.sort { it.containerDir.toString() }
    }.get('/bind/content') { req, resp ->
        def id = req.param('id')
        assert id

        def instance = InMemoryAllContainerManager.instance
        def nodeIp = instance.getNodeIpByContainerId(id)
        if (!nodeIp) {
            resp.halt(500, 'no node ip get')
        }

        def appId = instance.getAppIpByContainerId(id)
        if (!appId) {
            resp.halt(500, 'no app get')
        }

        User u = req.attr('user') as User
        if (!u.isAccessApp(appId)) {
            resp.halt(403, 'not this app manager')
        }

        def clusterId = InMemoryCacheSupport.instance.getClusterIdByAppId(appId)

        def r = AgentCaller.instance.agentScriptExeAs(clusterId, nodeIp, 'container inspect', ContainerInspectInfo, [id: id])
        def mounts = r.mounts

        def containerDir = req.param('containerDir')
        def oneMount = mounts.find { mount -> mount.destination == containerDir }
        if (!oneMount) {
            resp.end ''
            return
        }

        def hostFilePath = oneMount.source
        def r2 = AgentCaller.instance.agentScriptExe(clusterId, nodeIp, 'file content', [path: hostFilePath])
        resp.end r2.content ?: ''
    }.get('/port/bind') { req, resp ->
        def id = req.param('id')
        assert id

        def instance = InMemoryAllContainerManager.instance
        def nodeIp = instance.getNodeIpByContainerId(id)
        if (!nodeIp) {
            resp.halt(500, 'no node ip get')
        }

        def appId = instance.getAppIpByContainerId(id)
        if (!appId) {
            resp.halt(500, 'no app get')
        }

        User u = req.attr('user') as User
        if (!u.isAccessApp(appId)) {
            resp.halt(403, 'not this app manager')
        }

        def clusterId = InMemoryCacheSupport.instance.getClusterIdByAppId(appId)

        def r = AgentCaller.instance.agentScriptExeAs(clusterId, nodeIp, 'container inspect', ContainerInspectInfo, [id: id])
        r.ports ?: 'No Port Bindings'
    }
}

private static void checkIfAppJobNotDone(Req req, Resp resp) {
    def containerId = req.param('id')

    def instance = InMemoryAllContainerManager.instance
    def appId = instance.getAppIpByContainerId(containerId)
    if (!appId) {
        return
    }
    def one = new AppJobDTO().where('status != ?', AppJobDTO.Status.done).
            where('app_id = ?', appId).queryFields('id').one()
    if (one != null) {
        resp.halt(500, 'there is a job not done, wait until it is done or delete it')
    }
}

private static void callAgentScript(Req req, Resp resp, String scriptName) {
    def id = req.param('id')
    assert id

    def instance = InMemoryAllContainerManager.instance
    def nodeIp = instance.getNodeIpByContainerId(id)
    if (!nodeIp) {
        resp.halt(500, 'no node ip get')
    }

    def appId = instance.getAppIpByContainerId(id)
    if (!appId) {
        resp.halt(500, 'no app get')
    }

    User u = req.attr('user') as User
    if (!u.isAccessApp(appId)) {
        resp.halt(403, 'not this app manager')
    }

    def clusterId = InMemoryCacheSupport.instance.getClusterIdByAppId(appId)

    if (scriptName == 'container inspect') {
        def r = AgentCaller.instance.agentScriptExeAs(clusterId, nodeIp, 'container inspect',
                ContainerInspectInfo, [id: id])
        resp.json(r)
        return
    }

    int readTimeout = 1000 * 10

    if (scriptName == 'container stop') {
        // need check if need to remove gateway backend
        def app = new AppDTO(id: appId).one()
        if (app.gatewayConf) {
            def r = AgentCaller.instance.agentScriptExeAs(clusterId, nodeIp, 'container inspect',
                    ContainerInspectInfo, [id: id])

            int privatePort = app.gatewayConf.containerPrivatePort
            int publicPort = privatePort
            if ('host' != r.networkMode && r.ports) {
                for (port in r.ports) {
                    if (port.privatePort == privatePort) {
                        publicPort = port.publicPort
                        break
                    }
                }
            }
        }
        readTimeout = 1000 * 30
    }

    Event.builder().type(Event.Type.app).reason('manage container').result(appId).
            build().log('user: ' + u.name + ', nodeIp: ' + nodeIp + ', operation: ' + scriptName).toDto().add()

    if (ContainerHelper.isProcess(id) && appId && scriptName == 'container start') {
        def app = new AppDTO(id: appId).one()
        if (app.conf.isRunningUnbox) {
            String fixPwd = app.conf.envList.find { it.key == 'PWD' }?.value
            int pid = HostProcessSupport.instance.startCmdWithSsh(fixPwd, app.conf.cmd, app.clusterId, app.id, nodeIp)
            JSONObject r = AgentCaller.instance.agentScriptExe(clusterId, nodeIp, 'replace pid', [appId: app.id, id: id, pid: pid])
            resp.end r.toJSONString()

            if (app.conf.cpusetCpus) {
                HostProcessSupport.instance.setProcessCpuset(pid, app.conf.cpusetCpus, app.clusterId, nodeIp)
            }
            return
        }
    }

    def lock = SpiSupport.createLock()
    lock.lockKey = '/app/operate' + appId
    boolean isDone = lock.exe {
        JSONObject r = AgentCaller.instance.agentScriptExe(clusterId, nodeIp, scriptName, [id: id, readTimeout: readTimeout])
        resp.end r.toJSONString()
    }
    if (!isDone) {
        resp.json([error: 'get lock fail'])
    }
}

h.post('/api/container/create/tpl') { req, resp ->
    /*
    def content = Agent.instance.post('/dms/api/container/create/tpl',
            [clusterId       : createConf.clusterId,
             appId           : createConf.appId,
             appIdList       : createConf.appIdList,
             nodeIp          : createConf.nodeIp,
             nodeIpList      : createConf.nodeIpList,
             targetNodeIpList: createConf.conf.targetNodeIpList,
             instanceIndex   : createConf.instanceIndex,
             containerNumber : conf.containerNumber,
             envList         : envList,
             imageTplId      : one.imageTplId], String)
     */
    def map = req.bodyAs(HashMap)
    int clusterId = map.clusterId as int
    int appId = map.appId as int
    int imageTplId = map.imageTplId as int

    def app = new AppDTO(id: appId).one()
    def tplOne = new ImageTplDTO(id: imageTplId).one()
    def paramList = app.conf.fileVolumeList.find { it.imageTplId == imageTplId }.paramList
    paramList.each {
        // value is always string, use as to transfer Type.
        // eg. def appId = super.binding.getProperty('appId') as int
        map[it.key] = it.value
    }
    map.conf = app.conf
    map.applications = ContainerMountFileGenerator.prepare(User.Admin, clusterId)

    // all, maybe too many jobs, only one cluster may be ok
    def instance = InMemoryCacheSupport.instance
    List<AppDTO> appMonitorList = instance.appList.findAll {
        it.status == AppDTO.Status.auto && it.monitorConf != null && it.monitorConf
    }
    map.appMonitorList = appMonitorList

    List<AppDTO> appLogList = instance.appList.findAll {
        it.status == AppDTO.Status.auto && it.logConf != null && it.logConf
    }
    map.appLogList = appLogList

    def content = CachedGroovyClassLoader.instance.eval(tplOne.content, map)
    resp.end content
}

h.group('/container') {
    h.get('/list') { req, resp ->
        def appId = req.param('appId')
        assert appId

        def app = new AppDTO(id: appId as int).one()
        assert app

        User u = req.attr('user') as User

        def instance = InMemoryAllContainerManager.instance
        List<ContainerInfo> containerList = instance.getContainerList(app.clusterId, app.id, null, u) ?: []
        def simpleContainerList = containerList.collect { x ->
            x.simple()
        }
        [isMonitorOn: app.monitorConf as Boolean, list: simpleContainerList]
    }.get('/log') { req, resp ->
        def id = req.param('id')
        assert id

        def instance = InMemoryAllContainerManager.instance
        def nodeIp = instance.getNodeIpByContainerId(id)
        if (!nodeIp) {
            resp.halt(500, 'no node ip get')
        }

        def appId = instance.getAppIpByContainerId(id)
        if (!appId) {
            resp.halt(500, 'no app get')
        }

        User u = req.attr('user') as User
        if (!u.isAccessApp(appId)) {
            resp.halt(403, 'not this app manager')
        }

        def clusterId = InMemoryCacheSupport.instance.getClusterIdByAppId(appId)

        def since = req.param('since')
        def tail = req.param('tail')
        def r = AgentCaller.instance.agentScriptExeBody(clusterId, nodeIp, 'container log viewer',
                [id: id, since: since, tail: tail, isBodyRaw: 1])
        resp.end r
    }
}
