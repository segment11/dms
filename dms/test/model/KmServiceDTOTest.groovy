package model

import model.json.BrokerDetail
import model.json.ExtendParams
import model.json.LogPolicy
import org.segment.d.D
import org.segment.d.Ds
import spock.lang.Specification
import support.DmsTestDbSupport

class KmServiceDTOTest extends Specification {
    private Ds ds
    private D d

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, '''
create table km_service
(
    id                         int auto_increment primary key,
    name                       varchar(50),
    des                        varchar(200),
    mode                       varchar(20),
    kafka_version              varchar(20),
    config_template_id         int,
    config_overrides           varchar(2000),
    zk_connect_string          varchar(500),
    zk_chroot                  varchar(200) not null,
    app_id                     int,
    port                       int,
    brokers                    int,
    default_replication_factor int,
    default_partitions         int,
    heap_mb                    int,
    pass                       varchar(200),
    is_sasl_on                 bit,
    is_tls_on                  bit,
    node_tags                  varchar(100),
    node_tags_by_broker_index  varchar(500),
    log_policy                 varchar(200),
    status                     varchar(20),
    extend_params              varchar(2000),
    broker_detail              varchar(4000),
    last_updated_message       varchar(200),
    created_date               timestamp,
    updated_date               timestamp default current_timestamp
)
''')
    }

    def cleanup() {
        ds?.closeConnect()
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    def 'insert and load km service with all field types'() {
        given:
        def dto = new KmServiceDTO(
                name: 'test-kafka',
                des: 'test cluster',
                mode: KmServiceDTO.Mode.cluster,
                kafkaVersion: '2.8.2',
                configTemplateId: 1,
                configOverrides: new ExtendParams(['num.io.threads': '8', 'num.network.threads': '3']),
                zkConnectString: 'zk1:2181,zk2:2181',
                zkChroot: '/kafka/test-kafka',
                appId: 100,
                port: 9092,
                brokers: 3,
                defaultReplicationFactor: 2,
                defaultPartitions: 12,
                heapMb: 2048,
                pass: 'secret',
                isSaslOn: true,
                isTlsOn: false,
                nodeTags: ['tag1', 'tag2'] as String[],
                nodeTagsByBrokerIndex: ['broker-0:tag1'] as String[],
                logPolicy: new LogPolicy(isSlowCollect: true, isEngineCollect: false),
                status: KmServiceDTO.Status.creating,
                extendParams: new ExtendParams(['key1': 'value1']),
                brokerDetail: new BrokerDetail(brokers: [
                        new BrokerDetail.BrokerNode(brokerId: 0, ip: '10.0.0.1', port: 9092)
                ]),
                lastUpdatedMessage: 'creating',
                createdDate: new Date(),
                updatedDate: new Date()
        )

        when:
        def id = dto.add()

        then:
        id > 0

        when:
        def loaded = new KmServiceDTO(id: id).one()

        then:
        loaded.name == 'test-kafka'
        loaded.des == 'test cluster'
        loaded.mode == KmServiceDTO.Mode.cluster
        loaded.kafkaVersion == '2.8.2'
        loaded.configTemplateId == 1
        loaded.configOverrides?.params?.'num.io.threads' == '8'
        loaded.configOverrides?.params?.'num.network.threads' == '3'
        loaded.zkConnectString == 'zk1:2181,zk2:2181'
        loaded.zkChroot == '/kafka/test-kafka'
        loaded.appId == 100
        loaded.port == 9092
        loaded.brokers == 3
        loaded.defaultReplicationFactor == 2
        loaded.defaultPartitions == 12
        loaded.heapMb == 2048
        loaded.pass == 'secret'
        loaded.isSaslOn == true
        loaded.isTlsOn == false
        loaded.nodeTags as List == ['tag1', 'tag2']
        loaded.nodeTagsByBrokerIndex as List == ['broker-0:tag1']
        loaded.logPolicy.isSlowCollect == true
        loaded.logPolicy.isEngineCollect == false
        loaded.status == KmServiceDTO.Status.creating
        loaded.extendParams?.params?.key1 == 'value1'
        loaded.brokerDetail.brokers.size() == 1
        loaded.brokerDetail.brokers[0].brokerId == 0
        loaded.brokerDetail.brokers[0].ip == '10.0.0.1'
        loaded.lastUpdatedMessage == 'creating'
        loaded.createdDate != null
        loaded.updatedDate != null
    }

    def 'update persists changed fields'() {
        given:
        def dto = new KmServiceDTO(
                name: 'update-test',
                zkChroot: '/kafka/update-test',
                mode: KmServiceDTO.Mode.standalone,
                status: KmServiceDTO.Status.creating,
                createdDate: new Date(),
                updatedDate: new Date()
        )
        def id = dto.add()

        when:
        def loaded = new KmServiceDTO(id: id).one()
        loaded.status = KmServiceDTO.Status.running
        loaded.lastUpdatedMessage = 'now running'
        loaded.brokers = 5
        loaded.nodeTags = ['new-tag'] as String[]
        loaded.updatedDate = new Date()
        def updated = loaded.update()

        then:
        updated == 1

        when:
        def reloaded = new KmServiceDTO(id: id).one()

        then:
        reloaded.status == KmServiceDTO.Status.running
        reloaded.lastUpdatedMessage == 'now running'
        reloaded.brokers == 5
        reloaded.nodeTags as List == ['new-tag']
    }

    def 'delete removes record'() {
        given:
        def dto = new KmServiceDTO(
                name: 'delete-test',
                zkChroot: '/kafka/delete-test',
                mode: KmServiceDTO.Mode.standalone,
                status: KmServiceDTO.Status.stopped,
                createdDate: new Date(),
                updatedDate: new Date()
        )
        def id = dto.add()

        when:
        def deleted = new KmServiceDTO(id: id).delete()

        then:
        deleted == 1
        new KmServiceDTO(id: id).one() == null
    }

    def 'status canChangeToRunningWhenInstancesRunningOk'() {
        expect:
        KmServiceDTO.Status.creating.canChangeToRunningWhenInstancesRunningOk()
        KmServiceDTO.Status.scaling_up.canChangeToRunningWhenInstancesRunningOk()
        KmServiceDTO.Status.scaling_down.canChangeToRunningWhenInstancesRunningOk()
        !KmServiceDTO.Status.running.canChangeToRunningWhenInstancesRunningOk()
        !KmServiceDTO.Status.stopped.canChangeToRunningWhenInstancesRunningOk()
        !KmServiceDTO.Status.deleted.canChangeToRunningWhenInstancesRunningOk()
        !KmServiceDTO.Status.unhealthy.canChangeToRunningWhenInstancesRunningOk()
    }

    def 'query by status returns matching records'() {
        given:
        new KmServiceDTO(
                name: 'svc-1', zkChroot: '/kafka/svc-1', mode: KmServiceDTO.Mode.standalone,
                status: KmServiceDTO.Status.running, createdDate: new Date(), updatedDate: new Date()
        ).add()
        new KmServiceDTO(
                name: 'svc-2', zkChroot: '/kafka/svc-2', mode: KmServiceDTO.Mode.cluster,
                status: KmServiceDTO.Status.creating, createdDate: new Date(), updatedDate: new Date()
        ).add()

        when:
        def running = new KmServiceDTO(status: KmServiceDTO.Status.running).list()

        then:
        running.size() == 1
        running[0].name == 'svc-1'
        running[0].mode == KmServiceDTO.Mode.standalone
    }
}
