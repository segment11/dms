package script.tpl

// eg. path:topic,path2:topic2
def pathTopicStr = super.binding.getProperty('pathTopic').toString()
def nodeIp = super.binding.getProperty('nodeIp')
def allAppTopic = super.binding.getProperty('allAppTopic')
def allAppLogDir = super.binding.getProperty('allAppLogDir')
def appIdList = super.binding.getProperty('appIdList') as List<Integer>

def list = []
pathTopicStr.split(',').each {
    def arr = it.toString().split(':')
    def path = arr[0]
    def topic = arr[1]

    list << """
- type: log
  paths:
    - ${path}
  multiline.pattern: '^[[:space:]]+(at|\\.{3})[[:space:]]+\\b|^Caused by:'
  multiline.negate: false
  multiline.match: after
  tail_files: true
  fields:
    log_topic: ${topic}
    node_ip: ${nodeIp}
  enabled: true
"""
}

appIdList.each {
    list << """
- type: log
  paths:
    - "${allAppLogDir}/${it}/*log*"
  multiline.pattern: '^[[:space:]]+(at|\\.{3})[[:space:]]+\\b|^Caused by:'
  multiline.negate: false
  multiline.match: after
  tail_files: true
  fields:
    log_topic: ${allAppTopic}
    app_id: ${it}
    node_ip: ${nodeIp}
  enabled: true
"""
}

list.join("\r\n")
