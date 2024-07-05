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
        [list: new GwClusterDTO().noWhere().queryFields('id,name,des').list()]
    }.get('/overview') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId

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
        User u = req.session('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        assert id
        new GwRouterDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        User u = req.session('user') as User
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