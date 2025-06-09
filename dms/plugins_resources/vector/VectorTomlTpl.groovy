package vector

import model.AppDTO
import model.json.LogConf
import model.server.ContainerMountTplHelper
import rm.RedisManager
import server.InMemoryAllContainerManager

def appId = super.binding.getProperty('appId') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def nodeIp = super.binding.getProperty('nodeIp') as String

def list = []

def openobserveAppName = super.binding.getProperty('openobserveAppName') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp ooApp = applications.app(openobserveAppName)

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

[sources.redis_instances]
type = "file"
include = [ "${RedisManager.dataDir()}/**/redis.log" ]
"""

Set<String> appSourceIds = []

List<AppDTO> appLogList = super.binding.getProperty('appLogList') as List<AppDTO>
def instance = InMemoryAllContainerManager.instance
appLogList.each { app ->
    def containerList = instance.getContainerList(app.clusterId, app.id, nodeIp)
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

if (ooApp && ooApp.containerList) {
    def user = ooApp.app.conf.envList.find { it.key == 'ZO_ROOT_USER_EMAIL' }.value as String
    def pass = ooApp.app.conf.envList.find { it.key == 'ZO_ROOT_USER_PASSWORD' }.value as String

    def ooNodeIp = ooApp.containerList[0].nodeIp
    int ooPort = ooApp.containerList[0].publicPort(5080)

    appSourceIds << 'local_docker_logs'
    appSourceIds << 'dms_server'
    appSourceIds << 'dms_agent'

    list << """
[sources.local_docker_logs]
type = "docker_logs"
exclude_containers = [ "app_${appId}_${instanceIndex}", "dms" ]

[sinks.openobserve]
type = "http"
inputs = [${appSourceIds.collect { '"' + it + '"' }.join(',')}]
uri = "http://${ooNodeIp}:${ooPort}/api/default/default/_json"
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