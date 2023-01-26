package ctrl

import auth.User
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
            def clusterList = new ClusterDTO().where('1=1').queryFields('id,name').loadList()
            def namespaceList = new NamespaceDTO().where('1=1').queryFields('id,cluster_id,name').loadList()
            def registryList = new ImageRegistryDTO().where('1=1').queryFields('id,url').loadList()
            def nodeList = new NodeDTO().where('1=1').queryFields('ip,tags,clusterId').loadList()
            def appOtherList = new AppDTO().where('1=1').queryFields('id,cluster_id,name').loadList()
            def deployFileList = new DeployFileDTO().where('1=1').queryFields('id,dest_path').loadList()
            // for skip
            deployFileList << new DeployFileDTO(id: 0, destPath: '/tmp/ignore')

            def instance = InMemoryAllContainerManager.instance
            def dat = Utils.getNodeAliveCheckLastDate(3)
            def nodeIpList = nodeList.collect { [ip: it.ip, clusterId: it.clusterId] }.findAll { one ->
                instance.getHeartBeatDate(one.ip.toString()) > dat
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
            new ImageEnvDTO(imageName: image).queryFields('env,name').loadList()
        }.get('/image/port/list') { req, resp ->
            def image = req.param('image')
            assert image
            new ImagePortDTO(imageName: image).queryFields('port,name').loadList()
        }.get('/image/tpl/list') { req, resp ->
            def image = req.param('image')
            assert image
            new ImageTplDTO(imageName: image, tplType: ImageTplDTO.TplType.mount.name()).loadList()
        }.get('/image/volume/list') { req, resp ->
            def clusterId = req.param('clusterId')
            assert clusterId
            def image = req.param('image')
            new NodeVolumeDTO(clusterId: clusterId as int).where(!!image,
                    'image_name = ? or image_name is null or image_name = ?', image, '').
                    queryFields('id,dir,name').loadList()
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
        def pager = new AppDTO().where('1=1').where(!!clusterId && !namespaceId, 'cluster_id = ?', clusterId).
                where(!!namespaceId, 'namespace_id = ?', namespaceId).
                where(!!keyword, '(name like ?) or (des like ?)',
                        '%' + keyword + '%', '%' + keyword + '%').loadPager(pageNum, pageSize)

        def instance = InMemoryAllContainerManager.instance
        pager.transfer {
            def dto = it as AppDTO
            def map = dto.rawProps()
            map.isConfMonitor = dto.monitorConf as Boolean
            map.isConfGateway = dto.gatewayConf as Boolean
            map.isConfAb = dto.abConf as Boolean
            def isConfLiveCheck = dto.liveCheckConf as Boolean
            map.isConfLiveCheck = isConfLiveCheck
            map.isConfJob = dto.jobConf as Boolean

            if (isConfLiveCheck) {
                def containerList = instance.getContainerList(clusterId ? clusterId as int : 0, dto.id)
                if (containerList && containerList.any { x -> !x.checkOk() }) {
                    map.isLiveCheckOk = false
                } else {
                    map.isLiveCheckOk = true
                }
            } else {
                map.isLiveCheckOk = true
            }
            map
        }
    }.get('/list/simple') { req, resp ->
        def namespaceId = req.param('namespaceId')
        assert namespaceId
        new AppDTO(namespaceId: namespaceId as int).loadList()
    }.delete('/delete') { req, resp ->
        def id = req.param('id')
        assert id

        AppDTO one = new AppDTO(id: id as int).one()
        User u = req.session('user') as User
        if (!u.isAccessNamespace(one.namespaceId)) {
            resp.halt(500, 'not this namespace manager')
        }

        // check if container running
        def list = InMemoryAllContainerManager.instance.getContainerList(one.clusterId, one.id)
        if (list) {
            resp.halt(500, 'this app has containers')
        }

        def appJobList = new AppJobDTO(appId: one.id).queryFields('id').loadList()
        if (appJobList) {
            new AppJobLogDTO().whereIn('job_id', appJobList.collect { it.id }).deleteAll()
            new AppJobDTO(appId: one.id).deleteAll()
        }
        new AppDTO(id: one.id).delete()

        String imageName = one.conf.group + '/' + one.conf.image
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

        def nodeList = InMemoryAllContainerManager.instance.getHeartBeatOkNodeList(one.clusterId)
        if (one.conf.containerNumber > nodeList.size() && !one.conf.isLimitNode) {
            resp.halt(500, 'container number <= available node size - ' + nodeList.size())
        }
        if (one.conf.targetNodeIpList && one.conf.containerNumber > one.conf.targetNodeIpList.size() && !one.conf.isLimitNode) {
            resp.halt(500, 'container number <= target node ip list size - ' + one.conf.targetNodeIpList.size())
        }
        if (one.conf.isRunningUnbox && !one.conf.deployFileIdList) {
            resp.halt(500, 'run as a process must choose deploy files')
        }

        one.updatedDate = new Date()
        if (one.id) {
            User u = req.session('user') as User
            if (!u.isAccessApp(one.id)) {
                resp.halt(500, 'not this app manager')
            }

            def oldOne = new AppDTO(id: one.id).one()
            def oldConf = oldOne.conf
            def conf = one.conf

            def isNeedScroll = conf != oldConf
            if (isNeedScroll) {
                if (oldConf.containerNumber != conf.containerNumber) {
                    resp.halt(500, 'change container number and change others at the same time not allowed')
                } else {
                    one.update()
                    log.info 'change from {} to {}', oldConf, conf

                    // do not create job
                    if (!oldOne.autoManage()) {
                        return [id: one.id]
                    }

                    // check if need a scroll job
                    List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(one.clusterId, one.id)
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
                    return [id: one.id]
                }

                boolean isAdd = conf.containerNumber > oldConf.containerNumber
                Map<String, Object> params
                if (isAdd) {
                    List<Integer> needRunInstanceIndexList = []

                    List<ContainerInfo> containerList = InMemoryAllContainerManager.instance.getContainerList(one.clusterId, one.id)
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
                log.info 'scale from {} to {}', oldConf.containerNumber, conf.containerNumber
                def jobType = isAdd ? AppJobDTO.JobType.create : AppJobDTO.JobType.remove
                def jobId = new AppJobDTO(appId: one.id, failNum: 0, jobType: jobType.val,
                        createdDate: new Date(), updatedDate: new Date(), params: new ExtendParams(params)).add()
                return [id: one.id, jobId: jobId]
            }
        } else {
            User u = req.session('user') as User
            if (!u.isAccessNamespace(one.namespaceId)) {
                resp.halt(500, 'not this namespace manager')
            }

            one.status = AppDTO.Status.auto.val
            def id = one.add()

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

        User u = req.session('user') as User
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

        User u = req.session('user') as User
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
    h.post('/monitor-conf/query') { req, resp ->
        HashMap map = req.bodyAs()
        List<Integer> appIdList = map.appIdList as List<Integer>
        def appList = InMemoryCacheSupport.instance.appList
        if (!appList) {
            return []
        }
        appList.findAll {
            it.id in appIdList && it.monitorConf
        }.collect {
            [id: it.id, monitorConf: it.monitorConf]
        }
    }.post('/live-check-conf/query') { req, resp ->
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
