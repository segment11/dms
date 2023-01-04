package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class ExtendParams implements JSONFiled {
    Map<String, Object> params = [:]

    ExtendParams() {

    }

    ExtendParams(Map<String, Object> params) {
        this.params = params
    }

    Object get(String key) {
        if (!params) {
            return null
        }
        params[key]
    }

    String getString(String key) {
        get(key) as String
    }

    void put(String key, Object value) {
        params[key] = value
    }

    boolean asBoolean() {
        params != null && params.size() > 0
    }
}
