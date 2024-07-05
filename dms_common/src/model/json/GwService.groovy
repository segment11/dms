package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GwService implements JSONFiled {
    @CompileStatic
    @ToString(includeNames = true)
    class Mirroring {
        HealthCheck healthCheck
        String service
        // service name -> percent, eg 10 means 10%
        List<Tuple2<String, Integer>> mirrors = []

        boolean asBoolean() {
            service && mirrors
        }
    }

    @CompileStatic
    @ToString(includeNames = true)
    class HealthCheck {
        String hostname
        String path
        String method
        int status
        int port
        String scheme
        int interval
        int timeout

        Map<String, String> headers = [:]

        boolean asBoolean() {
            path
        }
    }

    @CompileStatic
    @ToString(includeNames = true)
    class StickyCookie {
        boolean httpOnly
        String name
        boolean secure
        String samesite
        int maxAge

        boolean asBoolean() {
            name
        }
    }

    @CompileStatic
    @ToString(includeNames = true)
    class Weighted {
        // service name -> weight
        List<Tuple2<String, Integer>> services = []
        StickyCookie stickyCookie

        boolean asBoolean() {
            services
        }
    }

    String name

    GwLoadBalancer loadBalancer

    Mirroring mirroring

    Weighted weighted

    List<KVPair<String>> toKVList() {
        // refer to https://doc.traefik.io/traefik/routing/services/
        // https://doc.traefik.io/traefik/routing/providers/kv/
        final String prefix = '/http/services/' + name + '/'

        List<KVPair<String>> kvList = []

        def kvListLoadBalancer = loadBalancer?.toKVList()
        if (kvListLoadBalancer) {
            kvListLoadBalancer.each {
                it.key = prefix + it.key
                kvList << it
            }
        }

        if (mirroring) {
            kvList << new KVPair(prefix + 'mirroring/service', mirroring.service)
            mirroring.mirrors.eachWithIndex { Tuple2<String, Integer> mirror, int i ->
                kvList << new KVPair(prefix + 'mirroring/mirrors/' + i + '/name', mirror.v1)
                kvList << new KVPair(prefix + 'mirroring/mirrors/' + i + '/percent', mirror.v2.toString())
            }
        }

        if (weighted) {
            weighted.services.eachWithIndex { Tuple2<String, Integer> service, int i ->
                kvList << new KVPair(prefix + 'weighted/services/' + i + '/name', service.v1)
                kvList << new KVPair(prefix + 'weighted/services/' + i + '/weight', service.v2.toString())
            }

            if (weighted.stickyCookie) {
                kvList << new KVPair(prefix + 'sticky/cookie', 'true')
                kvList << new KVPair(prefix + 'sticky/cookie/httponly', weighted.stickyCookie.httpOnly.toString())
                kvList << new KVPair(prefix + 'sticky/cookie/name', weighted.stickyCookie.name)
                kvList << new KVPair(prefix + 'sticky/cookie/secure', weighted.stickyCookie.secure.toString())
                if (weighted.stickyCookie.samesite) {
                    kvList << new KVPair(prefix + 'sticky/cookie/samesite', weighted.stickyCookie.samesite)
                }
                if (weighted.stickyCookie.maxAge) {
                    kvList << new KVPair(prefix + 'sticky/cookie/maxage', weighted.stickyCookie.maxAge.toString())
                }
            }
        }

        kvList
    }
}
