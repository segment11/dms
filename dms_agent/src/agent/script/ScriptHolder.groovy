package agent.script

import agent.Agent
import com.alibaba.fastjson.JSON
import common.IntervalJob
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Singleton
@Slf4j
class ScriptHolder extends IntervalJob {
    private Map<String, OneScript> scripts = [:]

    String getContentByName(String name) {
        scripts[name]?.content
    }

    @Override
    String name() {
        'script holder'
    }

    @Override
    void doJob() {
        Map<String, Long> params = [:]
        scripts.each { k, v ->
            params[k] = v.updatedDate.time
        }
        def body = Agent.instance.post('/dms/api/agent/script/pull', params, String)
        def arr = JSON.parseArray(body, OneScript)
        for (one in arr) {
            def exist = scripts[one.name]
            if (exist) {
                exist.content = one.content
                exist.updatedDate = one.updatedDate
            } else {
                scripts[one.name] = one
            }
            log.info 'updated - ' + one.name
        }
    }
}
