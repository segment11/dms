package ctrl

import auth.LoginService
import auth.PermitType
import auth.User
import common.Conf
import common.Const
import common.Event
import model.AppDTO
import model.ClusterDTO
import model.NamespaceDTO
import model.UserPermitDTO
import org.apache.commons.codec.digest.DigestUtils
import org.segment.d.Pager
import org.segment.web.handler.ChainHandler
import spi.SpiSupport

def h = ChainHandler.instance

h.post('/login') { req, resp ->
    String user = req.param('user')
    String password = req.param('password')
    assert user && password

    if ('admin' == user) {
        def adminPassword = Conf.instance.get('adminPassword')
        if (adminPassword) {
            if (password == adminPassword) {
                def u = new User()
                u.name = user
                u.permitList << User.PermitAdmin

                req.session('user', u)
                Event.builder().type(Event.Type.user).reason('login').result(user).
                        build().log(req.ip()).toDto().add()
                resp.redirect('/admin/index.html')
                return
            } else {
                resp.redirect('/admin/login.html?error=1')
                return
            }
        }
    }

    LoginService loginService = SpiSupport.createLoginService()
    def u = loginService.login(user, password)
    if (!u) {
        resp.redirect('/admin/login.html?error=1')
        return
    }

    req.session('user', u)
    Event.builder().type(Event.Type.user).reason('login').result(user).
            build().log(req.ip()).toDto().add()
    resp.redirect('/admin/index.html')
}

h.get('/login/user') { req, resp ->
    User u = req.session('user') as User
    u
}

h.get('/logout') { req, resp ->
    User u = req.session('user') as User
    if (u) {
        Event.builder().type(Event.Type.user).reason('logout').result(u.name).
                build().log(req.ip()).toDto().add()
    }

    req.removeSession('user')
    resp.redirect('/admin/login.html')
}

h.get('/agent/auth') { req, resp ->
    def clusterId = req.header(Const.CLUSTER_ID_HEADER)
    def secret = req.param('secret')
    assert clusterId && secret
    def one = new ClusterDTO(id: clusterId as int).queryFields('secret').one()
    if (one.secret != secret) {
        resp.halt(403, 'secret not match')
    }
    resp.end DigestUtils.md5Hex(one.secret + req.host())
}

h.group('/permit') {
    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def keyword = req.param('keyword')
        def permitType = req.param('permitType')
        def resourceId = req.param('resourceId')
        Pager<UserPermitDTO> pager = new UserPermitDTO().where('1=1').where(!!keyword, 'user like ?',
                '%' + keyword + '%').where(!!permitType, 'permit_type=?', permitType).
                where(!!resourceId, 'resource_id=?', resourceId as Integer).loadPager(pageNum, pageSize)
        if (pager.list) {
            def clusterList = new ClusterDTO().where('1=1').queryFields('id,name').loadList()
            def namespaceList = new NamespaceDTO().where('1=1').queryFields('id,name').loadList()
            for (one in pager.list) {
                UserPermitDTO permit = one
                if (permit.permitType == 'cluster') {
                    def cluster = clusterList.find { it.id == permit.resourceId }
                    if (cluster) {
                        permit.prop('resourceName', cluster.name)
                        permit.prop('resourceDes', cluster.des)
                    }
                } else if (permit.permitType == 'namespace') {
                    def namespace = namespaceList.find { it.id == permit.resourceId }
                    if (namespace) {
                        permit.prop('resourceName', namespace.name)
                        permit.prop('resourceDes', namespace.des)
                    }
                }
            }

            def appPermitList = pager.list.findAll { one ->
                UserPermitDTO permit = one
                'app' == permit.permitType
            }
            if (appPermitList) {
                def appList = new AppDTO().whereIn('id', appPermitList.collect { one ->
                    UserPermitDTO permit = one
                    permit.resourceId
                }).loadList()
                for (one in appPermitList) {
                    UserPermitDTO permit = one
                    def app = appList.find { it.id == permit.resourceId }
                    if (app) {
                        permit.prop('resourceName', app.name)
                        permit.prop('resourceDes', app.des)
                    }
                }
            }
        }
        pager.transfer { one ->
            UserPermitDTO permit = one as UserPermitDTO
            permit.rawProps(true)
        }
    }.delete('/delete') { req, resp ->
        def id = req.param('id')
        assert id
        new UserPermitDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        def one = req.bodyAs(UserPermitDTO)
        assert one.user && one.permitType
        User u = req.session('user') as User
        one.createdUser = u.name
        one.updatedDate = new Date()
        if (one.id) {
            one.update()
            return [id: one.id]
        } else {
            def id = one.add()
            return [id: id]
        }
    }.get('/resource/list') { req, resp ->
        def permitType = req.param('permitType')
        if (PermitType.cluster.name() == permitType) {
            return new ClusterDTO().where('1=1').queryFields('id,name').loadList()
        } else if (PermitType.namespace.name() == permitType) {
            return new NamespaceDTO().where('1=1').queryFields('id,name').loadList()
        } else if (PermitType.app.name() == permitType) {
            return new AppDTO().where('1=1').queryFields('id,name').loadList()
        }
    }
}