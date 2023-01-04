package server.dns

import groovy.transform.CompileStatic

@CompileStatic
interface DnsOperator {
    boolean put(String hostname, String ip, int ttl)
}
