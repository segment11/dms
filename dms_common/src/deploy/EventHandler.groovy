package deploy

import common.Event
import groovy.transform.CompileStatic

@CompileStatic
interface EventHandler {
    void handle(Event event)
}