package server.dns

import com.google.common.net.HostAndPort
import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ClusterDTO
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class DnsOperator {
    static void refreshDns(ClusterDTO cluster, AppDTO app, List<ContainerInfo> runningContainerList) {
        def consulApp = InMemoryCacheSupport.instance.appList.find {
            it.conf.group == 'library' && it.conf.image == 'consul'
        }
        def dnsmasqApp = InMemoryCacheSupport.instance.appList.find {
            it.conf.group == 'jpillora' && it.conf.image == 'dnsmasq'
        }

        if (!consulApp || !dnsmasqApp) {
            return
        }

        def consulContainer = InMemoryAllContainerManager.instance.
                getContainerList(consulApp.clusterId, consulApp.id).find { it.running() }
        if (!consulContainer) {
            return
        }

        // default 5min
        def dnsTtl = cluster.globalEnvConf.dnsTtl ?: 300

        def hostAndPort = HostAndPort.fromParts(consulContainer.nodeIp, 8500)
        def client = Consul.builder().withHostAndPort(hostAndPort).build()
        def agentClient = client.agentClient()

        for (x in runningContainerList) {
            def instanceIndex = x.instanceIndex()
            def address = x.nodeIp

            // app_appId
            def appService = app.name.replaceAll(' ', '_').toLowerCase()
            def full = appService + '_' + instanceIndex

            def service = ImmutableRegistration.builder()
                    .id(full + '_0')
                    .name(full)
                    .address(address)
//                    .check(Registration.RegCheck.ttl(dnsTtl))
                    .tags(Collections.singletonList("dms"))
                    .build()
            agentClient.register(service)

            def service2 = ImmutableRegistration.builder()
                    .id(full)
                    .name(appService)
                    .address(address)
//                    .check(Registration.RegCheck.ttl(dnsTtl))
                    .tags(Collections.singletonList("dms"))
                    .build()
            agentClient.register(service2)
        }
    }
}
