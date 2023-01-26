package ctrl

import common.Event
import model.EventDTO
import org.segment.web.handler.ChainHandler
import server.AgentCaller

def h = ChainHandler.instance

h.group('/event') {
    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        int pageSize = req.param('pageSize') ? req.param('pageSize') as int : 10

        def type = req.param('type')
        def clusterId = req.param('clusterId')
        def nodeIp = req.param('nodeIp')
        def reason = req.param('reason')
        def appId = req.param('appId')

        if (nodeIp) {
            resp.end AgentCaller.instance.get(clusterId as int, nodeIp, '/dmc/event/list',
                    [type: type, reason: reason, pageNum: pageNum, pageSize: pageSize], String)
            return
        }

        def event = new EventDTO(type: type, reason: reason)
        if (appId) {
            event.type = Event.Type.app.name()
            event.result = appId
        } else {
            if (!type && !reason) {
                event.noWhere()
            }
        }
        event.orderBy('id desc')
        event.loadPager(pageNum, pageSize)
    }.get('/reason/list') { req, resp ->
        def type = req.param('type')
        assert type

        def clusterId = req.param('clusterId')
        def nodeIp = req.param('nodeIp')
        if (nodeIp) {
            resp.end AgentCaller.instance.get(clusterId as int, nodeIp, '/dmc/event/reason/list', [type: type], String)
            return
        }
        new EventDTO().useD().query("select distinct(reason) reason from event where type = ?", [type])
    }
}
