package server.scheduler

import com.segment.common.job.IntervalJob
import com.segment.common.job.leader.LeaderFlagHolder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.prometheus.client.Summary
import model.AppDTO
import model.ClusterDTO
import server.InMemoryAllContainerManager

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
@Slf4j
class Guardian extends IntervalJob {
    @Override
    String name() {
        'scheduler guardian'
    }

    // one application one thread
    private ConcurrentHashMap<Integer, OneAppGuardian> appGuardianByAppId = new ConcurrentHashMap<>()

    OneAppGuardian oneAppGuardian(Integer appId) {
        appGuardianByAppId[appId]
    }

    Set<Integer> failGuardAppIdSet = []

    boolean isHealth(Integer appId) {
        !failGuardAppIdSet.contains(appId)
    }

    Set<String> failHealthCheckAgentNodeIpSet = []
    private Set<String> failHealthCheckAgentNodeIpSetCopy = []

    boolean isCronJobRefreshDone = true

    volatile boolean isRunning = false

    Summary jobProcessTimeSummary = Summary.build().name('job_process_time').
            help('job process time cost').
            labelNames('app_id', 'job_type').
            quantile(0.5.doubleValue(), 0.05.doubleValue()).
            quantile(0.9.doubleValue(), 0.01.doubleValue()).register()

    private void agentHealthCheck(int clusterId) {
        def instance = InMemoryAllContainerManager.instance
        def nodeList = instance.getHeartBeatOkNodeList(clusterId)

        if (!nodeList) {
            return
        }

        for (one in nodeList) {
            def nodeInfo = instance.getNodeInfo(one.ip)
            if (!nodeInfo.isLiveCheckOk) {
                failHealthCheckAgentNodeIpSetCopy << one.ip
                log.warn 'agent live check fail - {}', one.ip
            }
        }
    }

    @Override
    void doJob() {
        if (!LeaderFlagHolder.instance.isLeader) {
            log.warn 'i am not the leader'
            return
        }
        if (!isRunning) {
            log.warn 'guard not running'
            return
        }

        EventCleaner.instance.clearOld(intervalCount, interval)
        isCronJobRefreshDone = CronJobRunner.instance.refresh()

        log.info 'i am alive'

        def clusterList = new ClusterDTO().noWhere().list()

        // node check health
        for (cluster in clusterList) {
            if (!cluster.isInGuard) {
                continue
            }
            agentHealthCheck(cluster.id)
        }
        failHealthCheckAgentNodeIpSet.clear()
        failHealthCheckAgentNodeIpSet.addAll(failHealthCheckAgentNodeIpSetCopy)

        // app check
        for (cluster in clusterList) {
            if (!cluster.isInGuard) {
                continue
            }

            def appList = new AppDTO().where('cluster_id = ?', cluster.id).
                    where('status = ?', AppDTO.Status.auto.val).list()
            if (intervalCount % 10 == 0) {
                log.info 'begin check cluster app - {} for cluster - {} - interval count - {}',
                        appList.collect { it.name }, cluster.name, intervalCount
            }

            List<AppDTO> needCheckAppList = []
            for (app in appList) {
                if (app.jobConf && app.jobConf.cronExp) {
                    log.debug 'skip guard app as it is cron job - {}', app.name
                    continue
                }
                needCheckAppList << app
            }

            if (!needCheckAppList) {
                continue
            }

            for (app in needCheckAppList) {
                def appId = app.id

                def oneAppGuardian = new OneAppGuardian()
                oneAppGuardian.cluster = cluster
                oneAppGuardian.app = app
                oneAppGuardian.containerList = InMemoryAllContainerManager.instance.getContainerList(cluster.id, appId)

                def old = appGuardianByAppId.putIfAbsent(appId, oneAppGuardian)
                if (old == null) {
                    oneAppGuardian.init()
                    oneAppGuardian.start()
                } else {
                    old.cluster = cluster
                    old.app = app
                    old.containerList = oneAppGuardian.containerList
                    old.init()
                    old.start()
                }
            }
        }
    }

    @Override
    void stop() {
        super.stop()
        isRunning = false
        CronJobRunner.instance.stop()

        stopRunning()
    }

    void stopRunning() {
        for (entry in appGuardianByAppId) {
            if (entry.value) {
                entry.value.shutdown()
            }
        }
    }

    void stopOneRunning(int appId) {
        appGuardianByAppId[appId]?.shutdown()
    }
}
