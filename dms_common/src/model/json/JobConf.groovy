package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class JobConf implements JSONFiled {
    String cronExp

    boolean isOn

    boolean asBoolean() {
        cronExp != null
    }
}
