package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.BackupPolicy
import model.json.ClusterSlotsDetail
import model.json.LogPolicy

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
    static enum EngineVersionForRedis {
        v5_0, v6_2, v7_2, v8_1
    }

    @CompileStatic
    static enum EngineVersionForValkey {
        v7_2, v8_1
    }

    @CompileStatic
    static enum EngineVersionForEngula {
        v2_0
    }

    @CompileStatic
    static enum EngineVersionForKvrocks {
        v2_8
    }

    @CompileStatic
    static enum EngineVersionForVelo {
        v1_0
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

    Integer sentinelServiceId

    String pass

    Integer port

    Integer shards

    Integer replicas

    BackupPolicy backupPolicy

    LogPolicy logPolicy

    Boolean isTlsOn

    String appIds

    Status status

    ClusterSlotsDetail clusterSlotsDetail

    Date createdDate

    Date updatedDate
}