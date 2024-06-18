package server.dns

import groovy.transform.CompileStatic
import io.netty.handler.codec.dns.DnsRecord

@CompileStatic
interface DmsDnsAnswerHandler {
    List<DnsRecord> answer(DnsRecord dnsQuestion, int ttl)
}
