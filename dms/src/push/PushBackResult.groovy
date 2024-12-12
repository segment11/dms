package push

import groovy.transform.CompileStatic

@CompileStatic
class PushBackResult {
    Map<String, Object> data

    boolean isOk

    String message

    static PushBackResult fail(String message) {
        new PushBackResult(isOk: false, message: message)
    }

    static PushBackResult ok(Map<String, Object> data) {
        new PushBackResult(isOk: true, data: data)
    }
}
