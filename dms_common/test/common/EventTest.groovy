package common

import spock.lang.Specification

class EventTest extends Specification {
    def 'test all'() {
        given:
        def event = new Event()
        event.id = 1
        event.type = Event.Type.cluster
        event.reason = 'reason'
        event.result = 'result'
        event.createdDate = new Date()

        event.log('log message')
        event.audit('audit message')

        when:
        def dto = event.toDto()
        then:
        dto.id == 1

        when:
        event.type = null
        dto = event.toDto()
        then:
        dto.type == null

        when:
        event.result = null
        dto = event.toDto()
        then:
        dto.result == null
    }
}
