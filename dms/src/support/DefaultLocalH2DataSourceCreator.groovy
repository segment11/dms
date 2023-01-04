package support

import common.Conf
import groovy.transform.CompileStatic
import org.segment.d.Ds

@CompileStatic
class DefaultLocalH2DataSourceCreator {
    Ds create() {
        def c = Conf.instance

        Map<String, String> dbParams = [:]
        c.params.findAll { it.key.startsWith('db.') }.each { k, v ->
            dbParams[k[3..-1]] = v
        }
        if (dbParams.url) {
            Ds.register('other', dbParams.driver as String) { String ip, int port, String db ->
                dbParams.url
            }
            int i = c.getInt('db.minPoolSize', 5)
            int j = c.getInt('db.maxPoolSize', 10)
            return Ds.dbType('other').cacheAs('dms_server_ds').
                    connectWithPool('', 0, '', dbParams.user.toString(), dbParams.password.toString(), i, j).
                    export()
        }

        String dbDataDir = c.getString('dbDataDir', '/opt/dms/data')
        // for sql stats
        Ds.h2LocalWithPool(dbDataDir, 'dms_server_ds').export()
    }
}
