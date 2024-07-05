package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class GwClusterDTO extends BaseRecord<GwClusterDTO> {

    Integer id

    Integer appId

    String name

    String des

    String serverUrl

    Integer serverPort

    Integer dashboardPort

    Date createdDate

    Date updatedDate
}