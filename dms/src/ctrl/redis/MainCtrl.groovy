package ctrl.redis

import model.RmServiceDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import rm.RedisManager
import server.InMemoryAllContainerManager
import transfer.NodeInfo

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis') {
    h.get('/overview') { req, resp ->
        def list = new RmServiceDTO(status: RmServiceDTO.Status.running).queryFields('app_id,maxmemory_mb,cluster_slots_detail').list()

        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getContainerList(RedisManager.CLUSTER_ID)
        def runningContainerList = containerList.findAll { x -> x.running() }

        Map<String, NodeInfo> r = instance.getAllNodeInfo(RedisManager.CLUSTER_ID)

        List<Map> nodeStatsList = []
        runningContainerList.groupBy { x ->
            x.nodeIp
        } each { nodeIp, subList ->
            int maxmemoryTotalMB = 0
            subList.each { x ->
                def one = list.find { service ->
                    service.checkIfAppBelongToThis(x.appId())
                }
                if (!one) {
                    return
                }

                maxmemoryTotalMB += (one.maxmemoryMb ?: 0)
            }

            def info = r[nodeIp]

            nodeStatsList << [
                    nodeIp           : nodeIp,
                    maxmemoryTotalMB : maxmemoryTotalMB,
                    instanceNumber   : subList.size(),
                    cpuUsedPercent   : (info.cpuUsedPercent() * 100).round(4),
                    memoryTotalMB    : info.mem.total,
                    memoryFreeMB     : info.mem.actualFree,
                    memoryUsedMB     : info.mem.actualUsed,
                    memoryUsedPercent: info.mem.usedPercent,
            ]
        }

        [nodeStatsList: nodeStatsList.sort { 0 - (it.memoryUsedPercent as double) }]
    }

    // options
    h.get('/node/tag/list') { req, resp ->
        def instance = InMemoryAllContainerManager.instance
        def hbOkNodeList = instance.hbOkNodeList(RedisManager.CLUSTER_ID, 'ip,tags')

        def tags = []
        for (one in hbOkNodeList) {
            if (one.tags) {
                tags.addAll(one.tags)
            }
        }

        [list: tags.unique().collect { [tag: it] }]
    }

    h.get('/setting') { req, resp ->
        def dataDir = RedisManager.dataDir()

        [dataDir: dataDir]
    }

    h.post('/setting/data-dir') { req, resp ->
        def map = req.bodyAs(HashMap)
        def dataDir = map.dataDir as String
        assert dataDir

        RedisManager.updateDataDir(dataDir)
        log.warn "update data dir to {}", dataDir
        [flag: true]
    }
}
