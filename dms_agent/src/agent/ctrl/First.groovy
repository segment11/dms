package agent.ctrl

import agent.Agent
import common.Utils
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.exceptionHandler { req, resp, t ->
    log.error('', t)
    resp.status = 500

    boolean isNeedFullStackTrace = req.getParameter('fullStackTrace') != null
    if (isNeedFullStackTrace) {
        resp.outputStream << Utils.getStackTraceString(t)
    } else {
        resp.outputStream << t.message
    }
}.get('/auth/token') { req, resp ->
    Agent.instance.authToken
}.get('/hz') { req, resp ->
    def instance = Agent.instance
    if (!instance.isSendNodeInfoOk) {
        resp.halt(500, 'agent send node info fail')
    } else if (!instance.isSendContainerInfoOk) {
        resp.halt(500, 'agent send container info fail')
    } else {
        'ok'
    }
}