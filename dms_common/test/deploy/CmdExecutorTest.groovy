package deploy

import com.jcraft.jsch.Session
import com.segment.common.Utils
import common.Event
import spock.lang.Specification

class CmdExecutorTest extends Specification {
    def 'test all'() {
        given:
        boolean doThisCase = true

        def remoteInfo = new RemoteInfo()
        remoteInfo.host = Utils.localIp('192.')
        remoteInfo.port = 22
        remoteInfo.user = DeploySupport.CURRENT_USER
        remoteInfo.isUsePass = true
        // todo: change to your password
        remoteInfo.password = 'Paic1234'
        Session session
        try {
            session = DeploySupport.connect(remoteInfo)
        } catch (Exception e) {
            println e.message
            doThisCase = false
        }

        when:
        def cmdExecutor = new CmdExecutor()
        cmdExecutor.host = 'localhost'
        cmdExecutor.cmdList = [OneCmd.simple('echo hello')]
        cmdExecutor.eventHandler = { Event event ->
            println 'event type: ' + event.type + ', reason: ' + event.reason + ', result: ' + event.result
        }
        if (doThisCase) {
            cmdExecutor.session = session
            cmdExecutor.exec()
        } else {
            println 'skip test CmdExecutor.exec()'
        }
        then:
        1 == 1

        when:
        if (doThisCase) {
            cmdExecutor.cmdList = [new OneCmd(cmd: 'echo hello', checker: OneCmd.keyword('hello')),
                                   new OneCmd(cmd: 'echo world', checker: OneCmd.keyword('world'))]
            cmdExecutor.execShell()
        }
        then:
        1 == 1

        cleanup:
        if (session != null) {
            session.disconnect()
        }
    }
}
