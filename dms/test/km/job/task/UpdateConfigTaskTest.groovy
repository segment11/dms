package km.job.task

import spock.lang.Specification

class UpdateConfigTaskTest extends Specification {

    void 'merge config keeps existing keys and overrides same keys'() {
        given:
        Map<String, String> existing = ['a': '1', 'b': '2']

        when:
        def r = UpdateConfigTask.mergeConfig(new LinkedHashMap<>(existing), [b: '20', c: '3'])

        then:
        r == [a: '1', b: '20', c: '3']
        // source map untouched
        existing == [a: '1', b: '2']
    }

    void 'merge config on null existing returns overrides copy'() {
        when:
        def r = UpdateConfigTask.mergeConfig(null, [x: '1'])

        then:
        r == [x: '1']
    }
}
