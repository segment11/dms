package server.scheduler

import common.*
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import model.AppDTO
import model.AppJobDTO
import model.ClusterDTO
import org.segment.d.json.JsonWriter
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.gateway.GatewayOperator
import server.scheduler.checker.HealthCheckerHolder
import server.scheduler.processor.CreateProcessor
import server.scheduler.processor.GuardianProcessor
import server.scheduler.processor.RemoveProcessor
import server.scheduler.processor.ScrollProcessor
import spi.SpiSupport
import transfer.ContainerInfo
import transfer.ContainerInspectInfo

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static common.ContainerHelper.generateContainerHostname

@CompileStatic
@Singleton
@Slf4j
class Guardian extends IntervalJob {
    @Override
    String name() {
        'scheduler guardian'
    }

    @Override
    void doJob() {
        if (!InMemoryCacheSupport.instance.isLeader) {
            log.warn 'i am not the leader'
            return
        }
        if (!isRunning) {
            log.warn 'guard not running'
            return
        }

        EventCleaner.instance.clearOld(intervalCount, interval)
        isCronJobRefreshDone = CronJobRunner.instance.refresh()

        failHealthCheckAgentNodeIpListCopy.clear()
        failGuardAppIdListCopy.clear()
        failAppJobIdListCopy.clear()

        log.info 'i am alive'

        def clusterList = new ClusterDTO().noWhere().loadList()
        for (cluster in clusterList) {
            if (!cluster.isInGuard) {
                continue
            }

            agentHealthCheck(cluster.id)

            def appList = new AppDTO().where('cluster_id = ?', cluster.id).
                    where('status = ?', AppDTO.Status.auto.val).loadList()
            if (intervalCount % 10 == 0) {
                log.info 'begin check cluster app - {} for cluster - {} - interval count - {}',
                        appList.collect { it.name }, cluster.name, intervalCount
            }

            List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(cluster.id)
            List<AppDTO> needCheckAppList = []
            for (app in appList) {
                if (!app.autoManage()) {
                    log.debug 'skip guard app - {}', app.name
                    continue
                }

                if (app.jobConf && app.jobConf.cronExp) {
                    log.debug 'skip guard app as it is cron job - {}', app.name
                    continue
                }
                needCheckAppList << app
            }

            if (!needCheckAppList) {
                continue
            }

            def latch = new CountDownLatch(needCheckAppList.size())
            for (app in needCheckAppList) {
                try {
                    def thisAppContainerList = containerList.findAll { x -> app.id == x.appId() }
                    def lock = SpiSupport.createLock()
                    lock.lockKey = 'guard ' + app.id
                    boolean isDone = lock.exe {
                        checkApp(cluster, app, thisAppContainerList)
                    }
                    if (!isDone) {
                        log.info 'get app guard lock fail - {}', app.name
                    }
                } finally {
                    latch.countDown()
                }
            }
            latch.await()
        }

        failHealthCheckAgentNodeIpList.clear()
        failHealthCheckAgentNodeIpList.addAll(failHealthCheckAgentNodeIpListCopy)
        failNodeStatusNumber.labels('0').set(failHealthCheckAgentNodeIpListCopy.size() as double)

        failGuardAppIdList.clear()
        failGuardAppIdList.addAll(failGuardAppIdListCopy)
        failAppStatusNumber.labels('0').set(failGuardAppIdListCopy.size() as double)

        failAppJobIdList.clear()
        failAppJobIdList.addAll(failAppJobIdListCopy)
        failAppJobNumber.labels('0').set(failAppJobIdListCopy.size() as double)
    }

    private void agentHealthCheck(int clusterId) {
        def instance = InMemoryAllContainerManager.instance
        def nodeList = instance.getHeartBeatOkNodeList(clusterId)

        nodeNumber.labels(clusterId.toString()).set(nodeList.size() as double)

        if (!nodeList) {
            return
        }

        for (one in nodeList) {
            def nodeInfo = instance.getNodeInfo(one.ip)
            if (!nodeInfo.isLiveCheckOk) {
                failHealthCheckAgentNodeIpListCopy << one.ip
                log.warn 'agent live check fail - {}', one.ip
            }
        }
    }

    @Override
    void stop() {
        super.stop()
        processPool.shutdown()
        Event.builder().type(Event.Type.cluster).reason('guard process pool stopped').
                result(Utils.localIp()).build().log().toDto().add()
        CronJobRunner.instance.stop()
        isRunning = false
    }

    private static Map<Integer, GuardianProcessor> processors = [:]

    static {
        processors[AppJobDTO.JobType.create.val] = new CreateProcessor()
        processors[AppJobDTO.JobType.remove.val] = new RemoveProcessor()
        processors[AppJobDTO.JobType.scroll.val] = new ScrollProcessor()
    }

    private ExecutorService processPool = Executors.newFixedThreadPool(
            Conf.instance.getInt('guardian.processThreadNumber', 10),
            new NamedThreadFactory('guard-process'))

    Set<Integer> failGuardAppIdList = []
    private Set<Integer> failGuardAppIdListCopy = []

    boolean isHealth(Integer appId) {
        !failGuardAppIdList.contains(appId)
    }

    Set<Integer> failAppJobIdList = []
    private Set<Integer> failAppJobIdListCopy = []

    Set<String> failHealthCheckAgentNodeIpList = []
    private Set<String> failHealthCheckAgentNodeIpListCopy = []

    private Gauge failAppJobNumber = Gauge.build().name('fail_app_job_number').
            help('cluster run application jobs failed number').labelNames('cluster_id').register()

    private Gauge failAppStatusNumber = Gauge.build().name('fail_app_status_number').
            help('cluster check applications failed number').labelNames('cluster_id').register()

    private Gauge nodeNumber = Gauge.build().name('node_number').
            help('cluster node number').labelNames('cluster_id').register()

    private Gauge failNodeStatusNumber = Gauge.build().name('fail_node_status_number').
            help('cluster check nodes failed number').labelNames('cluster_id').register()

    boolean isCronJobRefreshDone = true

    private Summary jobProcessTimeSummary = Summary.build().name('job_process_time').
            help('job process time cost').
            labelNames('app_id', 'job_type').
            quantile(0.5.doubleValue(), 0.05.doubleValue()).
            quantile(0.9.doubleValue(), 0.01.doubleValue()).register()

    volatile boolean isRunning = false

    private void stopNotRunning(Integer appId, List<ContainerInfo> containerList) {
        def clusterId = InMemoryCacheSupport.instance.getClusterIdByAppId(appId)
        containerList.findAll { x ->
            !x.running()
        }.each { x ->
            def id = x.id
            def nodeIp = x.nodeIp

            def p = [id: id]
            if ('created' == x.state || 'exited' == x.state) {
                AgentCaller.instance.agentScriptExe(clusterId, nodeIp, 'container remove', p) { String body ->
                    log.warn 'remove container fail - {}', body
                }
                log.info 'done remove container - {} - {}', id, appId
            }
        }
    }

    boolean guard(ClusterDTO cluster, AppDTO app, List<ContainerInfo> containerList) {
        try {
            stopNotRunning(app.id, containerList)
            def list = containerList.findAll { x -> x.running() }
            def nodeIpList = containerList.collect { it.nodeIp }

            // reload file mount
            def fileVolumeNeedReloadList = app.conf.fileVolumeList.findAll { it.isReloadInterval }
            if (fileVolumeNeedReloadList) {
                list.each { x ->
                    def containerId = x.id
                    def instanceIndex = x.instanceIndex()
                    def nodeIp = x.nodeIp

                    def createContainerConf = CreateProcessor.prepareCreateContainerConf(app.conf, app, instanceIndex,
                            nodeIp, nodeIpList)

                    def createP = [jsonStr: JsonWriter.instance.json(createContainerConf), containerId: containerId]
                    def r = AgentCaller.instance.agentScriptExe(cluster.id, nodeIp, 'container file volume reload', createP)
                    Event.builder().type(Event.Type.app).reason('container file volume reload').
                            result(app.id).build().log(fileVolumeNeedReloadList.collect { it.dist }.join(',') + ' - ' + r.toString()).
                            toDto().add()
                }
            }

            // refresh dns
            if (Conf.instance.isOn('interval.refreshDns')) {
                def dnsOperator = SpiSupport.createDnsOperator()
                def dnsTtl = Conf.instance.getInt('dnsTtl', 3600)
                for (x in list) {
                    dnsOperator.put(generateContainerHostname(app.id, x.instanceIndex()), x.nodeIp, dnsTtl)
                }
            }

            def containerNumber = app.conf.containerNumber
            if (containerNumber == list.size()) {
                def gatewayConf = app.gatewayConf
                if (!gatewayConf) {
                    return true
                }

                // check gateway
                def operator = GatewayOperator.create(app.id, gatewayConf)
                List<String> runningServerUrlList = list.findAll { x ->
                    def p = [id: x.id]
                    def r = AgentCaller.instance.agentScriptExeAs(cluster.id, x.nodeIp,
                            'container inspect', ContainerInspectInfo, p)
                    r.state.running
                }.collect { x ->
                    def publicPort = x.publicPort(gatewayConf.containerPrivatePort)
                    GatewayOperator.scheme(x.nodeIp, publicPort)
                }
                List<String> backendServerUrlList = operator.getBackendServerUrlListFromApi()
                (backendServerUrlList - runningServerUrlList).each {
                    operator.removeBackend(it)
                }
                (runningServerUrlList - backendServerUrlList).each {
                    operator.addBackend(it, false)
                }
                return true
            } else {
                log.warn 'app running not match {} but - {} for app - {}', containerNumber,
                        list.collect { x -> x.state }, app.name
                if (containerNumber < list.size()) {
                    new AppJobDTO(appId: app.id, failNum: 0, status: AppJobDTO.Status.created.val,
                            jobType: AppJobDTO.JobType.remove.val, createdDate: new Date(), updatedDate: new Date()).
                            addParam('toContainerNumber', containerNumber).add()
                } else if (containerNumber > list.size()) {
                    List<Integer> needRunInstanceIndexList = []
                    (0..<containerNumber).each { Integer i ->
                        def one = list.find { x -> i == x.instanceIndex() }
                        if (!one) {
                            needRunInstanceIndexList << i
                        }
                    }

                    new AppJobDTO(appId: app.id, failNum: 0, status: AppJobDTO.Status.created.val,
                            jobType: AppJobDTO.JobType.create.val, createdDate: new Date(), updatedDate: new Date()).
                            needRunInstanceIndexList(needRunInstanceIndexList).add()
                }
                Event.builder().type(Event.Type.app).reason('change container number').
                        result(app.id).build().log('from - ' + list.size() + ' -> ' + containerNumber).toDto().add()
                return false
            }
        } catch (Exception e) {
            log.error('guard app error - ' + app.name, e)
            return false
        }
    }

    boolean process(AppJobDTO job, List<ContainerInfo> containerList, boolean doStopNotRunning = true) {
        def timer = jobProcessTimeSummary.labels(job.appId.toString(), job.jobType.toString()).startTimer()
        try {
            new AppJobDTO(id: job.id, status: AppJobDTO.Status.processing.val, updatedDate: new Date()).update()
            Event.builder().type(Event.Type.app).reason('process job').
                    result(job.appId).build().log('update job status to processing').toDto().add()

            if (doStopNotRunning) {
                stopNotRunning(job.appId, containerList)
            }

            def thisAppContainerList = containerList.sort { x -> x.appId() }
            def app = new AppDTO(id: job.appId).one()
            if (!app) {
                log.warn 'no app found - {}', job.appId
                return false
            }

            def processor = processors[job.jobType]
            processor.process(job, app, thisAppContainerList)

            new AppJobDTO(id: job.id, status: AppJobDTO.Status.done.val, message: '', updatedDate: new Date()).update()
            Event.builder().type(Event.Type.app).reason('process job').
                    result(job.appId).build().log('update job status to done').toDto().add()
            return true
        } catch (Exception e) {
            log.error('process app job error - ' + job.appId, e)
            new AppJobDTO(id: job.id, status: AppJobDTO.Status.failed.val, message: Utils.getStackTraceString(e),
                    failNum: job.failNum + 1, updatedDate: new Date()).update()
            Event.builder().type(Event.Type.app).reason('process job').
                    result(job.appId).build().log('update job status to failed').toDto().add()
            return false
        } finally {
            timer.observeDuration()
        }
    }

    void checkApp(ClusterDTO cluster, AppDTO app, List<ContainerInfo> containerList) {
        final int appJobBatchSize = Conf.instance.getInt('guardian.appJobBatchSize', 10)
        final int appJobMaxFailTimes = Conf.instance.getInt('guardian.appJobMaxFailTimes', 100)

        AppJobDTO job
        def jobList = new AppJobDTO(appId: app.id).orderBy('created_date desc').
                loadList(appJobBatchSize) as List<AppJobDTO>
        if (jobList) {
            def uniqueJobList = jobList.unique { it.status }
            def todoJob = uniqueJobList.find {
                it.status != AppJobDTO.Status.failed.val && it.status != AppJobDTO.Status.done.val
            }
            if (todoJob) {
                job = todoJob
            } else {
                def failJob = uniqueJobList.find { it.status == AppJobDTO.Status.failed.val }
                if (failJob) {
                    if (failJob.failNum != null && failJob.failNum >= appJobMaxFailTimes) {
                        // go on check
                    } else {
                        job = failJob
                    }
                }
            }
        }

        if (job) {
            def isDone = process(job, containerList)
            if (!isDone) {
                failAppJobIdListCopy << app.id
            }
        } else {
            boolean isOk = guard(cluster, app, containerList)
            if (!isOk) {
                failGuardAppIdListCopy << app.id
            } else {
                // clear old fail job
                int num = new AppJobDTO().where('app_id=?', app.id).
                        where('status=?', AppJobDTO.Status.failed.val).
                        where('fail_num>=?', appJobMaxFailTimes).deleteAll()
                if (num) {
                    Event.builder().type(Event.Type.app).reason('delete failed job').
                            result(app.id).build().log('delete num - ' + num).toDto().add()
                }

                boolean isHealth = healthCheck(app)
                if (!isHealth) {
                    failGuardAppIdListCopy << app.id
                }
            }
        }
    }

    private boolean healthCheck(AppDTO app) {
        // user defined application health check
        String imageName = app.conf.group + '/' + app.conf.image
        def checkerList = HealthCheckerHolder.instance.checkerList.findAll { it.imageName() == imageName }
        if (!checkerList) {
            return true
        }
        for (checker in checkerList) {
            if (checker.check(app)) {
                continue
            }
            failGuardAppIdListCopy << app.id
            Event.builder().type(Event.Type.app).reason('health check not ok').
                    result(app.id).build().log('by checker - ' + checker.name()).toDto().add()
            return false
        }
        return true
    }
}
