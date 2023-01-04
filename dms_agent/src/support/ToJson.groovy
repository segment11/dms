package support

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic

@CompileStatic
class ToJson {
    static String json(Object obj) {
        def mapper = new ObjectMapper()
        mapper.serializationInclusion = JsonInclude.Include.NON_NULL
        mapper.writeValueAsString(obj)
    }

    static <T> T read(String string, Class<T> clz) {
        new ObjectMapper().readValue(string, clz)
    }

    static <T> T read(String string, TypeReference<T> clz) {
        new ObjectMapper().readValue(string, clz)
    }
}
