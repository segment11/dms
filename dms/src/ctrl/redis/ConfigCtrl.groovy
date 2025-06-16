package ctrl.redis

import model.RmServiceDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import rm.RedisManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis/config') {
    h.post('/update-maxmemory') { req, resp ->
        def map = req.bodyAs(HashMap)
        def id = map.id as int
        def maxmemoryMb = map.maxmemoryMb as int

        if (maxmemoryMb > RedisManager.ONE_INSTANCE_MAX_MEMORY_MB) {
            resp.halt(409, 'maxmemory mb can not bigger than ' + RedisManager.ONE_INSTANCE_MAX_MEMORY_MB)
        }

        def one = new RmServiceDTO(id: id).one()
        assert one

        def runningContainerList = one.runningContainerList()
        runningContainerList.each { x ->
            one.connectAndExe(x) { jedis ->
                def result = jedis.configSet('maxmemory', '' + maxmemoryMb + 'mb')
                log.warn 'set maxmemory {} MB to {}:{}, result: {}', maxmemoryMb, x.nodeIp, one.listenPort(x), result
            }
        }

        new RmServiceDTO(id: id, maxmemoryMb: maxmemoryMb, updatedDate: new Date()).update()
        [flag: true]
    }
}