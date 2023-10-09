package deploy

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.Session
import common.Event
import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class CmdExecutor {
    String host
    Session session

    EventHandler eventHandler
    List<OneCmd> cmdList

    boolean execShell() {
        ChannelShell shell = session.openChannel('shell') as ChannelShell

        def pipeIn = new PipedInputStream()
        def pipeOut = new PipedOutputStream(pipeIn)

        def pipeIn2 = new PipedInputStream()
        def pipeOut2 = new PipedOutputStream(pipeIn2)

        shell.inputStream = pipeIn
        shell.outputStream = pipeOut2

        try {
            shell.connect()
            for (int i = 0; i < cmdList.size(); i++) {
                def oneCmd = cmdList[i]
                def lastOneCmd = i == 0 ? null : cmdList[i - 1]
                oneCmd.lastOneCmd = lastOneCmd
                oneCmd.execInShell(pipeOut, pipeIn2)
                if (!oneCmd.ok()) {
                    return false
                }
            }
            true
        } catch (Exception ee) {
            log.error 'shell exec error - ' + ee.message
            return false
        } finally {
            if (shell != null) {
                shell.disconnect()
            }
        }
    }

    boolean exec() {
        for (one in cmdList) {
            def command = one.cmd
            log.info '<- ' + command
            long beginT = System.currentTimeMillis()
            ChannelExec channel
            try {
                channel = session.openChannel('exec') as ChannelExec
                channel.command = command
                def is = channel.inputStream
                def errIs = channel.errStream
                channel.connect()

                String result
                Integer status
                if (!command.contains('nohup')) {
                    // block
                    result = Utils.readFully(is)
                    Thread.sleep(one.waitMsOnce)
                    status = channel.exitStatus
                    if (status != 0) {
                        result = Utils.readFully(errIs)
                    }
                } else {
                    status = channel.exitStatus
                }

                long costT = System.currentTimeMillis() - beginT
                String message = """remote exec ${host} command ${command} cost ${costT}ms 
status ${status} result ${result}""".toString()
                if (eventHandler) {
                    def event = Event.builder().type(Event.Type.cluster).reason('remote exec').
                            result(host).build().log(message)
                    eventHandler.handle(event)
                } else {
                    log.info message
                }
                one.status = status
                one.result = result

                if (!one.ok()) {
                    return false
                }
            } finally {
                if (channel) {
                    try {
                        channel.inputStream.close()
                    } catch (Exception e) {
                    }
                    try {
                        channel.disconnect()
                    } catch (Exception e) {
                    }
                }
            }
        }
        true
    }
}
