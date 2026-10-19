package km

import org.segment.d.Ds
import spock.lang.Specification
import support.DmsTestDbSupport

class KafkaManagerDeployFileTest extends Specification {
    private Ds ds

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        def d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, '''
create table deploy_file
(
    id           int auto_increment primary key,
    local_path   varchar(200),
    dest_path    varchar(200),
    file_len     bigint,
    is_overwrite bit,
    init_cmd     varchar(200),
    updated_date timestamp default current_timestamp
)
''')
    }

    def cleanup() {
        ds?.closeConnect()
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    void 'start kafka deploy file id resolves by dest path'() {
        given:
        def d = DmsTestDbSupport.newMysqlStyleD(ds)
        d.exe("insert into deploy_file (local_path, dest_path) values ('/opt/x.sh', '/tmp/start-kafka.sh')")

        when:
        def id = KafkaManager.startKafkaDeployFileId()

        then:
        id > 0
        id == d.query('select id from deploy_file where dest_path = ?', ['/tmp/start-kafka.sh'])[0].id
    }

    void 'start kafka deploy file id falls back to 1 when not configured'() {
        expect:
        KafkaManager.startKafkaDeployFileId() == 1
    }
}
