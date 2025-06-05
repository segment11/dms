package rm

import groovy.transform.CompileStatic
import model.DynConfigDTO

@CompileStatic
class RedisManager {
    static final String DEFAULT_DATA_DIR = '/data/rm'

    static String dataDir() {
        def one = new DynConfigDTO(name: 'rm.data.dir').one()
        return one?.vv ?: DEFAULT_DATA_DIR
    }

    static void updateDataDir(String dataDir) {
        def one = new DynConfigDTO(name: 'rm.data.dir').one()
        if (one) {
            new DynConfigDTO(id: one.id, vv: dataDir, updatedDate: new Date()).update()
        } else {
            new DynConfigDTO(name: 'rm.data.dir', vv: dataDir, updatedDate: new Date()).add()
        }
    }
}
