package ctrl.push

import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import push.ClientAction
import push.ClientConnectHolder
import push.PushBackResult

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/push') {
    h.post('/to-client-event/add') { req, resp ->
        def clientHolder = ClientConnectHolder.instance

        def map = req.bodyAs(HashMap)
        def clientId = map.clientId as String
        def action = map.action as String
        assert clientId && action

        Map<String, Object> data = map.data as Map<String, Object>
        def f = clientHolder.pushToClient(clientId, action, data)

        try {
            def result = f.get(5000, TimeUnit.MILLISECONDS)
            if (result.isOk) {
                return result.data
            } else {
                resp.halt(500, result.message)
            }
        } catch (TimeoutException ignored) {
            resp.halt(500, 'timeout')
        }
    }

    h.post('/client-info/update') { req, resp ->
        def clientHolder = ClientConnectHolder.instance

        def map = req.bodyAs(HashMap)
        def clientId = map.clientId as String
        assert clientId

        def vpcId = map.vpcId as String
        def instanceId = map.instanceId as String
        assert vpcId && instanceId

        clientHolder.updateClientInfo(clientId, vpcId, instanceId)
        [flag: true]
    }

    h.get('/event/todo') { req, resp ->
        def clientHolder = ClientConnectHolder.instance

        def clientId = req.param('clientId')
        assert clientId

        def cloudProvider = req.param('cloudProvider')
        def region = req.param('region')
        assert cloudProvider && region
        clientHolder.addClientConnect(clientId, cloudProvider, region)

        def firstEventData = clientHolder.getFirstPushEvent(clientId)
        if (!firstEventData) {
            return [action: ClientAction.SKIP]
        }

        firstEventData
    }

    h.post('/event/complete') { req, resp ->
        def clientHolder = ClientConnectHolder.instance

        def map = req.bodyAs(HashMap)
        def clientId = map.clientId as String
        def uuid = map.uuid as String
        def errorMessage = map.errorMessage as String
        Map<String, Object> data = map.data as Map<String, Object>

        def result = errorMessage ? PushBackResult.fail(errorMessage) : PushBackResult.ok(data)

        def flag = clientHolder.complete(clientId, uuid, result)
        [flag: flag]
    }
}