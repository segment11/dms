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

    String getString(String key, String defaultValue) {
        def val = get(key)
        return val == null ? defaultValue : val as String
    }

    int getInt(String key, int defaultValue) {
        def val = get(key)
        return val == null ? defaultValue : val as int
    }

    double getDouble(String key, double defaultValue) {
        def val = get(key)
        return val == null ? defaultValue : val as double
    }

    void put(String key, Object value) {
        params[key] = value
    }

    boolean asBoolean() {
        params != null && params.size() > 0
    }
}
