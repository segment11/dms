package script

import com.segment.common.Utils

Map params = super.binding.getProperty('params') as Map
def r = common.Utils.isPortListenAvailable(params.port as int, Utils.localIp())

[flag: r]
