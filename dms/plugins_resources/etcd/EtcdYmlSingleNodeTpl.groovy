package etcd

def enableV2 = super.binding.getProperty('enableV2') as String
def dataDir = super.binding.getProperty('dataDir') as String

def appId = super.binding.getProperty('appId') as int
def containerNumber = super.binding.getProperty('containerNumber') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int
def nodeIp = super.binding.getProperty('nodeIp')
def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>

List<Map> envList = super.binding.getProperty('envList') as List<Map>
Boolean isNewMember = envList.find { 'isNewMember' == it.key }?.value as Boolean

def step = instanceIndex * 100

def list = []
containerNumber.times { i ->
    def ip = nodeIpList.size() > i ? nodeIpList[i] : nodeIpList[0]
    list << "etcd${i}=http://${ip}:${2380 + i * 100}"
}
def cluster = list.join(',')

"""
name: etcd${instanceIndex}
data-dir: ${dataDir ?: '/data/etcd'}/instance_${instanceIndex}
listen-client-urls: http://${nodeIp}:${2379 + step},http://127.0.0.1:${2379 + step}
advertise-client-urls: http://${nodeIp}:${2379 + step},http://127.0.0.1:${2379 + step}

listen-peer-urls: http://${nodeIp}:${2380 + step}
initial-cluster: ${cluster}
initial-cluster-state: ${isNewMember ? 'existing' : 'new'}
initial-cluster-token: cluster-${appId}

enable-v2: ${'true' == enableV2 ? 'true' : 'false'}
"""
