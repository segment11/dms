package model.json

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GwMiddlewares implements JSONFiled {
    // refer to https://doc.traefik.io/traefik/middlewares/http/overview/
    @CompileStatic
    static enum Type {
        AddPrefix, BasicAuth, Buffering, CircuitBreaker,
        Compress, ContentType, DigestAuth, Error, ForwardAuth,
        Headers, IPAllowList, InFlightReq, PassTLSClientCert,
        RateLimit, RedirectRegex, RedirectScheme, ReplacePath, ReplacePathRegex,
        Retry, StripPrefix, StripPrefixRegex
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class One {
        String name
        String type
        String json

        Map<String, JSONObject> toMap() {
            if (!json) {
                return null
            }

            Map<String, JSONObject> inner = [:]
            def key = type[0..1].toLowerCase() + type[2..-1]
            inner[key] = JSON.parseObject(json)

            inner
        }
    }

    List<One> list = []
}
