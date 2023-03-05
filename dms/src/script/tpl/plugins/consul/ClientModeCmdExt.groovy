package script.tpl.plugins.consul

def nodeIpList = super.binding.getProperty('nodeIpList') as List<String>

// $ClientModeCmdExt == --join *** --join ***
nodeIpList.collect { '--join ' + it }.join(' ')