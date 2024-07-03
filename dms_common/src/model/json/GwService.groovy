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

    List<KVPair> toKVList() {
        // refer to https://doc.traefik.io/traefik/routing/services/
        // https://doc.traefik.io/traefik/routing/providers/kv/
        final String prefix = '/http/services/' + name + '/'

        List<KVPair> kvList = []

        def kvListLoadBalancer = loadBalancer?.toKVList()
        if (kvListLoadBalancer) {
            kvListLoadBalancer.each {
                it.key = prefix + it.key
                kvList << it
            }
        }

        if (mirroring) {
            kvList << new KVPair(key: prefix + 'mirroring/service', value: mirroring.service)
            mirroring.mirrors.eachWithIndex { Tuple2<String, Integer> mirror, int i ->
                kvList << new KVPair(key: prefix + 'mirroring/mirrors/' + i + '/name', value: mirror.v1)
                kvList << new KVPair(key: prefix + 'mirroring/mirrors/' + i + '/percent', value: mirror.v2.toString())
            }
        }

        if (weighted) {
            weighted.services.eachWithIndex { Tuple2<String, Integer> service, int i ->
                kvList << new KVPair(key: prefix + 'weighted/services/' + i + '/name', value: service.v1)
                kvList << new KVPair(key: prefix + 'weighted/services/' + i + '/weight', value: service.v2.toString())
            }

            if (weighted.stickyCookie) {
                kvList << new KVPair(key: prefix + 'sticky/cookie', value: 'true')
                kvList << new KVPair(key: prefix + 'sticky/cookie/httponly', value: weighted.stickyCookie.httpOnly.toString())
                kvList << new KVPair(key: prefix + 'sticky/cookie/name', value: weighted.stickyCookie.name)
                kvList << new KVPair(key: prefix + 'sticky/cookie/secure', value: weighted.stickyCookie.secure.toString())
                if (weighted.stickyCookie.samesite) {
                    kvList << new KVPair(key: prefix + 'sticky/cookie/samesite', value: weighted.stickyCookie.samesite)
                }
                if (weighted.stickyCookie.maxAge) {
                    kvList << new KVPair(key: prefix + 'sticky/cookie/maxage', value: weighted.stickyCookie.maxAge.toString())
                }
            }
        }

        kvList
    }
}
