package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class KmSnapshotContent implements JSONFiled {

    String serviceName
    String mode
    String kafkaVersion
    Date snapshotDate

    List<BrokerEntry> brokers = []
    String zkConnectString
    String zkChroot

    List<TopicEntry> topics = []

    int defaultPartitions
    int defaultReplicationFactor
    int heapMb

    ArrayList<KVPair<String>> configItems = []
    Map<String, String> configOverrides = [:]

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class BrokerEntry {
        int brokerId
        String host
        int port
        String rackId
        String logDirs
    }

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class TopicEntry {
        String name
        int partitions
        int replicationFactor
        Map<String, String> configOverrides = [:]
    }
}
