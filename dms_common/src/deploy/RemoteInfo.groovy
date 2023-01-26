package deploy

import groovy.transform.CompileStatic
import model.NodeKeyPairDTO

@CompileStatic
class RemoteInfo {
    String host
    int port
    String user
    String password
    String rootPass

    boolean isUsePass = true
    String privateKeyContent
    String privateKeySuffix = '.rsa'

    static RemoteInfo fromKeyPair(NodeKeyPairDTO kp) {
        def remoteInfo = new RemoteInfo()
        remoteInfo.host = kp.ip
        remoteInfo.port = kp.sshPort
        remoteInfo.user = kp.user
        remoteInfo.password = kp.pass
        remoteInfo.rootPass = kp.rootPass
        // null or '' -> use private key
        remoteInfo.isUsePass = !!kp.pass
        remoteInfo.privateKeyContent = kp.keyPrivate
        remoteInfo.privateKeySuffix = '.' + kp.keyType
        remoteInfo
    }
}
