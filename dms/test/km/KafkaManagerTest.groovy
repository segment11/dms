package km

import spock.lang.Specification

class KafkaManagerTest extends Specification {

    def 'constants are correct'() {
        expect:
        KafkaManager.CLUSTER_ID == 1
        KafkaManager.ONE_CLUSTER_MAX_BROKERS == 32
        KafkaManager.MAX_PARTITIONS_PER_TOPIC == 256
    }

    def 'encode then decode returns original content'() {
        expect:
        KafkaManager.decode(KafkaManager.encode(content)) == content

        where:
        content << ['', 'hello', 'P@ssw0rd!', 'abc123XYZ', 'kafka-broker-test']
    }

    def 'encode produces different string from original'() {
        expect:
        KafkaManager.encode(content) != content

        where:
        content << ['hello', 'password', 'test123']
    }

    def 'encode shifts even index by 1 and odd index by 2'() {
        expect:
        char c0 = (char) 'a' + 1
        char c1 = (char) 'b' + 2
        KafkaManager.encode('ab') == new String([c0, c1] as char[])
    }
}
