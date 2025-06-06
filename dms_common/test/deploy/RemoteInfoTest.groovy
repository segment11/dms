package deploy

import model.NodeKeyPairDTO
import spock.lang.Specification

class RemoteInfoTest extends Specification {
    def 'test all'() {
        given:
        def kp = new NodeKeyPairDTO(ip: '192.168.1.10', sshPort: 22, userName: 'root', pass: '123456')
        def remoteInfo = RemoteInfo.fromKeyPair(kp)
        expect:
        remoteInfo.host == '192.168.1.10'

        when:
        kp.pass = ''
        remoteInfo = RemoteInfo.fromKeyPair(kp)
        then:
        !remoteInfo.isUsePass

        when:
        kp.pass = null
        remoteInfo = RemoteInfo.fromKeyPair(kp)
        then:
        !remoteInfo.isUsePass
    }
}
