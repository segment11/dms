package support

import groovy.transform.CompileStatic
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.dialect.MySQLDialect

@CompileStatic
class DmsTestDbSupport {
    static final String DMS_DS_NAME = 'dms_server_ds'

    static Ds newCachedDmsH2Ds() {
        Ds.disconnectOne(DMS_DS_NAME)
        Ds.remove(DMS_DS_NAME)
        Ds.h2mem('dms_test_' + System.nanoTime()).cacheAs(DMS_DS_NAME)
    }

    static D newMysqlStyleD(Ds ds) {
        new D(ds, new MySQLDialect())
    }

    static void execStatements(D d, String sqlText) {
        sqlText
                .split(/;\s*(?:\r?\n|$)/)
                .collect { it.trim() }
                .findAll { !it.isEmpty() }
                .each { d.exe(it) }
    }

    static void cleanupCachedDmsDs() {
        Ds.disconnectOne(DMS_DS_NAME)
        Ds.remove(DMS_DS_NAME)
    }
}
