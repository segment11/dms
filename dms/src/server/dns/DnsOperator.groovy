package server.dns

import com.google.common.net.HostAndPort
import com.orbitz.consul.AgentClient
import com.orbitz.consul.Consul
import com.orbitz.consul.NotRegisteredException
import com.orbitz.consul.model.agent.FullService
import com.orbitz.consul.model.agent.ImmutableRegistration
import com.orbitz.consul.option.QueryOptions
import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.GwClusterDTO
import model.GwFrontendDTO
import plugin.demo2.ConsulPlugin
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import transfer.ContainerInfo

@CompileStatic
@Singleton
@Slf4j
@Deprecated
class DnsOperator {
    static final List<String> tags = ['dms']

    private AgentClient agentClient

    AgentClient getAgentClient() {
        if (agentClient) {
            return agentClient
        }

        synchronized (this) {
            def consulApp = InMemoryCacheSupport.instance.appList.find {
                it.conf.imageName() == new ConsulPlugin().imageName()
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

    boolean isServiceIdMatchAddress(String id, String address) {
        QueryOptions options = QueryOptions.BLANK
        FullService serviceOld
        try {
            serviceOld = agentClient.getService(id, options).response
            if (serviceOld && serviceOld.address == address) {
                return true
            }
        } catch (NotRegisteredException ignored) {
            // ignore
        }
        false
    }

    static String appServiceName(AppDTO app) {
        app.name.replaceAll(' ', '_').toLowerCase()
    }

    void register(AppDTO app, String nodeIp, int instanceIndex) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        def address = nodeIp

        // app_appId
        def appService = appServiceName(app)
        def full = appService + '_' + instanceIndex

        if (isServiceIdMatchAddress(full, address)) {
            return
        }

        // default 10min
        def defaultTtl = Conf.instance.getInt('default.ttl', 600)
        def cluster = InMemoryCacheSupport.instance.oneCluster(app.clusterId)

        def service = ImmutableRegistration.builder()
                .id(full + '_host')
                .name(full)
                .address(address)
//                    .check(Registration.RegCheck.ttl(dnsTtl))
                .tags(tags)
                .build()
        agentClient.register(service)

        def service2 = ImmutableRegistration.builder()
                .id(full)
                .name(appService)
                .address(address)
//                    .check(Registration.RegCheck.ttl(dnsTtl))
                .tags(tags)
                .build()
        agentClient.register(service2)
    }

    void deregister(AppDTO app, int instanceIndex) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        def appService = appServiceName(app)
        def full = appService + '_' + instanceIndex

        agentClient.deregister(full + '_host')
        agentClient.deregister(full)
    }

    void refreshContainerDns(AppDTO app, List<ContainerInfo> runningContainerList) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        for (x in runningContainerList) {
            register(app, x.nodeIp, x.instanceIndex())
        }

        if (app.conf.image == 'traefik') {
            def gwClusterOne = new GwClusterDTO(appId: app.id).one()
            if (gwClusterOne) {
                def frontendList = new GwFrontendDTO(clusterId: gwClusterOne.id).list()
                for (frontend in frontendList) {
                    refreshGwFrontendDns(frontend, gwClusterOne)
                }
            }
        }
    }

    void refreshGwFrontendDns(GwFrontendDTO frontend, GwClusterDTO cluster = null) {
        def agentClient = getAgentClient()
        if (!agentClient) {
            return
        }

        if (cluster == null) {
            cluster = new GwClusterDTO(id: frontend.clusterId).one()
        }

        if (cluster) {
            def appOne = InMemoryCacheSupport.instance.oneApp(cluster.appId)
            def nodeIpList = InMemoryAllContainerManager.instance.getContainerList(appOne.clusterId, appOne.id).collect { x ->
                x.nodeIp
            }
            nodeIpList.eachWithIndex { String nodeIp, int i ->
                def name = "gw_${cluster.id}_${frontend.id}".toString()
                def id = name + '_' + i

                def address = nodeIp

                def service = ImmutableRegistration.builder()
                        .id(id)
                        .name(name)
                        .address(address)
                        .tags(tags)
                        .build()
                agentClient.register(service)
            }
        }
    }
}
