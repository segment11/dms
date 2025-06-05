package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RmSentinelServiceDTO extends BaseRecord<RmSentinelServiceDTO> {
    @CompileStatic
    static enum Status {
        creating, running, stopped, deleted
    }

    Integer id

    String name

    Integer replicas

    Integer configTemplateId

    Integer appId

    String status

    Date createdData

    Date updatedDate
}
