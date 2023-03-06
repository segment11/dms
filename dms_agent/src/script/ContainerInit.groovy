package script

import agent.support.ContainerLogViewCallback
import com.github.dockerjava.api.DockerClient
import common.Utils
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

import static common.ContainerHelper.isProcess

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String id = params.id
String initCmd = params.initCmd
if (!id || !initCmd) {
    return [error: 'id/initCmd required']
}

def log = LoggerFactory.getLogger('dyn')

List<String> list = []
for (cmdLine in initCmd.readLines().findAll { it.trim() }) {
    log.info 'ready to exec cmd - ' + cmdLine

    if (isProcess(id)) {
        def process = Runtime.getRuntime().exec(cmdLine)
        def isOk = process.waitFor(5, TimeUnit.SECONDS)
        log.info 'host shell execute is ok: {}', isOk
        def is = process.inputStream
        String shellResult = Utils.readFully(is)
        list << shellResult
    } else {
        String[] cmd = ['sh', '-c', cmdLine]
        def response = docker.execCreateCmd(id).
                withAttachStdout(true).
                withAttachStderr(true).
                withCmd(cmd).exec()

        def callBack = new ContainerLogViewCallback()
        docker.execStartCmd(response.id).exec(callBack)
        def isOk = callBack.awaitCompletion(10, TimeUnit.SECONDS)
        if (!isOk) {
            log.error 'exec cmd timeout: {}', cmdLine
            return [flag: false, message: 'exec cmd timeout: ' + cmdLine]
        }
        list << callBack.os.toString()
    }
}

[flag: true, message: list.join("\r\n")]