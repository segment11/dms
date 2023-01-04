package script

import org.apache.commons.io.FileUtils
import support.ToJson
import transfer.ContainerInfo

Map params = super.binding.getProperty('params') as Map

def containerInfoJsonObject = params.containerInfo
ContainerInfo containerInfo = ToJson.read(ToJson.json(containerInfoJsonObject), ContainerInfo)

int appId = containerInfo.appId()

def tmpDir = '/opt/dms/app_' + appId
def dir = new File(tmpDir)

if (!dir.exists()) {
    FileUtils.forceMkdir(dir)
}

new File(dir, 'container-info.json').text = ToJson.json(containerInfo)

[flag: true]