package ctrl

import auth.User
import deploy.InitAgentEnvSupport
import model.NodeKeyPairDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import server.AgentCaller
import server.InMemoryCacheSupport

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.post('/agent/image/init/load') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    Map params = req.bodyAs()
    String id = params.id
    assert id

    String imageTarGzName = params.imageTarGzName
    String localFilePath = InitAgentEnvSupport.BASE_DIR + '/images/' + imageTarGzName

    def kp = new NodeKeyPairDTO(id: id as int).one()
    assert kp

    if (!kp.rootPass) {
        return [flag: false, message: 'Root password need init']
    }

    def clusterOne = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
    def conf = clusterOne.globalEnvConf

    def support = new InitAgentEnvSupport(kp.clusterId, conf.proxyNodeIp)
    if (!support.proxyNodeIp || support.proxyNodeIp == kp.ip) {
        boolean isCopyDone = support.copyFileIfNotExists(kp, localFilePath, false, true)
        if (!isCopyDone) {
            return [flag   : isCopyDone,
                    steps  : support.getSteps(kp),
                    message: 'Please view log for detail']
        }
        boolean isLoadOk = support.loadDockerImage(kp, localFilePath)
        return [flag   : isLoadOk,
                steps  : support.getSteps(kp),
                message: 'Please view log for detail']
    } else {
        List steps = []
        def copyR = AgentCaller.instance.doSshCopy(kp, localFilePath, localFilePath,
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
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    String id = req.param('id')
    assert id

    def kp = new NodeKeyPairDTO(id: id as int).one()
    assert kp

    AgentCaller.instance.agentScriptExe(kp.clusterId, kp.ip, 'image view')
}
