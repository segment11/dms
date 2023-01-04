package common

import groovy.transform.CompileStatic

@CompileStatic
class ContainerHelper {
    static final String KEY_NODE_IP = 'Node_Ip'
    static final String KEY_NODE_IP_LIST = 'Node_Ip_List'
    static final String KEY_CLUSTER_ID = 'Cluster_Id'
    static final String KEY_APP_ID = 'App_Id'
    static final String KEY_INSTANCE_INDEX = 'Instance_Index'

    static final String CONTAINER_NAME_PRE = '/app_'

    static String generateContainerName(int appId, int instanceIndex) {
        CONTAINER_NAME_PRE + appId + '_' + instanceIndex
    }

    static String generateContainerHostname(int appId, int instanceIndex) {
        // not include first char /
        generateContainerName(appId, instanceIndex)[1..-1]
    }

    static String generateProcessAsContainerId(int appId, int instanceIndex, int pid) {
        "process_app_${appId}_${instanceIndex}_pid_${pid}".toString()
    }

    static boolean isProcess(String containerId) {
        containerId.startsWith('process_')
    }

    static int getPidFromProcess(String containerId) {
        def arr = containerId.split('_')
        arr[-1] as int
    }

    static int getAppIdFromProcess(String containerId) {
        def arr = containerId.split('_')
        arr[-4] as int
    }

    static int getInstanceIdFromProcess(String containerId) {
        def arr = containerId.split('_')
        arr[-3] as int
    }
}
