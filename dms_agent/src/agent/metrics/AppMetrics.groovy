package agent.metrics

import groovy.transform.CompileStatic

@CompileStatic
class AppMetrics {
    int appId
    int instanceIndex
    Date time
    String nodeIp
    String rawBody

    Map gauges

    Object getGaugeValue(String gaugeName) {
        if (!gauges) {
            return null
        }
        gauges[gaugeName]
    }

    Map toMap() {
        Map r = gauges ?: [:]
        r.appId = appId
        r.instanceIndex = instanceIndex
        r.time = time
        r.nodeIp = nodeIp
        r
    }
}
