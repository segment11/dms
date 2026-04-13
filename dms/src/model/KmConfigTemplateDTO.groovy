package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ConfigItems

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmConfigTemplateDTO extends BaseRecord<KmConfigTemplateDTO> {
    Integer id

    String name

    String des

    ConfigItems configItems

    Date updatedDate
}
