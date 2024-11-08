package common

import spock.lang.Specification

import java.nio.file.attribute.PosixFilePermission

class UtilsTest extends Specification {
    def 'test base'() {
        expect:
        Utils.getNodeAliveCheckLastDate(3) != null
        !Utils.isPortListenAvailable(53) || Utils.isPortListenAvailable(53)
        Utils.getOnePortListenAvailable() != -1

        Utils.compareIp('127.0.0.1', '127.0.0.1') == 0
        Utils.compareIp('127.0.0.1', '127.0.0.2') == -1
        Utils.compareIp('127.0.0.2', '127.0.0.1') == 1

        when:
        def is = new ByteArrayInputStream('test'.bytes)
        then:
        Utils.readFully(is) == 'test'
    }

    def 'test file permission set'() {
        given:
        def file = new File('test.txt')
        file.createNewFile()

        when:
        Utils.setFilePermission(file, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        then:
        file.canRead()
        file.canWrite()

        when:
        Utils.setFileRead(file)
        then:
        file.canRead()

        when:
        Utils.setFile600(file)
        then:
        file.canRead()

        cleanup:
        file.delete()
    }

    def 'test other'() {
        expect:
        Utils.cpusetCpusToList('0-3,5,7-9,10-11-12') == [0, 1, 2, 3, 5, 7, 8, 9, 10, 11, 12]
    }
}
