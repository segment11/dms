package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.GwAuth
import model.json.GwBackend
import model.json.GwFrontendConf

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
@Deprecated
class GwFrontendDTO extends BaseRecord<GwFrontendDTO> {

    Integer id

    Integer clusterId

    String name

    String des

    GwBackend backend

    GwAuth auth

    Integer priority

    GwFrontendConf conf

    Date createdDate

    Date updatedDate
}