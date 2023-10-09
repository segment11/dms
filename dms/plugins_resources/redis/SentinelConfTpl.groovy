package redis

def nodeIp = super.binding.getProperty('nodeIp') as String
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def port = super.binding.getProperty('port') as int
def password = super.binding.getProperty('password') as String
def isSingleNode = 'true' == (super.binding.getProperty('isSingleNode') as String)

String dataDir = isSingleNode ? "/data/sentinel/instance_${instanceIndex}" : '/data/sentinel'

"""
bind ${nodeIp} -::1
protected-mode no
port ${port + (isSingleNode ? instanceIndex : 0)}
${password ? 'requirepass ' + password : ''}
${password ? 'masterauth ' + password : ''}
daemonize no
pidfile /var/run/redis-sentinel.pid
logfile ""
dir ${dataDir}
acllog-max-len 128

sentinel deny-scripts-reconfig yes
SENTINEL resolve-hostnames no
SENTINEL announce-hostnames no
"""