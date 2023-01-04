package server.gateway

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.kevinsawicki.http.HttpRequest
import com.github.zkclient.ZkClient
import com.github.zkclient.exception.ZkNoNodeException
import common.Conf
import common.Event
import ex.GatewayProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.GwClusterDTO
import model.GwFrontendDTO
import model.json.GatewayConf
import model.json.GwBackendServer
import model.json.GwFrontendRuleConf
import model.json.KVPair
import server.scheduler.processor.ContainerRunResult
import spi.SpiSupport

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Slf4j
class GatewayOperator {
    private GatewayConf conf

    private GatewayOperator(GatewayConf conf) {
        this.conf = conf
    }

    private static ConcurrentHashMap<Integer, GatewayOperator> cached = new ConcurrentHashMap<>();

    static GatewayOperator create(Integer appId, GatewayConf conf = null) {
        def x = cached[appId]
        if (x) {
            x.conf = conf
            return x
        }

        def one = new GatewayOperator(conf)
        def old = cached.putIfAbsent(appId, one)
        if (old) {
            old.conf = conf
            return old
        }
        one
    }

    static final int DEFAULT_WEIGHT = 10

    static String scheme(String nodeIp, int publicPort) {
        "http://${nodeIp}:${publicPort}"
    }

    List<String> getBackendServerUrlList() {
        def frontend = new GwFrontendDTO(id: conf.frontendId).one()
        def r = frontend.backend.serverList.collect {
            it.url
        }
        r.sort()
    }

    Map<Integer, List<GwBackendServer>> getBackendListFromApi(int clusterId) {
        def one = new GwClusterDTO(id: clusterId).one()
        def serverUrl = one.serverUrl + ':' + one.dashboardPort

        Map<Integer, List<GwBackendServer>> r = [:]
        def body = HttpRequest.get(serverUrl + '/api').connectTimeout(500).readTimeout(1000).body()
        def obj = JSON.parseObject(body)
        def zk = obj.getJSONObject('zk')
        if (!zk) {
            return r
        }

        def backends = zk.getJSONObject('backends')
        if (!backends) {
            return r
        }

        backends.each { k, v ->
            if (k.contains('backend')) {
                def backend = v as JSONObject
                def servers = backend.getJSONObject('servers')
                if (servers) {
                    List<GwBackendServer> serverUrlList = servers.values().collect {
                        def x = it as JSONObject
                        new GwBackendServer(url: x.getString('url'), weight: x.getInteger('weight'))
                    }
                    r[k.replace('backend', '') as Integer] = serverUrlList.sort { it.url }
                }
            }
        }

        r
    }

    List<String> getBackendServerUrlListFromApi() {
        def one = new GwClusterDTO(id: conf.clusterId).one()
        def serverUrl = one.serverUrl + ':' + one.dashboardPort

        List<String> r = []
        def body = HttpRequest.get(serverUrl + '/api').connectTimeout(500).readTimeout(1000).body()
        def obj = JSON.parseObject(body)
        def zk = obj.getJSONObject('zk')
        if (!zk) {
            return r
        }

        def backends = zk.getJSONObject('backends')
        if (!backends) {
            return r
        }

        def backend = backends.getJSONObject('backend' + conf.frontendId)
        if (!backend) {
            return r
        }

        def servers = backend.getJSONObject('servers')
        if (!servers) {
            return r
        }

        servers.values().collect {
            def x = it as JSONObject
            r << x.getString('url')
        }
        r.sort()
    }

    boolean isBackendServerListMatch() {
        def list = getBackendServerUrlList()
        def apiList = getBackendServerUrlListFromApi()
        list == apiList
    }

    synchronized boolean changeBackend(String serverUrl, boolean isAdd = true, int weight = DEFAULT_WEIGHT, boolean waitUntilListenTrigger = false) {
        def frontend = new GwFrontendDTO(id: conf.frontendId).one()
        def backend = frontend.backend

        boolean needUpdate
        if (isAdd) {
            def one = backend.serverList.find { it.url == serverUrl }
            if (one) {
                if (one.weight != weight) {
                    one.weight = weight
                    needUpdate = true
                }
            } else {
                backend.serverList.add(new GwBackendServer(url: serverUrl, weight: weight))
                needUpdate = true
            }
        } else {
            def one = backend.serverList.find { it.url == serverUrl }
            if (one) {
                backend.serverList.remove(one)
            }
            needUpdate = true
        }

        boolean r
        if (needUpdate) {
            Event.builder().type(Event.Type.cluster).reason('gateway backend ' + (isAdd ? 'add' : 'remove')).
                    result(frontend.name).build().log(backend.serverList.toString()).toDto().add()

            frontend.update()

            def lock = SpiSupport.createLock()
            lock.lockKey = 'opt frontend ' + conf.frontendId
            boolean isDone = lock.exe {
                r = updateFrontend(frontend, waitUntilListenTrigger)
            }
            if (!isDone) {
                log.info 'get opt frontend lock fail - {}', conf.frontendId
                r = false
            }
        } else {
            r = true
        }
        log.info 'change backend result - {} for {} isAdd: {}', r, serverUrl, isAdd
        r
    }

    private static void addKVPair(List<KVPair<String>> list, String path, Object value) {
        list << new KVPair<String>(key: path, value: value.toString())
    }

    private synchronized static boolean deleteRecursive(ZkClient zk, String path) {
        if (!zk.exists(path)) {
            return true
        }
        List<String> children
        try {
            children = zk.getChildren(path)
        } catch (ZkNoNodeException e) {
            return true
        }
        if (children != null) {
            for (String subPath : children) {
                if (!deleteRecursive(zk, path + "/" + subPath)) {
                    return false
                }
            }
        }
        return zk.delete(path)
    }

    synchronized boolean updateFrontend(GwFrontendDTO frontend, boolean waitUntilListenTrigger = false) {
        def one = new GwClusterDTO(id: frontend.clusterId).one()
        def zk = ZkClientHolder.instance.create(one.zkConnectString)

        String rootPrefix
        if (!one.prefix.startsWith('/')) {
            rootPrefix = '/' + one.prefix
        } else {
            rootPrefix = one.prefix
        }

        String prefixFrontend = rootPrefix + '/frontends/frontend' + frontend.id
        String prefixBackend = rootPrefix + '/backends/backend' + frontend.id

        List<KVPair<String>> list = []
        addKVPair(list, prefixFrontend + '/backend', 'backend' + frontend.id)
        addKVPair(list, prefixFrontend + '/priority', frontend.priority)
        addKVPair(list, prefixFrontend + '/passhostheader', frontend.conf.passHostHeader)

        frontend.conf.ruleConfList.eachWithIndex { GwFrontendRuleConf it, int i ->
            addKVPair(list, prefixFrontend + "/routes/rule_${i}/rule", "${it.type}${it.rule}")
        }
        frontend.auth.basicList.eachWithIndex { KVPair it, int i ->
            def val = it.key + ':' + HttpRequest.Base64.encode("${it.value}")
            addKVPair(list, prefixFrontend + "/auth/basic/users/${i}", val)
        }

        def backend = frontend.backend
        if (backend.maxConn) {
            addKVPair(list, prefixBackend + "/maxconn/amount", backend.maxConn)
        }
        if (backend.loadBalancer) {
            addKVPair(list, prefixBackend + "/loadbalancer/method", backend.loadBalancer)
        }
        if (backend.circuitBreaker) {
            addKVPair(list, prefixBackend + "/circuitbreaker/expression", backend.circuitBreaker)
        }
        if (backend.stickiness) {
            addKVPair(list, prefixBackend + "/loadbalancer/sticky", backend.stickiness)
        }
        if (backend.healthCheckUri) {
            addKVPair(list, prefixBackend + "/healthcheck/path", backend.stickiness)
        }
        backend.serverList.eachWithIndex { GwBackendServer it, int i ->
            addKVPair(list, prefixBackend + "/servers/server${i}/url", it.url)
            addKVPair(list, prefixBackend + "/servers/server${i}/weight", it.weight)
        }

        // remove frontend/backend
        def r = deleteRecursive(zk, prefixFrontend)
        if (!r) {
            throw new GatewayProcessException('failed to delete - ' + prefixFrontend)
        }
        def r2 = deleteRecursive(zk, prefixBackend)
        if (!r2) {
            throw new GatewayProcessException('failed to delete - ' + prefixBackend)
        }

        list.each {
            def key = it.key
            def value = it.value
            if (!zk.exists(key)) {
                zk.createPersistent(key, true)
                zk.writeData(key, value.bytes)
                log.info 'write - {}:{}', key, value
            } else {
                def data = zk.readData(key)
                if (data && new String(data) == value.toString()) {
                    log.info 'skip - {}:{}', key, value
                } else {
                    zk.writeData(key, value.bytes)
                    log.info 'write - {}:{}', key, value
                }
            }
        }
        // trigger reload
        def r3 = deleteRecursive(zk, rootPrefix + '/leader')
        if (!r3) {
            throw new GatewayProcessException('failed to delete - ' + rootPrefix + '/leader')
        }

        if (!waitUntilListenTrigger) {
            return true
        }

        final int times = Conf.instance.getInt('gw.waitUntilListenTriggerTimes', 3)
        final int intervalSeconds = Conf.instance.getInt('gw.waitUntilListenTriggerIntervalSeconds', 1)

        Thread.sleep(intervalSeconds * 1000)
        for (i in (0..<times)) {
            boolean isMatch = isBackendServerListMatch()
            if (isMatch) {
                return true
            }
            if (i != times - 1) {
                Thread.sleep(intervalSeconds * 1000)
            }
        }
        false
    }

    boolean addBackend(ContainerRunResult result, int weight) {
        addBackend(result.nodeIp, result.port, weight)
    }

    boolean addBackend(String nodeIp, int port, int weight = DEFAULT_WEIGHT) {
        addBackend(scheme(nodeIp, port), true, weight)
    }

    boolean addBackend(String serverUrl, boolean waitDelayFirst = true, int weight = DEFAULT_WEIGHT) {
        if (!waitUntilHealthCheckOk(serverUrl, waitDelayFirst)) {
            return false
        }
        changeBackend(serverUrl, true, weight, true)
    }

    boolean removeBackend(String nodeIp, Integer port) {
        removeBackend(scheme(nodeIp, port))
    }

    boolean removeBackend(String serverUrl) {
        changeBackend(serverUrl, false, DEFAULT_WEIGHT, true)
    }

    boolean isBackendReady(String nodeIp, int port) {
        waitUntilHealthCheckOk(scheme(nodeIp, port), false)
    }

    private boolean waitUntilHealthCheckOk(String serverUrl, boolean waitDelayFirst) {
        if (!conf.healthCheckUri) {
            return true
        }

        String url = serverUrl + conf.healthCheckUri
        log.info 'begin health check - {}', url


        if (waitDelayFirst) {
            Thread.sleep((conf.healthCheckDelaySeconds ?: 3) * 1000)
        }

        def timeout = conf.healthCheckTimeoutSeconds ?: 3
        def times = conf.healthCheckTotalTimes ?: 3
        for (i in (0..<times)) {
            try {
                def req = HttpRequest.get(url).connectTimeout(timeout * 1000).readTimeout(timeout * 1000)
                def code = req.code()
                if (200 == code) {
                    log.info 'health check ready for {}', url
                    return true
                }
                log.warn 'health check fail for ' + url + ' - code - ' + code + ' - ' + req.body()
            } catch (Exception e) {
                log.error('health check error for ' + url, e)
            } finally {
                if (i != times - 1) {
                    Thread.sleep((conf.healthCheckIntervalSeconds ?: 10) * 1000)
                }
            }
        }
        return false
    }
}