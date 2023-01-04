package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ScriptPullContent

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class AgentScriptPullLogDTO extends BaseRecord<AgentScriptPullLogDTO> {
    Integer id

    String agentHost

    ScriptPullContent content

    Date createdDate
}