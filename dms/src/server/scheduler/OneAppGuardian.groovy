package server.scheduler

import common.Conf
import common.Event
import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import model.ClusterDTO
import org.segment.d.json.JsonWriter
import server.AgentCaller
import server.dns.DnsOperator
import server.gateway.GatewayOperator
import server.scheduler.checker.HealthCheckerHolder
import server.scheduler.processor.CreateProcessor
import server.scheduler.processor.GuardianProcessor
import server.scheduler.processor.RemoveProcessor
import server.scheduler.processor.ScrollProcessor
import spi.SpiSupport
import transfer.ContainerInfo
import transfer.ContainerInspectInfo

@CompileStatic
@Slf4j
class OneAppGuardian extends Thread {
    ClusterDTO cluster

    AppDTO app

    List<ContainerInfo> containerList

    private static Map<Integer, GuardianProcessor> processors = [:]

    static {
        processors[AppJobDTO.JobType.create.val] = new CreateProcessor()
        processors[AppJobDTO.JobType.remove.val] = new RemoveProcessor()
        processors[AppJobDTO.JobType.scroll.val] = new ScrollProcessor()
    }

    @Override
    void run() {
        def lock = SpiSupport.createLock()
        lock.lockKey = 'guard ' + app.id
        boolean isDone = lock.exe {
            check()
        }
        if (!isDone) {
            log.info 'get app guard lock fail - {}', app.name
        }
    }

    void check() {
        final int appJobBatchSize = Conf.instance.getInt('guardian.appJobBatchSize', 10)
        final int appJobMaxFailTimes = Conf.instance.getInt('guardian.appJobMaxFailTimes', 3)

        AppJobDTO job
        def jobList = new AppJobDTO(appId: app.id).orderBy('created_date desc').
                loadList(appJobBatchSize)
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
            def isDone = process(job)
            if (!isDone) {
                Guardian.instance.failGuardAppIdSet << app.id
            } else {
                Guardian.instance.failGuardAppIdSet.remove(app.id)
            }
        } else {
            boolean isOk = guard()
            if (!isOk) {
                Guardian.instance.failGuardAppIdSet << app.id
            } else {
                // clear old fail job
                int num = new AppJobDTO().where('app_id=?', app.id).
                        where('status=?', AppJobDTO.Status.failed.val).
                        where('fail_num>=?', appJobMaxFailTimes).deleteAll()
                if (num) {
                    Event.builder().type(Event.Type.app).reason('delete failed job').
                            result(app.id).build().log('delete num - ' + num).toDto().add()
                }

                if (!healthCheck()) {
                    Guardian.instance.failGuardAppIdSet << app.id
                } else {
                    Guardian.instance.failGuardAppIdSet.remove(app.id)
                }
            }
        }
    }

    boolean process(AppJobDTO job, boolean doStopNotRunning = true) {
        def timer = Guardian.instance.jobProcessTimeSummary.
                labels(job.appId.toString(), job.jobType.toString()).startTimer()
        try {
            new AppJobDTO(id: job.id, status: AppJobDTO.Status.processing.val, updatedDate: new Date()).update()
            Event.builder().type(Event.Type.app).reason('process job').
                    result(job.appId).build().log('update job status to processing').toDto().add()

            if (doStopNotRunning) {
                stopNotRunning()
            }

            def app = new AppDTO(id: job.appId).one()
            if (!app) {
                log.warn 'no app found - {}', job.appId
                return false
            }

            def processor = processors[job.jobType]
            processor.process(job, app, containerList)

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

    boolean guard() {
        try {
            stopNotRunning()
            def runningContainerList = containerList.findAll { x -> x.running() }
            def nodeIpList = app.conf.targetNodeIpList ?: containerList.collect { it.nodeIp }

            // reload file mount
            def fileVolumeNeedReloadList = app.conf.fileVolumeList.findAll { it.isReloadInterval }
            if (fileVolumeNeedReloadList) {
                runningContainerList.each { x ->
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
            DnsOperator.refreshContainerDns(cluster, app, runningContainerList)

            def containerNumber = app.conf.containerNumber
            if (containerNumber == runningContainerList.size()) {
                def gatewayConf = app.gatewayConf
                if (!gatewayConf) {
                    return true
                }

                // check gateway
                def operator = GatewayOperator.create(app.id, gatewayConf)
                List<String> runningServerUrlList = runningContainerList.findAll { x ->
                    def p = [id: x.id]
                    def r = AgentCaller.instance.agentScriptExeAs(cluster.id, x.nodeIp,
                            'container inspect', ContainerInspectInfo, p)
                    r.state.running
                }.collect { x ->
                    def publicPort = x.publicPort(gatewayConf.containerPrivatePort)
                    GatewayOperator.scheme(x.nodeIp, publicPort)
                }
                List<String> backendServerUrlList = operator.getBackendServerUrlListFromApi()
                // gateway container not running yet
                if (backendServerUrlList == null) {
                    return true
                }

                (backendServerUrlList - runningServerUrlList).each {
                    operator.removeBackend(it)
                }
                (runningServerUrlList - backendServerUrlList).each {
                    operator.addBackend(it, false)
                }
                return true
            } else {
                log.warn 'app running not match {} but - {} for app - {}', containerNumber,
                        runningContainerList.collect { x -> x.state }, app.name
                if (containerNumber < runningContainerList.size()) {
                    new AppJobDTO(appId: app.id, failNum: 0, status: AppJobDTO.Status.created.val,
                            jobType: AppJobDTO.JobType.remove.val, createdDate: new Date(), updatedDate: new Date()).
                            addParam('toContainerNumber', containerNumber).add()
                } else if (containerNumber > runningContainerList.size()) {
                    List<Integer> needRunInstanceIndexList = []
                    (0..<containerNumber).each { Integer i ->
                        def one = runningContainerList.find { x -> i == x.instanceIndex() }
                        if (!one) {
                            needRunInstanceIndexList << i
                        }
                    }

                    new AppJobDTO(appId: app.id, failNum: 0, status: AppJobDTO.Status.created.val,
                            jobType: AppJobDTO.JobType.create.val, createdDate: new Date(), updatedDate: new Date()).
                            needRunInstanceIndexList(needRunInstanceIndexList).add()
                }
                Event.builder().type(Event.Type.app).reason('change container number').
                        result(app.id).build().log('from - ' + runningContainerList.size() + ' -> ' + containerNumber).toDto().add()
                return false
            }
        } catch (Exception e) {
            log.error('guard app error - ' + app.name, e)
            return false
        }
    }

    private void stopNotRunning() {
        for (x in containerList) {
            if (x.running()) {
                continue
            }

            def id = x.id
            def nodeIp = x.nodeIp

            def p = [id: id]
            if ('created' == x.state || 'exited' == x.state) {
                // do not throw exception
                AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container remove', p) { String body ->
                    log.warn 'remove container fail - {}', body
                }
                log.info 'done remove container - {} - {}', id, app.id
            }
        }
    }

    private boolean healthCheck() {
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
            Event.builder().type(Event.Type.app).reason('health check not ok').
                    result(app.id).build().log('by checker - ' + checker.name()).toDto().add()
            return false
        }
        return true
    }
}
