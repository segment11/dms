package ctrl

import auth.User
import org.segment.d.Pager
import org.segment.web.handler.ChainHandler
import plugin.PluginManager

def h = ChainHandler.instance

h.before('/plugin/**') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not a admin')
    }
}

h.group('/plugin') {
    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def keyword = req.param('keyword')

        def list = PluginManager.instance.pluginList
        def filterList = keyword ? list.findAll {
            it.name().contains(keyword) || it.group().contains(keyword) || it.image().contains(keyword)
        } : list

        def totalCount = filterList.size()
        def pager = new Pager(pageNum, pageSize)
        pager.totalCount = totalCount
        def thisPageList = filterList[pager.start..<pager.end]

        [list     : thisPageList.collect {
            [name : it.name(), version: it.version(), registry: it.registry(), group: it.group(),
             image: it.image(), tag: it.tag(), expressions: it.expressions().keySet()]
        }, pageNum: pageNum, pageSize: pageSize, total: totalCount]
    }.post('/load') { req, resp ->
        def map = req.bodyAs(HashMap)
        def className = map.className as String
        assert className

        PluginManager.instance.loadPlugin(className)
        [flag: true]
    }.delete('/delete') { req, resp ->
        def name = req.param('name')
        assert name

        def pluginList = PluginManager.instance.pluginList
        def one = pluginList.find { it.name() == name }
        if (one) {
            pluginList.remove(one)
        }
        [flag: true]
    }
}