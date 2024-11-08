package ctrl

import auth.User
import com.segment.common.Conf
import common.Event
import common.Utils
import model.*
import model.json.ExtendParams
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport
import server.hpa.ScaleRequest
import server.hpa.ScaleRequestHandler
import server.scheduler.Guardian
import server.scheduler.checker.CleanerHolder
import transfer.ContainerInfo

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.get('/api/app/scale') { req, resp ->
    def appId = req.param('appId')
    def scaleCmd = req.param('scaleCmd')
    def nodeIp = req.param('nodeIp')

    Event.builder().type(Event.Type.cluster).reason('trigger scale').result(appId).
            build().log(nodeIp + ' - ' + scaleCmd).toDto().add()
    ScaleRequestHandler.instance.add(appId as int, new ScaleRequest(nodeIp: nodeIp, scaleCmd: scaleCmd as int))
    [flag: true]
}

h.get('/api/image/pull/hub/info') { req, resp ->
    def registryId = req.param('registryId')
    assert registryId
    new ImageRegistryDTO(id: registryId as int).one()
}

h.group('/app') {
    h.group('/option') {
        h.get('/list') { req, resp ->
            def clusterList = new ClusterDTO().noWhere().queryFields('id,name').list()
            def namespaceList = new NamespaceDTO().noWhere().queryFields('id,cluster_id,name').list()
            def registryList = new ImageRegistryDTO().noWhere().queryFields('id,url').list()
            def nodeList = new NodeDTO().noWhere().queryFields('ip,tags,clusterId').list()
            def appOtherList = new AppDTO().noWhere().queryFields('id,cluster_id,name').list()
            def deployFileList = new DeployFileDTO().noWhere().queryFields('id,dest_path').list()
            // for skip
            deployFileList << new DeployFileDTO(id: 0, destPath: '/tmp/ignore')

            def instance = InMemoryAllContainerManager.instance
            def dat = Utils.getNodeAliveCheckLastDate(3)
            def nodeIpList = nodeList.findAll { one ->
                instance.getHeartBeatDate(one.ip) > dat
            }.collect {
                def nodeInfoHb = instance.getNodeInfo(it.ip)
                [ip            : it.ip,
                 clusterId     : it.clusterId,
                 memUsedPercent: nodeInfoHb.mem.usedPercent,
                 cpuUsedPercent: nodeInfoHb.cpuUsedPercent()]
            }

            Set<String> nodeTagSet = []
            nodeList.each {
                def tags = it.tags
                if (tags) {
                    tags.split(',').each { tag ->
                        nodeTagSet << '' + it.clusterId + ',' + tag
                    }
                }
            }
            def nodeTagList = nodeTagSet.collect {
                def arr = it.split(',')
                [clusterId: arr[0] as int, tag: arr[1]]
            }

            [clusterList: clusterList, namespaceList: namespaceList, registryList: registryList,
             nodeIpList : nodeIpList, nodeTagList: nodeTagList, appOtherList: appOtherList, deployFileList: deployFileList]
        }.get('/image/env/list') { req, resp ->
            def image = req.param('image')
            assert image
            new ImageEnvDTO(imageName: image).queryFields('env,name').list()
        }.get('/image/port/list') { req, resp ->
            def image = req.param('image')
            assert image
            new ImagePortDTO(imageName: image).queryFields('port,name').list()
        }.get('/image/tpl/list') { req, resp ->
            def image = req.param('image')
            assert image
            new ImageTplDTO(imageName: image, tplType: ImageTplDTO.TplType.mount.name()).list()
        }.get('/image/volume/list') { req, resp ->
            def clusterId = req.param('clusterId')
            assert clusterId
            def image = req.param('image')
            new NodeVolumeDTO(clusterId: clusterId as int).where(image as boolean,
                    'image_name = ? or image_name is null or image_name = ?', image, '').
                    queryFields('id,dir,name').list()
        }
    }

    h.get('/list') { req, resp ->
        def clusterId = req.param('clusterId')
        def namespaceId = req.param('namespaceId')
//        assert clusterId || namespaceId

        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def keyword = req.param('keyword')
        def pager = new AppDTO().noWhere().where((clusterId as boolean) && !namespaceId, 'cluster_id = ?', clusterId).
                where(namespaceId as boolean, 'namespace_id = ?', namespaceId).
                where(keyword as boolean, '(name like ?) or (des like ?)',
                        '%' + keyword + '%', '%' + keyword + '%').listPager(pageNum, pageSize)

        def guardian = Guardian.instance
        def instance = InMemoryAllContainerManager.instance
        pager.transfer {
            def dto = it as AppDTO
            def map = dto.rawProps()
            map.isConfMonitor = dto.monitorConf as Boolean
            map.isConfLog = dto.logConf as Boolean
            map.isConfGateway = dto.gatewayConf as Boolean
            map.isConfAb = dto.abConf as Boolean
            def isConfLiveCheck = dto.liveCheckConf as Boolean
            map.isConfLiveCheck = isConfLiveCheck
            map.isConfJob = dto.jobConf as Boolean

            if (isConfLiveCheck) {
                def containerList = instance.getContainerList(clusterId ? clusterId as int : 0, dto.id)
                map.isLiveCheckOk = !(containerList && containerList.any { x -> !x.checkOk() })
            } else {
                map.isLiveCheckOk = true
            }

            map.healthCheckResults = guardian.oneAppGuardian(dto.id)?.healthCheckResults
            map
        }
    }.get('/list/simple') { req, resp ->
        def namespaceId = req.param('namespaceId')
        assert namespaceId
        new AppDTO(namespaceId: namespaceId as int).list()
    }.delete('/delete') { req, resp ->
        def id = req.param('id')
        assert id

        AppDTO one = new AppDTO(id: id as int).one()
        User u = req.attr('user') as User
        if (!u.isAccessNamespace(one.namespaceId)) {
            resp.halt(500, 'not this namespace manager')
        }

        // check if container running
        def list = InMemoryAllContainerManager.instance.getContainerList(one.clusterId, one.id)
        if (list) {
            resp.halt(500, 'this app has containers')
        }

        def appJobList = new AppJobDTO(appId: one.id).queryFields('id').list()
        if (appJobList) {
            new AppJobLogDTO().whereIn('job_id', appJobList.collect { it.id }).deleteAll()
            new AppJobDTO(appId: one.id).deleteAll()
        }
        new AppDTO(id: one.id).delete()
        InMemoryCacheSupport.instance.appDeleted(one.id)

        Guardian.instance.stopOneRunning(one.id)

        String imageName = one.conf.imageName()
        def cleanerList = CleanerHolder.instance.cleanerList.findAll { it.imageName().contains(imageName) }
        if (cleanerList) {
            for (cleaner in cleanerList) {
                boolean isCleanOk = cleaner.clean(one)
                if (!isCleanOk) {
                    log.warn 'clean app fail app: {}, cleaner: {}', one.name, cleaner.name()
                }
            }
        }
        [flag: true]
    }.post('/update') { req, resp ->
        def one = req.bodyAs(AppDTO)
        assert one.name && one.namespaceId

        def instance = InMemoryAllContainerManager.instance

        def conf = one.conf
        def nodeList = instance.getHeartBeatOkNodeList(one.clusterId)
        if (conf.containerNumber > nodeList.size() && !conf.isLimitNode) {
            resp.halt(500, 'container number <= available node size - ' + nodeList.size())
        }
        if (conf.targetNodeIpList && conf.containerNumber > conf.targetNodeIpList.size() && !conf.isLimitNode) {
            resp.halt(500, 'container number <= target node ip list size - ' + conf.targetNodeIpList.size())
        }
        if (conf.isRunningUnbox && !conf.deployFileIdList) {
            resp.halt(500, 'run as a process must choose deploy files')
        }

        if (conf.targetNodeIpList && conf.isRunningUnbox) {
            // check if target node agent is not running in docker
            for (targetNodeIp in conf.targetNodeIpList) {
                if (instance.getNodeInfo(targetNodeIp).isDmsAgentRunningInDocker) {
                    resp.halt(500, 'target node agent is running in docker - ' + targetNodeIp)
                }
            }
        }

        if (conf.cpusetCpus) {
            def targetNodeIpList = conf.targetNodeIpList
            if (!targetNodeIpList) {
                resp.halt(500, 'must choose target node ip list if set cpuset cpus')
            }

            def cpusetCpuList = Utils.cpusetCpusToList(conf.cpusetCpus)
            for (nodeIp in targetNodeIpList) {
                def nodeInfo = instance.getNodeInfo(nodeIp)
                if (!nodeInfo) {
                    resp.halt(500, 'node ip not found - ' + nodeIp)
                }
                def allNodeCpuList = 0..<nodeInfo.cpuNumber()
                if (cpusetCpuList.any { !allNodeCpuList.contains(it) }) {
                    resp.halt(500, 'cpuset cpus must be in node cpu list - ' + nodeIp)
                }
            }
        }

        def cacheSupport = InMemoryCacheSupport.instance
        if (conf.memMB && conf.targetNodeIpList) {
            // check if memory require > node memory - system used memory
            def systemUsedMB = Conf.instance.getInt('node.systemUsedMB', 1024)
            def targetNodeIpList = conf.targetNodeIpList
            for (nodeIp in targetNodeIpList) {
                def nodeInfo = instance.getNodeInfo(nodeIp)
                if (!nodeInfo) {
                    resp.halt(500, 'node ip not found - ' + nodeIp)
                }

                int memMBTotal = nodeInfo.mem.total.intValue()

                def containerList = instance.getContainerList(0, 0, nodeIp)
                int memMBUsed = 0
                for (x in containerList) {
                    def appOne = cacheSupport.oneApp(x.appId())
                    if (appOne.id == one.id) {
                        continue
                    }
                    memMBUsed += appOne.conf.memMB
                }
                if (conf.memMB + memMBUsed > memMBTotal - systemUsedMB) {
                    resp.halt(500, 'memory require > node memory - system used memory - ' + nodeIp)
                }
            }
        }

        one.updatedDate = new Date()
        if (one.id) {
            User u = req.attr('user') as User
            if (!u.isAccessApp(one.id)) {
                resp.halt(500, 'not this app manager')
            }

            def oldOne = new AppDTO(id: one.id).one()
            def oldConf = oldOne.conf

            def isNeedScroll = conf != oldConf
            if (isNeedScroll) {
                if (oldConf.containerNumber != conf.containerNumber) {
                    resp.halt(500, 'change container number and change others at the same time not allowed')
                } else {
                    one.update()
                    log.info 'change from {} to {}', oldConf, conf
                    cacheSupport.appUpdated(one)

                    // do not create job
                    if (!oldOne.autoManage()) {
                        return [id: one.id]
                    }

                    // check if need a scroll job
                    List<ContainerInfo> containerList = instance.getContainerList(one.clusterId, one.id)
                    if (containerList.size() == oldConf.containerNumber) {
                        def jobId = new AppJobDTO(appId: one.id, failNum: 0,
                                status: AppJobDTO.Status.created.val,
                                jobType: AppJobDTO.JobType.scroll.val,
                                createdDate: new Date(), updatedDate: new Date()).add()
                        return oldOne.autoManage() ? [id: one.id, jobId: jobId] : [id: one.id]
                    } else {
                        return [id: one.id]
                    }
                }
            } else {
                if (oldConf.containerNumber == conf.containerNumber || !oldOne.autoManage()) {
                    one.update()
                    cacheSupport.appUpdated(one)
                    return [id: one.id]
                }

                boolean isAdd = conf.containerNumber > oldConf.containerNumber
                Map<String, Object> params
                if (isAdd) {
                    List<Integer> needRunInstanceIndexList = []

                    List<ContainerInfo> containerList = instance.getContainerList(one.clusterId, one.id)
                    (0..<conf.containerNumber).each { i ->
                        def runningOne = containerList.find { x ->
                            i == x.instanceIndex()
                        }
                        if (!runningOne) {
                            needRunInstanceIndexList << i
                        }
                    }
                    params = [needRunInstanceIndexList: needRunInstanceIndexList as Object]
                } else {
                    params = [toContainerNumber: conf.containerNumber as Object]
                }

                one.update()
                cacheSupport.appUpdated(one)

                log.info 'scale from {} to {}', oldConf.containerNumber, conf.containerNumber
                def jobType = isAdd ? AppJobDTO.JobType.create : AppJobDTO.JobType.remove
                def jobId = new AppJobDTO(appId: one.id, failNum: 0, jobType: jobType.val,
                        createdDate: new Date(), updatedDate: new Date(), params: new ExtendParams(params)).add()
                return [id: one.id, jobId: jobId]
            }
        } else {
            User u = req.attr('user') as User
            if (!u.isAccessNamespace(one.namespaceId)) {
                resp.halt(500, 'not this namespace manager')
            }

            one.status = AppDTO.Status.auto.val
            def id = one.add()
            one.id = id
            cacheSupport.appUpdated(one)

            def jobId = new AppJobDTO(appId: one.id, failNum: 0, jobType: AppJobDTO.JobType.create.val,
                    createdDate: new Date(), updatedDate: new Date()).add()
            return [id: id, jobId: jobId]
        }
    }.get('/manual') { req, resp ->
        def id = req.param('id')
        assert id
        def one = new AppDTO(id: id as int).queryFields('id,status').one()

        one.status = one.status == AppDTO.Status.auto.val ? AppDTO.Status.manual.val : AppDTO.Status.auto.val
        one.update()
        [status: one.status]
    }

    // update monitor/live check/ab/job/gateway
    h.post('/conf/update') { req, resp ->
        def one = req.bodyAs(AppDTO)
        assert one.id

        User u = req.attr('user') as User
        if (!u.isAccessApp(one.id)) {
            resp.halt(500, 'not this app manager')
        }

        // unbox process not support shell, too heavy, and not in controller
        if (one.liveCheckConf && one.liveCheckConf.isShellScript) {
            def app = new AppDTO(id: one.id).queryFields('conf').one()
            if (app.conf.isRunningUnbox) {
                return [message: 'Run as process not support shell live check!']
            }
        }

        one.updatedDate = new Date()
        one.update()
        return [id: one.id]
    }

    h.post('/scale') { req, resp ->
        HashMap form = req.bodyAs()
        def id = form.id as int
        def scaleNumber = form.scaleNumber as int

        User u = req.attr('user') as User
        if (!u.isAccessApp(id)) {
            resp.halt(500, 'not this app manager')
        }

        def app = new AppDTO(id: id).queryFields('id,conf').one()
        assert app
        if (scaleNumber != app.conf.containerNumber) {
            app.conf.containerNumber = scaleNumber
            app.update()
        }

        return [id: id]
    }
}

h.group('/api/app') {
    h.post('/live-check-conf/query') { req, resp ->
        HashMap map = req.bodyAs()
        List<Integer> appIdList = map.appIdList as List<Integer>
        def appList = InMemoryCacheSupport.instance.appList
        if (!appList) {
            return []
        }
        appList.findAll {
            it.id in appIdList && it.liveCheckConf
        }.collect {
            [id: it.id, liveCheckConf: it.liveCheckConf]
        }
    }
}
