package tools

@Grab(group = 'com.h2database', module = 'h2', version = '1.4.200')
@Grab(group = 'xerces', module = 'xercesImpl', version = '2.12.2')
// cp ~/.groovy/grapes/com.h2database/h2/jars/h2-1.4.200.jar ./
// cp ~/.groovy/grapes/xerces/xercesImpl/jars/xercesImpl-2.12.2.jar ./
// groovy -cp h2-1.4.200.jar:xercesImpl-2.12.2.jar:../../build/libs/dms_server-1.2.jar ChangeJsonField.groovy

import com.alibaba.fastjson.JSON
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.dialect.MySQLDialect

def ds = Ds.h2Local('/data/dms/db;FILE_LOCK=SOCKET')
def d = new D(ds, new MySQLDialect())

d.query('select id, conf from app').each {
    def id = it.id as int
    def json = JSON.parseObject(it.conf as String)
    json.put('cpuShares', json.get('cpuShare'))
    json.remove('cpuShare')
    json.remove('isNetworkHostsUsingCluster')

    d.update([id: id, conf: json.toString()], 'app', 'id')
    println 'update app ' + id + ' success'
}

ds.closeConnect()