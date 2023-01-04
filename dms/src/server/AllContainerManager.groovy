package server

import auth.User
import groovy.transform.CompileStatic
import model.NodeDTO
import transfer.ContainerInfo
import transfer.NodeInfo

@CompileStatic
interface AllContainerManager {
    void addAuthToken(String nodeIp, String authToken)

    String getAuthToken(String nodeIp)

    void addNodeInfo(String nodeIp, NodeInfo node)

    Date getHeartBeatDate(String nodeIp)

    List<NodeDTO> getHeartBeatOkNodeList(int clusterId)

    NodeInfo getNodeInfo(String nodeIp)

    Map<String, NodeInfo> getAllNodeInfo(Integer clusterId)

    void addContainers(Integer clusterId, String nodeIp, List<ContainerInfo> containers)

    List<ContainerInfo> getContainerListByNodeIp(String nodeIp)

    String getNodeIpByContainerId(String containerId)

    Integer getAppIpByContainerId(String containerId)

    List<ContainerInfo> getContainerList(int clusterId, int appId, String nodeIp, User user)
}