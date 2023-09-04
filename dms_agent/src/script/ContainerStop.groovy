package script

import agent.Agent
import com.github.dockerjava.api.DockerClient
import org.hyperic.sigar.Sigar
import org.slf4j.LoggerFactory

import static common.ContainerHelper.*

def log = LoggerFactory.getLogger(this.getClass())

Sigar sigar = super.binding.getProperty('sigar') as Sigar
DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String id = params.id
if (!id) {
    return [error: 'id required']
}

if (isProcess(id)) {
    int pid = getPidFromProcess(id)

    try {
        /*
HUP     1    终端断线
INT       2    中断（同 Ctrl + C）
QUIT    3    退出（同 Ctrl + \）
TERM    15    终止
KILL      9    强制终止
CONT   18    继续（与STOP相反， fg/bg命令）
STOP    19    暂停（同 Ctrl + Z）
         */
        sigar.kill(pid as long, 15)
        log.info 'stop TERM - ' + pid
    } catch (Exception e) {
        // ignore
        log.error 'error stop TERM - ' + pid, e
    }

    if (params.isRemoveAfterStop) {
        int appId = getAppIdFromProcess(id)
        def wrapJsonFile = new File('/opt/dms/app_' + appId + '/container-info.json')
        if (wrapJsonFile.exists()) {
            wrapJsonFile.delete()
        }
    }
} else {
    int secondsToWaitBeforeKilling = params.secondsToWaitBeforeKilling ?
            params.secondsToWaitBeforeKilling as int : 10
    docker.stopContainerCmd(id).withTimeout(Math.max(secondsToWaitBeforeKilling, 30)).exec()
    if (params.isRemoveAfterStop) {
        docker.removeContainerCmd(id).exec()
    }
}

Thread.sleep(1000)
Agent.instance.sendContainer()
[flag: true]
