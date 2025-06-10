package model.cluster

import groovy.transform.CompileStatic
import model.json.KVPair

@CompileStatic
class MessageReader {
    // cluster info / info replication etc.
    static List<KVPair<String>> fromClusterInfo(String info) {
        List<KVPair<String>> list = []
        def lines = info.readLines().findAll { it.trim() }
        for (line in lines) {
            def arr = line.split(':')
            if (arr.length == 2) {
                list << new KVPair(arr[0], arr[1])
            }
        }
        list
    }

    static List<ClusterNode> fromClusterNodes(String content, String ipWithPort) {
        /*
07c37dfeb235213a872192d90877d0cd55635b91 127.0.0.1:30004 slave e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca 0 1426238317239 4 connected
e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca 127.0.0.1:30001 myself,master - 0 0 1 connected 0-5460
*/
        List<ClusterNode> list = []
        def lines = content.readLines().findAll { it.trim() }
        for (line in lines) {
            def one = new ClusterNode()

            def arr = line.split(' ')
            one.nodeId = arr[0]
            one.isMySelf = line.contains('myself') || arr[1].contains(ipWithPort)

            def subArr = arr[1].split(':')
            one.ip = subArr[0]
            one.port = subArr[1].split('@')[0] as int

            // redis
            if (!one.ip && one.isMySelf) {
                def givenArr = ipWithPort.split(':')
                one.ip = givenArr[0]
                one.port = givenArr[1] as int
            }

            one.isPrimary = line.contains('master')
            one.followNodeId = one.isPrimary ? null : arr[3]

            list << one
        }

        // set replica follow node ip
        for (one in list) {
            if (!one.isPrimary) {
                def followOne = list.find { it.nodeId == one.followNodeId }
                one.followNodeIp = followOne.ip
                one.followNodePort = followOne.port
            }
        }

        list
    }

    static List<SlotNode> fromClusterSlots(List slotsInfoList) {
        List<SlotNode> list = []

        for (one in slotsInfoList) {
            def subList = one as List
            def beginSlot = subList[0] as int
            def endSlot = subList[1] as int

            for (inner in subList[2..-1]) {
                def innerList = inner as List

                def slotNode = new SlotNode()
                slotNode.beginSlot = beginSlot
                slotNode.endSlot = endSlot
                slotNode.ip = new String(innerList[0] as byte[])
                slotNode.port = innerList[1] as Integer
                slotNode.nodeId = new String(innerList[2] as byte[])
                list << slotNode
            }
        }

        list
    }
}
