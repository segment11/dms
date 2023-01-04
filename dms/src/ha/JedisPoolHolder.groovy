package ha

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

@CompileStatic
@Singleton
@Slf4j
class JedisPoolHolder {

    private Map<String, JedisPool> cached = [:]

    synchronized JedisPool create(String host, int port, String password = null, int timeoutMills = 2000) {
        String key = host + ':' + port
        def client = cached[key]
        if (client) {
            return client
        }

        def conf = new JedisPoolConfig()
        conf.maxTotal = 5
        conf.maxIdle = 2
        conf.maxWaitMillis = 2000L

        def one = new JedisPool(conf, host, port, timeoutMills, password)
        log.info 'connected - {}', key
        cached[key] = one
        one
    }

    static Object useRedisPool(JedisPool jedisPool, JedisCallback callback) {
        Jedis jedis = jedisPool.resource
        try {
            callback.call(jedis)
        } finally {
            jedis.close()
        }
    }

    void close() {
        cached.each { k, v ->
            log.info 'ready to close redis pool - {}', k
            v.close()
            log.info 'done close redis pool - {}', k
        }
    }
}
