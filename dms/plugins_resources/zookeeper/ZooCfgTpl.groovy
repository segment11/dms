package zookeeper

def tickTime = super.binding.getProperty('tickTime') as int
def initLimit = super.binding.getProperty('initLimit') as int
def syncLimit = super.binding.getProperty('syncLimit') as int
def dataDir = super.binding.getProperty('dataDir') as String

def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>

def list = []
nodeIpList.eachWithIndex { String nodeIp, int i ->
    list << "server.${i}=${nodeIp}:2888:3888"
}

def isMetricsExport = super.binding.getProperty('isMetricsExport') as String

List<String> r = []

r <<
        """
tickTime=${tickTime ?: 2000}
initLimit=${initLimit ?: 5}
syncLimit=${syncLimit ?: 2}
dataDir=${dataDir}
clientPort=2181
"""

if ('true' == isMetricsExport) {
    r << """
metricsProvider.className=org.apache.zookeeper.metrics.prometheus.PrometheusMetricsProvider
metricsProvider.httpPort=7000
metricsProvider.exportJvmInfo=true

admin.enableServer=false
"""
}

r << list.join("\r\n")

r.join("\r\n")