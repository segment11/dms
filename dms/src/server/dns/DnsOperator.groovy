package server.dns

import com.google.common.net.HostAndPort
import com.orbitz.consul.AgentClient
import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.ImmutableRegistration
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ClusterDTO
import model.GwClusterDTO
import model.GwFrontendDTO
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class DnsOperator {
    static final List<String> tags = ['dms']

    static AgentClient getAgentClient() {
        def consulApp = InMemoryCacheSupport.instance.appList.find {
            it.conf.group == 'library' && it.conf.image == 'consul'
        }
        if (!consulApp) {
            return null
        }

        def consulContainer = InMemoryAllContainerManager.instance.
                getContainerList(consulApp.clusterId, consulApp.id).find { it.running() }
        if (!consulContainer) {
            return null
        }

        def hostAndPort = HostAndPort.fromParts(consulContainer.nodeIp, 8500)
        def client = Consul.builder().withHostAndPort(hostAndPort).build()
        client.agentClient()
    }

    static void refreshDns(ClusterDTO cluster, AppDTO app, List<ContainerInfo> runningContainerList) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        // default 5min
        def dnsTtl = cluster.globalEnvConf.dnsTtl ?: 300


        for (x in runningContainerList) {
            def instanceIndex = x.instanceIndex()
            def address = x.nodeIp

            // app_appId
            def appService = app.name.replaceAll(' ', '_').toLowerCase()
            def full = appService + '_' + instanceIndex

            // name: app_appId_instanceIndex, id: app_appId_instanceIndex_host
            def service = ImmutableRegistration.builder()
                    .id(full + '_host')
                    .name(full)
                    .address(address)
//                    .check(Registration.RegCheck.ttl(dnsTtl))
                    .tags(tags)
                    .build()
            agentClient.register(service)

            // name: app_appId, id: app_appId_instanceIndex
            def service2 = ImmutableRegistration.builder()
                    .id(full)
                    .name(appService)
                    .address(address)
//                    .check(Registration.RegCheck.ttl(dnsTtl))
                    .tags(tags)
                    .build()
            agentClient.register(service2)
        }
    }

    static void refreshGwFrontendDns(GwFrontendDTO frontend) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        def cluster = new GwClusterDTO(id: frontend.clusterId).one()
        def address = cluster.serverUrl

        def name = "gw_${cluster.id}_${frontend.id}".toString()
        def id = name + '_host'

        def service = ImmutableRegistration.builder()
                .id(id)
                .name(name)
                .address(address)
                .tags(tags)
                .build()
        agentClient.register(service)
    }
}
