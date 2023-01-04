package script.tpl

import model.server.ContainerMountTplHelper

def kafkaAppName = super.binding.getProperty('kafkaAppName') as String
def esAppName = super.binding.getProperty('esAppName') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp kafkaApp = applications.app(kafkaAppName)
ContainerMountTplHelper.OneApp esApp = applications.app(esAppName)

def brokers = kafkaApp.allNodeIpList.collect { it + ':9092' }.join(',')
def esServers = esApp.allNodeIpList.collect { '"' + it + ':9200' + '"' }.join(',')

"""
input {
    kafka {
        type => "beat_app"
        bootstrap_servers => "${brokers}"
        topics => "beat_app"
        group_id => "dms_logstash"
        codec => json {
            charset => "utf-8"
        }
    }
}
output {
    if [type] == "beat_app" {
        elasticsearch {
            hosts => [${esServers}]
            index => "%{[fields][app_id]}"
        }
    }
}
"""