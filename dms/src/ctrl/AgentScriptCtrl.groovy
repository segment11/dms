package ctrl

import auth.User
import model.AgentScriptDTO
import model.AgentScriptPullLogDTO
import model.json.ScriptPullContent
import org.segment.web.handler.ChainHandler
import server.AgentCaller
import support.LocalGroovyScriptLoader

def h = ChainHandler.instance

h.group('/agent/script') {
    h.get('/pull/log') { req, resp ->
        new AgentScriptPullLogDTO().noWhere().list()
    }.get('/pull/test') { req, resp ->
        def content = new ScriptPullContent()
        content.list << new ScriptPullContent.ScriptPullOne(id: 1, name: 'test', updatedDate: new Date())
        content.list << new ScriptPullContent.ScriptPullOne(id: 2, name: 'test2', updatedDate: new Date())
        new AgentScriptPullLogDTO(agentHost: req.host(), content: content, createdDate: new Date()).add()
        'ok'
    }.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def keyword = req.param('keyword')
        new AgentScriptDTO().noWhere().where(keyword as boolean, '(name like ?) or (des like ?)',
                '%' + keyword + '%', '%' + keyword + '%').listPager(pageNum, pageSize)
    }.delete('/delete') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        assert id
        new AgentScriptDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def one = req.bodyAs(AgentScriptDTO)
        assert one.name && one.content
        one.updatedDate = new Date()
        if (one.id) {
            one.update()
            return [id: one.id]
        } else {
            def id = one.add()
            return [id: id]
        }
    }.get('/update/batch') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def scriptNameGiven = req.param('scriptName')
        LocalGroovyScriptLoader.load(scriptNameGiven)

        [flag: true]
    }.options('/exe') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(500, 'not admin')
        }

        Map<String, String> params
        if (req.method() == 'GET') {
            params = [:]
            req.raw().getParameterNames().each { String key ->
                params[key] = req.param(key)
            }
        } else {
            params = req.bodyAs() as Map<String, String>
        }

        def clusterId = params.clusterId
        def nodeIp = params.nodeIp
        def scriptName = params.scriptName
        assert clusterId && nodeIp && scriptName

        def r = AgentCaller.instance.agentScriptExeBody(clusterId as int, nodeIp, scriptName, params)
        resp.end r
    }
}

// for script holder
h.post('/api/agent/script/pull') { req, resp ->
    def map = req.bodyAs(HashMap)
    def list = new AgentScriptDTO().noWhere().list()
    // compare updated date
    def r = []
    def content = new ScriptPullContent()

    list.findAll {
        def name = it.name
        def time = map[name] as Long
        time == null || time != it.updatedDate.time
    }.each {
        r << [name: it.name, content: it.content, updatedDate: it.updatedDate]
        content.list << new ScriptPullContent.ScriptPullOne(id: it.id, name: it.name, updatedDate: it.updatedDate)
    }

    String host = req.header('X-REAL-IP') ?: req.host()
    def oneLog = new AgentScriptPullLogDTO(agentHost: host).queryFields('id').one()
    if (oneLog) {
        if (content.list) {
            oneLog.content = content
            oneLog.createdDate = new Date()
            oneLog.update()
        }
    } else {
        new AgentScriptPullLogDTO(agentHost: host, content: content, createdDate: new Date()).add()
    }
    r
}