package ctrl.dnsmasq

import org.segment.web.handler.ChainHandler
import org.xbill.DNS.Lookup
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.Type
import plugin.demo2.ConsulPlugin
import plugin.demo2.DnsmasqPlugin
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.dns.DnsOperator

def h = ChainHandler.instance

h.group('/dns') {
    h.get('/lookup') { req, res ->
        def clusterIdStr = req.param('clusterId')
        assert clusterIdStr
        int clusterId = clusterIdStr as int

        def dnsmasqPlugin = new DnsmasqPlugin()
        def consulPlugin = new ConsulPlugin()

        def dnsmasqApp = InMemoryCacheSupport.instance.appList.find {
//            it.clusterId == clusterId &&
            it.conf.imageName() == dnsmasqPlugin.imageName()
        }
        def consulApp = InMemoryCacheSupport.instance.appList.find {
//            it.clusterId == clusterId &&
            it.conf.imageName() == consulPlugin.imageName()
        }
        if (!dnsmasqApp || !consulApp) {
            return [list: []]
        }

        def resolveHost = InMemoryAllContainerManager.instance.getContainerList(clusterId).find {
            it.running()
        }?.nodeIp
        if (!resolveHost) {
            return [list: []]
        }
        def resolvePort = DnsmasqPlugin.getParamValueFromTpl(dnsmasqApp.conf, '/etc/dnsmasq.conf', 'port') as int
        def resolver = new SimpleResolver(new InetSocketAddress(resolveHost, resolvePort))

        def dataCenter = DnsmasqPlugin.getEnvValue(consulApp.conf, 'DATA_CENTER')
        def domain = DnsmasqPlugin.getEnvValue(consulApp.conf, 'DOMAIN')

        def suffix = '.service.' + dataCenter + '.' + domain

        def appList = InMemoryCacheSupport.instance.getAppList().findAll { it.clusterId == clusterId }
        def list = appList.collect { app ->
            def r = [:]
            // for page change
            r.appId = app.id
            r.appName = app.name
            r.appDes = app.des
            r.clusterId = app.clusterId
            r.namespaceId = app.namespaceId

            def appService = DnsOperator.appServiceName(app)
            def service = appService + suffix
            r.service = service

            def lookup = new Lookup(service, Type.A)
            lookup.resolver = resolver
            lookup.run()

            if (lookup.result == Lookup.SUCCESSFUL) {
                r.answers = lookup.answers.collect { record ->
                    record.rdataToString()
                }
            }
            r
        }
        [list: list]
    }
}
