package push

import groovy.transform.CompileStatic

import java.util.concurrent.CompletableFuture

@CompileStatic
class PushEvent {
    final String action
    final Map<String, Object> data
    final CompletableFuture<PushBackResult> future

    final String uuid
    final long createdMillis = System.currentTimeMillis()

    PushEvent(String action, Map<String, Object> data, CompletableFuture<PushBackResult> future) {
        this.action = action
        this.data = data
        this.future = future
        this.uuid = UUID.randomUUID().toString()
    }
}
