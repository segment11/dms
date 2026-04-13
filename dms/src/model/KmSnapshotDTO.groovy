package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmSnapshotDTO extends BaseRecord<KmSnapshotDTO> {

    @CompileStatic
    static enum Status { created, failed, done }

    Integer id

    String name

    Integer serviceId

    String snapshotDir

    Status status

    String message

    Integer costMs

    Date createdDate

    Date updatedDate
}
