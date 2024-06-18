package server

import com.segment.common.job.IntervalJob
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ClusterDTO
import model.NamespaceDTO

@CompileStatic
@Singleton
@Slf4j
class InMemoryCacheSupport extends IntervalJob {
    List<AppDTO> appList = []
    List<NamespaceDTO> namespaceList = []
    List<ClusterDTO> clusterList = []

    @Override
    String name() {
        'in memory cache load'
    }

    synchronized ClusterDTO oneCluster(int clusterId) {
        if (!clusterList) {
            return null
        }
        clusterList.find { it.id == clusterId }
    }

    synchronized AppDTO oneApp(int appId) {
        if (!appList) {
            return null
        }
        appList.find { it.id == appId }
    }

    synchronized int getClusterIdByAppId(int appId) {
        // null not safe
        def app = oneApp(appId)
        app ? app.clusterId : 0
    }

    synchronized void appUpdated(AppDTO app) {
        def exist = appList.find { it.id == app.id }
        if (exist) {
            if (exist.updatedDate != app.updatedDate) {
                appList.remove(exist)
                appList.add(app)
            }
        } else {
            appList.add(app)
        }
    }

    synchronized void appDeleted(int appId) {
        appList?.removeIf { it.id == appId }
    }

    @Override
    void doJob() {
        synchronized (this) {
            // cost too much time, todo: compare update date / version
            def beginT = System.currentTimeMillis()
            appList = new AppDTO().noWhere().list()
            def costT = System.currentTimeMillis() - beginT
            log.info 'load app, app size: {}, cost {} ms', appList.size(), costT

            namespaceList = new NamespaceDTO().noWhere().list()
            clusterList = new ClusterDTO().noWhere().list()
        }

        if (intervalCount % 10 == 0) {
            log.info 'load app, app size: {}', appList.size()
            log.info 'load namespace, namespace size: {}', namespaceList.size()
            log.info 'load cluster, cluster size: {}', clusterList.size()
        }
    }
}
