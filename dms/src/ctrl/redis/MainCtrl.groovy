package ctrl.redis

import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import rm.RedisManager
import server.InMemoryAllContainerManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis') {
    h.get('/overview') { req, resp ->
        [:]
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
