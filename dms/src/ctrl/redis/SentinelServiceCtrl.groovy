package ctrl.redis

import com.segment.common.Conf
import com.segment.common.Utils
import model.*
import model.json.*
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import plugin.BasePlugin
import rm.RedisManager
import rm.RmJobExecutor
import server.InMemoryAllContainerManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

final int clusterId = 1

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
                    def containerList = instance.getContainerList(clusterId, one.appId)
                    def runningNumber = containerList.findAll { x ->
                        x.running()
                    }.size()

                    if (one.replicas != runningNumber) {
                        one.status = RmSentinelServiceDTO.Status.unhealthy
                    }
                } else if (one.status == RmSentinelServiceDTO.Status.creating) {
                    def containerList = instance.getContainerList(clusterId, one.appId)
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
            resp.halt(500, 'only support 1, 3 or 5 replicas')
        }

        // check name
        def existOne = new RmSentinelServiceDTO(name: one.name).queryFields('id').one()
        if (existOne) {
            resp.halt(500, 'name already exists')
        }

        def namespaceId = NamespaceDTO.createIfNotExist(clusterId, 'sentinel')

        // check node ready
        def instance = InMemoryAllContainerManager.instance
        def hbOkNodeList = instance.hbOkNodeList(clusterId, 'ip,tags')
        if (one.nodeTags) {
            def matchNodeList = hbOkNodeList.findAll { node ->
                node.tags && node.tags.any { tag ->
                    tag in one.nodeTags
                }
            }
            if (matchNodeList.size() < one.replicas) {
                resp.halt(500, 'not enough node ready, for tags: ' + one.nodeTags)
            }
        } else {
            if (hbOkNodeList.size() < one.replicas && !Conf.instance.isOn('rm.isSingleNodeTest')) {
                resp.halt(500, 'not enough node ready')
            }
        }

        // create app
        def app = new AppDTO()
        app.clusterId = clusterId
        app.namespaceId = namespaceId
        app.name = 'rm_sentinel_' + one.name
        app.status = AppDTO.Status.manual.val
        app.updatedDate = new Date()

        def conf = new AppConf()
        conf.containerNumber = one.replicas
        conf.registryId = BasePlugin.addRegistryIfNotExist('docker.1ms.run', 'https://docker.1ms.run')
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

        final String dataDir = RedisManager.dataDir()
        def sentinelDataDir = dataDir + '/sentinel_data_' + Utils.uuid()
        def nodeVolumeId = new NodeVolumeDTO(imageName: 'library/redis', name: 'for sentinel ' + one.name, dir: sentinelDataDir,
                clusterId: clusterId, des: 'data dir for sentinel').add()
        def dirOne = new DirVolumeMount(
                dir: sentinelDataDir, dist: '/data/sentinel', mode: 'rw',
                nodeVolumeId: nodeVolumeId)
        conf.dirVolumeList << dirOne

        def c = Conf.instance

        def tplOne = new ImageTplDTO(imageName: 'library/redis', name: 'sentinel.conf.tpl').one()
        def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content, dist: sentinelDataDir + '/${appId}_${instanceIndex}.conf')
        mountOne.isParentDirMount = true

        mountOne.paramList << new KVPair<String>('isSingleNode', c.isOn('rm.isSingleNodeTest').toString())
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

        RmJobExecutor.instance.runCreatingAppJob(app)

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
            resp.halt(500, 'not exists')
        }

        if (one.status == RmSentinelServiceDTO.Status.deleted) {
            resp.halt(500, 'already deleted')
        }

        if (one.status == RmSentinelServiceDTO.Status.creating) {
            resp.halt(500, 'creating, please wait')
        }

        // check if is used
        def serviceOne = new RmServiceDTO(sentinelServiceId: id).queryFields('name,status').one()
        if (serviceOne && serviceOne.status != RmServiceDTO.Status.deleted) {
            resp.halt(500, "this sentinel service is used by service: ${serviceOne.name}")
        }

        RedisManager.stopContainers(one.appId)

        new RmSentinelServiceDTO(id: id, status: RmSentinelServiceDTO.Status.deleted, updatedDate: new Date()).update()
        [flag: true]
    }
}
