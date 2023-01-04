package model.server

import common.ContainerHelper
import groovy.transform.CompileStatic
import model.AppDTO
import transfer.ContainerInfo

@CompileStatic
class ContainerMountTplHelper {
    @CompileStatic
    static class OneApp {
        Integer appId

        List<ContainerInfo> containerList

        boolean running() {
            containerList && containerList.any { it.running() }
        }

        OneApp(Integer appId, List<ContainerInfo> containerList) {
            this.appId = appId
            this.containerList = containerList
        }

        AppDTO app

        int getPublicPortByPrivatePort(int privatePort) {
            containerList[0].publicPort(privatePort)
        }

        List<String> getAllNodeIpList() {
            def targetNodeIpList = app.conf.targetNodeIpList
            if (targetNodeIpList) {
                return targetNodeIpList
            }

            containerList.collect { x -> x.nodeIp }
        }

        List<String> getAllHostnameList() {
            def containerNumber = app.conf.containerNumber
            (0..<containerNumber).collect {
                ContainerHelper.generateContainerHostname(appId, it)
            }
        }
    }

    private Map<Integer, List<ContainerInfo>> groupByApp

    List<OneApp> list = []

    ContainerMountTplHelper(Map<Integer, List<ContainerInfo>> groupByApp) {
        this.groupByApp = groupByApp
    }

    OneApp app(String name) {
        list.find { it.app?.name == name }
    }
}
