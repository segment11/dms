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

    // internet dms server http server host port
    String internetHostPort
    String sameVpcNodeIpPrefix

    List<ProxyInfo> proxyInfoList = []

    ProxyInfo getProxyInfo(String nodeIp) {
        proxyInfoList.find { nodeIp.startsWith(it.matchNodeIpPrefix) }
    }

    @CompileStatic
    static class ProxyInfo {
        // internet dms agent http server host
        String proxyNodeIp
        Integer proxyNodePort
        String matchNodeIpPrefix
    }
}
