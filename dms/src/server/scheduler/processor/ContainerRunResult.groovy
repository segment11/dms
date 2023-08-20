package server.scheduler.processor

import groovy.transform.CompileStatic
import transfer.ContainerConfigInfo

@CompileStatic
class ContainerRunResult {

    JobStepKeeper keeper

    String nodeIp

    int port

    ContainerConfigInfo containerConfig
}
