package script

import common.Utils

Map params = super.binding.getProperty('params') as Map
def r = Utils.isPortListenAvailable(params.port as int, Utils.localIp())

[flag: r]
