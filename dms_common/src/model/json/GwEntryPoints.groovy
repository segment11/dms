package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class GwEntryPoints implements JSONFiled {
    List<String> entryPoints = []

    boolean asBoolean() {
        entryPoints
    }
}
