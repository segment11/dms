package tools

import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.dialect.MySQLDialect

def ds = Ds.h2Local('/home/kerry/dms/db;FILE_LOCK=SOCKET')
def ds2 = Ds.dbType(Ds.DBType.mysql).connect('192.168.1.14', 3306, 'dms', 'root', 'test1234')
def d = new D(ds, new MySQLDialect())
def d2 = new D(ds2, new MySQLDialect())

Set<String> skipTables = ['APP_JOB', 'APP_JOB_LOG', 'CLUSTER', 'EVENT']
d.query('show tables', String).each { table ->
    if (skipTables.contains(table)) {
        println 'skip ' + table
        return
    }

    def tableTo = table.toLowerCase()
    def ids = d2.query('select id from ' + tableTo, Integer)

    def list = d.query('select * from ' + table, HashMap)
    if (list) {
        list.each { map ->
            def id = map.id as Integer
            if (ids.contains(id)) {
                return
            }
            d2.add(map, tableTo)
        }
        println 'done ' + table + ', count: ' + list.size()
    }
}

ds.closeConnect()
ds2.closeConnect()