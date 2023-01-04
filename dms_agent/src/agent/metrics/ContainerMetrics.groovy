package agent.metrics

import groovy.transform.CompileStatic

@CompileStatic
class ContainerMetrics {
    int appId
    int instanceIndex
    String containerId
    Date time
    String nodeIp

    long costT
    double cpuPerc
    long memUsage
    long memMaxUsage
    long memLimit

    Map toMap() {
        Map r = [:]
        r.appId = appId
        r.instanceIndex = instanceIndex
        r.time = time
        r.nodeIp = nodeIp
        r.containerId = containerId
        r.costT = costT
        r.cpuPerc = cpuPerc
        r.memUsage = memUsage
        r.memMaxUsage = memMaxUsage
        r.memLimit = memLimit
        r
    }
}
