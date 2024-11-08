package common

import spock.lang.Specification

class ContainerHelperTest extends Specification {
    def 'test all'() {
        expect:
        ContainerHelper.generateContainerName(1, 1) == '/app_1_1'
        ContainerHelper.generateContainerHostname(1, 1) == 'app_1_1'

        ContainerHelper.generateProcessAsContainerId(1, 1, 1) == 'process_app_1_1_pid_1'
        ContainerHelper.isProcess('process_app_1_1_pid_1')
        ContainerHelper.getPidFromProcess('process_app_1_1_pid_1') == 1
        ContainerHelper.getAppIdFromProcess('process_app_1_1_pid_1') == 1
        ContainerHelper.getInstanceIdFromProcess('process_app_1_1_pid_1') == 1
    }
}
