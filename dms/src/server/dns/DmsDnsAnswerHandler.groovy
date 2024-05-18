package server.dns

import groovy.transform.CompileStatic

@CompileStatic
interface DmsDnsAnswerHandler {
    byte[] answer(String domain)
}
