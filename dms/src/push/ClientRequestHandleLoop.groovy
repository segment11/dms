package push

import com.segment.common.http.Invoker
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@CompileStatic
@Singleton
@Slf4j
class ClientRequestHandleLoop {
    String clientId
    String cloudProvider
    String region

    final String eventTodoUri = '/dms/push/event/todo'
    final String eventDoneUri = '/dms/push/event/complete'
    final String updateInfoUri = '/dms/push/client-info/update'

    Invoker invoker
    ClientEventHandler eventHandler

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()

    void start(long intervalMillis) {
        executorService.scheduleWithFixedDelay({
            requestEventAndHandle()
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS)
        log.warn 'Client request scheduler started'
    }

    void stop() {
        executorService.shutdown()
        log.warn 'Client request scheduler stopped'
    }

    void updateInfo(String vpcId, String instanceId) {
        def params = [clientId: clientId, vpcId: vpcId, instanceId: instanceId]
        invoker.request(updateInfoUri, params, String.class, null, true)
    }

    private long skipHandleLoopCount = 0

    void requestEventAndHandle() {
        def params = [clientId: clientId, cloudProvider: cloudProvider, region: region]
        def clientAction = invoker.request(eventTodoUri, params, ClientAction.class, null, false)
        if (clientAction.action == ClientAction.SKIP) {
            skipHandleLoopCount++
            if (skipHandleLoopCount % 100 == 0) {
                log.info 'no event to handle, request loop count: {}', skipHandleLoopCount
            }
            return
        }

        skipHandleLoopCount = 0
        try {
            def handleResult = eventHandler.handle(clientAction)
            completeSuccess(clientAction.uuid, handleResult)
        } catch (Exception e) {
            log.error 'Handle event error', e
            completeError(clientAction.uuid, e.message)
        }
    }

    void completeEvent(String uuid, String errorMessage, Map<String, Object> data) {
        def params = [clientId: clientId, uuid: uuid, errorMessage: errorMessage, data: data]
        def flagMap = invoker.request(eventDoneUri, params, HashMap.class, null, true)
        def flag = flagMap.flag as boolean
        if (!flag) {
            log.error 'Complete event error'
        }
    }

    void completeError(String uuid, String errorMessage) {
        completeEvent(uuid, errorMessage, null)
    }

    void completeSuccess(String uuid, Map<String, Object> data) {
        completeEvent(uuid, null, data)
    }
}
