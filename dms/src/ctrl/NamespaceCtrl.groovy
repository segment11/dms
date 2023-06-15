package ctrl

import auth.User
import model.AppDTO
import model.NamespaceDTO
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/namespace') {
    h.get('/list') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId

        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        new NamespaceDTO(clusterId: clusterId as int).listPager(pageNum, pageSize)
    }.get('/list/simple') { req, resp ->
        def clusterId = req.param('clusterId')
        assert clusterId
        new NamespaceDTO(clusterId: clusterId as int).list()
    }.delete('/delete') { req, resp ->
        def id = req.param('id')
        assert id

        NamespaceDTO one = new NamespaceDTO(id: id as int).queryFields('cluster_id').one()
        User u = req.session('user') as User
        if (!u.isAccessCluster(one.clusterId)) {
            resp.halt(403, 'not this cluster manager')
        }
        // check if has app
        def appList = new AppDTO(namespaceId: id as int).queryFields('id,name').list()
        if (appList) {
            resp.halt(500, 'has app - ' + appList.collect { it.name })
        }

        new NamespaceDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        def one = req.bodyAs(NamespaceDTO)
        assert one.name && one.clusterId
        one.updatedDate = new Date()
        if (one.id) {
            User u = req.session('user') as User
            if (!u.isAccessNamespace(one.id)) {
                resp.halt(403, 'not this namespace manager')
            }

            one.update()
            return [id: one.id]
        } else {
            User u = req.session('user') as User
            if (!u.isAccessCluster(one.clusterId)) {
                resp.halt(403, 'not this cluster manager')
            }

            def id = one.add()
            return [id: id]
        }
    }
}