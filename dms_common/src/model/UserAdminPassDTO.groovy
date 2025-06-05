package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class UserAdminPassDTO extends BaseRecord<UserAdminPassDTO> {
    Integer id

    String passwordMd5

    Date updatedDate
}