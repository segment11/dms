package transfer


import common.Utils
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class NodeInfo {
    String nodeIp
    int clusterId
    boolean isLiveCheckOk
    String version
    Date time
    Date hbTime

    // server side
    boolean isOk

    void checkIfOk(Date d) {
        def dat = Utils.getNodeAliveCheckLastDate(3)
        isOk = d > dat
    }

    double[] loadAverage
    List<FileUsage> fileUsageList = []
    List<CpuPerc> cpuPercList = []
    Mem mem

    int cpuNumber() {
        cpuPercList.size()
    }

    double cpuUsedPercent() {
        double sum = 0
        for (it in cpuPercList) {
            sum += (it.sys + it.user)
        }
        double r = sum / cpuPercList.size()
        r.round(4)
    }

    Map toMap() {
        [nodeIp        : nodeIp,
         loadAverage   : loadAverage,
         fileUsageList : fileUsageList,
         cpuPercList   : cpuPercList,
         time          : time,
         hbTime        : hbTime,
         cpuUsedPercent: cpuUsedPercent()]
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class CpuPerc {
        double user
        double sys
        double idle

        double usedPercent() {
            def sum = user + sys + idle
            if (sum == 0) {
                return 0
            }
            ((user + sys) / sum).round(4)
        }
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class Mem {
        double total
        double free
        double actualFree
        double actualUsed
        double used
        double usedPercent
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class FileUsage {
        String dirName
        double total
        double free
        double usePercent
    }
}