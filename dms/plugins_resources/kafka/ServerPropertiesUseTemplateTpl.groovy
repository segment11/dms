package kafka

import model.KmConfigTemplateDTO

def port = super.binding.getProperty('port') as int
def dataDir = super.binding.getProperty('dataDir') as String
def zkConnectString = super.binding.getProperty('zkConnectString') as String
def zkChroot = super.binding.getProperty('zkChroot') as String
def defaultPartitions = super.binding.getProperty('defaultPartitions') as int
def defaultReplicationFactor = super.binding.getProperty('defaultReplicationFactor') as int
def brokerCount = super.binding.getProperty('brokerCount') as int

def nodeIp = super.binding.getProperty('nodeIp') as String
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def zkConnect = zkConnectString + zkChroot

def minReplication = Math.min(3, brokerCount)

def configTemplateId = super.binding.getProperty('configTemplateId') as int
def configTemplateOne = new KmConfigTemplateDTO(id: configTemplateId).one()
def kvList = configTemplateOne ? configTemplateOne.configItems.items : null
String customSegment = ''
if (kvList) {
    kvList.removeAll {
        it.key in ['broker.id', 'listeners', 'advertised.listeners', 'zookeeper.connect', 'log.dirs']
    }

    if (kvList) {
        customSegment = kvList.collect { it.key + '=' + it.value }.join('\n')
    }
}

"""
broker.id=${instanceIndex}
listeners=PLAINTEXT://0.0.0.0:${port + instanceIndex}
advertised.listeners=PLAINTEXT://${nodeIp}:${port + instanceIndex}
zookeeper.connect=${zkConnect}
log.dirs=${dataDir}_${instanceIndex}
num.partitions=${defaultPartitions}
default.replication.factor=${defaultReplicationFactor}
offsets.topic.replication.factor=${minReplication}
transaction.state.log.replication.factor=${minReplication}
log.retention.hours=168
log.segment.bytes=1073741824
num.io.threads=8
num.network.threads=3
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

${customSegment}
"""
