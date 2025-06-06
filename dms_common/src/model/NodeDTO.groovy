package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class NodeDTO extends BaseRecord<NodeDTO> {

    Integer id

    Integer clusterId

    String ip

    String[] tags

    String agentVersion

    Date updatedDate
}