package script

import com.github.dockerjava.api.DockerClient

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String name = params.name
if (!name) {
    return [error: 'name required']
}

def alreadyExistsList = docker.listContainersCmd().
        withShowAll(true).
        withNameFilter([name]).exec()
boolean flag = alreadyExistsList != null && alreadyExistsList.size() > 0
[flag: flag]