package script

import org.hyperic.sigar.Sigar
import org.slf4j.LoggerFactory

Sigar sigar = super.binding.getProperty('sigar') as Sigar
Map params = super.binding.getProperty('params') as Map

def log = LoggerFactory.getLogger(this.getClass())

String cmd = params.cmd.toString()
try {
    for (pid in sigar.getProcList()) {
        def procArgs = sigar.getProcArgs(pid)
        if (procArgs.length > 0 && cmd.startsWith(procArgs[0])) {
            return [pid: pid]
        }
    }

    return [isError: true, message: 'pid not found']
} catch (Exception ex) {
    log.error('get pid error', ex)
    return [isError: true, message: ex.message]
}
