package model

import com.segment.common.job.chain.JobResult
import groovy.transform.CompileStatic
import groovy.transform.ToString
import ha.JedisCallback
import ha.JedisPoolHolder
import model.cluster.ClusterNode
import model.json.*
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
        creating, running, scaling_up, scaling_down, updating_replicas, stopped, deleted, unhealthy

        boolean canChangeToRunningWhenInstancesRunningOk() {
            return this == creating || this == scaling_up || this == scaling_down || this == updating_replicas
        }
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

    Integer sentinelAppId

    Integer appId

    // encoded
    String pass

    Integer maxmemoryMb

    String maxmemoryPolicy

    Integer port

    Integer shards

    // include primary node
    Integer replicas

    String[] nodeTags

    String[] nodeTagsByReplicaIndex

    BackupPolicy backupPolicy

    LogPolicy logPolicy

    Boolean isTlsOn

    Status status

    ExtendParams extendParams

    // for cluster mode
    ClusterSlotsDetail clusterSlotsDetail

    // for sentinel mode or standalone mode
    PrimaryReplicasDetail primaryReplicasDetail

    String lastUpdatedMessage

    Date createdDate

    Date updatedDate

    void updateStatus(Status status, String message) {
        assert id
        new RmServiceDTO(id: id, status: status, lastUpdatedMessage: message, updatedDate: new Date()).update()
    }

    int listenPort(ContainerInfo x) {
        if (mode == Mode.cluster) {
            def shardIndex = clusterSlotsDetail.shards.find { it.appId == x.appId() }.shardIndex
            def portForThisShard = port + shardIndex * RedisManager.ONE_SHARD_MAX_REPLICAS
            return portForThisShard + x.instanceIndex()
        } else {
            return port + x.instanceIndex()
        }
    }

    private JedisPool connect(ContainerInfo x) {
        JedisPoolHolder.instance.create(x.nodeIp, listenPort(x), pass)
    }

    private JedisPool connect(ClusterSlotsDetail.Node node) {
        JedisPoolHolder.instance.create(node.ip, node.port, pass)
    }

    private JedisPool connect(PrimaryReplicasDetail.Node node) {
        JedisPoolHolder.instance.create(node.ip, node.port, pass)
    }

    <R> R connectAndExe(ContainerInfo x, JedisCallback<R> callback) {
        def jedisPool = connect(x)
        JedisPoolHolder.exe(jedisPool, callback)
    }

    <R> R connectAndExe(ClusterSlotsDetail.Node node, JedisCallback<R> callback) {
        def jedisPool = connect(node)
        JedisPoolHolder.exe(jedisPool, callback)
    }

    <R> R connectAndExe(PrimaryReplicasDetail.Node node, JedisCallback<R> callback) {
        def jedisPool = connect(node)
        JedisPoolHolder.exe(jedisPool, callback)
    }

    List<ContainerInfo> runningContainerList() {
        List<ContainerInfo> allContainerList = []
        def instance = InMemoryAllContainerManager.instance

        if (mode == Mode.cluster) {
            assert clusterSlotsDetail && clusterSlotsDetail.shards
            for (shard in clusterSlotsDetail.shards) {
                allContainerList.addAll instance.getRunningContainerList(RedisManager.CLUSTER_ID, shard.appId)
            }
        } else {
            assert appId
            allContainerList.addAll instance.getRunningContainerList(RedisManager.CLUSTER_ID, appId)
        }

        allContainerList
    }

    JobResult checkNodes() {
        if (mode == Mode.sentinel) {
            return checkPrimaryReplicaNodes()
        } else if (mode == Mode.cluster) {
            return checkClusterNodesAndSlots()
        } else {
            def runningContainerList = runningContainerList()
            if (runningContainerList.size() == 0) {
                return JobResult.fail('no running container')
            } else {
                return JobResult.ok('running container ok')
            }
        }
    }

    @CompileStatic
    static record RoleResult(String nodeIp, int nodePort, List<Object> roleList) {
        String role() {
            roleList[0].toString()
        }
    }

    JobResult checkPrimaryReplicaNodes() {
        assert mode == Mode.sentinel
        assert primaryReplicasDetail

        def runningContainerList = runningContainerList()
        if (!primaryReplicasDetail.nodes) {
            runningContainerList.each { x ->
                def node = new PrimaryReplicasDetail.Node()
                node.ip = x.nodeIp
                node.port = listenPort(x)
                node.replicaIndex = x.instanceIndex()
                // when first created, the first replica is primary
                node.isPrimary = node.replicaIndex == 0
                primaryReplicasDetail.nodes << node
            }

            new RmServiceDTO(id: id, primaryReplicasDetail: primaryReplicasDetail, updatedDate: new Date()).update()
        } else {
            if (primaryReplicasDetail.nodes.size() != runningContainerList.size()) {
                return JobResult.fail('running container size not equal, ' + primaryReplicasDetail.nodes.size() + ', ' + runningContainerList.size())
            }
        }

        def primaryNode = primaryReplicasDetail.nodes.find { it.isPrimary }
        assert primaryNode
        def primaryNodeIp = primaryNode.ip
        int primaryNodePort = primaryNode.port

        List<RoleResult> roleResultList = []
        for (x in runningContainerList) {
            List<Object> roleList = connectAndExe(x) { jedis ->
                jedis.role()
            }
            assert roleList.size() >= 3
            roleResultList << new RoleResult(x.nodeIp, listenPort(x), roleList)
        }

        // master first
        def sortedList = roleResultList.sort { a, b ->
            if (a.role() == 'master') {
                if (b.role() == 'master') {
                    return a.nodePort <=> b.nodePort
                } else {
                    return -1
                }
            } else {
                if (b.role() == 'master') {
                    return 1
                } else {
                    return a.nodePort <=> b.nodePort
                }
            }
        }

        for (roleResult in sortedList) {
            if (roleResult.role() == 'master') {
                if (primaryNodeIp != roleResult.nodeIp || primaryNodePort != roleResult.nodePort) {
                    return JobResult.fail('primary node ip not equal to master node ip, ' +
                            roleResult.nodeIp + ':' + roleResult.nodePort + ', expected ' + primaryNodeIp + ':' + primaryNodePort)
                }
            } else {
                assert roleResult.role() == 'slave'

                if (roleResult.roleList[1].toString() != primaryNodeIp) {
                    return JobResult.fail('slave node ip not equal to primary node ip, ' + roleResult.roleList[1] + ', ' + primaryNodeIp)
                }
                if (roleResult.roleList[2].toString() != primaryNodePort.toString()) {
                    return JobResult.fail('slave node port not equal to primary node port, ' + roleResult.roleList[2] + ', ' + primaryNodePort)
                }
            }
        }

        JobResult.ok('primary and replica nodes role ok')
    }

    JobResult checkClusterInfoState() {
        assert mode == Mode.cluster
        assert clusterSlotsDetail && clusterSlotsDetail.shards

        if (!clusterSlotsDetail.shards.every { it.nodes.size() == replicas }) {
            return JobResult.fail('nodes number not equal to replicas')
        }

        for (shard in clusterSlotsDetail.shards) {
            for (node in shard.nodes) {
                def lines = connectAndExe(node) { jedis ->
                    jedis.clusterInfo()
                }
                def isStateOk = lines && lines.contains('cluster_state:ok')
                if (!isStateOk) {
                    return JobResult.fail('cluster state not ok')
                }
            }
        }
        JobResult.ok('cluster state ok')
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

    boolean checkIfAppBelongToThis(int appId) {
        if (this.appId == appId) {
            return true
        }

        if (!clusterSlotsDetail) {
            return false
        }

        clusterSlotsDetail.shards.any { it.appId == appId }
    }
}