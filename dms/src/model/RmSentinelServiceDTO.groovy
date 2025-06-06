package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ExtendParams

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RmSentinelServiceDTO extends BaseRecord<RmSentinelServiceDTO> {
    @CompileStatic
    static enum Status {
        creating, running, stopped, deleted, unhealthy
    }

    Integer id

    String name

    Integer replicas

    String[] nodeTags

    Integer appId

    Status status

    ExtendParams extendParams

    Date createdDate

    Date updatedDate
}
