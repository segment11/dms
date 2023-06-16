package script

import agent.Agent
import com.github.dockerjava.api.DockerClient
import common.Event
import common.Utils
import model.json.FileVolumeMount
import model.server.CreateContainerConf
import org.apache.commons.io.FileUtils
import org.segment.web.common.CachedGroovyClassLoader
import org.slf4j.LoggerFactory
import support.ToJson

import static common.ContainerHelper.isProcess

DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map

String containerId = params.containerId
String jsonStr = params.jsonStr
if (!containerId || !jsonStr) {
    return [error: 'containerId and jsonStr required']
}

def createConf = ToJson.read(jsonStr, CreateContainerConf)
def conf = createConf.conf

def log = LoggerFactory.getLogger(this.getClass())

conf.fileVolumeList.findAll { it.isReloadInterval }.each { FileVolumeMount one ->
    def content = Agent.instance.post('/dms/api/container/create/tpl',
            [clusterId       : createConf.clusterId,
             appId           : createConf.appId,
             appIdList       : createConf.appIdList,
             nodeIp          : createConf.nodeIp,
             nodeIpList      : createConf.nodeIpList,
             targetNodeIpList: createConf.conf.targetNodeIpList,
             instanceIndex   : createConf.instanceIndex,
             containerNumber : conf.containerNumber,
             imageTplId      : one.imageTplId], String)

    if (one.isParentDirMount || isProcess(containerId)) {
        String fileLocal = one.dist
        // dyn
        String hostFileFinal
        if (fileLocal.contains('${')) {
            hostFileFinal = CachedGroovyClassLoader.instance.eval('"' + fileLocal + '"',
                    [appId: createConf.appId as Object, instanceIndex: createConf.instanceIndex])
        } else {
            hostFileFinal = fileLocal
        }

        def localFile = new File(hostFileFinal)
        if (localFile.exists() && localFile.text == content) {
            // skip
            log.debug 'skip file volume reload  - ' + hostFileFinal
        } else {
            FileUtils.forceMkdirParent(localFile)
            localFile.text = content

            Agent.instance.addEvent Event.builder().type(Event.Type.node).reason('file volume reload').result('app ' + createConf.appId).
                    build().log(content)
        }
    } else {
        def response = docker.inspectContainerCmd(containerId).exec()
        def mount = response.mounts.find { mount -> mount.destination.path == one.dist }
        if (mount) {
            String fileLocal = mount.source

            def localFile = new File(fileLocal)
            if (localFile.exists() && localFile.text == content) {
                // skip
                log.debug 'skip file volume reload  - ' + fileLocal
            } else {
                FileUtils.forceMkdirParent(localFile)
                localFile.text = content
                Utils.setFileRead(localFile)

                Agent.instance.addEvent Event.builder().type(Event.Type.node).reason('file volume reload').result('app ' + createConf.appId).
                        build().log(content)
            }
        } else {
            log.warn 'mount not found destination: {}', one.dist
        }
    }
}

[flag: true]