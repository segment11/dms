package model.job

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.BaseRecord

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RmBackupLogDTO extends BaseRecord<RmBackupLogDTO> {
    @CompileStatic
    enum Status {
        created, failed, done
    }

    Integer id

    String name

    Integer serviceId

    Integer shardIndex

    Integer replicaIndex

    Integer backupTemplateId

    Integer costMs

    Status status

    String message

    // redis save time
    Date saveDate

    Date createdDate

    Date updatedDate
}
