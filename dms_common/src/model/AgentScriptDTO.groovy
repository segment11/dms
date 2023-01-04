package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class AgentScriptDTO extends BaseRecord<AgentScriptDTO> {

    Integer id

    String name

    String des

    String content

    Date updatedDate
}