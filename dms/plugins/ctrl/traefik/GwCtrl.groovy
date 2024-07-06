package ctrl.traefik

import auth.User
import model.GwClusterDTO
import model.GwRouterDTO
import org.segment.web.handler.ChainHandler
import server.InMemoryCacheSupport

def h = ChainHandler.instance

// use model.traefik directly

model.traefik.JustTest.hi('test')

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

            def ymlOne = app.conf.fileVolumeList.find { it.dist == '/etc/traefik/traefik.yml' }
            one.serverPort = ymlOne.paramList.find { it.key == 'serverPort' }.value as int
            one.dashboardPort = ymlOne.paramList.find { it.key == 'dashboardPort' }.value as int

            one.updatedDate = new Date()
            def id = one.add()
            one.id = id

            list << one
        }

        list
    }.get('/list/simple') { req, resp ->
        [list: new GwClusterDTO().noWhere().queryFields('id,name,des').list()]
    }.get('/overview') { req, resp ->
        def clusterIdStr = req.param('clusterId')
        assert clusterIdStr

        def clusterId = clusterIdStr as int

        // eg.
        // list one -> [id: 1, isNotMatch: false, serverUrlList: [[url: '', weight: 10]]

        GwClusterDTO clusterOne = new GwClusterDTO(id: clusterId).one()
        if (!clusterOne) {
            return [list: []]
        }

        def gwRouterList = new GwRouterDTO(clusterId: clusterId).list()
        if (!gwRouterList) {
            return [list: []]
        }

        def list = []

        for (gwRouter in gwRouterList) {
            def one = [:]
            one.id = gwRouter.id
            one.name = gwRouter.name
            one.des = gwRouter.des
            one.rule = gwRouter.rule

            // todo: check gateway config applications running containers
            one.isNotMatch = false

            def serverUrlList = []
            one.serverUrlList = serverUrlList

            gwRouter.service.loadBalancer?.serverUrlList.each { serverUrl ->
                def inner = [:]
                inner.url = serverUrl.url
                inner.weight = 10
                serverUrlList << inner
            }

            if (gwRouter.service.weighted) {

            }

            list << one
        }

        [list: list]
    }
}

h.group('/gw/router') {
    h.get('/list') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId
        new GwRouterDTO(clusterId: clusterId as int).list()
    }.get('/list/simple') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId
        new GwRouterDTO(clusterId: clusterId as int).queryFields('id,name,des').list()
    }.delete('/delete') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        assert id
        new GwRouterDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def one = req.bodyAs(GwRouterDTO)
        assert one.name && one.clusterId
        one.updatedDate = new Date()

        def gwCluster = new GwClusterDTO(id: one.clusterId).one()
        if (!gwCluster) {
            resp.halt(400, 'gw cluster not found')
        }

        def appOne = InMemoryCacheSupport.instance.appList.find { it.id == gwCluster.appId }
        if (!appOne) {
            resp.halt(400, 'app not found')
        }

        // default rule
        // refer ClusterDnsAnswerHandler.answerGw
        boolean needUpdateRule = false
        if (!one.rule || !one.rule.contains('Host(`gw_')) {
            one.rule = "Host(`gw_cluster_${appOne.clusterId}.app_${appOne.id}.router_${one.id}.local`)"
            needUpdateRule = true
        }

        if (one.id) {
            one.update()
        } else {
            def id = one.add()
            one.id = id

            // id == null when add, need update again
            if (needUpdateRule) {
                def rule = "Host(`gw_cluster_${appOne.clusterId}.app_${appOne.id}.router_${one.id}.local`)"
                new GwRouterDTO(id: id, rule: rule).update()
            }
        }

        return [id: one.id]
    }
}

// for traefik provider, http endpoint
h.get('/api/gw/provider/json/:appId') { req, resp ->
    def appId = req.attr(':appId') as int

    def gwCluster = new GwClusterDTO(appId: appId).one()
    if (!gwCluster) {
        return [:]
    }

    def gwRouterList = new GwRouterDTO(clusterId: gwCluster.id).list()
    if (!gwRouterList) {
        return [:]
    }

    def routers = [:]
    def services = [:]

    for (gwRouter in gwRouterList) {
        def gwService = gwRouter.service

        def router = [:]
        router.rule = gwRouter.rule
        router.service = gwService.name
        routers[gwRouter.name] = router

        def loadBalancer = [:]

        if (gwService.loadBalancer) {
            loadBalancer.passHostHeader = gwService.loadBalancer.passHostHeader
            loadBalancer.serversTransport = gwService.loadBalancer.serversTransport
            if (gwService.loadBalancer.serverUrlList) {
                loadBalancer.servers = gwService.loadBalancer.serverUrlList.collect {
                    [url: it.url, weight: it.weight]
                }
            }

            if (gwService.loadBalancer.healthCheck) {
                def healthCheck = [:]
                healthCheck.path = gwService.loadBalancer.healthCheck.path
                healthCheck.interval = gwService.loadBalancer.healthCheck.interval
                healthCheck.timeout = gwService.loadBalancer.healthCheck.timeout

                loadBalancer.healthCheck = healthCheck
            }
        }

        def service = [loadBalancer: loadBalancer]
        services[gwService.name] = service
    }

    def http = [routers: routers, services: services]

    def r = [http: http]
    r
}