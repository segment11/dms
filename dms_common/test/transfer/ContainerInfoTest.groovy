package transfer

import model.json.KVPair
import spock.lang.Specification

class ContainerInfoTest extends Specification {
    def 'test compare'() {
        given:
        def x = new ContainerInfo()
        x.names = ['/app_1_1']
        def y = new ContainerInfo()
        y.names = ['/app_1_2']
        def z = new ContainerInfo()
        z.names = ['/app_2_1']

        expect:
        x < y
        x < z
        y < z
    }

    def 'test base'() {
        given:
        def x = new ContainerInfo()
        x.names = ['/app_1_1']

        expect:
        x.name() == '/app_1_1'
        x.appId() == 1
        x.appId() == 1
        x.instanceIndex() == 1
        x.instanceIndex() == 1
        x.checkOk()

        when:
        x.isLiveCheckOk = false
        then:
        !x.checkOk()

        when:
        x.isLiveCheckOk = true
        then:
        x.checkOk()

        when:
        x.state = ContainerInfo.STATE_RUNNING
        then:
        x.running()

        when:
        x.state = ContainerInfo.STATE_EXITED
        then:
        !x.running()

        when:
        def x1 = x.simple()
        then:
        x1.names == x.names

        when:
        x1.names = ['/xxx']
        then:
        x1.appId() == 0
        x1.instanceIndex() == 0

        when:
        x1.envList = [new KVPair(ContainerInfo.ENV_KEY_PUBLIC_PORT + 8080, '9999')]
        then:
        x1.publicPort(8080) == 9999
        x1.publicPort(8088) == 8088

        when:
        x1.envList = []
        x1.ports = [new ContainerInfo.PortMapping(privatePort: 8080, publicPort: 9999)]
        then:
        x1.publicPort(8080) == 9999
        x1.publicPort(8088) == 8088
    }
}
