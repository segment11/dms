package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false, excludes = ['keyPrivate', 'keyPublic', 'pass', 'rootPass'])
class NodeKeyPairDTO extends BaseRecord<NodeKeyPairDTO> {

    Integer id

    Integer clusterId

    String ip

    Integer sshPort

    String user

    String pass

    // root user is not allowed ssh login by default
    String rootPass

    String keyName

    String keyType

    String keyPrivate

    String keyPublic

    Date updatedDate
}