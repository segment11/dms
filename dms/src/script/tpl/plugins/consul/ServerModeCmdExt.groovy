package script.tpl.plugins.consul

def instanceIndex = super.binding.getProperty('instanceIndex') as int
def nodeIp = super.binding.getProperty('nodeIp')
def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>

List<String> cmdArgs = []
cmdArgs << "-bind=${nodeIp}".toString()
cmdArgs << "-bootstrap-expect=${nodeIpList.size()}".toString()

if (instanceIndex > 0) {
    cmdArgs << "-join ${nodeIpList[0]}".toString()
}

// $ServerModeCmdExt == -bind=*** --bootstrap-expect=*** -join ***
cmdArgs.join(' ')