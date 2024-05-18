package server.dns.impl

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import server.dns.DmsDnsAnswerHandler

@CompileStatic
@Slf4j
class ClusterDnsAnswerHandler implements DmsDnsAnswerHandler {
    @Override
    byte[] answer(String domain) {
        null
    }
}
