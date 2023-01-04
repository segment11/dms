package agent.ctrl

import agent.Agent
import agent.script.ScriptHolder
import common.Const
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.options('/script/exe') { req, resp ->
    def authToken = req.header(Const.AUTH_TOKEN_HEADER) ?: req.param('authToken')
    assert authToken && authToken == Agent.instance.authToken

    HashMap<String, String> params
    if (req.method() == 'GET') {
        params = [:]
        req.raw().getParameterNames().each {
            params[it.toString()] = req.param(it.toString())
        }
    } else {
        params = req.bodyAs(HashMap)
    }

    def scriptName = params.scriptName
    assert scriptName

    String scriptContent = ScriptHolder.instance.getContentByName(scriptName)
    assert scriptContent
    def r = CachedGroovyClassLoader.instance.eval(scriptContent,
            [sigar: Agent.instance.sigar, docker: Agent.instance.docker, params: params])

    boolean isBodyRaw = !!params.isBodyRaw
    if (isBodyRaw) {
        resp.end(r ? r.toString() : '')
    } else {
        return r
    }
}
