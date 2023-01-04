package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.*
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.MySQLDialect

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class AppDTO extends BaseRecord<AppDTO> {
    @CompileStatic
    static enum Status {
        auto(0), manual(1)

        int val

        Status(int val) {
            this.val = val
        }
    }

    Integer id

    Integer clusterId

    Integer namespaceId

    String name

    String des

    AppConf conf

    LiveCheckConf liveCheckConf

    MonitorConf monitorConf

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