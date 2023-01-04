package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class ABConf implements JSONFiled {
    Integer containerNumber

    String image

    String tag

    Integer weight

    boolean asBoolean() {
        image != null
    }
}
