package script.tpl

import model.AppDTO
import server.InMemoryAllContainerManager

def nodeIp = super.binding.getProperty('nodeIp') as String

def list = []

// dms server and dms agent
list << """
- type: log
  paths:
    - /opt/log/dms.log
  multiline.pattern: '^[[:space:]]+(at|\\\\.{3})[[:space:]]+\\\\b|^Caused by:'
  multiline.negate: false
  multiline.match: after
  tail_files: true
  fields:
    log_topic: dms_server
    node_ip: ${nodeIp}
  enabled: true
"""

list << """
- type: log
  paths:
    - /opt/log/dms_agent.log
  multiline.pattern: '^[[:space:]]+(at|\\\\.{3})[[:space:]]+\\\\b|^Caused by:'
  multiline.negate: false
  multiline.match: after
  tail_files: true
  fields:
    log_topic: dms_agent
    node_ip: ${nodeIp}
  enabled: true
"""

List<AppDTO> appLogList = super.binding.getProperty('appLogList') as List<AppDTO>
appLogList.each { app ->
    def containerList = InMemoryAllContainerManager.instance.getContainerList(app.clusterId, app.id, nodeIp)
    // this node running a filebeat, but do not run the target application
    if (containerList.size() == 0) {
        return
    }

    def logConf = app.logConf
    for (logFile in logConf.logFileList) {
        if (logFile.isMultilineSupport) {
            list << """
- type: log
  paths:
    - ${logFile.pathPattern}
  multiline.pattern: '${logFile.multilinePattern}'
  multiline.negate: false
  multiline.match: after
  tail_files: true
  fields:
    log_topic: app_${app.id}
    node_ip: ${nodeIp}
    app_id: ${app.id}
  enabled: true
"""
        } else {
            list << """
- type: log
  paths:
    - ${logFile.pathPattern}
  tail_files: true
  fields:
    log_topic: app_${app.id}
    node_ip: ${nodeIp}
    app_id: ${app.id}
  enabled: true
"""
        }
    }

}

list.join("\r\n")