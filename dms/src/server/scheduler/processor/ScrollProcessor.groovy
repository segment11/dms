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
        def nodeIpList = chooseNodeIpList(app, containerList)

        def conf = app.conf
        nodeIpList.eachWithIndex { String targetNodeIp, int instanceIndex ->
            def confCopy = conf.copy()
            def abConf = app.abConf
            if (abConf) {
                if (instanceIndex < abConf.containerNumber) {
                    confCopy.image = abConf.image
                }
            }
            def nodeIpListCopy = new ArrayList<String>(nodeIpList)

            def x = containerList.find { x -> x.instanceIndex() == instanceIndex }
            if (x) {
                def keeper = stopOneContainer(job.id, app, x)
                startOneContainer(app, job.id, instanceIndex, nodeIpListCopy, targetNodeIp, confCopy, keeper)
            } else {
                startOneContainer(app, job.id, instanceIndex, nodeIpListCopy, targetNodeIp, confCopy, null)
            }
        }
    }
}
