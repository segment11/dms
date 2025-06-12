package ctrl.redis

import com.segment.common.Conf
import com.segment.common.Utils
import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import model.*
import model.json.*
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import rm.RedisManager
import rm.RmJobExecutor
import rm.job.RmJob
import rm.job.RmJobTypes
import rm.job.task.RunCreatingAppJobTask
import server.InMemoryAllContainerManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis/sentinel-service') {
    h.get('/simple-list') { req, resp ->
        def list = new RmSentinelServiceDTO(status: RmSentinelServiceDTO.Status.running).
                queryFields('id,name').
                list()

        [list: list.collect {
            return [
                    id  : it.id,
                    name: it.name,
            ]
        }]
    }

    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def keyword = req.param('keyword')
        def pager = new RmSentinelServiceDTO().
                noWhere().
                where(keyword as boolean, '(name like ?)', '%' + keyword + '%').
                listPager(pageNum, pageSize)

        def instance = InMemoryAllContainerManager.instance
        if (pager.list) {
            pager.list.each { one ->
                if (one.status == RmSentinelServiceDTO.Status.running) {
                    def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, one.appId)
                    def runningNumber = containerList.findAll { x ->
                        x.running()
                    }.size()

                    if (one.replicas != runningNumber) {
                        one.status = RmSentinelServiceDTO.Status.unhealthy
                    }
                } else if (one.status == RmSentinelServiceDTO.Status.creating) {
                    def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, one.appId)
                    def runningNumber = containerList.findAll { x ->
                        x.running()
                    }.size()

                    if (one.replicas == runningNumber) {
                        one.status = RmSentinelServiceDTO.Status.running
                        new RmSentinelServiceDTO(id: one.id, status: RmSentinelServiceDTO.Status.running).update()
                    }
                }
            }
        }

        pager
    }

    h.post('/add') { req, resp ->
        def one = req.bodyAs(RmSentinelServiceDTO)

        // only support 1, 3 or 5
        if (one.replicas != 1 && one.replicas != 3 && one.replicas != 5) {
            resp.halt(409, 'only support 1, 3 or 5 replicas')
        }

        // check name
        def existOne = new RmSentinelServiceDTO(name: one.name).queryFields('id').one()
        if (existOne) {
            resp.halt(409, 'name already exists')
        }

        // check node ready
        def instance = InMemoryAllContainerManager.instance
        def hbOkNodeList = instance.hbOkNodeList(RedisManager.CLUSTER_ID, 'ip,tags')
        if (one.nodeTags) {
            def matchNodeList = hbOkNodeList.findAll { node ->
                node.tags && node.tags.any { tag ->
                    tag in one.nodeTags
                }
            }
            if (matchNodeList.size() < one.replicas) {
                resp.halt(409, 'not enough node ready, for tags: ' + one.nodeTags)
            }
        } else {
            if (hbOkNodeList.size() < one.replicas && !Conf.instance.isOn('rm.isSingleNodeTest')) {
                resp.halt(409, 'not enough node ready')
            }
        }

        // create app
        def namespaceId = NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'sentinel')

        def app = new AppDTO()
        app.clusterId = RedisManager.CLUSTER_ID
        app.namespaceId = namespaceId
        app.name = 'rm_sentinel_' + one.name
        app.status = AppDTO.Status.manual.val
        app.updatedDate = new Date()

        def conf = new AppConf()
        conf.containerNumber = one.replicas
        conf.registryId = RedisManager.preferRegistryId()
        conf.group = 'library'
        conf.image = 'redis'
        conf.tag = '7.2'

        def extendParams = one.extendParams

        // default docker container resource config values
        conf.memMB = extendParams.getInt('memMB', 256)
        conf.memReservationMB = conf.memMB
        conf.cpuFixed = extendParams.getDouble('cpuFixed', 0.2d)

        conf.networkMode = 'host'
        conf.portList << new PortMapping(privatePort: one.port, publicPort: one.port)

        conf.targetNodeTagList = []
        if (one.nodeTags) {
            one.nodeTags.each {
                conf.targetNodeTagList << it
            }
        }

        def c = Conf.instance
        def isSingleNode = c.isOn('rm.isSingleNodeTest')
        conf.isLimitNode = isSingleNode

        final String dataDir = RedisManager.dataDir()
        def sentinelDataDir = dataDir + '/sentinel_data_' + Utils.uuid()
        def nodeVolumeId = new NodeVolumeDTO(imageName: 'library/redis', name: 'for sentinel ' + one.name, dir: sentinelDataDir,
                clusterId: RedisManager.CLUSTER_ID, des: 'data dir for sentinel').add()
        def dirOne = new DirVolumeMount(
                dir: sentinelDataDir, dist: '/data/sentinel', mode: 'rw',
                nodeVolumeId: nodeVolumeId)
        conf.dirVolumeList << dirOne

        def tplOne = new ImageTplDTO(imageName: 'library/redis', name: 'sentinel.conf.tpl').one()
        def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content, dist: sentinelDataDir + '/${appId}_${instanceIndex}.conf')
        mountOne.isParentDirMount = true

        mountOne.paramList << new KVPair<String>('isSingleNode', isSingleNode.toString())
        mountOne.paramList << new KVPair<String>('port', '' + one.port)
        mountOne.paramList << new KVPair<String>('dataDir', '/data/sentinel')
        mountOne.paramList << new KVPair<String>('password', one.pass ?: '')
        mountOne.paramList << new KVPair<String>('downAfterMs', extendParams.getString('downAfterMs', '30000'))
        mountOne.paramList << new KVPair<String>('failoverTimeout', extendParams.getString('failoverTimeout', '180000'))
        conf.fileVolumeList << mountOne

        app.conf = conf
        def appId = app.add()
        app.id = appId

        // set app id to sentinel service
        one.appId = appId
        one.status = RmSentinelServiceDTO.Status.creating
        one.createdDate = new Date()

        def id = one.add()
        one.id = id

        def rmJob = new RmJob()
        rmJob.rmSentinelService = one
        rmJob.type = RmJobTypes.SENTINEL_CREATE
        rmJob.status = JobStatus.created
        rmJob.params = new JobParams()
        rmJob.params.put('rmSentinelServiceId', id.toString())
        // only one task
        rmJob.taskList << new RunCreatingAppJobTask(rmJob, 0, app)

        rmJob.createdDate = new Date()
        rmJob.updatedDate = new Date()
        rmJob.save()

        RmJobExecutor.instance.execute {
            rmJob.run()
        }

        [id: id]
    }

    h.delete('/delete') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int
        assert id > 0

        // check if exists
        def one = new RmSentinelServiceDTO(id: id).queryFields('id,status,app_id').one()
        if (!one) {
            resp.halt(404, 'sentinel service not exists')
        }

        if (one.status == RmSentinelServiceDTO.Status.deleted) {
            resp.halt(409, 'already deleted')
        }

        if (one.status == RmSentinelServiceDTO.Status.creating) {
            resp.halt(409, 'creating, please wait')
        }

        // check if is used
        def serviceOne = new RmServiceDTO(sentinelServiceId: id).queryFields('name,status').one()
        if (serviceOne && serviceOne.status != RmServiceDTO.Status.deleted) {
            resp.halt(409, "this sentinel service is used by service: ${serviceOne.name}")
        }

        RedisManager.stopContainers(one.appId)

        new RmSentinelServiceDTO(id: id, status: RmSentinelServiceDTO.Status.deleted, updatedDate: new Date()).update()
        [flag: true]
    }
}
