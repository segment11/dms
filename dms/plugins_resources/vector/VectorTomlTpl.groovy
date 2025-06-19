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

def lokiAppName = super.binding.getProperty('lokiAppName') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp lokiApp = applications.app(lokiAppName)

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

if (lokiApp && lokiApp.containerList) {
    def lokiNodeIp = lokiApp.containerList[0].nodeIp
    int lokiPort = lokiApp.containerList[0].publicPort(3100)

    appSourceIds << 'local_docker_logs'
    appSourceIds << 'dms_server'
    appSourceIds << 'dms_agent'
    appSourceIds << 'redis_instances'

    list << """
[sources.local_docker_logs]
type = "docker_logs"
exclude_containers = [ "app_${appId}_${instanceIndex}", "dms" ]

[sinks.loki]
type = "loki"
inputs = [${appSourceIds.collect { '"' + it + '"' }.join(',')}]
endpoint = "http://${lokiNodeIp}:${lokiPort}"
"""
}

list.join("\r\n")