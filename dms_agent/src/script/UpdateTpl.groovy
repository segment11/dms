package script

import agent.Agent
import common.Utils
import model.json.FileVolumeMount
import model.server.CreateContainerConf
import org.apache.commons.io.FileUtils
import support.ToJson

Map params = super.binding.getProperty('params') as Map
String jsonStr = params.jsonStr.toString()

def createConf = ToJson.read(jsonStr, CreateContainerConf)
def conf = createConf.conf

conf.fileVolumeList.eachWithIndex { FileVolumeMount one, int i ->
    def beginT = System.currentTimeMillis()

    def content = Agent.instance.post('/dms/api/container/create/tpl',
            [clusterId           : createConf.clusterId,
             appId               : createConf.appId,
             appIdList           : createConf.appIdList,
             nodeIp              : createConf.nodeIp,
             nodeIpList          : createConf.nodeIpList,
             targetNodeIpList    : createConf.conf.targetNodeIpList,
             instanceIndex       : createConf.instanceIndex,
             containerNumber     : conf.containerNumber,
             imageTplId          : one.imageTplId], String)


    def localFile = new File(one.dist)
    FileUtils.forceMkdirParent(localFile)
    localFile.text = content
    Utils.setFileRead(localFile)

    def costT = System.currentTimeMillis() - beginT
    Agent.instance.addJobStep(createConf.jobId, createConf.instanceIndex,
            'update tpl', [hostFile: one.dist, fileContent: content], costT.intValue())
}

[flag: true]