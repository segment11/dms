package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ExtendParams

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmTopicDTO extends BaseRecord<KmTopicDTO> {

    @CompileStatic
    static enum Status { creating, active, deleting, deleted }

    Integer id

    Integer serviceId

    String name

    Integer partitions

    Integer replicationFactor

    ExtendParams configOverrides

    Status status

    Date createdDate

    Date updatedDate
}
