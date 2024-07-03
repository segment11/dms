package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
@Deprecated
class GwFrontendRuleConf {
    String type
    String rule
}
