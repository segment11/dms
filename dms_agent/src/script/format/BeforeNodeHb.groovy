package script.format

import transfer.NodeInfo

NodeInfo info = super.binding.getProperty('info') as NodeInfo

// add test tags
if (info.nodeIp.startsWith('192.168.')) {
    info.tags << 'test'
    info.labels.key1 = 'value1'
}

// void
return



