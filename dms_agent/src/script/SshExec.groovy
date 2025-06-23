package script

import agent.DeployInit
import agent.support.OneCmdListType
import deploy.DeploySupport
import deploy.OneCmd
import model.NodeKeyPairDTO
import support.ToJson

Map params = super.binding.getProperty('params') as Map
String ip = params.ip as String
int port = params.port as int
String user = params.user as String
String pass = params.pass as String
String rootPass = params.rootPass as String

String keyPrivate = params.keyPrivate as String
String command = params.command as String
String cmdListJson = params.cmdListJson as String

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

if (cmdListJson) {
    List<OneCmd> cmdList = ToJson.read(cmdListJson, new OneCmdListType())
    DeploySupport.instance.exec(kp, cmdList, cmdList.size() * 10, true)

    [flag: cmdList.every { it.ok() }, cmdList: cmdList]
} else {
    def oneCmd = OneCmd.simple(command)
    DeploySupport.instance.exec(kp, oneCmd)

    [flag: oneCmd.ok(), status: oneCmd.status, result: oneCmd.result]
}


