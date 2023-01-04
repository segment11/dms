package server.scheduler.processor

import groovy.transform.CompileStatic
import model.json.GatewayConf
import transfer.ContainerConfigInfo

@CompileStatic
class ContainerRunResult {

    JobStepKeeper keeper

    String nodeIp

    int port

    ContainerConfigInfo containerConfig

    void extract(GatewayConf gatewayConf) {
        port = containerConfig.publicPort(gatewayConf.containerPrivatePort)
    }

}
