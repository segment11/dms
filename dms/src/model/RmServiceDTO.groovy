package model

import com.segment.common.job.chain.JobResult
import groovy.transform.CompileStatic
import groovy.transform.ToString
import ha.JedisCallback
import ha.JedisPoolHolder
import model.cluster.ClusterNode
import model.json.BackupPolicy
import model.json.ClusterSlotsDetail
import model.json.ExtendParams
import model.json.LogPolicy
import redis.clients.jedis.JedisPool
import rm.RedisManager
import server.InMemoryAllContainerManager
import transfer.ContainerInfo

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RmServiceDTO extends BaseRecord<RmServiceDTO> {

    @CompileStatic
    static enum Mode {
        standalone, sentinel, cluster
    }

    @CompileStatic
    static enum EngineType {
        redis, valkey, engula, kvrocks, velo
    }

    @CompileStatic
    static enum Status {
        creating, running, stopped, deleted, unhealthy
    }

    Integer id

    String name

    String des

    Mode mode

    EngineType engineType

    String engineVersion

    Integer configTemplateId

    // for sentinel mode
    Integer sentinelServiceId

    Integer appId

    String pass

    Integer maxmemoryMb

    Integer port

    Integer shards

    // include primary node
    Integer replicas

    String[] nodeTags

    BackupPolicy backupPolicy

    LogPolicy logPolicy

    Boolean isTlsOn

    Status status

    ExtendParams extendParams

    // for cluster mode
    ClusterSlotsDetail clusterSlotsDetail

    Date createdDate

    Date updatedDate

    int listenPort(ContainerInfo x) {
        def shardIndex = clusterSlotsDetail.shards.find { it.appId == x.appId() }.shardIndex
        def portForThisShard = port + shardIndex * RedisManager.ONE_SHARD_MAX_REPLICAS
        portForThisShard + x.instanceIndex()
    }

    private JedisPool connect(ContainerInfo x) {
        JedisPoolHolder.instance.create(x.nodeIp, listenPort(x), pass)
    }

    <R> R connectAndExe(ContainerInfo x, JedisCallback<R> callback) {
        def jedisPool = connect(x)
        JedisPoolHolder.exe(jedisPool, callback)
    }

    List<ContainerInfo> runningContainerList() {
        List<ContainerInfo> allContainerList = []
        def instance = InMemoryAllContainerManager.instance

        if (mode == Mode.cluster) {
            assert clusterSlotsDetail && clusterSlotsDetail.shards
            for (shard in clusterSlotsDetail.shards) {
                def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, shard.appId)
                allContainerList.addAll(containerList.findAll { x -> x.running() })
            }

        } else {
            assert appId

            def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, appId)
            allContainerList.addAll(containerList.findAll { x -> x.running() })
        }

        allContainerList
    }

    JobResult checkNodes() {
        if (mode == Mode.sentinel) {
            return checkPrimaryReplicaNodes()
        } else if (mode == Mode.cluster) {
            return checkClusterNodesAndSlots()
        } else {
            def containerList = runningContainerList()
            if (containerList.size() != 1) {
                return JobResult.fail('no running container')
            } else {
                return JobResult.ok('running container ok')
            }
        }
    }

    @CompileStatic
    static record RoleResult(String nodeIp, int port, List<Object> roleList) {
        String role() {
            roleList[0].toString()
        }
    }

    JobResult checkPrimaryReplicaNodes() {
        assert mode == Mode.sentinel
        assert replicas > 1

        def containerList = runningContainerList()

        String primaryNodeIp
        int primaryPort = 0

        List<RoleResult> roleResultList = []
        for (x in containerList) {
            List<Object> roleList = connectAndExe(x) { jedis ->
                jedis.role()
            }
            assert roleList.size() >= 3
            roleResultList << new RoleResult(x.nodeIp, x.publicPort(port), roleList)
        }

        // master first
        def sortedList = roleResultList.sort { roleResult ->
            roleResult.role() == 'master' ? 0 : 1
        }

        for (roleResult in sortedList) {
            if (roleResult.role() == 'master') {
                // already set
                if (primaryNodeIp != null) {
                    return JobResult.fail('more than one primary node, ' + primaryNodeIp + ':' + primaryPort + ', ' + roleResult.nodeIp + ':' + roleResult.port)
                }

                primaryNodeIp = roleResult.nodeIp
                primaryPort = roleResult.port
            } else {
                assert roleResult.role() == 'slave'
                if (primaryNodeIp == null) {
                    return JobResult.fail('slave node ip not equal to primary node ip, ' + roleResult.roleList[1].toString() + ':' + roleResult.roleList[2].toString() + ', primary node ip not set yet')
                }

                if (roleResult.roleList[1].toString() != primaryNodeIp) {
                    return JobResult.fail('slave node ip not equal to primary node ip, ' + roleResult.roleList[1].toString() + ':' + roleResult.roleList[2].toString() + ', ' + primaryNodeIp + ':' + primaryPort)
                }
                if (roleResult.roleList[2].toString() != primaryPort.toString()) {
                    return JobResult.fail('slave node port not equal to primary node port, ' + roleResult.roleList[1].toString() + ':' + roleResult.roleList[2].toString() + ', ' + primaryNodeIp + ':' + primaryPort)
                }
            }
        }

        JobResult.ok('primary and replica nodes role ok')
    }

    JobResult checkClusterNodesAndSlots() {
        assert mode == Mode.cluster
        assert clusterSlotsDetail && clusterSlotsDetail.shards

        if (!clusterSlotsDetail.shards.every { it.nodes.size() == replicas }) {
            return JobResult.fail('nodes number not equal to replicas')
        }

        def nodesCount = clusterSlotsDetail.shards.sum { it.nodes.size() } as int
        for (shard in clusterSlotsDetail.shards) {
            for (node in shard.nodes) {
                def cn = new ClusterNode(node.ip, node.port)
                cn.read()

                if ('ok' != cn.clusterState()) {
                    return JobResult.fail('cluster state not ok')
                }

                if (nodesCount != cn.clusterInfoValue('cluster_known_nodes') as int) {
                    return JobResult.fail('cluster known nodes not equal to nodes count')
                }

                for (one in cn.allClusterNodeList) {
                    def localNode = clusterSlotsDetail.findNodeByIpPort(one.ip, one.port)
                    if (!localNode) {
                        return JobResult.fail('cluster node not found in local shard in db: ' + one.uuid())
                    }

                    // check role
                    if (localNode.isPrimary != one.isPrimary) {
                        return JobResult.fail('cluster node role not match, expect: ' +
                                (localNode.isPrimary ? 'master' : 'slave') + ', ip/port: ' + one.uuid())
                    }

                    def localShard = clusterSlotsDetail.findShardByIpPort(one.ip, one.port)
                    // check slave follow
                    if (!localNode.isPrimary) {
                        def primary = localShard.primary()
                        if (primary.ip != one.followNodeIp || primary.port != one.followNodePort) {
                            return JobResult.fail('cluster node slave follow ip/port not match, expect: ' + primary.uuid() + ', but: ' +
                                    one.followNodeIp + ':' + one.followNodePort)
                        }
                    }

                    // check primary node id
                    def localNodeId = localNode.nodeId()
                    if (localNodeId != one.nodeId) {
                        return JobResult.fail('cluster node id not match, expect: ' + localNodeId + ', but: ' + one.nodeId)
                    }
                }

                // check slot range
                if (cn.isPrimary) {
                    def localShard = clusterSlotsDetail.findShardByIpPort(node.ip, node.port)
                    def multiSlotRange = shard.multiSlotRange
                    if (!cn.multiSlotRange?.list) {
                        // not null
                        if (multiSlotRange && multiSlotRange.list) {
                            return JobResult.fail('cluster node slot range not match, expect: null, but: ' + multiSlotRange.toString())
                        }
                    } else {
                        if (multiSlotRange.toString() != cn.multiSlotRange.toString()) {
                            return JobResult.fail('cluster node slot range not match, expect: ' + multiSlotRange.toString() + ', but: ' +
                                    cn.multiSlotRange.toString())
                        }
                    }
                }
            }
        }

        JobResult.ok('cluster nodes and slots check ok')
    }
}