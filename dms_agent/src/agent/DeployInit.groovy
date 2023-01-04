package agent

import common.Event
import deploy.DeploySupport
import deploy.EventHandler
import groovy.transform.CompileStatic

@CompileStatic
class DeployInit {
    volatile static boolean isInitDone = false

    static void initDeployEventCallback() {
        if (isInitDone) {
            return
        }
        DeploySupport.instance.eventHandler = new EventHandler() {
            @Override
            void handle(Event event) {
                Agent.instance.addEvent(event)
            }
        }
        isInitDone = true
    }
}
