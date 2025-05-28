package model

import common.Const
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class DynConfigDTO extends BaseRecord<DynConfigDTO> {
    Integer id

    String name

    String vv

    Date updatedDate

    static boolean acquireLock(String vv, int ttl) {
        def now = new Date()
        def expiredDate = new Date(System.currentTimeMillis() - ttl * 1000)

        int c = new DynConfigDTO().useD().exeUpdate('''
update dyn_config set vv = ?, updated_date = ? where name = ? and 
    (vv = ? or updated_date < ?)
''', [vv, now, Const.SERVER_LEADER_LOCK_KEY, vv, expiredDate])
        c == 1
    }

    static void addServerLeaderLockRow() {
        def one = new DynConfigDTO(name: Const.SERVER_LEADER_LOCK_KEY).one()
        if (one == null) {
            new DynConfigDTO(name: Const.SERVER_LEADER_LOCK_KEY, vv: '0').add()
        }
    }
}