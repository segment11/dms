package model


import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class DynConfigDTO extends BaseRecord<DynConfigDTO> {
    Integer id

    String name

    String vv

    Date updatedDate

    static boolean acquireLock(String vv, int ttl, String name) {
        def now = new Date()
        def expiredDate = new Date(System.currentTimeMillis() - ttl * 1000)

        int c = new DynConfigDTO().useD().exeUpdate('''
update dyn_config set vv = ?, updated_date = ? where name = ? and 
    (vv = ? or updated_date < ?)
''', [vv, now, name, vv, expiredDate])
        c == 1
    }

    static boolean releaseLock(String expectedVv, String name) {
        def now = new Date()
        int c = new DynConfigDTO().useD().exeUpdate('''
update dyn_config set vv = '0', updated_date = ? where name = ? and vv = ?
''', [now, name, expectedVv])
        c == 1
    }

    static void addLockRow(String name) {
        def one = new DynConfigDTO(name: name).one()
        if (one == null) {
            new DynConfigDTO(name: name, vv: '0').add()
        }
    }
}