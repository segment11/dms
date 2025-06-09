package prometheus

import model.server.ContainerMountTplHelper
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

def intervalSecondsGlobal = super.binding.getProperty('intervalSecondsGlobal') as Integer
def eachContainerChargeRedisServerCount = super.binding.getProperty('eachContainerChargeRedisServerCount') as Integer

ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp redisExporterApp = applications.app('redis_exporter')

ContainerMountTplHelper.OneApp nodeExporterApp = applications.app('node-exporter')
def nodeExporterAddrList = []
if (nodeExporterApp && nodeExporterApp.running()) {
    redisExporterApp.containerList.each { x ->
        nodeExporterAddrList << (x.nodeIp + ':' + x.publicPort(9100))
    }
}

def redisExporterAddrList = []
if (redisExporterApp && redisExporterApp.running()) {
    redisExporterApp.containerList.each { x ->
        redisExporterAddrList << (x.nodeIp + ':' + x.publicPort(9121))
    }
}

if (!nodeExporterAddrList && !redisExporterAddrList) {
    return """
global:
  scrape_interval:     ${intervalSecondsGlobal}s
  evaluation_interval: 15s
"""
}

def nodeExporterJobConf = nodeExporterAddrList ? """
  - job_name: node
    metrics_path: /metrics
    static_configs:
      - targets: [${nodeExporterAddrList.collect { "'" + it + "'" }.join(',')}]
""" : ""

def instance = InMemoryAllContainerManager.instance
def appList = InMemoryCacheSupport.instance.appList

List<String> list = []

for (app in appList) {
    def conf = app.conf
    def isRedis = conf.group == 'library' && conf.image == 'redis'
    def isValkey = conf.group == 'library' && conf.image == 'valkey'
    def isEngula = conf.group == 'montplex' && conf.image == 'engula'
    if (isRedis || isValkey || isEngula) {
        def confOne = conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
        if (!confOne) {
            // exclude sentinel
            continue
        }

        def isSingleNode = confOne.paramValue('isSingleNode') == 'true'
        def configPort = confOne.paramValue('port') as int

        def containerList = instance.getContainerList(app.clusterId, app.id)
        containerList.each { x ->
            if (!x.running()) {
                return
            }

            def listenPort = x.publicPort(configPort) + (isSingleNode ? x.instanceIndex() : 0)
            def addr = ' ' * 8 + "- redis://${x.nodeIp}:${listenPort}".toString()
            list << addr
        }
    }
}

if (!list) {
    list << (' ' * 8 + '- redis://127.0.0.1:6379')
}

List<String> targetsConfigList = []

// each redis exporter charge several redis servers
def groups = (list.size() / eachContainerChargeRedisServerCount).intValue()
if (list.size() % eachContainerChargeRedisServerCount != 0) {
    groups++
}
for (i in 0..<groups) {
    def start = i * eachContainerChargeRedisServerCount
    def end = Math.min(list.size(), start + eachContainerChargeRedisServerCount)
    def subList = list[start..<end]

    def redisExporterAddr = redisExporterAddrList.size() > i ? redisExporterAddrList[i] : '127.0.0.1:9121'

    targetsConfigList << """
  - job_name: 'redis_exporter${i}_targets'
    static_configs:
      - targets:
${subList.join("\r\n")}
    metrics_path: /scrape
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: ${redisExporterAddr}
""".toString()
}

"""
global:
  scrape_interval:     ${intervalSecondsGlobal}s
  evaluation_interval: 15s

scrape_configs:
  ## config for the multiple Redis targets that the exporter will scrape

${nodeExporterJobConf}  
  
${targetsConfigList.join("\\r\\n")}      
"""