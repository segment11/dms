package zookeeper

def tickTime = super.binding.getProperty('tickTime') as int
def initLimit = super.binding.getProperty('initLimit') as int
def syncLimit = super.binding.getProperty('syncLimit') as int
def dataDir = super.binding.getProperty('dataDir') as String

def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>

def instanceIndex = super.binding.getProperty('instanceIndex') as int
def step = instanceIndex * -10

def list = []
nodeIpList.eachWithIndex { String nodeIp, int i ->
    list << "server.${i}=${nodeIp}:${2888 + i * -10}:${3888 + i * -10}"
}

def isMetricsExport = super.binding.getProperty('isMetricsExport') as String

List<String> r = []

r <<
        """
tickTime=${tickTime ?: 2000}
initLimit=${initLimit ?: 5}
syncLimit=${syncLimit ?: 2}
dataDir=${dataDir}/instance_${instanceIndex}
clientPort=${2181 + step}
"""

if ('true' == isMetricsExport) {
    r << """
metricsProvider.className=org.apache.zookeeper.metrics.prometheus.PrometheusMetricsProvider
metricsProvider.httpPort=${7000 + step}
metricsProvider.exportJvmInfo=true

admin.enableServer=false
"""
}

r << list.join("\r\n")

r.join("\r\n")