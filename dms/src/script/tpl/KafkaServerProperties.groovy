package script.tpl

import model.server.ContainerMountTplHelper

def instanceIndex = super.binding.getProperty('instanceIndex') as int
def nodeIp = super.binding.getProperty('nodeIp') as String
def logDirs = super.binding.getProperty('logDirs') as String
def numPartitions = super.binding.getProperty('numPartitions') as int
def logRetentionHours = super.binding.getProperty('logRetentionHours') as int

def zkAppName = super.binding.getProperty('zkAppName') as String

ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp zkApp = applications.app(zkAppName)
def zookeeperConnect = zkApp.getAllNodeIpList().collect { it + ':2181' }.join(',')

"""
broker.id=${instanceIndex}

listeners=PLAINTEXT://0.0.0.0:${9092}
advertised.listeners=PLAINTEXT://${nodeIp}:${9092}

num.network.threads=3
num.io.threads=8

socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

log.dirs=${logDirs}

num.partitions=${numPartitions}

num.recovery.threads.per.data.dir=1

offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1

log.retention.hours=${logRetentionHours}
log.segment.bytes=1073741824

log.retention.check.interval.ms=300000

zookeeper.connect=${zookeeperConnect}
zookeeper.connection.timeout.ms=18000

group.initial.rebalance.delay.ms=0
"""