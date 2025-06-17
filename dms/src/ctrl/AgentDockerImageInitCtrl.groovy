package ctrl

import auth.User
import deploy.InitAgentEnvSupport
import model.NodeKeyPairDTO
import org.segment.web.handler.ChainHandler
import server.AgentCaller
import server.InMemoryCacheSupport

def h = ChainHandler.instance

h.post('/agent/image/init/load') { req, resp ->
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

    def clusterOne = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
    def proxyInfo = clusterOne.globalEnvConf.getProxyInfo(kp.ip)
    def needProxy = proxyInfo && proxyInfo.proxyNodeIp != kp.ip

    def support = new InitAgentEnvSupport(kp)
    String imageTarGzName = params.imageTarGzName
    String localFilePath = support.userHomeDir + '/images/' + imageTarGzName

    if (!needProxy) {
        boolean isCopyDone = support.copyFileIfNotExists(localFilePath, false, true)
        if (!isCopyDone) {
            return [flag   : isCopyDone,
                    steps  : support.getSteps(),
                    message: 'Please view log for detail']
        }
        boolean isLoadOk = support.loadDockerImage(localFilePath)
        return [flag   : isLoadOk,
                steps  : support.getSteps(),
                message: 'Please view log for detail']
    } else {
        List steps = []
        def copyR = AgentCaller.instance.doSshCopy(kp, null, localFilePath, localFilePath,
                null, 30000, [isTarX: false, isMkdir: true])
        def isCopyOk = copyR.getBoolean('flag').booleanValue()
        steps.addAll copyR.getJSONArray('steps')
        if (!isCopyOk) {
            return [flag   : isCopyOk,
                    steps  : steps,
                    message: 'Please view log for detail']
        }
        def loadR = AgentCaller.instance.doSshLoadDockerImage(kp, localFilePath)
        def isLoadOk = loadR.getBoolean('flag').booleanValue()
        steps.addAll loadR.getJSONArray('steps')
        return [flag   : isLoadOk,
                steps  : steps,
                message: 'Please view log for detail']
    }
}

h.get('/agent/image/init/view') { req, resp ->
    User u = req.attr('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    String id = req.param('id')
    assert id

    def kp = new NodeKeyPairDTO(id: id as int).one()
    assert kp

    AgentCaller.instance.agentScriptExe(kp.clusterId, kp.ip, 'image view')
}
