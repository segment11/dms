package server.scheduler.processor

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import common.Conf
import common.ContainerHelper
import common.Event
import common.Utils
import ex.JobProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.*
import model.json.AppConf
import model.json.ContainerResourceAsk
import model.server.CreateContainerConf
import org.segment.d.json.JsonWriter
import org.segment.web.common.CachedGroovyClassLoader
import plugin.Plugin
import plugin.PluginManager
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.gateway.GatewayOperator
import server.scheduler.Guardian
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import transfer.ContainerConfigInfo
import transfer.ContainerInfo
import transfer.ContainerInspectInfo
import transfer.NodeInfo

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

@CompileStatic
@Slf4j
class CreateProcessor implements GuardianProcessor {
    @Override
    void process(AppJobDTO job, AppDTO app, List<ContainerInfo> containerList) {
        def conf = app.conf
        int containerNumber = conf.containerNumber
        List<Integer> needRunInstanceIndexList = job.runInstanceIndexList()
        List<Integer> instanceIndexList = []
        if (needRunInstanceIndexList != null) {
            instanceIndexList.addAll(needRunInstanceIndexList)
        } else {
            (0..<containerNumber).each { Integer i ->
                instanceIndexList << i
            }
        }

        def runNumber = instanceIndexList.max { it } + 1
        List<String> nodeIpList = []
        def targetNodeIpList = conf.targetNodeIpList
        if (targetNodeIpList) {
            if (targetNodeIpList.size() < runNumber) {
                if (!app.conf.isLimitNode) {
                    throw new JobProcessException('node not ok - ' + runNumber + ' but available node number - ' + targetNodeIpList.size())
                } else {
                    (runNumber - targetNodeIpList.size()).times { i ->
                        targetNodeIpList << targetNodeIpList[i]
                    }
                }
            }
            instanceIndexList.each { i ->
                nodeIpList << targetNodeIpList[i]
            }
        } else {
            nodeIpList.addAll(chooseNodeList(app.clusterId, app.id, runNumber, conf))
        }
        log.info 'choose node - {} for app {}, {}', nodeIpList, app.id, instanceIndexList
        def nodeIpListCopy = targetNodeIpList ?: nodeIpList

        if (!conf.isParallel) {
            nodeIpList.eachWithIndex { String nodeIp, int i ->
                def instanceIndex = instanceIndexList[i]
                def confCopy = conf.copy()
                def abConf = app.abConf
                if (abConf) {
                    if (instanceIndex < abConf.containerNumber) {
                        confCopy.image = abConf.image
                        confCopy.tag = abConf.tag
                    }
                }

                def result = startOneContainer(app, job.id, instanceIndex, nodeIpListCopy, nodeIp, confCopy)
                if (result) {
                    addToGateway(result, instanceIndex, app)
                }
            }
            return
        }

        ConcurrentHashMap<Integer, Exception> exceptionByInstanceIndex = new ConcurrentHashMap<>()
        def latch = new CountDownLatch(nodeIpList.size())
        nodeIpList.eachWithIndex { String nodeIp, int i ->
            Integer instanceIndex = instanceIndexList[i]
            AppConf confCopy = conf.copy()
            def abConf = app.abConf
            if (abConf) {
                if (instanceIndex < abConf.containerNumber) {
                    confCopy.image = abConf.image
                    confCopy.tag = abConf.tag
                }
            }

            Thread.start {
                try {
                    def result = startOneContainer(app, job.id, instanceIndex, nodeIpListCopy, nodeIp, confCopy)
                    if (result) {
                        addToGateway(result, instanceIndex, app)
                    }
                } catch (Exception e) {
                    log.error('start one container error - ' + confCopy.image + ' - ' + instanceIndex + ' - ' + nodeIp, e)
                    exceptionByInstanceIndex[instanceIndex] = e
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        if (exceptionByInstanceIndex) {
            throw new JobProcessException(exceptionByInstanceIndex.collect {
                "${it.key} - " + Utils.getStackTraceString(it.value)
            }.join("\r\n"))
        }
    }

    protected void addToGateway(ContainerRunResult result, int instanceIndex, AppDTO app) {
        def gatewayConf = app.gatewayConf
        if (gatewayConf) {
            def abConf = app.abConf
            int w = abConf && instanceIndex < abConf.containerNumber ? abConf.weight :
                    GatewayOperator.DEFAULT_WEIGHT
            result.extract(gatewayConf)

            def isAddOk = GatewayOperator.create(app.id, gatewayConf).addBackend(result, w)
            String message = "add to cluster ${gatewayConf.clusterId} frontend ${gatewayConf.frontendId}".toString()
            result.keeper.next(JobStepKeeper.Step.addToGateway, 'add real server to gateway', message, isAddOk)
            result.keeper.next(JobStepKeeper.Step.done, 'job finished', '', isAddOk)
        } else {
            result.keeper.next(JobStepKeeper.Step.done, 'job finished')
        }
    }

    protected List<NodeDTO> chooseNodeList(int clusterId, List<String> excludeNodeTagList,
                                           List<String> excludeNodeIpList, List<String> targetNodeTagList) {
        def nodeList = InMemoryAllContainerManager.instance.getHeartBeatOkNodeList(clusterId)
        if (!nodeList) {
            throw new JobProcessException('node not ready')
        }

        List<NodeDTO> list = nodeList
        if (excludeNodeTagList) {
            list = list.findAll {
                if (!it.tags) {
                    return true
                }
                !it.tags.split(',').any { tag -> tag in excludeNodeTagList }
            }
        }
        if (targetNodeTagList) {
            list = list.findAll {
                if (!it.tags) {
                    return false
                }
                it.tags.split(',').any { tag -> tag in targetNodeTagList }
            }
        }
        if (excludeNodeIpList) {
            list = list.findAll {
                !(it.ip in excludeNodeIpList)
            }
        }
        list
    }

    protected List<String> chooseNodeList(int clusterId, int appId, int runNumber, AppConf conf,
                                          List<String> excludeNodeIpList = null) {
        def nodeList = chooseNodeList(clusterId, conf.excludeNodeTagList, excludeNodeIpList, conf.targetNodeTagList)

        List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(clusterId)
        Map<String, List<ContainerInfo>> groupByNodeIp = containerList.groupBy { x ->
            x.nodeIp
        }

        // exclude node that has this app's running container
        def list = nodeList.findAll {
            def subList = groupByNodeIp[it.ip]
            if (!subList) {
                return true
            }
            if (conf.isLimitNode) {
                return true
            }

            def isThisNodeIncludeThisAppContainer = subList.any { x ->
                appId == x.appId() && x.running()
            }
            !isThisNodeIncludeThisAppContainer
        }
        if (list.size() < runNumber) {
            if (conf.isLimitNode) {
                (runNumber - list.size()).times {
                    list << list[-1]
                }
            } else {
                throw new JobProcessException('node not enough - ' + runNumber + ' but only have node available - ' + list.size())
            }
        }

        Map<Integer, AppDTO> otherAppCached = [:]
        List<ContainerResourceAsk> leftResourceList = []

        Map<String, NodeInfo> allNodeInfo = InMemoryAllContainerManager.instance.getAllNodeInfo(clusterId)
        for (node in list) {
            def nodeInfo = allNodeInfo[node.ip]
            int cpuNumber = nodeInfo.cpuNumber()
            int memMBTotal = nodeInfo.mem.total.intValue()

            def subList = groupByNodeIp[node.ip]
            List<ContainerResourceAsk> otherAppResourceAskList = subList ? subList.collect { x ->
                def otherAppId = x.appId()
                def otherApp = otherAppCached[otherAppId]
                if (!otherApp) {
                    def appOne = new AppDTO(id: otherAppId).queryFields('conf').one()
                    if (!appOne) {
                        throw new JobProcessException('app not define for id - ' + otherAppId)
                    }
                    otherAppCached[otherAppId] = appOne
                    return new ContainerResourceAsk(appOne.conf)
                } else {
                    return new ContainerResourceAsk(otherApp.conf)
                }
            } : [] as List<ContainerResourceAsk>

            int memMBUsed = 0
            int cpuShareUsed = 0
            double cpuFixedUsed = 0
            for (one in otherAppResourceAskList) {
                memMBUsed += one.memMB
                cpuShareUsed += one.cpuShare
                cpuFixedUsed += one.cpuFixed
            }

            def leftResource = new ContainerResourceAsk(nodeIp: node.ip,
                    memMB: memMBTotal - memMBUsed,
                    cpuShare: cpuNumber * 1024 - cpuShareUsed,
                    cpuFixed: cpuNumber - cpuFixedUsed)
            if (leftResource.memMB < conf.memMB) {
                log.warn 'mem left no enough for ' + conf.memMB + ' but left ' + leftResource.memMB + ' on ' + node.ip
            } else {
                leftResourceList << leftResource
            }
        }
        if (leftResourceList.size() < runNumber) {
            throw new JobProcessException('node memory not enough - ' + conf.memMB + 'MB but only have node available - ' + leftResourceList.size())
        }

        List<String> nodeIpList = []
        // sort by agent script
        def sortR = AgentCaller.instance.agentScriptExe(clusterId, list[0].ip, 'node choose',
                [leftResourceListJson: JSON.toJSONString(leftResourceList)])
        def arrR = sortR.getJSONArray('list')
        arrR[0..<runNumber].each {
            nodeIpList << it.toString()
        }
        nodeIpList
    }

    JobStepKeeper stopOneContainer(int jobId, AppDTO app, ContainerInfo x) {
        def nodeIp = x.nodeIp
        def instanceIndex = x.instanceIndex()
        def keeper = new JobStepKeeper(jobId: jobId, instanceIndex: instanceIndex, nodeIp: nodeIp)
        def gatewayConf = app.gatewayConf
        if (gatewayConf) {
            def publicPort = x.publicPort(gatewayConf.containerPrivatePort)
            if (!publicPort) {
                throw new JobProcessException('no public port get for ' + app.name)
            }

            def isRemoveBackendOk = GatewayOperator.create(app.id, gatewayConf).removeBackend(nodeIp, publicPort)
            keeper.next(JobStepKeeper.Step.removeFromGateway, 'remove backend result - ' + isRemoveBackendOk)
        }

        def id = x.id
        def p = [id: id]

        try {
            def r = AgentCaller.instance.agentScriptExeAs(app.clusterId, nodeIp,
                    'container inspect', ContainerInspectInfo, p)
            def state = r.state.status
            JSONObject stopR
            if ('created' == state || 'running' == state) {
                p.isRemoveAfterStop = '1'
                p.readTimeout = 30 * 1000
                stopR = AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container stop', p)
                log.info 'done stop and remove container - {} - {}', id, app.id
            } else if ('exited' == state) {
                stopR = AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container remove', p)
                log.info 'done remove container - {} - {}', id, app.id
            }
            Boolean isOk = stopR.getBoolean('flag')
            keeper.next(JobStepKeeper.Step.stopAndRemoveContainer, 'stop container', 'state: ' + state, isOk)
            keeper.next(JobStepKeeper.Step.done, 'job finished')
        } catch (Exception e) {
            log.error('get container info error - ' + id + ' for app - ' + app.name, e)
            keeper.next(JobStepKeeper.Step.stopAndRemoveContainer, 'stop container', 'error: ' + e.message, false)
            keeper.next(JobStepKeeper.Step.done, 'job finished', '', false)
        }
        keeper
    }

    static CreateContainerConf prepareCreateContainerConf(AppConf confCopy, AppDTO app, Integer instanceIndex,
                                                          String nodeIp, List<String> nodeIpList, String imageWithTag = null) {
        String imageWithTagFinal
        if (imageWithTag == null) {
            def registryOne = new ImageRegistryDTO(id: confCopy.registryId).one()
            assert registryOne
            imageWithTagFinal = registryOne.trimScheme() + '/' + confCopy.group + '/' + confCopy.image + ':' + confCopy.tag
        } else {
            imageWithTagFinal = imageWithTag
        }

        def createContainerConf = new CreateContainerConf()
        createContainerConf.conf = confCopy
        createContainerConf.app = app
        createContainerConf.clusterId = app.clusterId
        createContainerConf.namespaceId = app.namespaceId
        createContainerConf.nodeIp = nodeIp
        createContainerConf.nodeIpList = nodeIpList
        createContainerConf.appId = app.id
        createContainerConf.instanceIndex = instanceIndex
        createContainerConf.imageWithTag = imageWithTagFinal

        def allCachedAppList = InMemoryCacheSupport.instance.appList
        if (allCachedAppList) {
            createContainerConf.appIdList = allCachedAppList.findAll { it.clusterId == app.clusterId }.collect { it.id }
        } else {
            createContainerConf.appIdList = [app.id]
        }

        def cluster = InMemoryCacheSupport.instance.oneCluster(app.clusterId)
        createContainerConf.globalEnvConf = cluster.globalEnvConf

        createContainerConf
    }

    boolean beforeCheck(CreateContainerConf c, JobStepKeeper keeper) {
        String imageName = c.conf.group + '/' + c.conf.image

        def checkerList = CheckerHolder.instance.checkerList.findAll { it.imageName() == imageName }
        def beforeCheckList = checkerList.findAll { it.type() == Checker.Type.before }

        if (beforeCheckList && !c.conf.isParallel) {
            for (checker in beforeCheckList) {
                def isCheckOk = checker.check(c, keeper)
                if (!isCheckOk) {
                    Event.builder().type(Event.Type.app).reason('before create check fail').
                            result(c.appId).build().
                            log('container run before check fail - ' +
                                    c.instanceIndex + ' - when check ' + checker.name()).toDto().add()
                    return false
                }
                keeper.next(JobStepKeeper.Step.preCheck, 'before create container check', checker.name())
            }
            return true
        } else {
            keeper.next(JobStepKeeper.Step.preCheck, 'before create container check skip')
            return true
        }
    }

    boolean afterCheck(CreateContainerConf c, JobStepKeeper keeper) {
        String imageName = c.conf.group + '/' + c.conf.image

        def checkerList = CheckerHolder.instance.checkerList.findAll { it.imageName() == imageName }
        def afterCheckList = checkerList.findAll { it.type() == Checker.Type.after }

        // need wait if container start but service will start last long time
        if (afterCheckList && !c.conf.isParallel) {
            for (checker in afterCheckList) {
                def isCheckOk = checker.check(c, keeper)
                if (!isCheckOk) {
                    Event.builder().type(Event.Type.app).reason('after start check fail').
                            result(c.appId).build().
                            log('container run after check fail - ' +
                                    c.instanceIndex + ' - when check ' + checker.name()).toDto().add()
                    return false
                }
                keeper.next(JobStepKeeper.Step.afterCheck, 'after container start check', checker.name(), isCheckOk)
            }
            return true
        } else {
            keeper.next(JobStepKeeper.Step.afterCheck, 'after container start check skip')
            return true
        }
    }

    boolean initCheck(CreateContainerConf c, JobStepKeeper keeper, String containerId) {
        String imageName = c.conf.group + '/' + c.conf.image

        def checkerList = CheckerHolder.instance.checkerList.findAll { it.imageName() == imageName }
        def initCheckList = checkerList.findAll { it.type() == Checker.Type.init }

        if (initCheckList) {
            for (checker in initCheckList) {
                String initCmd = CachedGroovyClassLoader.instance.eval(checker.script(c)).toString()
                if (initCmd) {
                    def initR = AgentCaller.instance.agentScriptExe(c.clusterId, c.nodeIp, 'container init',
                            [id: containerId, initCmd: initCmd])
                    Boolean isErrorInit = initR.getBoolean('isError')
                    if (isErrorInit != null && isErrorInit.booleanValue()) {
                        Event.builder().type(Event.Type.app).reason('after start init fail').
                                result(c.appId).build().
                                log('init container fail - ' + c.conf.group + '/' + c.conf.image + ' - ' +
                                        initR.getString('message')).toDto().add()
                        return false
                    }
                    keeper.next(JobStepKeeper.Step.initContainer, 'init container',
                            'done ' + checker.name() + ' - ' + initCmd + ' - ' + initR.getString('message'), isErrorInit)
                } else {
                    keeper.next(JobStepKeeper.Step.initContainer, 'init container skip', checker.name())
                }
            }
            return true
        } else {
            keeper.next(JobStepKeeper.Step.initContainer, 'init container skip')
            return true
        }
    }

    ContainerRunResult startOneContainer(AppDTO app, int jobId, int instanceIndex,
                                         List<String> nodeIpList, String nodeIp,
                                         AppConf confCopy, JobStepKeeper passedKeeper = null) {
        checkDependApp(confCopy, app.clusterId)

        // check if already created
        def checkR = AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container name check',
                [name: ContainerHelper.generateContainerName(app.id, instanceIndex)])
        if (checkR.getBoolean('flag')) {
            return null
        }

        // *** *** *** as process
        if (confCopy.isRunningUnbox) {
            return startOneProcess(app, jobId, instanceIndex, nodeIpList, nodeIp, confCopy, passedKeeper)
        }

        // *** *** *** as docker container
        def registryOne = new ImageRegistryDTO(id: confCopy.registryId).one()
        if (!registryOne) {
            throw new JobProcessException('docker image registry not found - ' + confCopy.registryId)
        }

        String imageWithTag
        if (registryOne.local() || registryOne.dockerIo()) {
            imageWithTag = confCopy.group + '/' + confCopy.image + ':' + confCopy.tag
        } else {
            imageWithTag = registryOne.trimScheme() + '/' + confCopy.group + '/' + confCopy.image + ':' + confCopy.tag
        }

        def keeper = passedKeeper ?: new JobStepKeeper(jobId: jobId, instanceIndex: instanceIndex, nodeIp: nodeIp)
        keeper.next(JobStepKeeper.Step.chooseNode, 'choose deploy node', nodeIpList.toString())

        def p = [keyword: imageWithTag]
        def listImageR = AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container image list', p)
        boolean isExists = listImageR.getBoolean('isExists')
        if (!isExists) {
            // if no internet connect
            if (registryOne.local()) {
                throw new JobProcessException('only use image local, please load image first - ' + imageWithTag)
            }

            // need pull
            p.image = imageWithTag
            p.registryId = confCopy.registryId
            p.readTimeout = Conf.instance.getInt('pull.image.readTimeout.Millis', 1000 * 60)
            def pullImageR = AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container image pull', p)
            Boolean isError = pullImageR.getBoolean('isError')
            if (isError != null && isError.booleanValue()) {
                throw new JobProcessException('pull image fail - ' + imageWithTag + ' - ' + pullImageR.getString('message'))
            }

            def listImageAgainR = AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container image list', p)
            if (listImageAgainR.getBoolean('isExists')) {
                keeper.next(JobStepKeeper.Step.pullImage, 'pull docker image', imageWithTag)
            } else {
                throw new JobProcessException('pull image fail - ' + imageWithTag + ' - ' + nodeIp)
            }
        } else {
            keeper.next(JobStepKeeper.Step.pullImage, 'pull docker image skip', imageWithTag)
        }

        // node volume conflict check
        def cluster = new ClusterDTO(id: app.clusterId).one()
        def appList = new AppDTO(clusterId: app.clusterId).queryFields('id,conf').loadList()
        List<String> skipVolumeDirSet = cluster.globalEnvConf?.skipConflictCheckVolumeDirList.collect { it.value.toString() } ?: []
        def otherAppMountVolumeDirList = appList.findAll { it.id != app.id }.collect { one ->
            one.conf.dirVolumeList.collect { it.dir }.findAll { !(it in skipVolumeDirSet) }
        }.flatten()
        def thisAppMountVolumeDirList = confCopy.dirVolumeList.collect { it.dir }.findAll { !(it in skipVolumeDirSet) }
        if (thisAppMountVolumeDirList.any { it in otherAppMountVolumeDirList }) {
            throw new JobProcessException('node volume conflict check fail - ' + nodeIp + ' - ' + thisAppMountVolumeDirList)
        }

        // before init after check
        def createContainerConf = prepareCreateContainerConf(confCopy, app, instanceIndex, nodeIp, nodeIpList, imageWithTag)
        log.info createContainerConf.toString()
        createContainerConf.jobId = jobId

        if (!beforeCheck(createContainerConf, keeper)) {
            throw new JobProcessException('before check fail')
        }

        // support dyn cmd using plugin expression
        def plugin = PluginManager.instance.pluginList.
                find { it.group() == confCopy.group && it.image() == confCopy.image }

        if (confCopy.cmd?.contains('$')) {
            confCopy.cmd = evalUsingPluginExpression(plugin, createContainerConf, confCopy.cmd)
        }
        for (kvPair in confCopy.envList) {
            if (kvPair.value.toString().contains('$')) {
                kvPair.value = evalUsingPluginExpression(plugin, createContainerConf, kvPair.value.toString())
            }
        }

        def createP = [jsonStr: JsonWriter.instance.json(createContainerConf)]
        ContainerConfigInfo containerConfigInfo = AgentCaller.instance.agentScriptExeAs(app.clusterId, nodeIp, 'container create',
                ContainerConfigInfo, createP)
        keeper.next(JobStepKeeper.Step.createContainer, 'created container', containerConfigInfo.toString())

        def containerId = containerConfigInfo.containerId
        def startR = AgentCaller.instance.agentScriptExe(app.clusterId, nodeIp, 'container start', [id: containerId])
        Boolean isErrorStart = startR.getBoolean('isError')
        if (isErrorStart != null && isErrorStart.booleanValue()) {
            throw new JobProcessException('start container fail - ' + imageWithTag + ' - ' + startR.getString('message'))
        }
        keeper.next(JobStepKeeper.Step.startContainer, 'start container', containerId)

        initCheck(createContainerConf, keeper, containerId)
        afterCheck(createContainerConf, keeper)
        new ContainerRunResult(nodeIp: nodeIp, containerConfig: containerConfigInfo, keeper: keeper)
    }

    private ContainerRunResult startOneProcess(AppDTO app, int jobId, int instanceIndex,
                                               List<String> nodeIpList, String nodeIp,
                                               AppConf confCopy, JobStepKeeper passedKeeper) {
        def imageWithTag = confCopy.group + '/' + confCopy.image + ':' + confCopy.tag
        def createContainerConf = prepareCreateContainerConf(confCopy, app, instanceIndex, nodeIp, nodeIpList, imageWithTag)
        log.info createContainerConf.toString()
        createContainerConf.jobId = jobId

        def keeper = passedKeeper ?: new JobStepKeeper(jobId: jobId, instanceIndex: instanceIndex, nodeIp: nodeIp)

        if (!beforeCheck(createContainerConf, keeper)) {
            throw new JobProcessException('before check fail')
        }

        int pid = HostProcessSupport.instance.startOneProcess(createContainerConf, keeper)

        afterCheck(createContainerConf, keeper)

        def containerConfigInfo = new ContainerConfigInfo(pid: pid, networkMode: 'host')
        return new ContainerRunResult(nodeIp: nodeIp, containerConfig: containerConfigInfo, keeper: keeper)
    }

    private void checkDependApp(AppConf confCopy, int clusterId) {
        if (!confCopy.dependAppIdList) {
            return
        }
        def instance = InMemoryAllContainerManager.instance
        for (dependAppId in confCopy.dependAppIdList) {
            String dependAppName = InMemoryCacheSupport.instance.oneApp(dependAppId)?.name ?: dependAppId.toString()

            if (!Guardian.instance.isHealth(dependAppId)) {
                throw new JobProcessException('wait depend app ready, app: ' + dependAppName)
            }
            def containerList = instance.getContainerList(clusterId, dependAppId)
            if (!containerList || containerList.any { x -> !x.checkOk() }) {
                throw new JobProcessException('wait depend app ready, app: ' + dependAppName)
            }
        }
    }

    private String evalUsingPluginExpression(Plugin plugin, CreateContainerConf createContainerConf, String value) {
        if (!plugin) {
            return value
        }

        def expressions = plugin.expressions()
        if (!expressions) {
            return value
        }

        log.info 'before eval expression: {}', value

        Map<String, Object> variables = [:]
        variables.appId = createContainerConf.appId
        variables.nodeIp = createContainerConf.nodeIp
        variables.nodeIpList = createContainerConf.nodeIpList
        variables.instanceIndex = createContainerConf.instanceIndex

        variables.each { k, v ->
            value = value.replace('$' + k, v.toString())
            value = value.replace('${' + k + '}', v.toString())
        }

        expressions.each { k, v ->
            def finalValue = CachedGroovyClassLoader.instance.eval(v, variables).toString()
            value = value.replace('$' + k, finalValue)
            value = value.replace('${' + k + '}', finalValue)
        }

        log.info 'after eval expression: {}', value
        value
    }
}
