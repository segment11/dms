package model.job

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.BaseRecord

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmTaskLogDTO extends BaseRecord<KmTaskLogDTO> {
    Integer id

    Integer jobId

    String step

    String jobResult

    Integer costMs

    Date createdDate

    Date updatedDate
}
