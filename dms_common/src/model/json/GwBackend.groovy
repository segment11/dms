package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GwBackend implements JSONFiled {
    List<GwBackendServer> serverList = []

    String circuitBreaker

    int maxConn

    String loadBalancer

    String stickiness

    String healthCheckUri

    int healthCheckInterval
}
