package deploy

import spock.lang.Specification

class FilePutProgressMonitorTest extends Specification {
    def 'test base'() {
        given:
        def monitor = new FilePutProgressMonitor(1000)
        monitor.sendProgressMessage(100)

        when:
        monitor.init(0, '/tmp/test.txt', 'test.txt', 1000)
        monitor.count(500)
        monitor.run()
        monitor.count(500)
        monitor.run()
        monitor.run()
        monitor.end()
        monitor.count(500)
        then:
        monitor.transferred == 1000
        monitor.end

        when:
        def monitor2 = new FilePutProgressMonitor(0)
        monitor2.sendProgressMessage(100)
        monitor2.stop()
        then:
        1 == 1
    }
}
