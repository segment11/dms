package redis

def nodeIp = super.binding.getProperty('nodeIp') as String
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def port = super.binding.getProperty('port') as int
def dataDir = super.binding.getProperty('dataDir') as String
def password = super.binding.getProperty('password') as String

// eg. key1 value1,key2 value2
def customParameters = super.binding.getProperty('customParameters') as String
String customSegment = ''
if (customParameters) {
    customSegment = customParameters.split(',').join('\r\n')
}

"""
bind ${nodeIp} -::1
port ${port + instanceIndex}
logfile ${dataDir}/instance_${instanceIndex}/redis.log
dir ${dataDir}/instance_${instanceIndex}
${password ? 'requirepass ' + password : ''}
${password ? 'masterauth ' + password : ''}

${customSegment}
"""