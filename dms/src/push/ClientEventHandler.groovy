package push

import groovy.transform.CompileStatic

@CompileStatic
interface ClientEventHandler {
    Map<String, Object> handle(ClientAction clientAction)
}