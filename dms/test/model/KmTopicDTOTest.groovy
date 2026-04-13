package model

import model.json.ExtendParams
import org.segment.d.D
import org.segment.d.Ds
import spock.lang.Specification
import support.DmsTestDbSupport

class KmTopicDTOTest extends Specification {
    private Ds ds
    private D d

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, '''
create table km_topic
(
    id                int auto_increment primary key,
    service_id        int,
    name              varchar(200),
    partitions        int,
    replication_factor int,
    config_overrides  varchar(2000),
    status            varchar(20),
    created_date      timestamp,
    updated_date      timestamp default current_timestamp
)
''')
    }

    def cleanup() {
        ds?.closeConnect()
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    def 'crud persists enum and json fields for km topic'() {
        given:
        def created = new KmTopicDTO(
                serviceId: 1,
                name: 'test-topic',
                partitions: 6,
                replicationFactor: 3,
                configOverrides: new ExtendParams([
                        'retention.ms': '86400000',
                        'cleanup.policy': 'compact'
                ]),
                status: KmTopicDTO.Status.creating,
                createdDate: new Date()
        )

        when:
        def id = created.add()

        then:
        id > 0

        when:
        def loaded = new KmTopicDTO(id: id).one()

        then:
        loaded.serviceId == 1
        loaded.name == 'test-topic'
        loaded.partitions == 6
        loaded.replicationFactor == 3
        loaded.configOverrides.getString('retention.ms', '') == '86400000'
        loaded.configOverrides.getString('cleanup.policy', '') == 'compact'
        loaded.status == KmTopicDTO.Status.creating

        when:
        loaded.status = KmTopicDTO.Status.active
        loaded.partitions = 12
        loaded.configOverrides = new ExtendParams([
                'retention.ms': '172800000'
        ])
        def updated = loaded.update()

        then:
        updated == 1

        when:
        def reloaded = new KmTopicDTO(id: id).one()

        then:
        reloaded.status == KmTopicDTO.Status.active
        reloaded.partitions == 12
        reloaded.configOverrides.getString('retention.ms', '') == '172800000'
        reloaded.configOverrides.get('cleanup.policy') == null

        when:
        def deleted = new KmTopicDTO(id: id).delete()

        then:
        deleted == 1
        new KmTopicDTO(id: id).one() == null
    }
}
