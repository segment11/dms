package ex

import groovy.transform.CompileStatic

@CompileStatic
class HttpInvokeException extends RuntimeException {
    HttpInvokeException(String var1) {
        super(var1)
    }
}
