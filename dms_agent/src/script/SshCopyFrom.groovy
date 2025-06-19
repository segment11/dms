package script

import agent.DeployInit
import deploy.DeploySupport
import model.NodeKeyPairDTO
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())

Map params = super.binding.getProperty('params') as Map

String ip = params.ip as String
int port = params.port as int
String user = params.user as String
String pass = params.pass as String
String rootPass = params.rootPass as String

String keyPrivate = params.keyPrivate as String
String localFilePath = params.localFilePath as String
String remoteFilePath = params.remoteFilePath as String
Long remoteFileSavedMillis = params.remoteFileSavedMillis as Long
Boolean isTarX = Boolean.valueOf(params.isTarX as String)
Boolean isOverwrite = Boolean.valueOf(params.isOverwrite as String)
Boolean isUseNewestFile = Boolean.valueOf(params.isUseNewestFile as String)

log.info 'do ssh copy from {} to {}', remoteFilePath, localFilePath

def f = new File(localFilePath)
if (f.exists()) {
    if (isUseNewestFile) {
        // check file last modified time
        if (f.lastModified() > remoteFileSavedMillis) {
            log.info 'remote file {} is not the newest, local: {}, remote:{}, skip', remoteFilePath,
                    new Date(f.lastModified()), new Date(remoteFileSavedMillis)
            return [flag: true]
        }
    }
}

def kp = new NodeKeyPairDTO()
kp.ip = ip
kp.sshPort = port
kp.userName = user
kp.rootPass = rootPass
if (pass) {
    kp.pass = pass
} else {
    kp.keyPrivate = keyPrivate
}

DeploySupport.instance.isAgent = true
DeployInit.initDeployEventCallback()

DeploySupport.instance.receive(kp, remoteFilePath, localFilePath)
[flag: true]