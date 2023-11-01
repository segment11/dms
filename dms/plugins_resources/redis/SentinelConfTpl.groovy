package redis

def nodeIp = super.binding.getProperty('nodeIp') as String
def appId = super.binding.getProperty('appId') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def port = super.binding.getProperty('port') as int
def password = super.binding.getProperty('password') as String
def isSingleNode = 'true' == (super.binding.getProperty('isSingleNode') as String)

String dataDir = isSingleNode ? "/data/sentinel/instance_${instanceIndex}" : '/data/sentinel'

def prefix = "s${appId}x${instanceIndex}".toString()
final int len = 40
int moreNumber = len - prefix.length() - 1
def nodeId = prefix + 'x' + (0..<moreNumber).collect { 'a' }.join('')

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

SENTINEL deny-scripts-reconfig yes
SENTINEL resolve-hostnames no
SENTINEL announce-hostnames no
SENTINEL myid ${nodeId}
"""