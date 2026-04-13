package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.BrokerDetail
import model.json.ExtendParams
import model.json.LogPolicy

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmServiceDTO extends BaseRecord<KmServiceDTO> {

    @CompileStatic
    static enum Mode { standalone, cluster }

    @CompileStatic
    static enum Status {
        creating, running, scaling_up, scaling_down, stopped, deleted, unhealthy

        boolean canChangeToRunningWhenInstancesRunningOk() {
            return this == creating || this == scaling_up || this == scaling_down
        }
    }

    Integer id

    String name

    String des

    Mode mode

    String kafkaVersion

    Integer configTemplateId

    ExtendParams configOverrides

    String zkConnectString

    String zkChroot

    Integer appId

    Integer port

    Integer brokers

    Integer defaultReplicationFactor

    Integer defaultPartitions

    Integer heapMb

    String pass

    Boolean isSaslOn

    Boolean isTlsOn

    String[] nodeTags

    String[] nodeTagsByBrokerIndex

    LogPolicy logPolicy

    Status status

    ExtendParams extendParams

    BrokerDetail brokerDetail

    String lastUpdatedMessage

    Date createdDate

    Date updatedDate
}
