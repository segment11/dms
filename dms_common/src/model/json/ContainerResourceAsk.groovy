package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class ContainerResourceAsk implements Comparable<ContainerResourceAsk> {
    ContainerResourceAsk() {}

    ContainerResourceAsk(AppConf conf) {
        memMB = conf.memMB
        cpuShare = conf.cpuShare
        cpuFixed = conf.cpuFixed
    }

    String nodeIp

    int memMB = 1024

    int cpuShare = 1024

    double cpuFixed = 1.0

    int getWeight() {
        memMB * 5 + cpuShare * 2 + ((cpuFixed * 48 * 20) as int)
    }

    @Override
    int compareTo(ContainerResourceAsk o) {
        weight - o.weight
    }
}
