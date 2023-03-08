package etcd

def enableV2 = super.binding.getProperty('enableV2') as String
def dataDir = super.binding.getProperty('dataDir') as String

def appId = super.binding.getProperty('appId') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int
def nodeIp = super.binding.getProperty('nodeIp')
def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>
def list = []
nodeIpList.eachWithIndex { String ip, int i ->
    list << "etcd${i}=http://${ip}:2380"
}
def cluster = list.join(',')

"""
name: etcd${instanceIndex}
data-dir: ${dataDir ?: '/data/etcd'}
listen-client-urls: http://${nodeIp}:2379,http://127.0.0.1:2379
advertise-client-urls: http://${nodeIp}:2379,http://127.0.0.1:2379

listen-peer-urls: http://${nodeIp}:2380
initial-advertise-peer-urls: http://${nodeIp}:2380
initial-cluster: ${cluster}
initial-cluster-state: new
initial-cluster-token: cluster-${appId}

enable-v2: ${'true' == enableV2 ? 'true' : 'false'}
"""
