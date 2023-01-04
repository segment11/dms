package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GatewayConf implements JSONFiled {

    Integer clusterId

    Integer frontendId

    Integer containerPrivatePort

    String healthCheckUri

    Integer healthCheckDelaySeconds

    Integer healthCheckIntervalSeconds

    Integer healthCheckTimeoutSeconds

    Integer healthCheckTotalTimes

    boolean asBoolean() {
        clusterId != null && frontendId != null
    }
}
