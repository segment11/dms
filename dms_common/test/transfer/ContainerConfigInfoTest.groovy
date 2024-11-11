package transfer

import spock.lang.Specification

class ContainerConfigInfoTest extends Specification {
    def 'test base'() {
        given:
        def c = new ContainerConfigInfo()

        when:
        c.networkMode = 'host'
        then:
        c.publicPort(80) == 80

        when:
        c.networkMode = 'bridge'
        c.ports = [new ContainerInfo.PortMapping(privatePort: 80, publicPort: 8080)]
        then:
        c.publicPort(80) == 8080
    }
}
