package script

import com.github.dockerjava.api.DockerClient

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String imageId = params.imageId
if (!imageId) {
    return [error: 'imageId required']
}
[list: docker.removeImageCmd(imageId).exec()]