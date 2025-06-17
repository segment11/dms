package script

import agent.DeployInit
import deploy.DeploySupport
import deploy.InitAgentEnvSupport
import model.NodeKeyPairDTO
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())

Map params = super.binding.getProperty('params') as Map

String ip = params.ip as String
int port = params.port as int
String user = params.user as String
String pass = params.pass as String
String rootPass = params.rootPass as String

String keyPrivate = params.keyPrivate as String
String localFileContent = params.localFileContent as String
String localFilePath = params.localFilePath as String
String remoteFilePath = params.remoteFilePath as String
Boolean isTarX = Boolean.valueOf(params.isTarX as String)
Boolean isMkdir = Boolean.valueOf(params.isMkdir as String)
Boolean isOverwrite = Boolean.valueOf(params.isOverwrite as String)

log.info 'do ssh copy from {} to {}', localFilePath, remoteFilePath

if (localFileContent) {
    def f = new File(localFilePath)
    if (!f.exists()) {
        FileUtils.touch(f)
    }
    f.text = localFileContent
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

if (isOverwrite) {
    DeploySupport.instance.send(kp, localFilePath, remoteFilePath)
    [flag: true]
} else {
    def support = new InitAgentEnvSupport(kp)
    def flag = support.copyFileIfNotExists(localFilePath, isTarX.booleanValue(), isMkdir.booleanValue())
    [flag: flag, steps: support.getSteps()]
}
