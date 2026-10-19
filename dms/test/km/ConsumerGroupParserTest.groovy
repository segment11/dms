package km

import spock.lang.Specification

class ConsumerGroupParserTest extends Specification {

    private static final String DESCRIBE_OUTPUT = '''
Note: This will only show information about consumers that use the Java consumer API (non-ZooKeeper-based consumers).
To see information about consumers that use ZooKeeper (non-Java consumers), run the CLI with --zookeeper.

GROUP    TOPIC     PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG   CONSUMER-ID                                             HOST         CLIENT-ID
testgrp  e2e-topic 0          100             1000            900   consumer-testgrp-1-8738f5e5-5d51-4a46-9fbf-fc57c1b0b11f /10.0.0.1    consumer-testgrp-1
testgrp  e2e-topic 1          50              500             450   consumer-testgrp-1-8738f5e5-5d51-4a46-9fbf-fc57c1b0b11f /10.0.0.1    consumer-testgrp-1
testgrp  other-t   0          10              20              10    -                                                       -            -
'''

    void 'parseGroupList filters note and blank lines'() {
        expect:
        ConsumerGroupParser.parseGroupList('''
Warning: --bootstrap-server only shows consumers using the Java consumer API

testgrp
another-group
''') == ['testgrp', 'another-group']
    }

    void 'parseGroupList returns empty list for blank output'() {
        expect:
        ConsumerGroupParser.parseGroupList('') == []
        ConsumerGroupParser.parseGroupList(null) == []
    }

    void 'parseGroupDescribe maps header columns to entries'() {
        when:
        def r = ConsumerGroupParser.parseGroupDescribe(DESCRIBE_OUTPUT, 'testgrp')

        then:
        r.groupId == 'testgrp'
        def partitions = r.partitions as List
        partitions.size() == 3
        partitions[0].TOPIC == 'e2e-topic'
        partitions[0].PARTITION == '0'
        partitions[0]['CURRENT-OFFSET'] == '100'
        partitions[0]['LOG-END-OFFSET'] == '1000'
        partitions[0].LAG == '900'
        partitions[0]['CONSUMER-ID'] ==~ /consumer-testgrp-1-.+/
        partitions[2]['CONSUMER-ID'] == '-'
    }

    void 'parseGroupDescribe no offsets case'() {
        expect:
        ConsumerGroupParser.parseGroupDescribe("Consumer group 'testgrp' does not exist.", 'testgrp').partitions == []
    }

    void 'parseGroupLag sums lag per topic and total'() {
        when:
        def r = ConsumerGroupParser.parseGroupLag(DESCRIBE_OUTPUT, 'testgrp')

        then:
        r.groupId == 'testgrp'
        r.totalLag == 1360L
        def topics = r.topics as List
        topics.size() == 2
        def e2e = topics.find { it.topic == 'e2e-topic' }
        e2e.lag == 1350L
        e2e.logEndOffset == 1500L
        e2e.currentOffset == 150L
        e2e.partitionCount == 2
        def other = topics.find { it.topic == 'other-t' }
        other.lag == 10L
        other.logEndOffset == 20L
        other.currentOffset == 10L
        other.partitionCount == 1
    }

    void 'parseGroupLag no data case'() {
        when:
        def r = ConsumerGroupParser.parseGroupLag('Note: nothing here', 'g')

        then:
        r.totalLag == -1L
        r.topics == []
        r.raw != null
    }
}
