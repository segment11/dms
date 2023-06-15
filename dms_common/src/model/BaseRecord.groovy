package model

import common.Conf
import groovy.transform.CompileStatic
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.Record
import org.segment.d.dialect.MySQLDialect
import org.segment.d.dialect.PGDialect

@CompileStatic
class BaseRecord<V extends BaseRecord> extends Record<V> {
    @Override
    String pk() {
        'id'
    }

    @Override
    D useD() {
        boolean isPG = Conf.instance.getString('db.driver', '').contains('postgre')
        new D(Ds.one('dms_server_ds'), isPG ? new PGDialect() : new MySQLDialect())
    }
}
