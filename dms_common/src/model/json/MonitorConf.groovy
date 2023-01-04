package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true)
class MonitorConf implements JSONFiled {
    public static final String KEY_SCALE_OUT = 'dms_scale_out'
    public static final Integer SCALE_OUT = 100
    public static final int SCALE_IN = 0

    Boolean isHttpRequest

    String httpRequestUri

    Integer port

    Integer httpTimeoutSeconds = 1

    Boolean isShellScript

    String shellScript

    Integer intervalSeconds = 30

    Integer scaleMin

    Integer scaleMax

    Boolean isScaleAuto

    Boolean isScaleDependOnCpuPerc

    Integer cpuPerc

    String metricFormatScriptContent

    Boolean isFirstInstancePullOnly

    boolean asBoolean() {
        isHttpRequest || isShellScript
    }
}
