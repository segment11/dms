package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class ScriptPullContent implements JSONFiled {
    @CompileStatic
    @ToString(includeNames = true)
    static class ScriptPullOne {
        int id
        String name
        Date updatedDate
    }

    List<ScriptPullOne> list = []
}
