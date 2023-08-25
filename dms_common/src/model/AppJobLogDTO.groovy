package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class AppJobLogDTO extends BaseRecord<AppJobLogDTO> {

    Integer id

    Integer jobId

    Integer instanceIndex

    String title

    String message

    Boolean isOk

    Integer costMs

    Date createdDate
}