package script.tpl

def appId = super.binding.getProperty('appId') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def nodeIp = super.binding.getProperty('nodeIp') as String
def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>

def dbPassword = super.binding.getProperty('dbPassword') as String
def dataDir = super.binding.getProperty('dataDir') as String
def logDir = super.binding.getProperty('logDir') as String
def port = super.binding.getProperty('port') as int

def customParameters = super.binding.getProperty('customParameters') as String
def defaultParameters = super.binding.getProperty('defaultParameters') as String

"""
bind ${nodeIp}
port ${port}
requirepass ${dbPassword}
db-name engula_dms_app_${appId}
dir ${dataDir}
log-dir ${logDir}
cluster-enabled yes

${customParameters}

${defaultParameters}
"""
