package script

import agent.Agent
import agent.support.PullImageCallback
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.AuthConfig
import com.segment.common.Conf
import model.ImageRegistryDTO

import java.util.concurrent.TimeUnit

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String image = params.image
String registryId = params.registryId
if (!image) {
    return [error: 'image required']
}

def jobId = params.jobId as int
def instanceIndex = params.instanceIndex as int

ImageRegistryDTO hub = Agent.instance.get('/dms/api/image/pull/hub/info',
        [registryId: registryId as Object], ImageRegistryDTO)

AuthConfig authConfig = new AuthConfig().
        withRegistryAddress(hub.trimScheme()).
        withUsername(hub.anon() ? null : hub.loginUser).
        withPassword(hub.anon() ? null : hub.loginPassword)

def cmd = docker.pullImageCmd(image).withAuthConfig(authConfig)

def callback = new PullImageCallback()
callback.trigger = { percent, costMs ->
    Agent.instance.addJobStep(jobId, instanceIndex, 'image pull percent', [percent: percent], costMs as int)
}

cmd.exec(callback)

def timeout = Conf.instance.getInt('agent.imagePullTimeoutSeconds', 120) as long
def flag = callback.awaitCompletion(timeout, TimeUnit.SECONDS)
[isError: !flag]