package ctrl

import auth.User
import model.AppDTO
import model.ClusterDTO
import model.NamespaceDTO
import model.NodeDTO
import org.segment.web.handler.ChainHandler
import server.InMemoryAllContainerManager
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
        new ClusterDTO().where('1=1').loadList()
    }.get('/list/simple') { req, resp ->
        def list = new ClusterDTO().where('1=1').queryFields('id,name,des,is_in_guard').loadList()
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

h.get('/api/cluster/hosts') { req, resp ->
    def clusterId = req.param('clusterId')
    assert clusterId

    def namespaceList = new NamespaceDTO(clusterId: clusterId as int).
            queryFields('id,name').loadList()
    def appList = new AppDTO(clusterId: clusterId as int).
            queryFields('id,name,namespace_id,conf').loadList()

    def instance = InMemoryAllContainerManager.instance

    Set<String> list = []
    for (app in appList) {
        def namespace = namespaceList.find { it.id == app.namespaceId }
        def targetNodeIpList = app.conf.targetNodeIpList
        if (targetNodeIpList) {
            list << "${targetNodeIpList[0]} ${namespace ? namespace.name : ''}.${app.name}".toString()
            targetNodeIpList.eachWithIndex { String nodeIp, int i ->
                list << "${nodeIp} ${namespace ? namespace.name : ''}.${app.name}${i}".toString()
            }
        } else {
            def containerList = instance.getContainerList(clusterId as int, app.id)
            containerList.each { x ->
                def instanceIndex = x.instanceIndex()
                if (0 == instanceIndex) {
                    list << "${x.nodeIp} ${namespace ? namespace.name : ''}.${app.name}".toString()
                }
                list << "${x.nodeIp} ${namespace ? namespace.name : ''}.${app.name}${instanceIndex}".toString()
            }
        }
    }

    resp.end list.join("\r\n")
}