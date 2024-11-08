package agent.script

import agent.Agent
import com.alibaba.fastjson.JSON
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException
import com.segment.common.job.IntervalJob
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

        try {
            def body = Agent.instance.post('/dms/api/agent/script/pull', params, String)
            def arr = JSON.parseArray(body, OneScript)
            for (one in arr) {
                def exist = scripts[one.name]
                if (exist) {
                    exist.content = one.content
                    exist.updatedDate = one.updatedDate
                    log.info 'updated - ' + one.name
                } else {
                    scripts[one.name] = one
                    log.info 'added - ' + one.name
                }
            }
        } catch (HttpRequestException e) {
            log.error 'pull script error: ' + e.message
        }
    }
}
