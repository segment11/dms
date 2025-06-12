package server


import auth.User
import com.segment.common.Conf
import com.segment.common.job.IntervalJob
import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.NodeDTO
import transfer.ContainerInfo
import transfer.NodeInfo

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
@Slf4j
class InMemoryAllContainerManager extends IntervalJob implements AllContainerManager {
    // proxy, fuck OCP, in memory is ok, fuck redis
    // one dms server is ok, fuck http proxy, h2 database file embedded is ok, fuck master/slave.
    InRedisAllContainerManager inner = InRedisAllContainerManager.instance
    boolean isUseRedis = false

    void init() {
        isUseRedis = Conf.instance.get('redis.host') != null
        if (isUseRedis) {
            inner.init()
        }
    }

    private ConcurrentHashMap<String, List<ContainerInfo>> containersByNodeIp = new ConcurrentHashMap()
    private ConcurrentHashMap<String, NodeInfo> nodeInfoByNodeIp = new ConcurrentHashMap()
    private ConcurrentHashMap<String, Date> heartBeatDateByNodeIp = new ConcurrentHashMap()
    private ConcurrentHashMap<String, String> authTokenByNodeIp = new ConcurrentHashMap()

    void clear() {
        containersByNodeIp.clear()
        nodeInfoByNodeIp.clear()
        heartBeatDateByNodeIp.clear()
        authTokenByNodeIp.clear()
    }

    @Override
    void addAuthToken(String nodeIp, String authToken) {
        if (isUseRedis) {
            inner.addAuthToken(nodeIp, authToken)
            return
        }
        authTokenByNodeIp[nodeIp] = authToken
    }

    @Override
    String getAuthToken(String nodeIp) {
        if (isUseRedis) {
            return inner.getAuthToken(nodeIp)
        }
        authTokenByNodeIp[nodeIp]
    }

    @Override
    void addNodeInfo(String nodeIp, NodeInfo node) {
        if (isUseRedis) {
            inner.addNodeInfo(nodeIp, node)
            return
        }
        heartBeatDateByNodeIp[nodeIp] = node.hbTime
        nodeInfoByNodeIp[nodeIp] = node
    }

    @Override
    NodeInfo getNodeInfo(String nodeIp) {
        if (isUseRedis) {
            return inner.getNodeInfo(nodeIp)
        }
        nodeInfoByNodeIp[nodeIp]
    }

    @Override
    Date getHeartBeatDate(String nodeIp) {
        if (isUseRedis) {
            return inner.getHeartBeatDate(nodeIp)
        }
        heartBeatDateByNodeIp[nodeIp]
    }

    @Override
    List<NodeDTO> getHeartBeatOkNodeList(int clusterId) {
        def dat = Utils.getNodeAliveCheckLastDate(3)
        def r = new NodeDTO().where('cluster_id = ?', clusterId).
                where('updated_date > ?', dat).list()
        r.sort { a, b -> Utils.compareIp(a.ip, b.ip) }
        r
    }

    List<NodeDTO> hbOkNodeList(int clusterId, String fields) {
        def dto = new NodeDTO(clusterId: clusterId)
        if (fields) {
            dto.queryFields(fields)
        }
        def nodeList = dto.list()

        def dat = Utils.getNodeAliveCheckLastDate(3)
        nodeList.findAll { one ->
            def hbData = getHeartBeatDate(one.ip)
            hbData && hbData > dat
        }
    }

    @Override
    Map<String, NodeInfo> getAllNodeInfo(Integer clusterId) {
        if (isUseRedis) {
            return inner.getAllNodeInfo(clusterId)
        }
        new HashMap<String, NodeInfo>(nodeInfoByNodeIp)
    }

    @Override
    List<NodeInfo> getHbOkNodeInfoList(Integer clusterId) {
        if (isUseRedis) {
            return inner.getHbOkNodeInfoList(clusterId)
        }
        List<NodeInfo> list = []
        nodeInfoByNodeIp.values().each {
            it.checkIfOk(getHeartBeatDate(it.nodeIp))
            if (it.isOk) {
                list << it
            }
        }
        list
    }

    @Override
    void addContainers(Integer clusterId, String nodeIp, List<ContainerInfo> containers) {
        if (isUseRedis) {
            inner.addContainers(clusterId, nodeIp, containers)
            return
        }
        heartBeatDateByNodeIp[nodeIp] = new Date()
        containersByNodeIp[nodeIp] = containers
    }

    @Override
    List<ContainerInfo> getContainerListByNodeIp(String nodeIp) {
        if (isUseRedis) {
            return inner.getContainerListByNodeIp(nodeIp)
        }
        containersByNodeIp[nodeIp]
    }

    @Override
    String getNodeIpByContainerId(String containerId) {
        if (isUseRedis) {
            return inner.getNodeIpByContainerId(containerId)
        }
        for (entry in containersByNodeIp) {
            def nodeIp = entry.key
            def list = entry.value
            for (x in list) {
                if (containerId == x.id) {
                    return nodeIp
                }
            }
        }
        null
    }

    @Override
    Integer getAppIpByContainerId(String containerId) {
        if (isUseRedis) {
            return inner.getAppIpByContainerId(containerId)
        }
        for (entry in containersByNodeIp) {
            def list = entry.value
            for (x in list) {
                if (containerId == x.id) {
                    return x.appId()
                }
            }
        }
        null
    }

    List<ContainerInfo> getRunningContainerList(int clusterId, int appId) {
        getContainerList(clusterId, appId).findAll { x -> x.running() }
    }

    @Override
    List<ContainerInfo> getContainerList(int clusterId, int appId = 0,
                                         String nodeIp = null, User user = null) {
        if (isUseRedis) {
            return inner.getContainerList(clusterId, appId, nodeIp, user)
        }

        Set<Integer> userAccessAppIdSet
        if (user && !user.isAdmin()) {
            userAccessAppIdSet = user.getAccessAppIdSet(clusterId)
            if (appId != 0 && appId !in userAccessAppIdSet) {
                return []
            }
        } else {
            userAccessAppIdSet = []
        }

        List<ContainerInfo> list = []
        containersByNodeIp.each { k, v ->
            if (nodeIp) {
                if (nodeIp == k) {
                    list.addAll(v)
                }
            } else {
                list.addAll(v)
            }
        }
        if (!list) {
            return []
        }

        List<ContainerInfo> filterList
        if (appId) {
            filterList = list.findAll { it.appId() == appId }
        } else {
            if (user && !user.isAdmin()) {
                filterList = list.findAll { it.appId() in userAccessAppIdSet }
            } else {
                filterList = list
            }
        }
        if (!filterList) {
            return []
        }

        List<ContainerInfo> filter2List
        if (clusterId) {
            filter2List = filterList.findAll { it.clusterId == clusterId }
        } else {
            filter2List = filterList
        }
        if (!filter2List) {
            return []
        }

        filter2List.sort()
    }

    @Override
    String name() {
        if (isUseRedis) {
            return inner.name()
        }
        'memory container manager'
    }

    @Override
    void doJob() {
        if (isUseRedis) {
            inner.doJob()
            return
        }
        // remove 1 minute ago
        def dat = Utils.getNodeAliveCheckLastDate(6)
        heartBeatDateByNodeIp.findAll { k, v ->
            v < dat
        }.each { k, v ->
            authTokenByNodeIp.remove(k)
            nodeInfoByNodeIp.remove(k)
            containersByNodeIp.remove(k)
            heartBeatDateByNodeIp.remove(k)
            log.info 'done remove heart beat too old node - {}', k
        }
    }
}
