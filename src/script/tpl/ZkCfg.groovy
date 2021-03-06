package script.tpl

def instanceIndex = super.binding.getProperty('instanceIndex')
def targetNodeIpList = super.binding.getProperty('targetNodeIpList') as List<String>
def list = []
targetNodeIpList.eachWithIndex { String nodeIp, int i ->
    list << "server.${i}=127.0.0.1:${2287 + i}:${3387 + i}"
//    list << "server.${i}=${nodeIp}:2888:3888"
}
"""
tickTime=2000
initLimit=5
syncLimit=2
dataDir=/opt/zk/data
clientPort=${2181 + instanceIndex}
#clientPort=2181
metricsProvider.className=org.apache.zookeeper.metrics.prometheus.PrometheusMetricsProvider
metricsProvider.httpPort=${7000 + instanceIndex}
#metricsProvider.httpPort=7000
metricsProvider.exportJvmInfo=true

admin.enableServer=false

${list.join("\r\n")}
"""