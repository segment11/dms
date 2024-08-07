package prometheus

import com.segment.common.Utils
import common.Const
import model.AppDTO
import model.server.ContainerMountTplHelper
import server.InMemoryAllContainerManager
import transfer.ContainerInfo

def intervalSecondsGlobal = super.binding.getProperty('intervalSecondsGlobal') as Integer
def dmsServerHost = super.binding.getProperty('dmsServerHost') as String

ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp n9eApp = applications.app('n9e')

def list = []

// add dms server self first
list << """
  - job_name: dms_server
    static_configs:
      - targets: ['${dmsServerHost ?: Utils.localIp()}:${Const.METRICS_HTTP_LISTEN_PORT}']
"""

if (n9eApp && n9eApp.running()) {
    list << """
  - job_name: n9e
    metrics_path: /metrics
    static_configs:
      - targets: [${n9eApp.allNodeIpList[0]}:17000]
"""
}

List<AppDTO> appMonitorList = super.binding.getProperty('appMonitorList') as List<AppDTO>
appMonitorList.each { app ->
    def monitorConf = app.monitorConf
    if (!monitorConf.isHttpRequest) {
        return
    }

    List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(app.clusterId, app.id)

    Set<String> set = []
    containerList.each { x ->
        if (monitorConf.isFirstInstancePullOnly) {
            if (x.instanceIndex() == 0) {
                set << "'${x.nodeIp}:${x.publicPort(monitorConf.port)}'".toString()
            }
        } else {
            set << "'${x.nodeIp}:${x.publicPort(monitorConf.port)}'".toString()
        }
    }

    if (set) {
        String inner = set.join(',')
        list << """
  - job_name: app_${app.id}
    metrics_path: ${monitorConf.httpRequestUri}
    scrape_interval: ${monitorConf.intervalSeconds}s
    static_configs:
      - targets: [${inner}]
"""
    }
}


"""
global:
  scrape_interval:     ${intervalSecondsGlobal}s
  evaluation_interval: 15s

scrape_configs:
${list.join("\r\n")}
"""