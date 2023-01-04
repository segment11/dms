package script

import agent.Agent
import org.slf4j.LoggerFactory
import support.ToJson
import transfer.ContainerInfo

def log = LoggerFactory.getLogger(this.getClass())

Map params = super.binding.getProperty('params') as Map
String id = params.id
int pid = params.pid as int
int appId = params.appId as int
if (!id || !pid || !appId) {
    return [error: 'id/pid/appId required']
}

String pwd = '/opt/dms/app_' + appId
def wrapJsonFile = new File(pwd + '/container-info.json')
if (!wrapJsonFile.exists()) {
    return [error: 'container-info.json file not exists']
}

def containerInfo = ToJson.read(wrapJsonFile.text, ContainerInfo)
def arr = id.split('_')
arr[-1] = pid
containerInfo.id = arr.join('_')
wrapJsonFile.text = ToJson.json(containerInfo)
log.info 'done replace pid'

Agent.instance.sendContainer()
[flag: true]
