package server.dns.impl

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.Unpooled
import io.netty.handler.codec.dns.DefaultDnsRawRecord
import io.netty.handler.codec.dns.DnsRecord
import io.netty.handler.codec.dns.DnsRecordType
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.dns.DmsDnsAnswerHandler

@CompileStatic
@Slf4j
class ClusterDnsAnswerHandler implements DmsDnsAnswerHandler {
    static final String GW_PREFIX = 'gw_'
    static final String APP_PREFIX = 'app_'
    static final String CLUSTER_PREFIX = 'cluster_'
    static final String ROUTER_PREFIX = 'router_'
    static final int DEFAULT_CLUSTER_ID = 1

    static final byte[] LOCALHOST_BYTES = [127, 0, 0, 1]

    List<DnsRecord> answerGw(String domain, int ttl) {
        // gw_cluster_id.app_id.router_id.local eg. gw_cluster_1.app_1.router_*.local

        def arr = domain.split(/\./)
        if (arr.length != 3 && arr.length != 4) {
            return null
        }

        if (arr.length == 4) {
            def domainSuffix = Conf.instance.getString('dns.domain.suffix', '.local')
            if (!domain.endsWith(domainSuffix)) {
                return null
            }
        }

        def inner1 = arr[0][GW_PREFIX.length()..-1]
        def inner2 = arr[1]
        def inner3 = arr[2]

        def clusterId = DEFAULT_CLUSTER_ID
        if (inner1.startsWith(CLUSTER_PREFIX)) {
            clusterId = inner1[CLUSTER_PREFIX.length()..-1] as int
        } else {
            return null
        }

        int appId = inner2[APP_PREFIX.length()..-1] as int
        // all routers match
        int routerId = inner3[ROUTER_PREFIX.length()..-1] as int

        answerByApp(clusterId, appId, domain, ttl)
    }

    List<DnsRecord> answer2(String domain, int ttl) {
        // app_name.namespace_name.cluster_id.local eg. pma.demo.cluster_1.local

        def arr = domain.split(/\./)
        if (arr.length > 4 || arr.length < 2) {
            return null
        }

        if (arr.length == 4) {
            def domainSuffix = Conf.instance.getString('dns.domain.suffix', '.local')
            if (!domain.endsWith(domainSuffix)) {
                return null
            }
        }

        int clusterId = DEFAULT_CLUSTER_ID
        if (arr.length >= 3) {
            def inner3 = arr[2]
            if (inner3.startsWith(CLUSTER_PREFIX)) {
                clusterId = inner3[CLUSTER_PREFIX.length()..-1] as int
            } else {
                return null
            }
        }

        def namespaceName = arr[1]
        def namespaceOne = InMemoryCacheSupport.instance.namespaceList.find { it.name == namespaceName }
        if (!namespaceOne) {
            return null
        }
        if (namespaceOne.clusterId != clusterId) {
            return null
        }

        def appName = arr[0]
        def appOne = InMemoryCacheSupport.instance.appList.find { it.name == appName }
        if (!appOne) {
            return null
        }

        int appId = appOne.id
        answerByApp(clusterId, appId, domain, ttl)
    }

    @Override
    List<DnsRecord> answer(DnsRecord dnsQuestion, int ttl) {
        // app_id eg. app_40
        // or app_id.cluster_id eg. app_40.cluster_1
        // or app_id.cluster_id.local eg. app_40.cluster_1.local
        // or app_name.namespace_name eg. pma.demo
        // or app_name.namespace_name.cluster_id eg. pma.demo.cluster_1
        // or app_name.namespace_name.cluster_id.local.com eg. pma.demo.cluster_1.local
        def domain = dnsQuestion.name()
        if (domain.endsWith('.')) {
            domain = domain[0..-2]
        }

        if (domain.startsWith(GW_PREFIX)) {
            return answerGw(domain, ttl)
        }

        if (!domain.startsWith(APP_PREFIX)) {
            return answer2(domain, ttl)
        }

        def arr = domain.split(/\./)
        if (arr.length > 3) {
            return null
        }

        if (arr.length == 3) {
            def domainSuffix = Conf.instance.getString('dns.domain.suffix', '.local')
            if (!domain.endsWith(domainSuffix)) {
                return null
            }
        }

        int clusterId = DEFAULT_CLUSTER_ID
        if (arr.length >= 2) {
            def inner2 = arr[1]
            if (inner2.startsWith(CLUSTER_PREFIX)) {
                clusterId = inner2[CLUSTER_PREFIX.length()..-1] as int
            } else {
                return null
            }
        }

        int appId = arr[0][APP_PREFIX.length()..-1] as int
        answerByApp(clusterId, appId, domain, ttl)
    }

    private List<DnsRecord> answerByApp(int clusterId, int appId, String domain, int ttl) {
        List<DnsRecord> answerList = []

        def containerList = InMemoryAllContainerManager.instance.getContainerList(clusterId, appId)
        if (!containerList) {
            log.warn 'no container found for cluster id: {}, app id: {}', clusterId, appId

            def localAnswer = new DefaultDnsRawRecord(
                    domain,
                    DnsRecordType.A,
                    ttl,
                    Unpooled.wrappedBuffer(LOCALHOST_BYTES))
            answerList << localAnswer
            return answerList
        }

        for (x in containerList) {
            if (x.checkOk()) {
                def nodeIp = x.nodeIp
                byte[] nodeIpBytes = new byte[4]
                def innerArr = nodeIp.split(/\./)
                for (int i = 0; i < 4; i++) {
                    nodeIpBytes[i] = (byte) (innerArr[i] as int)
                }
                def queryAnswer = new DefaultDnsRawRecord(
                        domain,
                        DnsRecordType.A,
                        ttl,
                        Unpooled.wrappedBuffer(nodeIpBytes))
                answerList << queryAnswer
            }
        }
        if (!answerList) {
            log.warn 'no container is live for cluster id: {}, app id: {}', clusterId, appId
            def localAnswer = new DefaultDnsRawRecord(
                    domain,
                    DnsRecordType.A,
                    ttl,
                    Unpooled.wrappedBuffer(LOCALHOST_BYTES))
            answerList << localAnswer
        }

        answerList
    }
}
