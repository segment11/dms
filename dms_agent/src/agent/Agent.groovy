package agent

import agent.script.ScriptHolder
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.dockerjava.api.DockerClient
import com.github.kevinsawicki.http.HttpRequest
import com.segment.common.Conf
import com.segment.common.job.IntervalJob
import common.*
import ex.HttpInvokeException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.json.KVPair
import model.json.LiveCheckConf
import org.hyperic.sigar.Sigar
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.json.DefaultJsonTransformer
import org.segment.web.json.JsonTransformer
import support.ToJson
import transfer.ContainerInfo
import transfer.NodeInfo
import transfer.X

@CompileStatic
@Singleton
@Slf4j
class Agent extends IntervalJob {
    String version = '1.2'

    String serverHost

    int serverPort

    // no need if agent can directly visit dms server
    String proxyNodeIp

    int proxyNodePort

    int clusterId

    String secret

    int connectTimeout = 500

    int readTimeout = 5000

    String nodeIp

    String authToken

    DockerClient docker

    Sigar sigar

    LimitQueue<Event> eventQueue = new LimitQueue<>(100)

    private JsonTransformer json = new DefaultJsonTransformer()

    void addEvent(Event event) {
        event.createdDate = new Date()
        eventQueue << event
    }

    boolean isSendNodeInfoOk = true

    boolean isSendContainerInfoOk = true

    boolean isLiveCheckOk = true

    void init() {
        def c = Conf.instance
        serverHost = c.getString('serverHost', 'localhost')
        serverPort = c.getInt('serverPort', Const.SERVER_HTTP_LISTEN_PORT)
        clusterId = c.getInt('clusterId', 1)
        secret = c.getString('secret', '1')
        connectTimeout = c.getInt('agentConnectTimeout', 500)
        readTimeout = c.getInt('agentReadTimeout', 5000)

        proxyNodeIp = c.get('proxyNodeIp')
        proxyNodePort = c.getInt('proxyNodePort', Const.AGENT_HTTP_LISTEN_PORT)

        auth()
        sigar = new Sigar()
        nodeIp = com.segment.common.Utils.localIp()
        interval = c.getInt('agentIntervalSeconds', 10)
    }

    <T> T get(String uri, Map<String, Object> params = null, Class<T> clz = String,
                     Closure<Void> failCallback = null) {
        def needProxy = proxyNodeIp && proxyNodeIp != nodeIp
        String serverHttpServerUrl = needProxy ?
                'http://' + proxyNodeIp + ':' + proxyNodePort :
                'http://' + serverHost + ':' + serverPort

        def req = HttpRequest.get(serverHttpServerUrl + uri, params ?: [:], true).
                connectTimeout(connectTimeout).readTimeout(readTimeout).
                header(Const.AUTH_TOKEN_HEADER, authToken ?: '').
                header(Const.CLUSTER_ID_HEADER, clusterId)
        if (needProxy) {
            req.header(Const.PROXY_TARGET_SERVER_ADDR_HEADER,
                    'http://' + serverHost + ':' + serverPort)
            req.header(Const.PROXY_READ_TIMEOUT_HEADER, readTimeout.toString())
        }
        def body = req.body()
        if (req.code() != 200) {
            if (failCallback) {
                log.warn('agent get server info fail - ' + uri + ' - ' + params + ' - ' + body)
                failCallback.call(body)
            } else {
                throw new HttpInvokeException('agent get server info fail - ' +
                        uri + ' - ' + params + ' - ' + body)
            }
        }
        if (clz == String) {
            return body as T
        }
        JSON.parseObject(body, clz)
    }

    <T> T post(String uri, Object params, Class<T> clz = String,
                      Closure failCallback = null) {
        def needProxy = proxyNodeIp && proxyNodeIp != nodeIp
        String serverHttpServerUrl = needProxy ?
                'http://' + proxyNodeIp + ':' + proxyNodePort :
                'http://' + serverHost + ':' + serverPort

        def req = HttpRequest.post(serverHttpServerUrl + uri).
                connectTimeout(connectTimeout).readTimeout(readTimeout).
                header(Const.AUTH_TOKEN_HEADER, authToken ?: '').
                header(Const.CLUSTER_ID_HEADER, clusterId).
                header('X-REAL-IP', nodeIp)
        if (needProxy) {
            req.header(Const.PROXY_TARGET_SERVER_ADDR_HEADER,
                    'http://' + serverHost + ':' + serverPort)
            req.header(Const.PROXY_READ_TIMEOUT_HEADER, readTimeout.toString())
        }
        def sendBody = json.json(params)
        def body = req.send(sendBody).body()
        if (req.code() != 200) {
            if (failCallback) {
                log.warn('agent post server info fail - ' + uri + ' - ' + sendBody + ' - ' + body)
                failCallback.call(body)
            } else {
                throw new HttpInvokeException('agent post server info fail - ' +
                        uri + ' - ' + sendBody + ' - ' + body)
            }
        }
        if (clz == String) {
            return body as T
        }
        JSON.parseObject(body, clz)
    }

    void addJobStep(int jobId, int instanceIndex, String title, Map data, int costMs = 0) {
        data.jobId = jobId
        data.instanceIndex = instanceIndex
        data.title = title
        data.costMs = costMs
        post('/dms/api/job/step/add', data, null) { body ->
            log.info 'add job step fail as body: ', body
        }
    }

    void auth() {
        Map<String, Object> p = [:]
        p.secret = secret
        authToken = get('/dms/agent/auth', p)
    }

    synchronized void sendNode() {
        def info = new NodeInfo()
        info.nodeIp = nodeIp
        info.clusterId = clusterId
        info.version = version
        info.isLiveCheckOk = isLiveCheckOk

        AgentHelper.collectNodeSigarInfo(info, sigar)

        info.time = new Date()
        AgentTempInfoHolder.instance.addNode(info)

        isSendNodeInfoOk = true
        def body = post('/dms/api/hb/node', info, String) { body ->
            isSendNodeInfoOk = false
        }
        if (isSendNodeInfoOk) {
            def jo = JSON.parseObject(body)
            String scriptContent = ScriptHolder.instance.getContentByName('after node hb')
            if (scriptContent) {
                Map<String, Object> params = [:]
                params.jo = jo
                try {
                    CachedGroovyClassLoader.instance.eval(scriptContent, params)
                } catch (Exception e) {
                    log.error('after node hb script execute error', e)
                }
            }
        }
    }

    private long lastSendTimeMillis = 0

    // send all for agent script eval
    synchronized void sendContainer() {
        // do not send if just sent
        if ((System.currentTimeMillis() - lastSendTimeMillis) < 1000) {
            return
        }

        def containers = collectContainers()
        liveCheck(containers)
        sendContainers(containers)

        lastSendTimeMillis = System.currentTimeMillis()
    }

    private List<ContainerInfo> collectContainers() {
        List<ContainerInfo> list = []
        list.addAll(collectContainersFromProcess())

        if (!Conf.instance.isOn('collectDockerDaemon')) {
            return list
        }

        def containers = docker.listContainersCmd().withShowAll(true).exec()
        List<ContainerInfo> listDaemon = containers.collect {
            def containerInfo = AgentHelper.convert(it)
            containerInfo.nodeIp = nodeIp
            containerInfo.clusterId = clusterId

            if (containerInfo.running()) {
                // get pid
                def inspect = docker.inspectContainerCmd(containerInfo.id).exec()
                def pid = inspect.state.pidLong
                containerInfo.pid = pid

                // get mem
                def procMem = sigar.getProcMem(pid)
                if (procMem) {
                    containerInfo.memResident = procMem.resident
                }

                // set env
                def env = inspect.config.env
                if (env) {
                    for (envStr in env) {
                        def arr = envStr.split('=')
                        def envValue = arr.length == 1 ? '' :
                                (arr.length > 2 ? arr[1..-1].join('=') : arr[-1])
                        containerInfo.envList << new KVPair<String>(arr[0], envValue)
                    }
                }
            }

            containerInfo
        }.findAll {
            // only send those dms created
            it.names.any { n ->
                n.contains(ContainerHelper.CONTAINER_NAME_PRE)
            }
        }

        list.addAll listDaemon
        list
    }

    private List<ContainerInfo> collectContainersFromProcess() {
        List<ContainerInfo> list = []

        // from process wrap
        final String subDirPre = 'app_'
        final File dir = new File('/opt/dms')
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            return list
        }

        for (File f in dir.listFiles()) {
            if (!f.isDirectory() || !f.name.startsWith(subDirPre)) {
                continue
            }

            def wrapJsonFile = new File(f, '/container-info.json')
            if (!wrapJsonFile.exists()) {
                continue
            }

            ContainerInfo containerInfo = ToJson.read(wrapJsonFile.text, ContainerInfo)
            // id -> "process_app_${c.appId}_${c.instanceIndex}_pid_${pid}".toString()
            long pid = containerInfo.id.split('_')[-1] as long
            containerInfo.pid = pid
            try {
                def procState = sigar.getProcState(pid)
                if (procState) {
                    containerInfo.state = ContainerInfo.STATE_RUNNING
                } else {
                    containerInfo.state = ContainerInfo.STATE_EXITED
                }

                def procMem = sigar.getProcMem(pid)
                if (procMem) {
                    containerInfo.memResident = procMem.resident
                }

                def procTime = sigar.getProcTime(pid)
                if (procTime) {
                    containerInfo.status = 'start at: ' + new Date(procTime.startTime)
                } else {
                    containerInfo.status = 'unknown'
                }
            } catch (Exception ignored) {
                containerInfo.state = 'exited'
                containerInfo.status = 'unknown'
            }
            list << containerInfo
        }

        list
    }

    private void sendContainers(List<ContainerInfo> list) {
        def x = new X()
        x.nodeIp = nodeIp
        x.clusterId = clusterId
        x.containers = list

        isSendContainerInfoOk = true
        post('/dms/api/hb/container', x, String) { body ->
            isSendContainerInfoOk = false
        }
    }

    private Map<String, Boolean> containerLiveCheckResultCache = new HashMap<>()

    private void liveCheck(List<ContainerInfo> containers) {
        def runningList = containers.findAll { it.running() }
        List<OneContainerId> containerIdList = runningList.collect {
            new OneContainerId(it.appId(), it.instanceIndex())
        }
        if (!containerIdList) {
            isLiveCheckOk = true
            return
        }

        Map<Integer, LiveCheckConf> confMap = [:]
        String body = post('/dms/api/app/live-check-conf/query', [appIdList: containerIdList.collect { it.appId }])
        def arr = JSON.parseArray(body)
        for (one in arr) {
            def obj = one as JSONObject
            confMap[obj.getInteger('id')] = JSON.parseObject(obj.getJSONObject('liveCheckConf').toString(), LiveCheckConf)
        }

        Map<Integer, Boolean> liveCheckResult = [:]
        confMap.each { appId, liveCheckConf ->
            def container = runningList.find {
                appId == it.appId()
            }
            if (!container) {
                return
            }

            boolean isCheckOk

            def decimal = liveCheckConf.intervalSeconds / interval
            def ceil = Math.ceil(decimal.doubleValue()) as int
            if (intervalCount % ceil != 0) {
                Boolean cachedResult = containerLiveCheckResultCache.get(container.id)
                if (cachedResult != null) {
                    isCheckOk = cachedResult.booleanValue()
                } else {
                    isCheckOk = true
                }
            } else {
                isCheckOk = liveCheckOneContainer(appId, container, liveCheckConf)
                containerLiveCheckResultCache.put(container.id, Boolean.valueOf(isCheckOk))
            }
            liveCheckResult[appId] = isCheckOk
            container.isLiveCheckOk = isCheckOk
            return
        }
        isLiveCheckOk = liveCheckResult.every { it.value }
    }

    // shell execute in docker only can be done in agent, server can only check port listen and http request result
    private boolean liveCheckOneContainer(int appId, ContainerInfo container, LiveCheckConf conf) {
        try {
            if (conf.isShellScript && docker != null) {
                def shellScript = conf.shellScript
                addEvent Event.builder().type(Event.Type.node).reason('live check').result(appId).
                        build().log(shellScript)
                if (!shellScript) {
                    return false
                }

                for (line in shellScript.readLines()) {
                    String cmdLine = line.trim()
                    if (!cmdLine) {
                        continue
                    }
                    String[] cmd = ['sh', '-c', cmdLine]
                    def response = docker.execCreateCmd(container.id).
                            withAttachStdout(true).
                            withAttachStderr(true).
                            withCmd(cmd).exec()
                    def is = docker.execStartCmd(response.id).stdin
                    String shellResult = Utils.readFully(is)
                    if (LiveCheckConf.SHELL_RESULT_OK != shellResult) {
                        addEvent Event.builder().type(Event.Type.node).reason('live check fail').result(appId).
                                build().log('shell <- ' + cmdLine)
                        return false
                    }
                }
                return true
            } else if (conf.isPortListen) {
                def publicPort = container.publicPort(conf.port)
                addEvent Event.builder().type(Event.Type.node).reason('live check').result(appId).
                        build().log('port listen ' + publicPort)
                if (Utils.isPortListenAvailable(publicPort)) {
                    addEvent Event.builder().type(Event.Type.node).reason('live check fail').result(appId).
                            build().log('port listen ' + publicPort)
                    return false
                } else {
                    return true
                }
            } else if (conf.isHttpRequest) {
                def publicPort = container.publicPort(conf.port)
                // eg. /health
                def uri = conf.httpRequestUri
                String url = 'http://' + nodeIp + ':' + publicPort + uri
                addEvent Event.builder().type(Event.Type.node).reason('live check').result(appId).
                        build().log('http check ' + url)

                try {
                    def req = HttpRequest.get(url)
                    def timeout = conf.httpTimeoutSeconds * 1000
                    def code = req.connectTimeout(timeout).readTimeout(timeout).code()
                    if (code != 200) {
                        addEvent Event.builder().type(Event.Type.node).reason('live check fail').result(appId).
                                build().log('http check ' + url + ' body: ' + req.body())
                        return false
                    } else {
                        return true
                    }
                } catch (Exception ee) {
                    log.warn('http check error: ' + ee.message)
                    addEvent Event.builder().type(Event.Type.node).reason('live check fail').result(appId).
                            build().log('http check ' + url + ' error: ' + ee.message)
                    // ignore
                    return false
                }
            } else {
                return true
            }
        } catch (Exception e) {
            log.error('live check error - ' + appId, e)
            return false
        }
    }

    @Override
    String name() {
        'dms agent'
    }

    @Override
    void doJob() {
        sendNode()
        sendContainer()
    }

    @Override
    void stop() {
        super.stop()
        if (docker) {
            docker.close()
            log.info 'done close docker client'
        }
        if (sigar) {
            sigar.close()
            log.info 'done close sigar client'
        }
    }
}
