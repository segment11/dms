package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class GwBackendServer {
    String url
    int weight
}
