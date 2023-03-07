package ctrl

import auth.User
import model.ClusterDTO
import model.NamespaceDTO
import model.NodeDTO
import model.NodeKeyPairDTO
import org.segment.web.handler.ChainHandler
import server.scheduler.Guardian

def h = ChainHandler.instance

h.get('/guard/toggle') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    Guardian.instance.isRunning = !Guardian.instance.isRunning
    [isGuardianRunning: Guardian.instance.isRunning]
}

h.group('/cluster') {
    h.get('/list') { req, resp ->
        new ClusterDTO().noWhere().loadList()
    }.get('/list/simple') { req, resp ->
        def list = new ClusterDTO().noWhere().queryFields('id,name,des,is_in_guard').loadList()
        [list: list, isGuardianRunning: Guardian.instance.isRunning]
    }.delete('/delete') { req, resp ->
        User u = req.session('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        assert id

        // check if has namespace
        def namespaceList = new NamespaceDTO(clusterId: id as int).
                queryFields('id,name').loadList()
        if (namespaceList) {
            resp.halt(500, 'has namespace - ' + namespaceList.collect { it.name })
        }

        // key pair
        def kpList = new NodeKeyPairDTO(clusterId: id as int).
                queryFields('id,ip').loadList()
        if (kpList) {
            resp.halt(500, 'has node key pair list - ' + kpList.collect { it.ip })
        }

        new ClusterDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        def one = req.bodyAs(ClusterDTO)
        assert one.name && one.secret
        one.updatedDate = new Date()
        if (one.id) {
            User u = req.session('user') as User
            if (!u.isAccessCluster(one.id)) {
                resp.halt(403, 'not this cluster manager')
            }

            one.update()
            return [id: one.id]
        } else {
            User u = req.session('user') as User
            if (!u.isAdmin()) {
                resp.halt(403, 'not admin')
            }

            def id = one.add()
            return [id: id]
        }
    }.get('/one') { req, resp ->
        // ? no use? todo
        def id = req.param('id')
        assert id
        def one = new ClusterDTO(id: id as int).one()
        def nodeList = new NodeDTO(clusterId: id as int).loadList()
        [one: one, nodeList: nodeList]
    }.get('/guard/toggle') { req, resp ->
        def id = req.param('id')
        assert id
        ClusterDTO one = new ClusterDTO(id: id as int).queryFields('id,is_in_guard').one()
        one.isInGuard = !one.isInGuard
        one.update()

        [isInGuard: one.isInGuard]
    }
}