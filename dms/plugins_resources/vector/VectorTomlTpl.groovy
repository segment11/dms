package vector

import model.AppDTO
import model.json.LogConf
import model.server.ContainerMountTplHelper
import server.InMemoryAllContainerManager

def nodeIp = super.binding.getProperty('nodeIp') as String

def list = []

def zincobserveAppName = super.binding.getProperty('zincobserveAppName') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp zincApp = applications.app(zincobserveAppName)

// dms server and dms agent
list << """
[sources.dms_server]
type = "file"
include = [ "/opt/log/dms.log" ]

[sources.dms_server.multiline]
mode = "continue_through"
timeout_ms = 1000
start_pattern = '^[^\\s]'
condition_pattern = '(?m)^[\\s|\\W].*\$|(?m)^(Caused|java|org|com|net).+\$|(?m)^\\}.*\$'
"""

list << """
[sources.dms_agent]
type = "file"
include = [ "/opt/log/dms_agent.log" ]

[sources.dms_agent.multiline]
mode = "continue_through"
timeout_ms = 1000
start_pattern = '^[^\\s]'
condition_pattern = '(?m)^[\\s|\\W].*\$|(?m)^(Caused|java|org|com|net).+\$|(?m)^\\}.*\$'
"""

Set<String> appSourceIds = []

List<AppDTO> appLogList = super.binding.getProperty('appLogList') as List<AppDTO>
appLogList.each { app ->
    def containerList = InMemoryAllContainerManager.instance.getContainerList(app.clusterId, app.id, nodeIp)
    // this node running a filebeat, but do not run the target application
    if (containerList.size() == 0) {
        return
    }

    def logConf = app.logConf

    logConf.logFileList.eachWithIndex { LogConf.LogFile logFile, int i ->
        appSourceIds << "app_${app.id}_${i}".toString()
        if (logFile.isMultilineSupport) {
            list << """
[sources.app_${app.id}_${i}]
type = "file"
include = [ "${logFile.pathPattern}" ]

[sources.app_${app.id}_${i}.multiline]
mode = "continue_through"
timeout_ms = 1000
start_pattern = '${logFile.multilinePattern}'
"""
        } else {
            list << """
[sources.app_${app.id}_${i}]
type = "file"
include = [ "${logFile.pathPattern}" ]
"""
        }
    }
}

if (zincApp && zincApp.containerList) {
    def user = zincApp.app.conf.envList.find { it.key == 'ZO_ROOT_USER_EMAIL' }.value as String
    def pass = zincApp.app.conf.envList.find { it.key == 'ZO_ROOT_USER_PASSWORD' }.value as String

    def zincNodeIp = zincApp.containerList[0].nodeIp
    int zincPort = 5080
    if (zincApp.containerList) {
        zincPort = zincApp.containerList[0].publicPort(5080)
    }

    appSourceIds << 'local_docker_logs'
    appSourceIds << 'dms_server'
    appSourceIds << 'dms_agent'

    list << """
[sources.local_docker_logs]
type = "docker_logs"

[sinks.zinc]
type = "http"
inputs = [${appSourceIds.collect { '"' + it + '"' }.join(',')}]
uri = "http://${zincNodeIp}:${zincPort}/api/default/default/_json"
method = "post"
auth.strategy = "basic"
auth.user = "${user}"
auth.password = "${pass}"
compression = "gzip"
encoding.codec = "json"
encoding.timestamp_format = "rfc3339"
healthcheck.enabled = false
"""
}

list.join("\r\n")