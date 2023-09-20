package ctrl

import auth.Permit
import auth.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import common.Const
import common.TimerSupport
import org.apache.commons.codec.digest.DigestUtils
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import server.InMemoryCacheSupport
import support.AuthTokenCacheHolder

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.before('/api/**') { req, resp ->
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
    def authToken = req.cookie('Auth-Token')
    if (!authToken) {
        resp.halt(403, 'need login')
    } else {
        // default 3600s
        AuthTokenCacheHolder.instance.setCookie(req, resp, authToken)
    }

    def instance = AuthTokenCacheHolder.instance
    def user = instance.get(authToken)
    if (user) {
        req.attr('user', user)
    } else {
        def algorithm = Algorithm.HMAC256(AuthTokenCacheHolder.ALG_SECRET + instance.date.toString())
        def verifier = JWT.require(algorithm)
                .withIssuer('dms')
                .build()

        try {
            def jwt = verifier.verify(authToken)
            def claims = jwt.claims
            def name = claims.get('name').asString()
            def permitFormatStringList = claims.get('permitList').asArray(String)
            def lastLoginTime = claims.get('lastLoginTime').asDate()

            def userPut = new User()
            userPut.name = name
            userPut.lastLoginTime = lastLoginTime
            for (str in permitFormatStringList) {
                userPut.permitList << Permit.fromFormatString(str)
            }

            req.attr('user', userPut)
            instance.put(authToken, userPut)
        } catch (JWTVerificationException e) {
            log.error('jwt verify fail', e)
            resp.halt(403, 'token not match')
        }
    }
    return
}

h.afterAfter('/**') { req, resp ->
    TimerSupport.stopUriHandle()
}
