package redis

import model.RmConfigTemplateDTO

def nodeIp = super.binding.getProperty('nodeIp') as String
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def port = super.binding.getProperty('port') as int
def maxmemoryMB = super.binding.getProperty('maxmemoryMB') as int
def dataDir = super.binding.getProperty('dataDir') as String
def password = super.binding.getProperty('password') as String
def isSingleNode = 'true' == (super.binding.getProperty('isSingleNode') as String)
def isCluster = 'true' == (super.binding.getProperty('isCluster') as String)

def dataDirFinal = dataDir + "/instance_${instanceIndex}"

def configTemplateId = super.binding.getProperty('configTemplateId') as int
def configTemplateOne = new RmConfigTemplateDTO(id: configTemplateId).one()
def kvList = configTemplateOne ? configTemplateOne.configItems.items : null
String customSegment = ''
if (kvList) {
    // remove overwrite items
    kvList.removeAll {
        it.key in ['bind', 'port', 'logfile', 'dir', 'requirepass', 'masterauth', 'pidfile', 'cluster-enabled']
    }

    if (kvList) {
        customSegment = kvList.collect { it.key + ' ' + it.value }.join('\r\n')
    }
}

"""
bind ${nodeIp} -::1
port ${port + (isSingleNode ? instanceIndex : 0)}
logfile ${dataDirFinal}/redis.log
dir ${dataDirFinal}
${password ? 'requirepass ' + password : ''}
${password ? 'masterauth ' + password : ''}
pidfile /var/run/redis_${port}.pid

maxmemory ${maxmemoryMB}mb

${customSegment}

${isCluster ? 'cluster-enabled yes' : ''}
"""