package ctrl.redis

import org.segment.web.handler.ChainHandler
import server.InMemoryAllContainerManager

def h = ChainHandler.instance

h.group('/redis') {
    h.get('/overview') { req, resp ->
        [:]
    }

    // options
    h.get('/node/tag/list') { req, resp ->
        final int clusterId = 1

        def instance = InMemoryAllContainerManager.instance
        def hbOkNodeList = instance.hbOkNodeList(clusterId, 'ip,tags')

        def tags = []
        for (one in hbOkNodeList) {
            if (one.tags) {
                tags.addAll(one.tags.split(','))
            }
        }

        [tags: tags.unique()]
    }
}
