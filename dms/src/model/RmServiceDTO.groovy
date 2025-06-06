package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import ha.JedisPoolHolder
import model.json.BackupPolicy
import model.json.ClusterSlotsDetail
import model.json.ExtendParams
import model.json.LogPolicy
import redis.clients.jedis.JedisPool
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

    Integer port

    Integer shards

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
        def portForThisShard = port + shardIndex * 10
        portForThisShard + x.instanceIndex()
    }

    JedisPool connect(ContainerInfo x) {
        JedisPoolHolder.instance.create(x.nodeIp, listenPort(x), pass)
    }
}