package server.scheduler.node

import ex.JobProcessException
import groovy.transform.CompileStatic
import model.AppDTO
import model.NodeDTO
import model.json.ContainerResourceAsk
import server.InMemoryAllContainerManager
import transfer.ContainerInfo
import transfer.NodeInfo

@CompileStatic
@Singleton
class NodeResourceCal {

    static List<ContainerResourceAsk> cal(int clusterId, List<NodeDTO> list, Map<String, List<ContainerInfo>> groupByNodeIp) {
        Map<Integer, AppDTO> otherAppCached = [:]
        List<ContainerResourceAsk> leftResourceList = []

        Map<String, NodeInfo> allNodeInfo = InMemoryAllContainerManager.instance.getAllNodeInfo(clusterId)
        for (node in list) {
            def nodeInfo = allNodeInfo[node.ip]
            int cpuNumber = nodeInfo.cpuNumber()
            int memMBTotal = nodeInfo.mem.total.intValue()

            def subList = groupByNodeIp[node.ip]
            List<ContainerResourceAsk> otherAppResourceAskList = subList ? subList.collect { x ->
                def otherAppId = x.appId()
                def otherApp = otherAppCached[otherAppId]
                if (!otherApp) {
                    def appOne = new AppDTO(id: otherAppId).queryFields('conf').one()
                    if (!appOne) {
                        throw new JobProcessException('app not define for id - ' + otherAppId)
                    }
                    otherAppCached[otherAppId] = appOne
                    return new ContainerResourceAsk(node.ip, appOne.conf)
                } else {
                    return new ContainerResourceAsk(node.ip, otherApp.conf)
                }
            } : [] as List<ContainerResourceAsk>

            int memMBUsed = 0
            int cpuSharesUsed = 0
            double cpuFixedUsed = 0
            for (one in otherAppResourceAskList) {
                memMBUsed += one.memMB
                cpuSharesUsed += one.cpuShares
                cpuFixedUsed += one.cpuFixed
            }

            def leftResource = new ContainerResourceAsk(nodeIp: node.ip,
                    memMB: memMBTotal - memMBUsed,
                    cpuShares: cpuNumber * 1024 - cpuSharesUsed,
                    cpuFixed: cpuNumber - cpuFixedUsed)
            leftResourceList << leftResource
        }
        leftResourceList
    }
}
