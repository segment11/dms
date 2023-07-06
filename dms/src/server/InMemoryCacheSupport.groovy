package server

import com.segment.common.job.IntervalJob
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ClusterDTO

@CompileStatic
@Singleton
@Slf4j
class InMemoryCacheSupport extends IntervalJob {
    List<AppDTO> appList = []
    List<ClusterDTO> clusterList = []

    @Override
    String name() {
        'in memory cache load'
    }

    ClusterDTO oneCluster(int clusterId) {
        if (!clusterList) {
            return null
        }
        clusterList.find { it.id == clusterId }
    }

    AppDTO oneApp(int appId) {
        if (!appList) {
            return null
        }
        appList.find { it.id == appId }
    }

    int getClusterIdByAppId(int appId) {
        oneApp(appId).clusterId
    }

    @Override
    void doJob() {
        // cost too much time, todo: use cache
        def beginT = System.currentTimeMillis()
        appList = new AppDTO().noWhere().list()
        def costT = System.currentTimeMillis() - beginT
        log.info 'load app, app size: {}, cost {} ms', appList.size(), costT
        clusterList = new ClusterDTO().noWhere().list()

        if (intervalCount % 10 == 0) {
            log.info 'load app, app size: {}', appList.size()
            log.info 'load cluster, cluster size: {}', clusterList.size()
        }
    }
}
