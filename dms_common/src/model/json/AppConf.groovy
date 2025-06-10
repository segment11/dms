package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.apache.commons.beanutils.BeanUtils
import org.segment.d.BeanReflector
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class AppConf implements JSONFiled {
    int registryId

    String group

    String image

    String imageName() {
        group + '/' + image
    }

    String tag

    String cmd

    String user

    int memMB = 0

    int memReservationMB = 256

    int cpuShares = 1024

    String cpusetCpus

    double cpuFixed = 1.0

    int containerNumber = 1

    boolean isParallel = false

    /**
     * set it true
     * if u want run as not a docker container, just a process in host
     */
    boolean isRunningUnbox = false

    List<Integer> deployFileIdList = []

    List<Integer> dependAppIdList = []

    /**
     * set it true
     * when there are one or two nodes in cluster
     * run some applications need more than nodes' number, like zookeeper
     */
    boolean isLimitNode = false

    List<String> targetNodeTagList = []

    List<String> targetNodeTagListByInstanceIndex = []

    List<String> targetNodeIpList = []

    List<String> excludeNodeTagList = []

    boolean isPrivileged = false

    String pidMode

    List<KVPair<String>> envList = []

    List<ULimit> uLimitList = []

    List<DirVolumeMount> dirVolumeList = []

    List<FileVolumeMount> fileVolumeList = []

    String networkMode

    boolean isNetworkDnsUsingCluster = false

    List<PortMapping> portList = []

    AppConf copy() {
        def r = new AppConf()
        BeanUtils.copyProperties(r, this)
        r
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof AppConf)) {
            return false
        }
        def fields = BeanReflector.getClassFields(AppConf).collect { it.name }
        // need not scroll if change these
        fields.remove('containerNumber')
        fields.remove('dependAppIdList')
        def that = this
        def other = (AppConf) obj
        fields.every {
            that.getProperty(it) == other.getProperty(it)
        }
    }
}
