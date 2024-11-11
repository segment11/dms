package transfer

import spock.lang.Specification

class NodeInfoTest extends Specification {
    def 'test base'() {
        given:
        def nodeInfo = new NodeInfo()
        nodeInfo.nodeIp = '192.168.1.99'
        expect:
        nodeInfo.toMap().nodeIp == nodeInfo.nodeIp

        when:
        nodeInfo.checkIfOk(new Date())
        then:
        nodeInfo.isOk

        when:
        nodeInfo.cpuPercList = [new NodeInfo.CpuPerc(user: 1.0, sys: 2.0, idle: 3.0), new NodeInfo.CpuPerc(user: 1.0, sys: 2.0, idle: 3.0)]
        then:
        nodeInfo.cpuNumber() == 2
        nodeInfo.cpuPercList[0].usedPercent() == 0.5
        nodeInfo.cpuUsedPercent() == 0.5

        when:
        nodeInfo.cpuPercList[0].user = 0.0
        nodeInfo.cpuPercList[0].sys = 0.0
        nodeInfo.cpuPercList[0].idle = 0.0
        then:
        nodeInfo.cpuPercList[0].usedPercent() == 0
    }
}
