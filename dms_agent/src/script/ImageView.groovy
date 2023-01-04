package script

import com.github.dockerjava.api.DockerClient

DockerClient docker = super.binding.getProperty('docker') as DockerClient

def list = docker.listImagesCmd().exec()
[list: list]
