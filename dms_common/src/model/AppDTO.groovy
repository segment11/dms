package model

import groovy.transform.CompileStatic
import model.json.*

@CompileStatic
class AppDTO extends BaseRecord<AppDTO> {
    @CompileStatic
    static enum Status {
        auto(0), manual(1)

        int val

        Status(int val) {
            this.val = val
        }
    }

    @Override
    String toString() {
        "app: " + name + ", id: " + id + ", status:" + Status.values().find { it.val == status }.name()
    }

    Integer id

    Integer clusterId

    Integer namespaceId

    String name

    String des

    AppConf conf

    LiveCheckConf liveCheckConf

    MonitorConf monitorConf

    LogConf logConf

    ABConf abConf

    JobConf jobConf

    GatewayConf gatewayConf

    Integer status

    Date updatedDate

    ExtendParams extendParams

    boolean autoManage() {
        status == Status.auto.val
    }
}