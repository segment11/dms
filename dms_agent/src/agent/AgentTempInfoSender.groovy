package agent

import groovy.transform.CompileStatic

@CompileStatic
interface AgentTempInfoSender {
    void send(String messageKey, Map content)
}
