package server.scheduler.processor

import groovy.transform.CompileStatic
import model.AppDTO
import model.AppJobDTO
import transfer.ContainerInfo

@CompileStatic
interface GuardianProcessor {
    void process(AppJobDTO job, AppDTO app, List<ContainerInfo> containerList)
}
