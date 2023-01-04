import common.Utils
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import server.scheduler.Guardian

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.exceptionHandler { req, resp, t ->
    log.error('', t)
    resp.status = 500
//    resp.outputStream << Utils.getStackTraceString(t)
    resp.outputStream << t.message ?: Utils.getStackTraceString(t)
}.get('/route/list') { req, resp ->
    [list: h.list.collect { it.name() }]
}.get('/hz') { req, resp ->
    def instance = Guardian.instance
    if (!instance.isCronJobRefreshDone) {
        resp.halt(500, 'cron job refresh fail')
    } else if (instance.failAppJobIdList) {
        resp.halt(500, 'fail application process job - ' + instance.failAppJobIdList)
    } else if (instance.failGuardAppIdList) {
        resp.halt(500, 'fail application guard - ' + instance.failGuardAppIdList)
    } else if (instance.failHealthCheckAgentNodeIpList) {
        resp.halt(500, 'fail agent health check - ' + instance.failHealthCheckAgentNodeIpList)
    } else {
        'ok'
    }
}