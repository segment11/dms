package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class GwLoadBalancer {
    // only round robin load balancing is supported
    List<String> serverUrlList = []
    boolean passHostHeader
    String serversTransport
    GwService.HealthCheck healthCheck
    GwService.StickyCookie stickyCookie

    List<KVPair> toKVList() {
        // refer to https://doc.traefik.io/traefik/routing/services/
        // https://doc.traefik.io/traefik/routing/providers/kv/
        // service prefix will be added in GwService.toKVList()
        final String prefix = 'loadbalancer/'

        List<KVPair> kvList = []
        serverUrlList.eachWithIndex { String serverUrl, int i ->
            kvList << new KVPair(key: prefix + 'servers/' + i + '/url', value: serverUrl)
        }

        kvList << new KVPair(key: prefix + 'passhostheader', value: passHostHeader.toString())
        kvList << new KVPair(key: prefix + 'serverstransport', value: serversTransport)

        if (healthCheck) {
            if (healthCheck.hostname) {
                kvList << new KVPair(key: prefix + 'healthcheck/hostname', value: healthCheck.hostname)
            }
            if (healthCheck.interval) {
                kvList << new KVPair(key: prefix + 'healthcheck/interval', value: healthCheck.interval.toString())
            }
            if (healthCheck.path) {
                kvList << new KVPair(key: prefix + 'healthcheck/path', value: healthCheck.path)
            }
            if (healthCheck.method) {
                kvList << new KVPair(key: prefix + 'healthcheck/method', value: healthCheck.method)
            }
            if (healthCheck.status) {
                kvList << new KVPair(key: prefix + 'healthcheck/status', value: healthCheck.status.toString())
            }
            if (healthCheck.port) {
                kvList << new KVPair(key: prefix + 'healthcheck/port', value: healthCheck.port.toString())
            }
            if (healthCheck.scheme) {
                kvList << new KVPair(key: prefix + 'healthcheck/scheme', value: healthCheck.scheme)
            }
            if (healthCheck.timeout) {
                kvList << new KVPair(key: prefix + 'healthcheck/timeout', value: healthCheck.timeout.toString())
            }
        }

        if (stickyCookie) {
            kvList << new KVPair(key: prefix + 'sticky/cookie', value: 'true')
            kvList << new KVPair(key: prefix + 'sticky/cookie/httponly', value: stickyCookie.httpOnly.toString())
            kvList << new KVPair(key: prefix + 'sticky/cookie/name', value: stickyCookie.name)
            kvList << new KVPair(key: prefix + 'sticky/cookie/secure', value: stickyCookie.secure.toString())
            if (stickyCookie.samesite) {
                kvList << new KVPair(key: prefix + 'sticky/cookie/samesite', value: stickyCookie.samesite)
            }
            if (stickyCookie.maxAge) {
                kvList << new KVPair(key: prefix + 'sticky/cookie/maxage', value: stickyCookie.maxAge.toString())
            }
        }

        kvList
    }
}
