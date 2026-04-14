package km

import groovy.transform.CompileStatic
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
@Singleton
class CuratorPoolHolder {

    private static final Logger log = LoggerFactory.getLogger(CuratorPoolHolder.class)

    private Map<String, CuratorFramework> cached = [:]

    synchronized CuratorFramework create(String connectionString) {
        def client = cached[connectionString]
        if (client) {
            return client
        }

        client = CuratorFrameworkFactory.newClient(connectionString,
                new ExponentialBackoffRetry(1000, 3))
        client.start()
        log.info 'curator client started for {}', connectionString
        cached[connectionString] = client
        client
    }

    static <R> R exe(CuratorFramework client, Closure<R> callback) {
        callback.call(client)
    }

    void close() {
        cached.each { String k, CuratorFramework v ->
            log.info 'ready to close curator client - {}', k
            v.close()
            log.info 'done close curator client - {}', k
        }
        cached.clear()
    }
}
