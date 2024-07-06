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

    Integer healthCheckIntervalSeconds

    Integer healthCheckTimeoutSeconds

    boolean asBoolean() {
        clusterId != null && routerId != null
    }
}
