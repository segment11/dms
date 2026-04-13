package utils

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.d.D
import org.segment.d.dialect.MySQLDialect
import support.DefaultLocalH2DataSourceCreator

@CompileStatic
@Slf4j
class DBInit {
    static void init() {
        def ds = DefaultLocalH2DataSourceCreator.create()
        def d = new D(ds, new MySQLDialect())

        def c = Conf.instance
        boolean isPG = c.getString('db.driver', '').contains('postgre')

        String queryTableNameSql = isPG ?
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
                : "SELECT table_name FROM information_schema.tables"
        def tableNameList = d.query(queryTableNameSql, String).collect { it.toUpperCase() }
        if (!tableNameList.contains('CLUSTER')) {
            def ddlInitFile = new File(c.projectPath('/init_h2.sql'))
            ddlInitFile.text.split(';').each {
                def ddl = it.trim()
                if (!ddl) {
                    return
                }
                try {
                    d.exe(ddl)
                    log.info 'done execute ddl: \n {}', ddl
                } catch (Exception e) {
                    log.error 'execute ddl error, ddl: {}\n', ddl, e
                }
            }
        }
    }
}
