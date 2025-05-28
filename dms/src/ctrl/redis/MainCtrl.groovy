package ctrl.redis

import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/redis') {
    h.get('/overview') { req, resp ->
        [:]
    }
}
