package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GlobalEnvConf implements JSONFiled {
    String dnsServer
    Integer dnsTtl
    // use KVPair so the web javascript bind easy
    List<KVPair> skipConflictCheckVolumeDirList = []
    List<KVPair> envList = []

    // vpc endpoint service vip for agent
    String proxyNodeIp
    Integer proxyNodePort

    // vpc endpoint service vip for server
    String serverHost
    Integer serverPort
    String localIpFilterPre
}
