package ctrl

import auth.LoginService
import auth.PermitType
import auth.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.segment.common.Conf
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
import support.AuthTokenCacheHolder

def h = ChainHandler.instance

h.post('/login') { req, resp ->
    String user = req.param('user')
    String password = req.param('password')
    assert user && password

    def passwordMd5 = DigestUtils.md5Hex(password)

    def instance = AuthTokenCacheHolder.instance
    def algorithm = Algorithm.HMAC256(AuthTokenCacheHolder.ALG_SECRET + instance.date.toString())

    if ('admin' == user) {
        def envPass = System.getenv('ADMIN_PASSWORD')
        def adminPasswordMd5 = envPass ? DigestUtils.md5Hex(envPass) :
                Conf.instance.get('adminPassword')
        if (adminPasswordMd5) {
            if (Conf.instance.isDev() || passwordMd5 == adminPasswordMd5) {
                def u = new User()
                u.name = user
                u.permitList << User.PermitAdmin
                String authToken = JWT.create()
                        .withIssuer('dms')
                        .withClaim('name', user)
                        .withClaim('permitList', u.permitList.collect { it.toFormatString() })
                        .withClaim('lastLoginTime', new Date())
                        .sign(algorithm)
                resp.cookie('Auth-Token', authToken)
                instance.remove(authToken)

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

    String authToken = JWT.create()
            .withIssuer('dms')
            .withClaim('name', user)
            .withClaim('permitList', u.permitList.collect { it.toFormatString() })
            .withClaim('lastLoginTime', new Date())
            .sign(algorithm)
    resp.cookie('Auth-Token', authToken)
    instance.remove(authToken)

    Event.builder().type(Event.Type.user).reason('login').result(user).
            build().log(req.ip()).toDto().add()
    resp.redirect('/admin/index.html')
}

h.get('/login/user') { req, resp ->
    User u = req.attr('user') as User
    u
}

h.get('/logout') { req, resp ->
    User u = req.attr('user') as User
    if (u) {
        Event.builder().type(Event.Type.user).reason('logout').result(u.name).
                build().log(req.ip()).toDto().add()
    }

    resp.removeCookie('Auth-Token')
    def authToken = req.cookie('Auth-Token')
    if (authToken) {
        AuthTokenCacheHolder.instance.remove(authToken)
    }
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
        Pager<UserPermitDTO> pager = new UserPermitDTO().noWhere().
                where(keyword as boolean, 'user like ?', '%' + keyword + '%').
                where(permitType as boolean, 'permit_type=?', permitType).
                where(resourceId as boolean, 'resource_id=?', resourceId as Integer).listPager(pageNum, pageSize)
        if (pager.list) {
            def clusterList = new ClusterDTO().noWhere().queryFields('id,name').list()
            def namespaceList = new NamespaceDTO().noWhere().queryFields('id,name').list()
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
                }).list()
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
        User u = req.attr('user') as User
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
            return new ClusterDTO().noWhere().queryFields('id,name').list()
        } else if (PermitType.namespace.name() == permitType) {
            return new NamespaceDTO().noWhere().queryFields('id,name').list()
        } else if (PermitType.app.name() == permitType) {
            return new AppDTO().noWhere().queryFields('id,name').list()
        }
    }
}
