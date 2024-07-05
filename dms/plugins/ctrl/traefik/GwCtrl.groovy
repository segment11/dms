package ctrl.traefik

import auth.User
import model.GwClusterDTO
import model.GwRouterDTO
import org.segment.web.handler.ChainHandler
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import transfer.ContainerInfo

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
        def clusterId = req.param('clusterId')
        assert clusterId

        // eg.
        // list one -> [id: 1, serverUrlList: [[url: '', weight: 10]]

        GwClusterDTO clusterOne = new GwClusterDTO(id: clusterId as int).one()
        List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(0, clusterOne.appId)
        if (!containerList || containerList.every { x ->
            !x.running()
        }) {
            return [list: []]
        }


        [list: []]
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

        if (one.id) {
            one.update()
        } else {
            def id = one.add()
            one.id = id
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
                    [url: it.v1]
                }
            }
        }

        def service = [loadBalancer: loadBalancer]
        services[gwService.name] = service
    }

    def http = [routers: routers, services: services]

    def r = [http: http]
    r
}