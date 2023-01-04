package transfer

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class ContainerConfigInfo {
    Integer pid

    String containerId

    String networkMode

    List<ContainerInfo.PortMapping> ports

    Integer publicPort(Integer privatePort) {
        if ('host' == networkMode) {
            return privatePort
        }

        def one = ports?.find {
            it.privatePort == privatePort
        }
        one ? one.publicPort : privatePort
    }
}
