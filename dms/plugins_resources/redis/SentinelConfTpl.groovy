package redis

import model.server.ContainerMountTplHelper

def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>
def nodeIp = super.binding.getProperty('nodeIp') as String
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def port = super.binding.getProperty('port') as int
def password = super.binding.getProperty('password') as String
def isSingleNode = 'true' == (super.binding.getProperty('isSingleNode') as String)

String dataDir = isSingleNode ? "/data/sentinel/instance_${instanceIndex}" : '/data/sentinel'

def downAfterMs = super.binding.getProperty('downAfterMs') as int
def failoverTimeout = super.binding.getProperty('failoverTimeout') as int

def redisAppNames = super.binding.getProperty('redisAppNames') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
List<String> list = []
redisAppNames.split(',').each { redisAppName ->
    ContainerMountTplHelper.OneApp redisApp = applications.app(redisAppName)

    def redisFirstNodeIp = redisApp.allNodeIpList[0]
    def confOne = redisApp.app.conf.fileVolumeList.find { it.dist == '/etc/redis/redis.conf' }
    def redisPort = confOne.paramList.find { it.key == 'port' }.value as int
    def redisPassword = confOne.paramList.find { it.key == 'password' }.value

    list << """
sentinel monitor ${redisAppName} ${redisFirstNodeIp} ${redisPort} ${nodeIpList.size() >= 3 ? 2 : 1}
sentinel auth-pass ${redisAppName} ${redisPassword}
sentinel down-after-milliseconds ${redisAppName} ${downAfterMs}
sentinel failover-timeout ${redisAppName} ${failoverTimeout}
sentinel parallel-syncs ${redisAppName} 1
"""
}

"""
bind ${nodeIp} -::1
protected-mode no
port ${port + (isSingleNode ? instanceIndex : 0)}
requirepass ${password}
sentinel sentinel-pass ${password}
daemonize no
pidfile /var/run/redis-sentinel.pid
logfile ""
dir ${dataDir}
acllog-max-len 128

sentinel deny-scripts-reconfig yes
SENTINEL resolve-hostnames no
SENTINEL announce-hostnames no

${list.join("\n\n")}
"""