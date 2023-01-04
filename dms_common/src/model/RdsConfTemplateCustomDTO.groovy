package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.CustomParameters

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RdsConfTemplateCustomDTO extends BaseRecord<RdsConfTemplateCustomDTO> {

    Integer id

    String name

    String dbType

    CustomParameters content

    Date updatedDate
}