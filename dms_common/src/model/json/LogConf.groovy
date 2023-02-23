package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class LogConf implements JSONFiled {
    @CompileStatic
    @ToString(includeNames = true)
    static class LogFile {
        String pathPattern
        Boolean isMultilineSupport
        String multilinePattern
    }

    List<LogFile> logFileList = []

    boolean asBoolean() {
        logFileList?.size() > 0
    }
}
