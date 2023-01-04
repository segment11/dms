package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class NodeVolumeDTO extends BaseRecord<NodeVolumeDTO> {

    Integer id

    Integer clusterId

    String imageName

    String name

    String des

    String dir

    Date updatedDate
}