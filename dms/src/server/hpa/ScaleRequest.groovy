package server.hpa

import groovy.transform.CompileStatic

@CompileStatic
class ScaleRequest {
    String nodeIp
    int scaleCmd
    Date time = new Date()

    @Override
    String toString() {
        '' + nodeIp.padRight(20, ' ') + scaleCmd + ' when ' + time
    }
}
