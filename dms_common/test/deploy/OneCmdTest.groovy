package deploy

import spock.lang.Specification

class OneCmdTest extends Specification {
    def 'test base'() {
        given:
        def one = new OneCmd()
        one.cmd = 'echo hello'

        expect:
        !one.ok()
        one.toString().contains('echo hello')

        when:
        one.status = OneCmd.STATUS_OK
        one.showCmdLog = false
        then:
        one.ok()
        one.toString().contains('***')

        when:
        one.clear()
        then:
        !one.ok()

        when:
        def cmd1 = OneCmd.simple('echo hello')
        then:
        cmd1.cmd == 'echo hello'
    }

    def 'test exe'() {
        given:
        def one = new OneCmd()
        one.cmd = 'echo hello'
        one.checker = new OneCmd.ContainsChecker('hello')

        and:
        def os = new ByteArrayOutputStream()
        def is = new ByteArrayInputStream('hello'.getBytes())

        when:
        one.execInShell(os, is)
        then:
        one.ok()

        when:
        is = new ByteArrayInputStream('hello'.getBytes())
        one.showCmdLog = false
        one.execInShell(os, is)
        then:
        one.ok()

        when:
        boolean exception = false
        one.maxWaitTimes = 0
        try {

            one.execInShell(os, is)
        } catch (DeployException e) {
            println e.message
            exception = true
        }
        then:
        exception

        when:
        one.cmd = null
        one.execInShell(os, is)
        then:
        1 == 1

        when:
        one.lastOneCmd = new OneCmd(endMatchKeyword: 'fail')
        one.dependOnEndMatchKeyword = { String lastCmdEndMatchKeyword ->
            'success' == lastCmdEndMatchKeyword ? 'echo next' : null
        }
        one.execInShell(os, is)
        then:
        1 == 1

        when:
        one.lastOneCmd = null
        one.execInShell(os, is)
        then:
        1 == 1
    }

    def 'test checker'() {
        given:
        def c0 = new OneCmd.ContainsChecker()
        def checker = OneCmd.keyword('hello', 'hi')
        def anyChecker = OneCmd.any()
        checker.failKeyword('error', 'fail')

        expect:
        anyChecker.isEnd('test')
        anyChecker.ok()

        !checker.isEnd(null)
        checker.isEnd('hello')
        checker.ok()
        checker.reset()
        checker.isEnd('hi')
        checker.ok()
        checker.reset()
        !checker.isEnd('world')
        !checker.ok()

        checker.reset()
        checker.isEnd('error')
        !checker.ok()
        checker.isEnd('fail')
        !checker.ok()
    }
}
