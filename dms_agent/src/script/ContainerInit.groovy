package script

import com.github.dockerjava.api.DockerClient
import common.Utils
import org.slf4j.LoggerFactory

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
    String[] cmd = ['sh', '-c', cmdLine]
    def response = docker.execCreateCmd(id).
            withAttachStdout(true).
            withAttachStderr(true).
            withCmd(cmd).exec()
    def is = docker.execStartCmd(response.id).stdin
    String shellResult = Utils.readFully(is)
    list << shellResult
}

[flag: true, message: list.join("\r\n")]