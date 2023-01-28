package script

import org.hyperic.sigar.Sigar
import org.slf4j.LoggerFactory

Sigar sigar = super.binding.getProperty('sigar') as Sigar
Map params = super.binding.getProperty('params') as Map

def log = LoggerFactory.getLogger(this.getClass())

String cmd = params.cmd.toString()
// only compare start command
def cmdArgs0 = cmd.contains(' ') ? cmd.split(' ')[0] : cmd

try {
    for (pid in sigar.getProcList()) {
        def procArgs = sigar.getProcArgs(pid)
        if (procArgs.length > 0) {
            def procArgs0 = procArgs[0]
            def x = procArgs0.contains(' ') ? procArgs0.split(' ')[0] : procArgs0
            if (cmdArgs0.startsWith(x)) {
                return [pid: pid]
            }
        }
    }

    return [isError: true, message: 'pid not found']
} catch (Exception ex) {
    log.error('get pid error', ex)
    return [isError: true, message: ex.message]
}
