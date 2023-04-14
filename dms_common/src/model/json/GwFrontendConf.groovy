package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GwFrontendConf implements JSONFiled {
    boolean passHostHeader

    String extractorFunc

    String extractorFuncHeaderName

    List<GwFrontendRateLimitConf> rateLimitConfList = []

    List<GwFrontendRuleConf> ruleConfList = []
}
