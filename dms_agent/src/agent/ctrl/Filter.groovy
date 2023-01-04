package agent.ctrl

import common.Const
import common.TimerSupport
import common.Utils
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.before('/**') { req, resp ->
    String targetServerAddr = req.header(Const.PROXY_TARGET_SERVER_ADDR_HEADER)
    if (targetServerAddr && !targetServerAddr.contains(Utils.localIp() + ':')) {
        return
    }

    def uri = req.uri()
    String key
    if (uri.endsWith('/script/exe')) {
        def scriptName = req.header(Const.SCRIPT_NAME_HEADER)
        key = 'script_exc_' + scriptName.replaceAll(' ', '_')
    } else {
        key = uri
    }

    TimerSupport.startUriHandle(key)
}

h.afterAfter('/**') { req, resp ->
    TimerSupport.stopUriHandle()
}
