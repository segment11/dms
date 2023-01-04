package script.tpl

import common.Const
import common.Utils
import model.AppDTO
import model.server.ContainerMountTplHelper
import server.InMemoryAllContainerManager
import transfer.ContainerInfo

ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp serverApp = applications.app('n9e_server')
ContainerMountTplHelper.OneApp webapiApp = applications.app('n9e_webapi')

def list = []

// add dms server self first
list << """
  - job_name: dms_server
    static_configs:
      - targets: [${Utils.localIp()}:${Const.METRICS_HTTP_LISTEN_PORT}]
        labels: 
          ip: ${Utils.localIp()}
"""

if (serverApp && serverApp.running()) {
    list << """
  - job_name: nserver
    static_configs:
      - targets: [${serverApp.allNodeIpList[0]}:19000]
"""
}

if (webapiApp && webapiApp.running()) {
    list << """
  - job_name: nwebapi
    static_configs:
      - targets: [${webapiApp.allNodeIpList[0]}:18000]
"""
}


List<AppDTO> appMonitorList = super.binding.getProperty('appMonitorList') as List<AppDTO>
appMonitorList.each { app ->
    def monitorConf = app.monitorConf
    List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(app.clusterId, app.id)

    Set<String> set = []
    containerList.collect { x ->
        set << "'${x.nodeIp}:${x.publicPort(monitorConf.port)}'".toString()
    }
    String inner = set.join(',')
    list << """
  - job_name: app_${app.id}
    static_configs:
      - targets: [${inner}]
"""
}


"""
global:
  scrape_interval:     15s
  evaluation_interval: 15s

scrape_configs:
${list.join("\r\n")}
"""