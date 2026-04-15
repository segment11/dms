package script

import org.hyperic.sigar.Sigar
import org.slf4j.LoggerFactory

Sigar sigar = super.binding.getProperty('sigar') as Sigar
Map params = super.binding.getProperty('params') as Map

def log = LoggerFactory.getLogger(this.getClass())

String cmd = params.cmd.toString()
def cmdArgs0 = cmd.contains(' ') ? cmd.split(' ')[0] : cmd
def scriptName = new File(cmdArgs0).getName()

def instanceIndex = null
def appId = null
def configPattern = null

def cmdParts = cmd.split(' ')
if (cmdParts.length >= 3) {
    instanceIndex = cmdParts[1]
    appId = cmdParts[2]
    configPattern = "/opt/bitnami/kafka/config_${appId}/server.properties_${instanceIndex}"
}

try {
    for (pid in sigar.getProcList()) {
        try {
            def procArgs = sigar.getProcArgs(pid)
            if (procArgs.length > 0) {
                def procArgsStr = procArgs.join(' ')

                if (procArgsStr.contains('kafka.Kafka') && procArgsStr.contains(configPattern)) {
                    return [pid: pid]
                }

                if (procArgsStr.contains(cmdArgs0) || procArgsStr.contains(scriptName)) {
                    return [pid: pid]
                }
            }
        } catch (Exception ignored) {
        }
    }

    return [isError: true, message: 'pid not found']
} catch (Exception ex) {
    log.error('get pid error', ex)
    return [isError: true, message: ex.message]
}
