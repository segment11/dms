package ctrl

import auth.User
import common.Const
import common.TimerSupport
import org.apache.commons.codec.digest.DigestUtils
import org.segment.web.handler.ChainHandler
import server.InMemoryCacheSupport

def h = ChainHandler.instance
h.before('/api/**') { req, resp ->
    User u = req.session('user') as User
    // admin can debug
    if (u && u.isAdmin()) {
        return
    }

    // check auth token
    def authToken = req.header(Const.AUTH_TOKEN_HEADER)
    def clusterId = req.header(Const.CLUSTER_ID_HEADER)
    if (!authToken || !clusterId) {
        resp.halt(403, 'require authToken && clusterId')
    }

    if (!InMemoryCacheSupport.instance.clusterList) {
        resp.halt(403, 'authToken not match')
    }
    def one = InMemoryCacheSupport.instance.oneCluster(clusterId as int)
    if (!one || DigestUtils.md5Hex(one.secret + req.host()) != authToken) {
        resp.halt(403, 'authToken not match')
    }
}

h.before('/**') { req, resp ->
    def uri = req.uri()
    TimerSupport.startUriHandle(uri)

    // not api
    if (uri.contains('/api/')) {
        return
    }

    List<String> skipEndsWithList = ['/login', '/logout', '/agent/auth', '/hz']
    if (skipEndsWithList.any { uri.endsWith(it) }) {
        return
    }

    // check login
    User u = req.session('user') as User
    if (!u) {
        resp.halt(403, 'need login')
    }
}

h.afterAfter('/**') { req, resp ->
    TimerSupport.stopUriHandle()
}
