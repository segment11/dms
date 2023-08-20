package plugin.callback

import groovy.transform.CompileStatic
import model.AppDTO
import server.scheduler.processor.ContainerRunResult
import server.scheduler.processor.JobStepKeeper
import transfer.ContainerInfo

@CompileStatic
interface Observer {
    void afterContainerRun(AppDTO app, int instanceIndex, ContainerRunResult result)

    void beforeContainerStop(AppDTO app, ContainerInfo x, JobStepKeeper keeper)

    void afterContainerStopped(AppDTO app, ContainerInfo x, boolean flag)

    void refresh(AppDTO app, List<ContainerInfo> runningContainerList)
}