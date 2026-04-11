package plugin.redis

import spock.lang.Specification

class RedisManagerBackupPluginTest extends Specification {
    def 'canUseTo only accepts supported redis manager engine images'() {
        given:
        def plugin = new RedisManagerBackupPlugin()

        expect:
        plugin.canUseTo(group, image) == expected

        where:
        group      | image    || expected
        'library'  | 'redis'  || true
        'library'  | 'valkey' || true
        'montplex' | 'engula' || true
        'library'  | 'mysql'  || false
        'other'    | 'redis'  || false
    }
}
