package vendor

import com.github.kevinsawicki.http.HttpRequest
import common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import server.dns.DnsOperator

@CompileStatic
@Slf4j
class RemoteDnsOperator implements DnsOperator {

    boolean put(String hostname, String ip, int ttl) {
        def c = Conf.instance
        def url = c.get('dns.opt.url')
        if (!url) {
            log.error('put dns record error - ' + hostname + ':' + ip + ' - no remote url given')
            // ignore
            return true
        }

        def req = HttpRequest.post(url)
                .connectTimeout(c.getInt('dns.opt.connectTimeout', 1000))
                .readTimeout(c.getInt('dns.opt.readTimeout', 1000))
                .form([hostname: hostname, ip: ip, ttl: ttl])
        if (200 != req.code()) {
            log.error('put dns record error - ' + hostname + ':' + ip + ' - ' + req.body())
            return false
        }
        true
    }

}
