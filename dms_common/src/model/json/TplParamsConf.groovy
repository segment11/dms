package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class TplParamsConf implements JSONFiled {
    @CompileStatic
    @ToString(includeNames = true)
    static class TplParam {
        String name
        String type
        String defaultValue
    }

    List<TplParam> paramList = []

    void addParam(String name, String defaultValue, String type) {
        paramList.add new TplParam(name: name, defaultValue: defaultValue, type: type)
    }
}
