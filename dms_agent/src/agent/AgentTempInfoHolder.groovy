package agent

import agent.metrics.AppMetrics
import agent.metrics.ContainerMetrics
import common.LimitQueue
import groovy.transform.CompileStatic
import transfer.NodeInfo

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
class AgentTempInfoHolder {
    static enum Type {
        node, container, app
    }

    LimitQueue<NodeInfo> nodeQueue
    ConcurrentHashMap<Integer, LimitQueue<AppMetrics>> appMetricQueues = new ConcurrentHashMap<>()
    ConcurrentHashMap<Integer, Set<String>> appMetricGaugeNameSet = new ConcurrentHashMap<>()
    ConcurrentHashMap<Integer, LimitQueue<ContainerMetrics>> containerMetricQueues = new ConcurrentHashMap<>()

    private int queueSize = 2880

    AgentTempInfoSender sender

    void addNode(NodeInfo one) {
        if (nodeQueue == null) {
            nodeQueue = new LimitQueue<NodeInfo>(queueSize)
        }
        nodeQueue << one
        if (sender) {
            sender.send(Agent.instance.nodeIp, one.toMap())
        }
    }

    void addAppMetric(Integer appId, Integer instanceIndex, Map gauges, String body) {
        def set = new HashSet(gauges.keySet())
        set.remove('containerId')
        appMetricGaugeNameSet[appId] = set

        AppMetrics one = new AppMetrics()
        one.time = new Date()
        one.nodeIp = Agent.instance.nodeIp
        one.appId = appId
        one.instanceIndex = instanceIndex
        one.rawBody = body
        one.gauges = gauges

        def queue = new LimitQueue<AppMetrics>(queueSize)
        queue << one
        def q = appMetricQueues.putIfAbsent(appId, queue)
        if (q) {
            synchronized (q) {
                q << one
            }
        }
        if (sender) {
            sender.send('' + appId, one.toMap())
        }
    }

    void addContainerMetric(ContainerMetrics one) {
        one.time = new Date()
        one.nodeIp = Agent.instance.nodeIp

        def queue = new LimitQueue<ContainerMetrics>(queueSize)
        queue << one
        def q = containerMetricQueues.putIfAbsent(one.appId, queue)
        if (q) {
            synchronized (q) {
                q << one
            }
        }
        if (sender) {
            sender.send('' + one.appId, one.toMap())
        }
    }

    void clear(Integer appId) {
        appMetricQueues.remove(appId)
        appMetricGaugeNameSet.remove(appId)
        containerMetricQueues.remove(appId)
    }
}
