package model.job

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.BaseRecord

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RmJobDTO extends BaseRecord<RmJobDTO> {
    Integer id

    Integer busiId

    String type

    String status

    String result

    Integer costMs

    String content

    Integer failedNum

    Date createdDate

    Date updatedDate
}
