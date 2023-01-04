package script

import agent.Agent
import com.github.dockerjava.api.DockerClient

import static common.ContainerHelper.getAppIdFromProcess
import static common.ContainerHelper.isProcess

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String id = params.id
if (!id) {
    return [error: 'id required']
}

if (isProcess(id)) {
    int appId = getAppIdFromProcess(id)
    def wrapJsonFile = new File('/opt/dms/app_' + appId + '/container-info.json')
    if (wrapJsonFile.exists()) {
        wrapJsonFile.delete()
    }
} else {
    docker.removeContainerCmd(id).exec()
}

Agent.instance.sendContainer()
[flag: true]
