package common

import spock.lang.Specification

class AgentConfTest extends Specification {
    def 'test all'() {
        given:
        def agentConf = new AgentConf()
        agentConf.serverHost = 'localhost'
        agentConf.serverPort = 5010
        agentConf.clusterId = 1
        agentConf.secret = '1'

        expect:
        agentConf.serverHost == 'localhost'
        agentConf.serverPort == 5010
        agentConf.clusterId == 1
        agentConf.secret == '1'

        when:
        def result = agentConf.generate()
        then:
        result.contains('serverHost=localhost')
    }
}
