package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class BrokerDetail implements JSONFiled {

    List<BrokerNode> brokers = []

    BrokerNode findByBrokerId(int brokerId) {
        brokers.find { it.brokerId == brokerId }
    }

    BrokerNode findByIpPort(String ip, int port) {
        brokers.find { it.ip == ip && it.port == port }
    }

    List<BrokerNode> activeBrokers() {
        brokers
    }

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class BrokerNode {
        int brokerId
        int brokerIndex
        boolean isController
        String ip
        int port
        String rackId

        String uuid() {
            ip + ':' + port
        }
    }
}
