package server

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.kevinsawicki.http.HttpRequest
import common.AgentConf
import common.Const
import common.Utils
import deploy.InitAgentEnvSupport
import deploy.OneCmd
import ex.HttpInvokeException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.NodeKeyPairDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonWriter

@CompileStatic
@Singleton
@Slf4j
class AgentCaller {

    int connectTimeout = 500

    int readTimeout = 2000

    private <T> T httpRequest(int clusterId, String nodeIp, String uri, Map params, Class<T> clz = String,
                              Closure failCallback, boolean isPost = false) {
        def one = InMemoryCacheSupport.instance.oneCluster(clusterId as int)
        String proxyNodeIp = one.globalEnvConf.proxyNodeIp
        Integer proxyNodePort = one.globalEnvConf.proxyNodePort

        def needProxy = proxyNodeIp && proxyNodeIp != nodeIp
        String agentHttpServerUrl = needProxy ?
                'http://' + proxyNodeIp + ':' + proxyNodePort :
                'http://' + nodeIp + ':' + Const.AGENT_HTTP_LISTEN_PORT

        def targetNodeIp = needProxy ? proxyNodeIp : nodeIp
        String authToken = InMemoryAllContainerManager.instance.getAuthToken(targetNodeIp)
        if (!authToken) {
            if (failCallback) {
                failCallback.call('target node ip is not connected - ' + targetNodeIp)
            } else {
                throw new HttpInvokeException('target node ip is not connected - ' + targetNodeIp)
            }
        }

        String authTokenRealServer = InMemoryAllContainerManager.instance.getAuthToken(nodeIp)

        try {
            def req = isPost ? HttpRequest.post(agentHttpServerUrl + uri) :
                    HttpRequest.get(agentHttpServerUrl + uri, params ?: [:], true)

            int readTimeoutFinal
            if (params?.readTimeout) {
                readTimeoutFinal = params.readTimeout as int
            } else {
                readTimeoutFinal = readTimeout
            }
            req.connectTimeout(connectTimeout).readTimeout(readTimeoutFinal)

            if (needProxy) {
                req.header(Const.PROXY_READ_TIMEOUT_HEADER, readTimeoutFinal.toString())
                req.header(Const.AUTH_TOKEN_HEADER, authTokenRealServer ?: '')
                req.header(Const.PROXY_TARGET_SERVER_ADDR_HEADER,
                        'http://' + nodeIp + ':' + Const.AGENT_HTTP_LISTEN_PORT)
            } else {
                req.header(Const.AUTH_TOKEN_HEADER, authToken)
            }

            def scriptName = params.scriptName
            if (scriptName) {
                req.header(Const.SCRIPT_NAME_HEADER, scriptName.toString())
            }

            if (isPost) {
                def sendBody = JsonWriter.instance.json(params ?: [:])
                req.send(sendBody)
            }
            def body = req.body()
            if (req.code() != 200) {
                if (failCallback) {
                    log.warn('server get agent info fail - ' + uri + ' - ' + params + ' - ' + body)
                    failCallback.call(body)
                } else {
                    throw new HttpInvokeException('server get agent info fail - ' + uri + ' - ' + params + ' - ' + body)
                }
            }
            if (clz == String) {
                return body as T
            }
            JSON.parseObject(body, clz)
        } catch (Exception e) {
            if (failCallback) {
                failCallback.call(Utils.getStackTraceString(e))
            } else {
                throw e
            }
        }
    }

    public <T> T get(int clusterId, String nodeIp, String uri, Map params = null, Class<T> clz = String,
                     Closure<Void> failCallback = null) {
        httpRequest(clusterId, nodeIp, uri, params, clz, failCallback)
    }

    public <T> T post(int clusterId, String nodeIp, String uri, Map params = null, Class<T> clz = String,
                      Closure failCallback = null) {
        httpRequest(clusterId, nodeIp, uri, params, clz, failCallback, true)
    }

    public <T> T agentScriptExeAs(int clusterId, String nodeIp, String scriptName, Class<T> clz, Map params = null,
                                  Closure failCallback = null) {
        Map p = params ?: [:]
        p.scriptName = scriptName
        String body = post(clusterId, nodeIp, '/dmc/script/exe', p, String, failCallback)
        JSONObject.parseObject(body, clz)
    }

    JSONObject agentScriptExe(int clusterId, String nodeIp, String scriptName, Map params = null,
                              Closure failCallback = null) {
        Map p = params ?: [:]
        p.scriptName = scriptName
        String body = post(clusterId, nodeIp, '/dmc/script/exe', p, String, failCallback)
        JSONObject.parseObject(body)
    }

    String agentScriptExeBody(int clusterId, String nodeIp, String scriptName, Map params = null,
                              Closure failCallback = null) {
        Map p = params ?: [:]
        p.scriptName = scriptName
        String body = post(clusterId, nodeIp, '/dmc/script/exe', p, String, failCallback)
        body
    }

    JSONObject doSshByScriptName(NodeKeyPairDTO kp, String scriptName, int readTimeout, Map ext = null) {
        Map params = [:]
        params.ip = kp.ip
        params.port = kp.sshPort
        params.user = kp.user
        params.rootPass = kp.rootPass
        params.keyPrivate = kp.keyPrivate

        params.readTimeout = readTimeout

        if (ext) {
            params.putAll(ext)
        }

        def one = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
        String proxyNodeIp = one.globalEnvConf.proxyNodeIp

        agentScriptExe(kp.clusterId, proxyNodeIp, scriptName, params)
    }

    JSONObject doSshCopy(NodeKeyPairDTO kp, String localFilePath, String remoteFilePath,
                         String localFileContent = null, int readTimeout = 30000, Map extParams = null) {
        Map ext = [:]
        ext.localFilePath = localFilePath
        ext.remoteFilePath = remoteFilePath
        ext.localFileContent = localFileContent

        ext.isTarX = localFilePath.contains('.tar')
        ext.isMkdir = false
        ext.isOverwrite = false

        if (extParams) {
            extParams.each { k, v ->
                ext[k] = v
            }
        }

        final String scriptName = 'ssh copy'
        doSshByScriptName(kp, scriptName, readTimeout, ext)
    }

    JSONObject doSshExec(NodeKeyPairDTO kp, String command, int readTimeout = 30000) {
        Map ext = [:]
        ext.command = command

        final String scriptName = 'ssh exec'
        doSshByScriptName(kp, scriptName, readTimeout, ext)
    }

    JSONObject doSshShell(NodeKeyPairDTO kp, List<OneCmd> cmdList, int readTimeout = 30000) {
        Map ext = [:]
        ext.cmdListJson = new DefaultJsonTransformer().json(cmdList)

        final String scriptName = 'ssh exec'
        doSshByScriptName(kp, scriptName, readTimeout, ext)
    }

    JSONObject doSshResetRootPassword(NodeKeyPairDTO kp, int readTimeout = 10000) {
        final String scriptName = 'ssh reset root password'
        doSshByScriptName(kp, scriptName, readTimeout)
    }

    JSONObject doSshInitAgent(NodeKeyPairDTO kp, int readTimeout = 30000) {
        final String scriptName = 'ssh init agent'
        doSshByScriptName(kp, scriptName, readTimeout)
    }

    JSONObject doSshCopyAgentConf(NodeKeyPairDTO kp, AgentConf agentConf, int readTimeout = 10000) {
        String localFileContent = agentConf.generate()
        String destAgentDir = InitAgentEnvSupport.AGENT_FILE.replace('.tar.gz', '')
        String localFilePath = destAgentDir + '/conf.properties'
        doSshCopy(kp, localFilePath, localFilePath, localFileContent, readTimeout)
    }

    JSONObject doSshStartAgent(NodeKeyPairDTO kp, int readTimeout = 10000) {
        final String scriptName = 'ssh start agent'
        doSshByScriptName(kp, scriptName, readTimeout)
    }

    JSONObject doSshStopAgent(NodeKeyPairDTO kp, int readTimeout = 10000) {
        final String scriptName = 'ssh stop agent'
        doSshByScriptName(kp, scriptName, readTimeout)
    }

    JSONObject doSshLoadDockerImage(NodeKeyPairDTO kp, String localFilePath, int readTimeout = 20000) {
        Map ext = [:]
        ext.localFilePath = localFilePath

        final String scriptName = 'ssh load image'
        doSshByScriptName(kp, scriptName, readTimeout, ext)
    }
}
