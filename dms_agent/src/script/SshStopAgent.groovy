package script

import agent.DeployInit
import deploy.DeploySupport
import deploy.InitAgentEnvSupport
import model.NodeKeyPairDTO

Map params = super.binding.getProperty('params') as Map

String ip = params.ip as String
int port = params.port as int
String user = params.user as String
String rootPass = params.rootPass as String
String keyPrivate = params.keyPrivate as String

def kp = new NodeKeyPairDTO()
kp.ip = ip
kp.sshPort = port
kp.userName = user
kp.rootPass = rootPass
kp.keyPrivate = keyPrivate

DeploySupport.instance.isAgent = true
DeployInit.initDeployEventCallback()

def support = new InitAgentEnvSupport(kp)
def flag = support.stopAgent()
[flag: flag, steps: support.getSteps()]
