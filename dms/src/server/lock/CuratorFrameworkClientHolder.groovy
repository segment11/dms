package server.lock

import org.apache.curator.framework.CuratorFramework

@Singleton
class CuratorFrameworkClientHolder {
    CuratorFramework client
}
