package ha

import groovy.transform.CompileStatic
import redis.clients.jedis.Jedis

@CompileStatic
interface JedisCallback {
    Object call(Jedis jedis)
}