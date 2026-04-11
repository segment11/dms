package model.job

import model.vendor.TargetBucket
import org.segment.d.D
import org.segment.d.Ds
import spock.lang.Specification
import support.DmsTestDbSupport

class RmBackupTemplateDTOTest extends Specification {
    private Ds ds
    private D d

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, '''
create table rm_backup_template
(
    id              int auto_increment primary key,
    name            varchar(50),
    target_type     varchar(20),
    provider        varchar(20),
    target_bucket   varchar(200),
    target_node_ips varchar(200),
    backup_data_dir varchar(200),
    updated_date    timestamp default current_timestamp
)
''')
    }

    def cleanup() {
        ds?.closeConnect()
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    def 'crud persists enum json and array fields for backup templates'() {
        given:
        def created = new RmBackupTemplateDTO(
                name: 'daily-backup',
                targetType: RmBackupTemplateDTO.TargetType.s3,
                provider: RmBackupTemplateDTO.Provider.aliyun,
                targetBucket: new TargetBucket(
                        bucketName: 'bucket-a',
                        endpoint: 'oss-cn-hangzhou.aliyuncs.com',
                        accessKey: 'ak',
                        secretKey: 'sk'
                ),
                targetNodeIps: ['10.0.0.11', '10.0.0.12'] as String[],
                backupDataDir: 'redis/daily',
                updatedDate: new Date()
        )

        when:
        def id = created.add()

        then:
        id > 0

        when:
        def loaded = new RmBackupTemplateDTO(id: id).one()

        then:
        loaded.name == 'daily-backup'
        loaded.targetType == RmBackupTemplateDTO.TargetType.s3
        loaded.provider == RmBackupTemplateDTO.Provider.aliyun
        loaded.targetBucket.bucketName == 'bucket-a'
        loaded.targetBucket.endpoint == 'oss-cn-hangzhou.aliyuncs.com'
        loaded.targetNodeIps as List == ['10.0.0.11', '10.0.0.12']
        loaded.backupDataDir == 'redis/daily'

        when:
        loaded.provider = RmBackupTemplateDTO.Provider.aws
        loaded.targetNodeIps = ['10.0.0.21'] as String[]
        loaded.backupDataDir = 'redis/weekly'
        def updated = loaded.update()

        then:
        updated == 1

        when:
        def reloaded = new RmBackupTemplateDTO(id: id).one()

        then:
        reloaded.provider == RmBackupTemplateDTO.Provider.aws
        reloaded.targetNodeIps as List == ['10.0.0.21']
        reloaded.backupDataDir == 'redis/weekly'

        when:
        def deleted = new RmBackupTemplateDTO(id: id).delete()

        then:
        deleted == 1
        new RmBackupTemplateDTO(id: id).one() == null
    }
}
