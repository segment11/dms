package server.scheduler.processor

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class ScrollProcessor extends CreateProcessor {
    @Override
    void process(AppJobDTO job, AppDTO app, List<ContainerInfo> containerList) {
        List<String> nodeIpList = containerList.collect { x ->
            x.nodeIp
        }

        def conf = app.conf
        List<String> targetNodeIpList = conf.targetNodeIpList ?: nodeIpList
        if (conf.targetNodeTagList || conf.excludeNodeTagList) {
            def nodeIpListAfterFilter = filterNodeList(app.clusterId,
                    conf.excludeNodeTagList,
                    null,
                    conf.targetNodeTagList,
                    conf.isRunningUnbox)
                    .collect { it.ip }
            targetNodeIpList = targetNodeIpList.findAll {
                it in nodeIpListAfterFilter
            }
        }
        log.info 'target node ip list - {}', targetNodeIpList

        // run more than one container instance in the same target node
        // eg. given: ip1, ip2, ip3 three target node, require run 5 instances, get ip1, ip2, ip3, ip1, ip2
        if (targetNodeIpList.size() < nodeIpList.size()) {
            int needAddInstanceNum = nodeIpList.size() - targetNodeIpList.size()
            if (conf.targetNodeIpList) {
                needAddInstanceNum.times { i ->
                    targetNodeIpList << conf.targetNodeIpList[i]
                }
            } else {
                def otherNodeIpList = chooseNodeList(app.clusterId, app.id, needAddInstanceNum, conf, targetNodeIpList)
                targetNodeIpList += otherNodeIpList
            }
        }
        log.info 'choose node - {} before - {}', targetNodeIpList, nodeIpList

        nodeIpList.eachWithIndex { String nodeIp, int i ->
            def x = containerList[i]
            def instanceIndex = x.instanceIndex()
            if (instanceIndex != i) {
                log.warn 'instance index not match - ' + i + ' - ' + instanceIndex
            }

            def keeper = stopOneContainer(job.id, app, x)
            // start new container
            if (targetNodeIpList.size() <= i) {
                return
            }

            def confCopy = conf.copy()
            def abConf = app.abConf
            if (abConf) {
                if (instanceIndex < abConf.containerNumber) {
                    confCopy.image = abConf.image
                }
            }

            def newNodeIp = targetNodeIpList[i]
            startOneContainer(app, job.id, instanceIndex, targetNodeIpList, newNodeIp, confCopy, keeper)
        }
    }
}
