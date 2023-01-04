package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class GwFrontendRuleConf {
    String type
    String rule
}
