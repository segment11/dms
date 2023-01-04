package script.tpl

import model.server.ContainerMountTplHelper

def brokers = super.binding.getProperty('brokers').toString().trim()

if (!brokers) {
    def kafkaAppName = super.binding.getProperty('kafkaAppName') as String
    ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
    ContainerMountTplHelper.OneApp kafkaApp = applications.app(kafkaAppName)
    brokers = kafkaApp.allNodeIpList.collect { '"' + it + ':9092' + '"' }.join(',')
} else {
    brokers = '"' + brokers + '"'
}

"""
---
filebeat.prospectors: []

filebeat.modules:
  path: /filebeat/modules.d/*.yml
  reload.enabled: false
 
filebeat.config.prospectors:
  path: /filebeat/conf.d/*.yml
  enabled: true
  
filebeat.registry_file: /var/log/filebeat_registry

logging.files:
  path: /var/log/filebeat_debug.log
logging.level: info

processors:
- drop_fields:
    fields: ["@metadata", "beat", "input", "source", "offset", prospector]
    
output.kafka:
  hosts: [${brokers}]
  topic: "%{[fields.log_topic]}"
  partition.round_robin:
    reachable_only: true
  worker: 2
  required_acks: 1
  max_message_bytes: 10000000  
"""