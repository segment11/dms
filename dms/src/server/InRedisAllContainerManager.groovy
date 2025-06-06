package server

import auth.User
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.segment.common.Conf
import com.segment.common.job.IntervalJob
import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ha.JedisPoolHolder
import model.NodeDTO
import org.segment.d.json.DefaultJsonTransformer
import redis.clients.jedis.JedisPool
import transfer.ContainerInfo
import transfer.NodeInfo

import static ha.JedisPoolHolder.exe as r

@CompileStatic
@Slf4j
@Singleton
class InRedisAllContainerManager extends IntervalJob implements AllContainerManager {
    private final String keyPrefix = 'dms_'

    private String key(String k) {
        keyPrefix + k
    }

    // agent.intervalSeconds
    int hbSeconds = 10

    int lostHbTimes = 10

    private int ttl() {
        hbSeconds * lostHbTimes
    }

    private JedisPool jedisPool

    void init() {
        def c = Conf.instance
        jedisPool = JedisPoolHolder.instance.create(c.get('redis.host'), c.getInt('redis.port', 6379),
                c.get('redis.pass'))
    }

    private DefaultJsonTransformer json = new DefaultJsonTransformer()

    @Override
    void addAuthToken(String nodeIp, String authToken) {
        r(jedisPool) { jedis ->
            jedis.hset(key('auth_token'), nodeIp, authToken)
        }
    }

    @Override
    String getAuthToken(String nodeIp) {
        r(jedisPool) { jedis ->
            jedis.hget(key('auth_token'), nodeIp)
        } as String
    }

    @Override
    void addNodeInfo(String nodeIp, NodeInfo node) {
        r(jedisPool) { jedis ->
            jedis.hset(key('cluster_node_ips_' + node.clusterId), nodeIp, '1')
            jedis.hset(key('node_info'), nodeIp, json.json(node))
            jedis.hset(key('node_hb'), nodeIp, '' + node.hbTime.time)
        }
    }

    @Override
    Date getHeartBeatDate(String nodeIp) {
        String str = r(jedisPool) { jedis ->
            jedis.hget(key('node_hb'), nodeIp)
        } as String
        if (!str) {
            return new Date(0)
        }
        new Date(str as long)
    }

    @Override
    NodeInfo getNodeInfo(String nodeIp) {
        String str = r(jedisPool) { jedis ->
            jedis.hget(key('node_info'), nodeIp)
        } as String
        if (!str) {
            return null
        }
        json.read(str, NodeInfo)
    }

    @Override
    Map<String, NodeInfo> getAllNodeInfo(Integer clusterId) {
        def map = new HashMap<String, NodeInfo>()

        Map<String, String> strMap = r(jedisPool) { jedis ->
            jedis.hgetAll(key('cluster_node_ips_' + clusterId))
        } as Map<String, String>
        if (!strMap) {
            return map
        }

        def nodeIpList = strMap.keySet()
        String[] nodeIpArray = nodeIpList.toArray(new String[nodeIpList.size()])

        List<String> strList = r(jedisPool) { jedis ->
            jedis.hmget(key('node_info'), nodeIpArray)
        } as List<String>

        nodeIpList.eachWithIndex { String nodeIp, int i ->
            def str = strList[i]
            if (str) {
                map[nodeIp] = json.read(str, NodeInfo)
            }
        }
        map
    }

    @Override
    List<NodeDTO> getHeartBeatOkNodeList(int clusterId) {
        def instance = InMemoryAllContainerManager.instance
        instance.getHeartBeatOkNodeList(clusterId)
    }

    @Override
    void addContainers(Integer clusterId, String nodeIp, List<ContainerInfo> containers) {
        r(jedisPool) { jedis ->
            jedis.hset(key('containers'), nodeIp, json.json(containers))

            for (container in containers) {
                jedis.setex(key('container_node_ip_' + container.id), ttl(), nodeIp)
                jedis.setex(key('container_app_id_' + container.id), ttl(), '' + container.appId())
            }
        }
    }

    @CompileStatic
    static class ContainerListType extends TypeReference<List<ContainerInfo>> {
    }

    @Override
    List<ContainerInfo> getContainerListByNodeIp(String nodeIp) {
        String str = r(jedisPool) { jedis ->
            jedis.hget(key('containers'), nodeIp)
        }
        if (!str) {
            return null
        }

        new ObjectMapper().readValue(str, new ContainerListType())
    }

    @Override
    String getNodeIpByContainerId(String containerId) {
        r(jedisPool) { jedis ->
            jedis.get(key('container_node_ip_' + containerId))
        } as String
    }

    @Override
    Integer getAppIpByContainerId(String containerId) {
        r(jedisPool) { jedis ->
            jedis.get(key('container_app_id_' + containerId))
        } as Integer
    }

    @Override
    List<ContainerInfo> getContainerList(int clusterId, int appId = 0,
                                         String nodeIp = null, User user = null) {
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
        if (nodeIp) {
            list.addAll getContainerListByNodeIp(nodeIp)
        } else {
            if (clusterId) {
                Map<String, String> strMap = r(jedisPool) { jedis ->
                    jedis.hgetAll(key('cluster_node_ips_' + clusterId))
                } as Map<String, String>
                if (strMap) {
                    def nodeIpList = strMap.keySet()
                    String[] nodeIpArray = nodeIpList.toArray(new String[nodeIpList.size()])

                    List<String> strList = r(jedisPool) { jedis ->
                        jedis.hmget(key('containers'), nodeIpArray)
                    } as List<String>
                    def mapper = new ObjectMapper()
                    for (str in strList) {
                        if (str) {
                            list.addAll mapper.readValue(str, new ContainerListType())
                        }
                    }
                }
            } else {
                Map<String, String> strMap = r(jedisPool) { jedis ->
                    jedis.hgetAll(key('containers'))
                } as Map<String, String>
                def mapper = new ObjectMapper()
                for (str in strMap.values()) {
                    list.addAll mapper.readValue(str, new ContainerListType())
                }
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

        filterList.sort()
    }

    @Override
    String name() {
        'redis container manager'
    }

    @Override
    void doJob() {
        // remove 1 minute ago
        def dat = Utils.getNodeAliveCheckLastDate(6)
        r(jedisPool) { jedis ->
            // nodeIp: timeMs
            Map<String, String> strMap = jedis.hgetAll(key('node_hb'))

            strMap.each { nodeIp, v ->
                if (new Date(v as long) >= dat) {
                    return
                }

                log.info 'delete in redis cache for node ip: {}', nodeIp
                jedis.hdel(key('auth_token'), nodeIp)
                jedis.hdel(key('node_info'), nodeIp)
                jedis.hdel(key('node_hb'), nodeIp)
                jedis.hdel(key('containers'), nodeIp)
            }
        }
    }
}
