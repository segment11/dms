package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class NamespaceDTO extends BaseRecord<NamespaceDTO> {

    Integer id

    Integer clusterId

    String name

    String des

    Date updatedDate

    static int createIfNotExist(int clusterId, String name) {
        def ns = new NamespaceDTO(clusterId: clusterId, name: name).one()
        if (ns) {
            return ns.id
        }

        new NamespaceDTO(clusterId: clusterId, name: name).add()
    }
}