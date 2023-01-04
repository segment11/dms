package script

import com.github.dockerjava.api.DockerClient

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String keyword = params.keyword
if (!keyword) {
    return [error: 'keyword required']
}

String k
if (keyword.startsWith('library/')) {
    k = keyword.replace('library/', '')
} else {
    k = keyword
}

def list = docker.listImagesCmd().exec()
[isExists: list && list.find {
    k in it.repoTags
} != null]
