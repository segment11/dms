package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GatewayConf implements JSONFiled {

    Integer clusterId

    Integer routerId

    Integer containerPrivatePort

    String healthCheckPath

    Integer healthCheckDelaySeconds

    Integer healthCheckIntervalSeconds

    Integer healthCheckTimeoutSeconds

    Integer healthCheckTotalTimes

    boolean asBoolean() {
        clusterId != null && routerId != null
    }
}
