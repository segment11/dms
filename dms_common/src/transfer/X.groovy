package transfer

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class X {
    String nodeIp
    Integer clusterId

    List<ContainerInfo> containers
}
