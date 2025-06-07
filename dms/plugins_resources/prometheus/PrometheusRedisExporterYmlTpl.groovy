package prometheus

import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

def intervalSecondsGlobal = super.binding.getProperty('intervalSecondsGlobal') as Integer
def redisExporterAddr = super.binding.getProperty('redisExporterAddr') as String

def instance = InMemoryAllContainerManager.instance
def appList = InMemoryCacheSupport.instance.appList

def list = []

for (app in appList) {
    def conf = app.conf
    def isRedis = conf.group == 'library' && conf.image == 'redis'
    def isValkey = conf.group == 'library' && conf.image == 'valkey'
    def isEngula = conf.group == 'montplex' && conf.image == 'engula'
    if (isRedis || isValkey || isEngula) {
        def confOne = conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
        assert confOne
        def isSingleNode = confOne.paramValue('isSingleNode') == 'true'
        def configPort = confOne.paramValue('port') as int

        def containerList = instance.getContainerList(app.clusterId, app.id)
        containerList.each { x ->
            if (!x.running()) {
                return
            }

            def listenPort = x.publicPort(configPort) + (isSingleNode ? x.instanceIndex : 0)
            def addr = "- redis://${x.nodeIp}:${listenPort}".padLeft(8, ' ')
            list << addr
        }
    }
}

if (!list) {
    list << "- redis://127.0.0.1:6379".padLeft(8, ' ')
}

"""
global:
  scrape_interval:     ${intervalSecondsGlobal}s
  evaluation_interval: 15s

scrape_configs:
  ## config for the multiple Redis targets that the exporter will scrape
  - job_name: 'redis_exporter_targets'
    static_configs:
      - targets:
${list.join("\\r\\n")}      
    metrics_path: /scrape
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: ${redisExporterAddr}

  ## config for scraping the exporter itself
  - job_name: 'redis_exporter'
    static_configs:
      - targets:
        - ${redisExporterAddr}
"""