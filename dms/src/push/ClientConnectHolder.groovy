package push

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@CompileStatic
@Singleton
@Slf4j
class ClientConnectHolder {
    @CompileStatic
    static class ClientConnect {
        private final String clientId

        ClientConnect(String clientId) {
            this.clientId = clientId
        }

        @Override
        int hashCode() {
            clientId.hashCode()
        }

        long lastHeartbeatTime
        long heartbeatLoopCount

        // cloud info
        String cloudProvider
        String region
        String vpcId
        String instanceId

        @Override
        String toString() {
            "ClientConnect {clientId:" +
                    clientId + ", cloudProvider:" +
                    cloudProvider + ", region:" +
                    region + ", vpcId:" +
                    vpcId + ", instanceId:" +
                    instanceId +
                    "}"
        }
    }

    private Map<ClientConnect, LinkedList<PushEvent>> eventQueueByClient = [:]

    synchronized void addClientConnect(String clientId, String cloudProvider, String region) {
        def clientConnect = eventQueueByClient.keySet().find { it.clientId == clientId }
        if (clientConnect) {
            clientConnect.cloudProvider = cloudProvider
            clientConnect.region = region
            clientConnect.lastHeartbeatTime = System.currentTimeMillis()
            clientConnect.heartbeatLoopCount++
            if (clientConnect.heartbeatLoopCount % 100 == 0) {
                log.info 'client connect updated, client id: {}, cloud provider: {}, region: {}, heartbeat loop count: {}',
                        clientId, cloudProvider, region, clientConnect.heartbeatLoopCount
            }
            return
        }

        def client = new ClientConnect(clientId)
        client.cloudProvider = cloudProvider
        client.region = region
        eventQueueByClient.put(client, new LinkedList<PushEvent>())
        log.warn 'client connect added, client id: {}, cloud provider: {}, region: {}', clientId, cloudProvider, region
    }

    synchronized void updateClientInfo(String clientId, String vpcId, String instanceId) {
        def clientConnect = eventQueueByClient.keySet().find { it.clientId == clientId }
        if (!clientConnect) {
            log.warn 'client connect not found, client id: {}, vpc id: {}, instance id: {}', clientId, vpcId, instanceId
            return
        }

        clientConnect.vpcId = vpcId
        clientConnect.instanceId = instanceId
        log.warn 'client connect updated, client id: {}, vpc id: {}, instance id: {}', clientId, vpcId, instanceId
    }

    synchronized CompletableFuture<PushBackResult> pushToClient(String clientId, String action, Map<String, Object> data) {
        // find client
        def client = eventQueueByClient.keySet().find { it.clientId == clientId }
        if (!client) {
            return CompletableFuture.completedFuture(PushBackResult.fail('client connect not found'))
        }

        // push event
        def future = new CompletableFuture<PushBackResult>()
        def event = new PushEvent(action, data, future)

        def eventQueue = eventQueueByClient.get(client)
        eventQueue.add(event)

        future
    }

    synchronized ClientAction getFirstPushEvent(String clientId) {
        // find client
        def client = eventQueueByClient.keySet().find { it.clientId == clientId }
        if (!client) {
            log.warn 'client connect not found, client id: {}', clientId
            return null
        }

        // get event
        def eventQueue = eventQueueByClient.get(client)
        if (eventQueue.isEmpty()) {
            return null
        }

        def first = eventQueue.first()
        new ClientAction(action: first.action, uuid: first.uuid, data: first.data)
    }

    synchronized boolean complete(String clientId, String uuid, PushBackResult result) {
        // find client
        def client = eventQueueByClient.keySet().find { it.clientId == clientId }
        if (!client) {
            log.warn 'client connect not found, client id: {}', clientId
            return false
        }

        // find event
        def eventQueue = eventQueueByClient.get(client)
        def event = eventQueue.find { it.uuid == uuid }
        if (!event) {
            log.warn 'event not found, client id: {}, uuid: {}', clientId, uuid
            return false
        }

        // complete
        event.future.complete(result)
        eventQueue.remove(event)
        true
    }

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()

    void startRemoveExpiredInterval(long intervalMillis, long eventExpiredMillis, long heartbeatExpiredMillis) {
        executorService.scheduleAtFixedRate({
            removeEventExpired(eventExpiredMillis)
            removeClientExpired(heartbeatExpiredMillis)
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS)
        log.warn 'Push event scheduler started'
    }

    void stopRemoveExpiredInterval() {
        executorService.shutdown()
        log.warn 'Push event scheduler stopped'
    }

    synchronized void removeEventExpired(long eventExpiredMillis) {
        eventQueueByClient.each { client, eventQueue ->
            eventQueue.removeIf { it.createdMillis + eventExpiredMillis < System.currentTimeMillis() }
        }
    }

    synchronized void removeClientExpired(long heartbeatExpiredMillis) {
        def it = eventQueueByClient.entrySet().iterator()
        while (it.hasNext()) {
            def entry = it.next()
            def client = entry.key
            if (client.lastHeartbeatTime + heartbeatExpiredMillis < System.currentTimeMillis()) {
                it.remove()
                log.warn 'client removed as heartbeat expired, client: {}', client
            }
        }
    }
}
