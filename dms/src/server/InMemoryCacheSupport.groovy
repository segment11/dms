package server

import common.Conf
import common.Event
import common.IntervalJob
import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ha.EtcdLeaderChecker
import ha.LeaderChecker
import model.AppDTO
import model.ClusterDTO

@CompileStatic
@Singleton
@Slf4j
class InMemoryCacheSupport extends IntervalJob {
    List<AppDTO> appList = []
    List<ClusterDTO> clusterList = []

    private LeaderChecker leaderChecker

    boolean isLeader = false

    void init() {
        def c = Conf.instance
        def checkEtcdAddr = c.get('leader.checkEtcdAddr')
        if (checkEtcdAddr) {
            int times = c.getInt('leader.checkTtlLostTimes', 3)
            leaderChecker = new EtcdLeaderChecker(checkEtcdAddr, interval * times)
        }
    }

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
        if (leaderChecker) {
            if (isLeader) {
                leaderChecker.continueLeader()
                Event.builder().type(Event.Type.cluster).reason('continue leader').
                        result(Utils.localIp()).build().log().toDto().add()
            } else {
                boolean isLeaderNew = leaderChecker.isLeader()
                if (isLeaderNew != isLeader) {
                    Event.builder().type(Event.Type.cluster).reason('change leader').
                            result(Utils.localIp()).build().log('i am the leader? ' + isLeaderNew).toDto().add()
                }
                isLeader = isLeaderNew
            }
        } else {
            if (!isLeader) {
                log.info 'i am the leader now'
            }
            isLeader = true
        }

        appList = new AppDTO().noWhere().
                queryFields('id,name,namespaceId,clusterId,status,conf,monitorConf,liveCheckConf').loadList()
        clusterList = new ClusterDTO().noWhere().loadList()

        if (intervalCount % 10 == 0) {
            log.info 'load app, app size: {}', appList.size()
            log.info 'load cluster, cluster size: {}', clusterList.size()
        }
    }
}
