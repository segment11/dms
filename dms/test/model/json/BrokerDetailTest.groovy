package model.json

import spock.lang.Specification

class BrokerDetailTest extends Specification {
    def 'findByBrokerId returns matching broker'() {
        given:
        def detail = new BrokerDetail(brokers: [
                new BrokerDetail.BrokerNode(brokerId: 0, ip: '10.0.0.1', port: 9092),
                new BrokerDetail.BrokerNode(brokerId: 1, ip: '10.0.0.2', port: 9092)
        ])

        expect:
        detail.findByBrokerId(1).ip == '10.0.0.2'
        detail.findByBrokerId(99) == null
    }

    def 'findByIpPort returns matching broker'() {
        given:
        def detail = new BrokerDetail(brokers: [
                new BrokerDetail.BrokerNode(brokerId: 0, ip: '10.0.0.1', port: 9092),
                new BrokerDetail.BrokerNode(brokerId: 1, ip: '10.0.0.2', port: 9093)
        ])

        expect:
        detail.findByIpPort('10.0.0.2', 9093).brokerId == 1
        detail.findByIpPort('10.0.0.1', 9093) == null
    }

    def 'activeBrokers returns all brokers'() {
        given:
        def brokers = [
                new BrokerDetail.BrokerNode(brokerId: 0, ip: '10.0.0.1', port: 9092),
                new BrokerDetail.BrokerNode(brokerId: 1, ip: '10.0.0.2', port: 9092)
        ]
        def detail = new BrokerDetail(brokers: brokers)

        expect:
        detail.activeBrokers() == brokers
    }

    def 'activeBrokers returns empty list when no brokers'() {
        given:
        def detail = new BrokerDetail()

        expect:
        detail.activeBrokers() == []
    }

    def 'BrokerNode uuid returns ip port format'() {
        given:
        def node = new BrokerDetail.BrokerNode(ip: '10.0.0.1', port: 9092)

        expect:
        node.uuid() == '10.0.0.1:9092'
    }

    def 'BrokerNode fields are set correctly'() {
        given:
        def node = new BrokerDetail.BrokerNode(
                brokerId: 2,
                brokerIndex: 1,
                isController: true,
                ip: '10.0.0.3',
                port: 9093,
                rackId: 'rack-A'
        )

        expect:
        node.brokerId == 2
        node.brokerIndex == 1
        node.isController
        node.ip == '10.0.0.3'
        node.port == 9093
        node.rackId == 'rack-A'
    }
}
