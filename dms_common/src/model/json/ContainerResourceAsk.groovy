package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class ContainerResourceAsk implements Comparable<ContainerResourceAsk> {
    ContainerResourceAsk() {}

    ContainerResourceAsk(String ip, AppConf conf) {
        nodeIp = ip
        memMB = conf.memMB
        cpuShares = conf.cpuShares
        cpuFixed = conf.cpuFixed
    }

    String nodeIp

    int memMB = 1024

    int cpuShares = 1024

    double cpuFixed = 1.0

    int getWeight() {
        memMB * 5 + cpuShares * 2 + ((cpuFixed * 48 * 20) as int)
    }

    @Override
    int compareTo(ContainerResourceAsk o) {
        weight - o.weight
    }
}
