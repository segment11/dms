package server.dns

import com.google.common.net.HostAndPort
import com.orbitz.consul.AgentClient
import com.orbitz.consul.Consul
import com.orbitz.consul.NotRegisteredException
import com.orbitz.consul.model.agent.FullService
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.option.QueryOptions
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

    // default dns ttl 5 minutes
    static final int defaultTtl = 300

    static AgentClient agentClient

    static AgentClient getAgentClient() {
        if (agentClient) {
            return agentClient
        }

        synchronized (DnsOperator.class) {
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
            agentClient = client.agentClient()
        }
        agentClient
    }

    static boolean isServiceIdMatchAddress(String id, String address) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return false
        }

        QueryOptions options = QueryOptions.BLANK
        FullService serviceOld
        try {
            serviceOld = agentClient.getService(id, options).response
            if (serviceOld && serviceOld.address == address) {
                return true
            }
        } catch (NotRegisteredException e) {
            // ignore
        }
        false
    }

    static void refreshContainerDns(ClusterDTO cluster, AppDTO app, List<ContainerInfo> runningContainerList) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        def dnsTtl = cluster.globalEnvConf.dnsTtl ?: defaultTtl

        for (x in runningContainerList) {
            def instanceIndex = x.instanceIndex()
            def address = x.nodeIp

            // app_appId
            def appService = app.name.replaceAll(' ', '_').toLowerCase()
            def full = appService + '_' + instanceIndex

            if (isServiceIdMatchAddress(full, address)) {
                continue
            }

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

        if (app.conf.image == 'traefik') {
            def gwClusterOne = new GwClusterDTO(appId: app.id).one()
            if (gwClusterOne) {
                def frontendList = new GwFrontendDTO(clusterId: gwClusterOne.id).loadList()
                for (frontend in frontendList) {
                    refreshGwFrontendDns(frontend, gwClusterOne)
                }
            }
        }
    }

    static void refreshGwFrontendDns(GwFrontendDTO frontend, GwClusterDTO cluster = null) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        if (cluster == null) {
            cluster = new GwClusterDTO(id: frontend.clusterId).one()
        }
        def address = cluster.serverUrl.replace('http://', '')

        def name = "gw_${cluster.id}_${frontend.id}".toString()
        def id = name + '_host'

        if (isServiceIdMatchAddress(id, address)) {
            return
        }

        def service = ImmutableRegistration.builder()
                .id(id)
                .name(name)
                .address(address)
                .tags(tags)
                .build()
        agentClient.register(service)
    }
}
