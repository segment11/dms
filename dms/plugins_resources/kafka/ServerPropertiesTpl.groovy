package kafka

def port = super.binding.getProperty('port') as int
def dataDir = super.binding.getProperty('dataDir') as String
def brokerId = super.binding.getProperty('brokerId') as int
def zkConnectString = super.binding.getProperty('zkConnectString') as String
def zkChroot = super.binding.getProperty('zkChroot') as String
def defaultPartitions = super.binding.getProperty('defaultPartitions') as int
def defaultReplicationFactor = super.binding.getProperty('defaultReplicationFactor') as int
def brokerCount = super.binding.getProperty('brokerCount') as int

def nodeIp = super.binding.getProperty('nodeIp') as String

def zkConnect = zkConnectString + zkChroot

def minReplication = Math.min(3, brokerCount)

"""
broker.id=${brokerId}
listeners=PLAINTEXT://0.0.0.0:${port}
advertised.listeners=PLAINTEXT://${nodeIp}:${port}
zookeeper.connect=${zkConnect}
log.dirs=${dataDir}
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
"""
