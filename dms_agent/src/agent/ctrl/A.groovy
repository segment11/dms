package agent.ctrl

import com.github.kevinsawicki.http.HttpRequest
import com.segment.common.Conf
import com.segment.common.Utils
import common.Const
import org.segment.web.handler.AbstractHandler
import org.segment.web.handler.ChainHandler
import org.segment.web.handler.Req
import org.segment.web.handler.Resp
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletRequest

def h = ChainHandler.instance

h.options('/proxy_handle', new AbstractHandler() {
    Logger log = LoggerFactory.getLogger(this.getClass())

    @Override
    protected boolean isRequestMatch(String method, String uriInput, HttpServletRequest request) {
        def targetServerAddr = request.getHeader(Const.PROXY_TARGET_SERVER_ADDR_HEADER)
        // need proxy
        targetServerAddr && !targetServerAddr.contains(Utils.localIp() + ':')
    }

    @Override
    Object hi(Req req, Resp resp) {
        String serverUrl = req.header(Const.PROXY_TARGET_SERVER_ADDR_HEADER)
        def realUrl = serverUrl + req.raw().requestURI
        log.info 'proxy -> ' + realUrl

        int connectTimeout = Conf.instance.getInt('agent.proxy.connectTimeout', 500)
        int readTimeout = Conf.instance.getInt('agent.proxy.readTimeout', 2000)
        def timeoutGiven = req.header(Const.PROXY_READ_TIMEOUT_HEADER)
        int readTimeoutFinal = (timeoutGiven ? timeoutGiven as int : readTimeout) - 500

        def method = req.method().toUpperCase()
        def isPost = 'POST' == method

        HttpRequest request
        if (isPost) {
            request = HttpRequest.post(realUrl)
            // controller -> agent
            // agent -> controller
            request.header(Const.AUTH_TOKEN_HEADER, req.header(Const.AUTH_TOKEN_HEADER) ?: '')
            request.header(Const.SCRIPT_NAME_HEADER, req.header(Const.SCRIPT_NAME_HEADER) ?: '')
            // agent -> controller
            request.header(Const.CLUSTER_ID_HEADER, req.header(Const.CLUSTER_ID_HEADER) ?: '')

            request.connectTimeout(connectTimeout).readTimeout(readTimeoutFinal)
            request.send(req.body())
        } else {
            Map params = [:]
            req.raw().getParameterNames().each { String key ->
                params[key] = req.param(key)
            }
            request = HttpRequest.get(realUrl, params, true)
            // controller -> agent
            // agent -> controller
            request.header(Const.AUTH_TOKEN_HEADER, req.header(Const.AUTH_TOKEN_HEADER) ?: '')
            request.header(Const.SCRIPT_NAME_HEADER, req.header(Const.SCRIPT_NAME_HEADER) ?: '')
            // agent -> controller
            request.header(Const.CLUSTER_ID_HEADER, req.header(Const.CLUSTER_ID_HEADER) ?: '')

            request.connectTimeout(connectTimeout).readTimeout(readTimeoutFinal)
        }

        resp.status(request.code())
        resp.end request.body()
    }
})
