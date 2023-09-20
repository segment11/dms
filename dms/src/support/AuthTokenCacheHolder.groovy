package support

import auth.User
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.segment.common.Conf
import groovy.transform.CompileStatic

import java.time.Duration

@CompileStatic
@Singleton
class AuthTokenCacheHolder {
    final static String ALG_SECRET = 'dms-as-secret'

    Date date

    Cache<String, User> cache

    void init() {
        def c = Conf.instance
        def maxSize = c.getInt('auth.token.cache.number', 1000)
        def expireHour = c.getInt('auth.token.cache.expire.hour', 1)

        cache = CacheBuilder.newBuilder().maximumSize(maxSize).
                expireAfterWrite(Duration.ofHours(expireHour)).build()
        date = new Date()
    }

    void cleanUp() {
        if (!cache) {
            return
        }
        cache.cleanUp()
    }

    User get(String authToken) {
        cache.getIfPresent(authToken)
    }

    void put(String authToken, User user) {
        cache.put(authToken, user)
    }

    void remove(String authToken) {
        cache.invalidate(authToken)
    }
}
