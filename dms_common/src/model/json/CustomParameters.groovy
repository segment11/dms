package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class CustomParameters implements JSONFiled {
    @CompileStatic
    @ToString(includeNames = true)
    static class Param {
        String name
        String types
        String ranges
        String defaultValue
        Boolean needRestart
    }

    List<Param> list

    boolean asBoolean() {
        list != null && list.size() > 0
    }
}
