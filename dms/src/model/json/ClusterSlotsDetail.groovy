package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import ha.JedisPoolHolder
import model.cluster.MultiSlotRange
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class ClusterSlotsDetail implements JSONFiled {
    // one shard, one application
    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class Shard {
        Integer shardIndex
        Integer appId
        MultiSlotRange multiSlotRange = new MultiSlotRange()
        List<Node> nodes = []

        Node primary() {
            nodes.find { it.isPrimary }
        }
    }

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class Node {
        Boolean isPrimary
        Integer replicaIndex
        String ip
        Integer port
        // for easy get
        Integer shardIndex

        String uuid() {
            ip + ':' + port
        }

        String nodeIdCached

        String nodeId() {
            if (!nodeIdCached) {
                def jedisPool = JedisPoolHolder.instance.create(ip, port)
                nodeIdCached = JedisPoolHolder.exe(jedisPool) { jedis ->
                    return jedis.clusterMyId()
                }
            }
            nodeIdCached
        }
    }

    List<Shard> shards = []

    Node findNodeByIpPort(String ip, Integer port) {
        def shard = findShardByIpPort(ip, port)
        shard ? shard.nodes.find {
            it.ip == ip && it.port == port
        } : null
    }

    Shard findShardByIpPort(String ip, Integer port) {
        shards.find {
            it.nodes.find {
                it.ip == ip && it.port == port
            }
        }
    }
}
