package script

import agent.Agent
import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String id = params.id
if (!id) {
    return [error: 'id required']
}

docker.startContainerCmd(id).exec()
log.info 'start container id {}', id

Agent.instance.sendContainer()
[flag: true]
