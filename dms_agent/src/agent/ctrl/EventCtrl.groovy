package agent.ctrl

import agent.Agent
import agent.AgentTempInfoHolder
import agent.AgentTempInfoHolder.Type
import common.Event
import common.Pager
import org.segment.web.handler.ChainHandler

import java.text.SimpleDateFormat

def h = ChainHandler.instance

h.group('/event') {
    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def type = req.param('type')
        def reason = req.param('reason')
        def appId = req.param('appId')

        def list = Agent.instance.eventQueue.findAll {
            if (type && it.type.name() != type) {
                return false
            }
            if (reason && it.reason != reason) {
                return false
            }
            if (appId) {
                if (it.type.name() != 'app' || it.result != appId.toString()) {
                    return false
                }
            }
            true
        }
        def pager = new Pager<Event>(pageNum, pageSize)
        pager.totalCount = list.size()
        pager.list = list[pager.start..<pager.end]
        pager
    }.get('/reason/list') { req, resp ->
        Set<String> set = []
        Agent.instance.eventQueue.each {
            set << it.reason
        }
        set.collect { [reason: it] }
    }
}

h.get('/metric/queue') { req, resp ->
    def queueType = req.param('queueType') ?: Type.node.name()
    def type = req.param('type')
    def appId = req.param('appId')
    def instanceIndex = req.param('instanceIndex')
    def containerId = req.param('containerId')
    def gaugeName = req.param('gaugeName')

    if (queueType == Type.node.name()) {
        def list = AgentTempInfoHolder.instance.nodeQueue
        return list ? [timelineList: list.collect {
            new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(it.time)
        }, list                    : list.collect {
            if ('cpu' == type) {
                return it.cpuUsedPercent()
            } else if ('mem' == type) {
                return it.mem.usedPercent
            }
            0
        }] : [:]
    }

    if (queueType == Type.app.name()) {
        def list = AgentTempInfoHolder.instance.appMetricQueues[appId as int]
        if (instanceIndex) {
            list = list.findAll { one ->
                one.instanceIndex == (instanceIndex as int)
            }
        }
        return list ? [timelineList: list.collect {
            new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(it.time)
        }, list                    : list.collect {
            if (!gaugeName) {
                return 0
            }
            def val = it.getGaugeValue(gaugeName)
            return val ? val as double : 0
        }] : [:]
    }

    def list = AgentTempInfoHolder.instance.containerMetricQueues[appId as int]
    if (!list) {
        return [:]
    }
    if (instanceIndex) {
        list = list.findAll { one ->
            one.instanceIndex == (instanceIndex as int)
        }
    }
    if (containerId) {
        list = list.findAll { one ->
            one.containerId == containerId
        }
    }
    return [timelineList: list.collect {
        new SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(it.time)
    }, list             : list.collect {
        if ('cpu' == type) {
            return it.cpuPerc
        } else if ('mem' == type) {
            return it.memUsage
        }
        0
    }]
}.get('/metric/gauge/name/list') { req, resp ->
    def appId = req.param('appId')
    AgentTempInfoHolder.instance.appMetricGaugeNameSet[appId as int] ?: []
}.get('/metric/app/clear') { req, resp ->
    def appId = req.param('appId')
    AgentTempInfoHolder.instance.clear(appId as int)
    [flag: true]
}
