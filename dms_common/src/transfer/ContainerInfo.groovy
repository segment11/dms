package transfer

import common.ContainerHelper
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class ContainerInfo implements Comparable<ContainerInfo> {
    final static String STATE_RUNNING = 'running'
    final static String STATE_EXITED = 'exited'

    @Override
    int compareTo(ContainerInfo y) {
        if (appId() != y.appId()) {
            return appId() <=> y.appId()
        }
        instanceIndex() <=> y.instanceIndex()
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class PortMapping {
        Integer privatePort
        Integer publicPort
        String type
        String ip
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class Mount {
        String name
        String source
        String destination
        String driver
        String mode
        Boolean rw
        String propagation
    }

    String nodeIp
    Integer clusterId
    String namespaceId
    String appName
    String appDes

    String id
    List<String> names
    String image
    String imageId
    String command
    Long created
    Long pid
    Long memResident
    String state
    String status
    Boolean isLiveCheckOk

    List<PortMapping> ports
    Map<String, String> labels
    Long sizeRw
    Long sizeRootFs

    List<Mount> mounts

    boolean checkOk() {
        isLiveCheckOk == null || isLiveCheckOk.booleanValue()
    }

    ContainerInfo simple() {
        def one = new ContainerInfo()
        one.nodeIp = nodeIp
        one.id = id
        one.names = names
        one.image = image
        one.imageId = imageId
        one.command = command
        one.created = created
        one.state = state
        one.status = status
        one.isLiveCheckOk = isLiveCheckOk

        one.name = name()
        one.appId = appId()
        one.instanceIndex = instanceIndex()
        one
    }

    String name
    String appId
    String instanceIndex

    boolean running() {
        STATE_RUNNING == state
    }

    Integer publicPort(Integer privatePort) {
        def one = ports?.find {
            it.privatePort == privatePort
        }
        one ? one.publicPort : privatePort
    }

    private volatile String nameInner

    String name() {
        if (nameInner) {
            return nameInner
        }
        nameInner = names.find { name -> name.contains(ContainerHelper.CONTAINER_NAME_PRE) }
        nameInner
    }

    private volatile Integer appIdInner

    Integer appId() {
        if (appIdInner) {
            return appIdInner
        }
        def name = this.name()
        if (!name || !name.startsWith(ContainerHelper.CONTAINER_NAME_PRE)) {
            return 0
        }
        appIdInner = name.split('_')[1] as int
        appIdInner
    }

    private volatile Integer instanceIndexInner

    Integer instanceIndex() {
        if (instanceIndexInner) {
            return instanceIndexInner
        }
        def name = this.name()
        if (!name || !name.startsWith(ContainerHelper.CONTAINER_NAME_PRE)) {
            return 0
        }
        instanceIndexInner = name.split('_')[-1] as int
        instanceIndexInner
    }
}
