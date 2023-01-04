package server.gateway

import com.github.zkclient.ZkClient
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Singleton
@Slf4j
class ZkClientHolder {
    private Map<String, ZkClient> cached = [:]

    synchronized ZkClient create(String connectString, int sessionTimeout = 1000 * 30,
                                 int connectionTimeout = 1000 * 10) {
        def client = cached[connectString]
        if (client) {
            return client
        }

        def one = new ZkClient(connectString, sessionTimeout, connectionTimeout)
        log.info 'connected - {}', connectString
        cached[connectString] = one
        one
    }

    void close() {
        cached.each { k, v ->
            log.info 'ready to close zk client - {}', k
            v.close()
            log.info 'done close zk client - {}', k
        }
    }
}
