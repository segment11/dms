package common

import spock.lang.Specification

class TimerSupportTest extends Specification {
    def 'test all'() {
        given:
        TimerSupport.startUriHandle('test')
        TimerSupport.startUriHandle('test')
        when:
        TimerSupport.stopUriHandle()
        TimerSupport.stopUriHandle()
        then:
        true
    }
}
