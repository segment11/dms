package ctrl

import auth.User
import model.NamespaceDTO
import org.segment.d.Pager
import org.segment.web.handler.ChainHandler
import plugin.BasePlugin
import plugin.PluginManager
import plugin.demo2.InitToolPlugin
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

def h = ChainHandler.instance

h.before('/plugin/**') { req, resp ->
    User u = req.session('user') as User
    if (!u?.isAdmin()) {
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
            [name       : it.name(), version: it.version(),
             registry   : it.registry(), group: it.group(), image: it.image(), tag: it.tag(),
             expressions: it.expressions().keySet(), loadTime: it.loadTime(), className: it.getClass().name]
        }, pageNum: pageNum, pageSize: pageSize, totalCount: totalCount]
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
    }.get('/menu/list') { req, resp ->
        List<HashMap> menus = []
        PluginManager.instance.pluginList.each { plugin ->
            def menusThisPlugin = plugin.menus()
            if (!menusThisPlugin) {
                return
            }

            for (menu in menusThisPlugin) {
                def map = [:]
                map.title = menu.title
                map.icon = menu.icon
                map.page = menu.module + '_' + menu.page
                map.list = menu.children?.collect {
                    [title: it.title, icon: it.icon, page: it.module + '_' + it.page]
                }
                menus << map
            }
        }
        [menus: menus]
    }.get('/demo/create') { req, resp ->
        def name = req.param('name')
        assert name

        def plugin = PluginManager.instance.pluginList.find { it.name() == name }
        if (!plugin) {
            return [flag: false, message: 'plugin not found']
        }

        def clusterId = InMemoryCacheSupport.instance.clusterList[0].id
        int namespaceId
        def namespace = new NamespaceDTO(clusterId: clusterId, name: 'demo').one()
        if (!namespace) {
            namespaceId = new NamespaceDTO(clusterId: clusterId, name: 'demo').add()
        } else {
            namespaceId = namespace.id
        }

        def nodeIpList = InMemoryAllContainerManager.instance.getAllNodeInfo(clusterId).collect {
            it.value.nodeIp
        }
        def app = plugin.demoApp(BasePlugin.tplApp(clusterId, namespaceId, nodeIpList))
        InitToolPlugin.addAppIfNotExists(app)

        [flag: true]
    }
}