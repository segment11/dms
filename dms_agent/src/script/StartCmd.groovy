package script

import org.hyperic.sigar.Sigar
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

def log = LoggerFactory.getLogger(this.getClass())

Sigar sigar = super.binding.getProperty('sigar') as Sigar
Map params = super.binding.getProperty('params') as Map

int appId = params.appId as int
String cmd = params.cmd.toString()

log.info '<- ' + cmd

String[] envp = ['appId=' + appId]
String pwd = '/opt/dms/app_' + appId
def command = ["cd ${pwd}".toString(), "nohup ${cmd} > main.log 2>&1 &"].join(' && ')

try {
    def process = Runtime.getRuntime().exec(command, envp)
    def isOk = process.waitFor(5, TimeUnit.SECONDS)
    if (!isOk) {
        log.warn('wait for 5 seconds, ignore')
    }

//    def f = process.getClass().getDeclaredField('handle')
//    f.setAccessible(true)
//    long pid = f.getLong(process)

    for (pid in sigar.getProcList()) {
        def envMap = sigar.getProcEnv(pid)
        if (envMap.containsKey('appId')) {
            def str = envMap.get('appId')
            if (str && str.toString() == appId.toString()) {
                return [pid: pid]
            }
        }
    }

    return [isError: true, message: 'pid not found']
} catch (Exception ex) {
    log.error('start cmd error - ' + cmd, ex)
    return [isError: true, message: ex.message]
}
