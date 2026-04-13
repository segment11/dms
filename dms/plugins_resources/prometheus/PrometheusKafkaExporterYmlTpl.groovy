package prometheus

import model.server.ContainerMountTplHelper

def intervalSecondsGlobal = super.binding.getProperty('intervalSecondsGlobal') as Integer

ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp kafkaExporterApp = applications.app('km_kafka_exporter')

def kafkaExporterAddrList = []
if (kafkaExporterApp && kafkaExporterApp.running()) {
    kafkaExporterApp.containerList.each { x ->
        kafkaExporterAddrList << (x.nodeIp + ':' + x.publicPort(9308))
    }
}

if (!kafkaExporterAddrList) {
    return """
global:
  scrape_interval:     ${intervalSecondsGlobal}s
  evaluation_interval: 15s
"""
}

def kafkaExporterJobConf = """
  - job_name: 'kafka'
    static_configs:
      - targets: [${kafkaExporterAddrList.collect { "'" + it + "'" }.join(',')}]
"""

"""
global:
  scrape_interval:     ${intervalSecondsGlobal}s
  evaluation_interval: 15s

scrape_configs:
${kafkaExporterJobConf}
"""
