package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class DeployFileDTO extends BaseRecord<DeployFileDTO> {

    Integer id

    String localPath

    String destPath

    Boolean isOverwrite

    Long fileLen

    String initCmd

    Date updatedDate
}