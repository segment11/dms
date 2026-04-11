package metric

import spock.lang.Specification

class SimpleGaugeTest extends Specification {
    def 'collect merges samples from non-null raw getters'() {
        given:
        def gauge = new SimpleGauge('dms_test_metric', 'sample gauge', ['cluster'])
        gauge.addRawGetter {
            [
                    'dms.metric.a': new SimpleGauge.ValueWithLabelValues(1.0d, ['cluster-a'])
            ]
        }
        gauge.addRawGetter {
            null
        }
        gauge.addRawGetter {
            [
                    'dms.metric.b': new SimpleGauge.ValueWithLabelValues(2.0d, ['cluster-b'])
            ]
        }

        when:
        def families = gauge.collect()

        then:
        families.size() == 1
        families[0].name == 'dms_test_metric'
        families[0].samples*.name == ['dms.metric.a', 'dms.metric.b']
        families[0].samples*.value == [1.0d, 2.0d]
        families[0].samples*.labelValues == [['cluster-a'], ['cluster-b']]

        when:
        gauge.clearRawGetterList()

        then:
        gauge.collect()[0].samples.isEmpty()
    }
}
