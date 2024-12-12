package push

import groovy.transform.CompileStatic

@CompileStatic
class ClientAction {
    static final String SKIP = 'NO_ACTION'

    String action
    String uuid
    Map<String, Object> data
}
