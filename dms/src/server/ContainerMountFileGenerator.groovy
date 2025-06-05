package server

import auth.User
import groovy.transform.CompileStatic
import model.server.ContainerMountTplHelper
import transfer.ContainerInfo

@CompileStatic
class ContainerMountFileGenerator {

    static ContainerMountTplHelper prepare(User u, int clusterId) {
        def instance = InMemoryAllContainerManager.instance
        List<ContainerInfo> containerList = instance.getContainerList(clusterId, 0, null, u)
        Map<Integer, List<ContainerInfo>> groupByApp = containerList.groupBy { x ->
            x.appId()
        }
        def helper = new ContainerMountTplHelper(groupByApp)
        groupByApp.each { k, v ->
            def oneApp = new ContainerMountTplHelper.OneApp(k, v)
            oneApp.app = InMemoryCacheSupport.instance.oneApp(k)
            helper.list << oneApp
        }
        helper
    }
}
