package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class GwLoadBalancer {
    // for web front bind
    @CompileStatic
    @ToString
    static class ServerUrl {
        String url
        Integer weight
    }

    // only round robin load balancing is supported
    List<ServerUrl> serverUrlList = []
    boolean passHostHeader
    String serversTransport
    GwService.HealthCheck healthCheck
    GwService.StickyCookie stickyCookie

    List<KVPair<String>> toKVList() {
        // refer to https://doc.traefik.io/traefik/routing/services/
        // https://doc.traefik.io/traefik/routing/providers/kv/
        // service prefix will be added in GwService.toKVList()
        final String prefix = 'loadbalancer/'

        List<KVPair<String>> kvList = []
        serverUrlList.eachWithIndex { ServerUrl serverUrl, int i ->
            kvList << new KVPair(prefix + 'servers/' + i + '/url', serverUrl.url)
//            kvList << new KVPair(prefix + 'servers/' + i + '/weight', serverUrl.weight)
        }

        kvList << new KVPair(prefix + 'passhostheader', passHostHeader.toString())
        kvList << new KVPair(prefix + 'serverstransport', serversTransport)

        if (healthCheck) {
            if (healthCheck.hostname) {
                kvList << new KVPair(prefix + 'healthcheck/hostname', healthCheck.hostname)
            }
            if (healthCheck.interval) {
                kvList << new KVPair(prefix + 'healthcheck/interval', healthCheck.interval.toString())
            }
            if (healthCheck.path) {
                kvList << new KVPair(prefix + 'healthcheck/path', healthCheck.path)
            }
            if (healthCheck.method) {
                kvList << new KVPair(prefix + 'healthcheck/method', healthCheck.method)
            }
            if (healthCheck.status) {
                kvList << new KVPair(prefix + 'healthcheck/status', healthCheck.status.toString())
            }
            if (healthCheck.port) {
                kvList << new KVPair(prefix + 'healthcheck/port', healthCheck.port.toString())
            }
            if (healthCheck.scheme) {
                kvList << new KVPair(prefix + 'healthcheck/scheme', healthCheck.scheme)
            }
            if (healthCheck.timeout) {
                kvList << new KVPair(prefix + 'healthcheck/timeout', healthCheck.timeout.toString())
            }
            if (healthCheck.headers) {
                healthCheck.headers.each { String header, String value ->
                    kvList << new KVPair(prefix + 'healthcheck/headers/' + header, value)
                }
            }
        }

        if (stickyCookie) {
            kvList << new KVPair(prefix + 'sticky/cookie', 'true')
            kvList << new KVPair(prefix + 'sticky/cookie/httponly', stickyCookie.httpOnly.toString())
            kvList << new KVPair(prefix + 'sticky/cookie/name', stickyCookie.name)
            kvList << new KVPair(prefix + 'sticky/cookie/secure', stickyCookie.secure.toString())
            if (stickyCookie.samesite) {
                kvList << new KVPair(prefix + 'sticky/cookie/samesite', stickyCookie.samesite)
            }
            if (stickyCookie.maxAge) {
                kvList << new KVPair(prefix + 'sticky/cookie/maxage', stickyCookie.maxAge.toString())
            }
        }

        kvList
    }
}
