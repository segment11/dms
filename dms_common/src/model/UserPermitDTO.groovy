package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class UserPermitDTO extends BaseRecord<UserPermitDTO> {

    Integer id

    String user

    String createdUser

    String permitType

    Integer resourceId

    Date updatedDate
}