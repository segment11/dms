package common

import groovy.transform.CompileStatic

@CompileStatic
class AgentConf {
    String serverHost
    int serverPort = 5010

    int clusterId = 1
    String secret = '1'

    String proxyNodeIp
    int proxyNodePort
    int proxyConnectTimeout = 500
    int proxyReadTimeout = 5000

    int imagePullTimeoutSeconds = 30

    String agentTplConfFileDir = '/opt/dms/config'

    int agentIntervalSeconds = 5
    int agentConnectTimeout = 500
    int agentReadTimeout = 500

    // for multi network interface
    String localIpFilterPre = '192.'

    String collectDockerDaemon = '1'

    String generate() {
        List<String> lines = []
        this.properties.each { k, v ->
            if (k != 'class') {
                lines << "${k}=${v ? v.toString() : ''}".toString()
            }
            return
        }
        lines << "server.runtime.jar=1"
        lines.join("\r\n")
    }
}
