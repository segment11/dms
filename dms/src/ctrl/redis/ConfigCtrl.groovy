package ctrl.redis

import model.RmServiceDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import rm.RedisManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis/config') {
    h.post('/update') { req, resp ->
        def map = req.bodyAs(HashMap)
        def id = map.id as int
        def key = map.key as String
        def value = map.value as String

        String configKey
        String configValue
        if (key == 'maxmemoryMb') {
            def maxmemoryMb = value as int
            if (maxmemoryMb > RedisManager.ONE_INSTANCE_MAX_MEMORY_MB) {
                resp.halt(409, 'maxmemory mb can not bigger than ' + RedisManager.ONE_INSTANCE_MAX_MEMORY_MB)
            }
            configKey = 'maxmemory'
            configValue = maxmemoryMb + 'mb'
        } else if (key == 'maxmemoryPolicy') {
            def acceptList = ['noeviction', 'allkeys-lru', 'allkeys-lfu', 'allkeys-random', 'volatile-lru', 'volatile-lfu', 'volatile-random', 'volatile-ttl']
            if (value !in acceptList) {
                resp.halt(409, 'maxmemory-policy can only be one of ' + acceptList.join(','))
            }
            configKey = 'maxmemory-policy'
            configValue = value
        } else {
            resp.halt(409, 'no support config key: ' + key)
        }

        def one = new RmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        def runningContainerList = one.runningContainerList()
        runningContainerList.each { x ->
            one.connectAndExe(x) { jedis ->
                def result = jedis.configSet(configKey, configValue)
                log.warn 'set {} {} MB to {}:{}, result: {}', key, configValue, x.nodeIp, one.listenPort(x), result
            }
        }

        log.warn 'update redis service config done, config key: {}, value: {}, service id: {}', configKey, configValue, id
        if (key == 'maxmemoryMb') {
            def maxmemoryMb = value as int
            new RmServiceDTO(id: id, maxmemoryMb: maxmemoryMb, updatedDate: new Date()).update()
        } else if (key == 'maxmemoryPolicy') {
            new RmServiceDTO(id: id, maxmemoryPolicy: value, updatedDate: new Date()).update()
        }
        [flag: true]
    }
}