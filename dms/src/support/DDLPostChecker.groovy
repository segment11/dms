package support

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.d.D

@CompileStatic
@Slf4j
@Singleton
class DDLPostChecker {
    private static final List<String> KAFKA_TABLE_NAME_LIST = [
            'KM_SERVICE',
            'KM_CONFIG_TEMPLATE',
            'KM_TOPIC',
            'KM_JOB',
            'KM_TASK_LOG',
            'KM_SNAPSHOT',
    ]
    private static final String KM_SERVICE_TABLE_NAME = 'km_service'
    private static final String NODE_TAGS_BY_BROKER_INDEX_COLUMN = 'node_tags_by_broker_index'
    private static final int NODE_TAGS_BY_BROKER_INDEX_LEN = 500

    void check(D d, boolean isPG) {
        def tableNameList = queryTableNameList(d, isPG)
        checkKafkaManager(d, isPG, tableNameList)
    }

    private void checkKafkaManager(D d, boolean isPG, List<String> tableNameList) {
        def missingTableNameList = KAFKA_TABLE_NAME_LIST.findAll { !(it in tableNameList) }
        if (missingTableNameList) {
            throw new IllegalStateException('ddl post check fail, missing kafka manager tables: ' + missingTableNameList)
        }

        Integer columnLen = queryColumnLen(d, isPG, KM_SERVICE_TABLE_NAME, NODE_TAGS_BY_BROKER_INDEX_COLUMN)
        if (columnLen == null) {
            throw new IllegalStateException('ddl post check fail, missing km_service.node_tags_by_broker_index')
        }
        if (columnLen < NODE_TAGS_BY_BROKER_INDEX_LEN) {
            def alterSql = isPG ?
                    'alter table km_service alter column node_tags_by_broker_index type varchar(500)'
                    : 'alter table km_service alter column node_tags_by_broker_index varchar(500)'
            d.exe(alterSql)
            log.info 'ddl post check repair success: {}', alterSql

            Integer repairedColumnLen = queryColumnLen(d, isPG, KM_SERVICE_TABLE_NAME, NODE_TAGS_BY_BROKER_INDEX_COLUMN)
            if (repairedColumnLen == null || repairedColumnLen < NODE_TAGS_BY_BROKER_INDEX_LEN) {
                throw new IllegalStateException('ddl post check fail, km_service.node_tags_by_broker_index len: ' + repairedColumnLen)
            }
        }
    }

    private List<String> queryTableNameList(D d, boolean isPG) {
        String sql = isPG ?
                "select table_name from information_schema.tables where table_schema = 'public'"
                : 'select table_name from information_schema.tables'
        d.query(sql, String).collect { it.toUpperCase() }
    }

    private Integer queryColumnLen(D d, boolean isPG, String tableName, String columnName) {
        String sql = isPG ?
                "select character_maximum_length from information_schema.columns where table_schema = 'public' and lower(table_name) = ? and lower(column_name) = ?"
                : 'select character_maximum_length from information_schema.columns where lower(table_name) = ? and lower(column_name) = ?'
        d.one(sql, [tableName.toLowerCase(), columnName.toLowerCase()], Integer)
    }
}
