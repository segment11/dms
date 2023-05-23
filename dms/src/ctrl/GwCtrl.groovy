package ctrl

import auth.User
import model.GwClusterDTO
import model.GwFrontendDTO
import model.json.GwFrontendRuleConf
import org.segment.web.handler.ChainHandler
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.dns.DnsOperator
import server.gateway.GatewayOperator
import transfer.ContainerInfo

def h = ChainHandler.instance

h.group('/gw/cluster') {
    h.get('/list') { req, resp ->
        def traefikAppList = InMemoryCacheSupport.instance.appList.findAll {
            it.conf.group == 'library' && it.conf.image == 'traefik'
        }
        if (!traefikAppList) {
            return []
        }

        def list = []
        for (app in traefikAppList) {
            def old = new GwClusterDTO(appId: app.id).one()
            if (old) {
                list << old
                continue
            }

            def one = new GwClusterDTO()
            one.appId = app.id
            one.name = app.name
            one.des = app.des
            one.serverUrl = 'http://' + app.conf.targetNodeIpList[0]

            def ymlOne = app.conf.fileVolumeList.find { it.dist == '/etc/traefik/traefik.toml' }
            one.serverPort = ymlOne.paramList.find { it.key == 'serverPort' }.value as int
            one.dashboardPort = ymlOne.paramList.find { it.key == 'dashboardPort' }.value as int
            one.prefix = ymlOne.paramList.find { it.key == 'prefix' }.value

            def zkAppName = ymlOne.paramList.find { it.key == 'zkAppName' }.value
            def zkApp = InMemoryCacheSupport.instance.appList.find { it.name == zkAppName }
            if (!zkApp) {
                resp.halt(400, 'app zk not found')
            }

            def zkConnectString = zkApp.conf.targetNodeIpList.collect { it + ':2181' }.join(',')
            one.zkConnectString = zkConnectString

            one.updatedDate = new Date()
            def id = one.add()
            one.id = id

            list << one
        }

        list
    }.get('/list/simple') { req, resp ->
        new GwClusterDTO().noWhere().queryFields('id,name,des').loadList()
    }.get('/overview') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId

        GwClusterDTO clusterOne = new GwClusterDTO(id: clusterId as int).one()
        List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(0, clusterOne.appId)
        if (!containerList || containerList.every { x ->
            !x.running()
        }) {
            return []
        }

        def r = GatewayOperator.getBackendListFromApi(clusterId as int)

        def list = new GwFrontendDTO(clusterId: clusterId as int).loadList()
        list.collect {
            def apiBackendList = r[it.id] ?: []
            def serverList = it.backend.serverList

            boolean isNotMatch = false
            serverList.each { server ->
                def one = apiBackendList.find { t -> t.url == server.url }
                if (!one) {
                    server.url += ' - !!! not found from api'
                    isNotMatch = true
                } else {
                    if (one.weight != server.weight) {
                        server.url += (' - !!! weight api - ' + one.weight)
                        isNotMatch = true
                    }
                }
            }
            apiBackendList.each { server ->
                def one = serverList.find { t -> t.url == server.url }
                if (!one) {
                    server.url += ' - !!! not found from local config'
                    isNotMatch = true
                } else {
                    if (one.weight != server.weight) {
                        server.url += (' - !!! weight local - ' + one.weight)
                        isNotMatch = true
                    }
                }
            }
            [id  : it.id, name: it.name, des: it.des, isNotMatch: isNotMatch,
             conf: it.conf, serverList: serverList, apiBackendList: apiBackendList]
        }
    }
}

h.group('/gw/frontend') {
    h.get('/list') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId
        new GwFrontendDTO(clusterId: clusterId as int).loadList()
    }.get('/list/simple') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId
        new GwFrontendDTO(clusterId: clusterId as int).queryFields('id,name,des').loadList()
    }.delete('/delete') { req, resp ->
        User u = req.session('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        assert id
        new GwFrontendDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        User u = req.session('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def one = req.bodyAs(GwFrontendDTO)
        assert one.name && one.clusterId

        def consulApp = InMemoryCacheSupport.instance.appList.find {
            it.conf.group == 'library' && it.conf.image == 'consul'
        }
        def envDc = consulApp.conf.envList.find { it.key == 'DATA_CENTER' }
        def envDomain = consulApp.conf.envList.find { it.key == 'DOMAIN' }

        // default dns service name
        def dc = envDc ? envDc.value.toString() : 'cn-north-1'
        def domain = envDomain ? envDomain.value.toString() : 'dms'
        String suffix = ".service.${dc}.${domain}".toString()
        one.updatedDate = new Date()

        if (one.id) {
            def dnsServiceName = "gw_${one.clusterId}_${one.id}".toString() + suffix
            if (one.conf.ruleConfList.find { it.rule == dnsServiceName } == null) {
                if (one.conf.ruleConfList.find { it.type == 'Host:' } == null) {
                    one.conf.ruleConfList << new GwFrontendRuleConf(type: 'Host:', rule: dnsServiceName)
                }
            }

            one.update()
            GatewayOperator.updateFrontend(one)
            DnsOperator.refreshGwFrontendDns(one)
            return [id: one.id]
        } else {
            def id = one.add()
            one.id = id

            def dnsServiceName = "gw_${one.clusterId}_${one.id}".toString() + suffix
            if (one.conf.ruleConfList.find { it.rule == dnsServiceName } == null) {
                if (one.conf.ruleConfList.find { it.type == 'Host:' } == null) {
                    one.conf.ruleConfList << new GwFrontendRuleConf(type: 'Host:', rule: dnsServiceName)
                }
            }
            one.update()

            GatewayOperator.updateFrontend(one)
            DnsOperator.refreshGwFrontendDns(one)
            return [id: id]
        }
    }
}