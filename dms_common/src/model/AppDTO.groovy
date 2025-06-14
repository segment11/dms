package model

import groovy.transform.CompileStatic
import model.json.*

@CompileStatic
class AppDTO extends BaseRecord<AppDTO> {
    @CompileStatic
    static enum Status {
        auto, manual
    }

    @Override
    String toString() {
        "app: " + name + ", id: " + id + ", status:" + status
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

    Status status

    Date updatedDate

    ExtendParams extendParams

    boolean autoManage() {
        status == Status.auto
    }
}