package model.json

import spock.lang.Specification

class KmSnapshotContentTest extends Specification {
    def 'construct with all fields'() {
        given:
        def broker = new KmSnapshotContent.BrokerEntry(
                brokerId: 1,
                host: '10.0.0.1',
                port: 9092,
                rackId: 'rack-A',
                logDirs: '/data/kafka/data'
        )
        def topic = new KmSnapshotContent.TopicEntry(
                name: 'my-topic',
                partitions: 6,
                replicationFactor: 3,
                configOverrides: ['retention.ms': '604800000']
        )
        def content = new KmSnapshotContent(
                serviceName: 'test-cluster',
                mode: 'cluster',
                kafkaVersion: '3.6.0',
                snapshotDate: new Date(),
                brokers: [broker],
                zkConnectString: 'zk1:2181,zk2:2181',
                zkChroot: '/kafka/my_cluster',
                topics: [topic],
                configItems: [new KVPair<String>('num.network.threads', '3')],
                configOverrides: ['log.retention.hours': '168']
        )

        expect:
        content.serviceName == 'test-cluster'
        content.mode == 'cluster'
        content.kafkaVersion == '3.6.0'
        content.snapshotDate != null
        content.brokers.size() == 1
        content.zkConnectString == 'zk1:2181,zk2:2181'
        content.zkChroot == '/kafka/my_cluster'
        content.topics.size() == 1
        content.configItems.size() == 1
        content.configOverrides['log.retention.hours'] == '168'
    }

    def 'default collections are empty'() {
        given:
        def content = new KmSnapshotContent()

        expect:
        content.brokers == []
        content.topics == []
        content.configOverrides == [:]
    }

    def 'BrokerEntry fields are set correctly'() {
        given:
        def broker = new KmSnapshotContent.BrokerEntry(
                brokerId: 2,
                host: '10.0.0.2',
                port: 9093,
                rackId: 'rack-B',
                logDirs: '/data/kafka/logs'
        )

        expect:
        broker.brokerId == 2
        broker.host == '10.0.0.2'
        broker.port == 9093
        broker.rackId == 'rack-B'
        broker.logDirs == '/data/kafka/logs'
    }

    def 'TopicEntry fields are set correctly'() {
        given:
        def topic = new KmSnapshotContent.TopicEntry(
                name: 'orders',
                partitions: 12,
                replicationFactor: 2,
                configOverrides: ['cleanup.policy': 'compact']
        )

        expect:
        topic.name == 'orders'
        topic.partitions == 12
        topic.replicationFactor == 2
        topic.configOverrides['cleanup.policy'] == 'compact'
    }
}
