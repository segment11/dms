package model

import common.Const
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class DynConfigDTO extends BaseRecord<DynConfigDTO> {

    String name

    String value

    Date updatedDate

    static boolean acquireLock(String value, int ttl) {
        def now = new Date()
        def expiredDate = new Date(System.currentTimeMillis() - ttl * 1000)

        int c = new DynConfigDTO().useD().exeUpdate('''
update dyn_config set value = ?, updated_date = ? where name = ? and 
    (value = ? or updated_date < ?)
''', [value, now, Const.SERVER_LEADER_LOCK_KEY, value, expiredDate])
        c == 1
    }

    static void addServerLeaderLockRow() {
        def one = new DynConfigDTO(name: Const.SERVER_LEADER_LOCK_KEY).one()
        if (one == null) {
            new DynConfigDTO(name: Const.SERVER_LEADER_LOCK_KEY, value: '0').add()
        }
    }
}