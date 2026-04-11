package support

import org.segment.web.handler.Req
import org.segment.web.handler.Resp
import spock.lang.Specification

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthTokenCacheHolderTest extends Specification {
    def 'setCookie strips the port from the host before writing the auth cookie'() {
        given:
        HttpServletRequest rawReq = Mock()
        HttpServletResponse rawResp = Mock()
        rawReq.getHeader(_) >> { String name ->
            name == 'Host' ? 'dms.example.com:5010' : null
        }
        def req = new Req(rawReq)
        def resp = new Resp(req, rawResp)

        when:
        AuthTokenCacheHolder.instance.setCookie(req, resp, 'token-123')

        then:
        1 * rawResp.addCookie({
            Cookie cookie ->
                cookie.name == 'Auth-Token' &&
                        cookie.value == 'token-123' &&
                        cookie.maxAge == 3600 &&
                        cookie.path == '/' &&
                        cookie.domain == 'dms.example.com'
        })
    }
}
