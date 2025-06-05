package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ConfigItems

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class RmConfigTemplateDTO extends BaseRecord<RmConfigTemplateDTO> {
    Integer id

    String name

    String des

    ConfigItems configItems

    Date createdData

    Date updatedDate
}
