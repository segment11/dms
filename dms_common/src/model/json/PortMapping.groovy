package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class PortMapping {
    @CompileStatic
    static enum ListenType {
        tcp, udp
    }

    Integer privatePort

    Integer publicPort

    boolean isGenerateByHost = false

    ListenType listenType = ListenType.tcp

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof PortMapping)) {
            return false
        }
        def one = (PortMapping) obj
        privatePort == one.privatePort && publicPort == one.publicPort &&
                isGenerateByHost == one.isGenerateByHost && listenType == one.listenType
    }
}
