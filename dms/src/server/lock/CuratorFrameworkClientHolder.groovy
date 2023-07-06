package server.lock

import com.segment.common.Conf
import groovy.util.logging.Slf4j
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

@Singleton
@Slf4j
class CuratorFrameworkClientHolder {
    CuratorFramework client

    void init() {
        def zkConnectString = Conf.instance.get('zk.connectString')
        if (zkConnectString) {
            client = CuratorFrameworkFactory.newClient(zkConnectString,
                    new ExponentialBackoffRetry(1000, 3))
            client.start()
            log.info 'curator client start, zk connect string: {}', zkConnectString
        }
    }

    void close() {
        if (client) {
            client.close()
            log.info 'curator client close'
        }
    }
}
