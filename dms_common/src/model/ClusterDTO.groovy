package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.GlobalEnvConf

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class ClusterDTO extends BaseRecord<ClusterDTO> {

    Integer id

    String name

    String des

    String secret

    Boolean isInGuard

    GlobalEnvConf globalEnvConf

    Date updatedDate
}