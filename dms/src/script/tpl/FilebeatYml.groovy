package script.tpl

def esHost = super.binding.getProperty('esHost') as String
def esPort = super.binding.getProperty('esPort') as int
def adminPassword = super.binding.getProperty('adminPassword') as String

// /home/admin/filebeat/filebeat -c /home/admin/filebeat/filebeat.yml

"""
---
filebeat.prospectors: []

filebeat.modules:
  path: /home/admin/filebeat/modules.d/*.yml
  reload.enabled: false
 
filebeat.config.prospectors:
  path: /home/admin/filebeat/conf.d/*.yml
  enabled: true
  reload.enabled: true

filebeat.registry_file: /var/log/filebeat/registry

logging.files:
  path: /var/log/filebeat/info.log
logging.level: info

processors:
- drop_fields:
    fields: ["@metadata", "beat", "input", "source", "offset", prospector]
    
setup.template.enabled: false
setup.template.name: "xxoo"
setup.template.pattern: "xxoo-*"

output.elasticsearch:
  hosts: ["http://${esHost}:${esPort}"]
  index: "%{[fields.log_topic]}_%{+yyyy.MM.dd}"
  path: "/es/"
  username: "admin"
  password: "${adminPassword}"
"""