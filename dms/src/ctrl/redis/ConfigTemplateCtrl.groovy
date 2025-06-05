package ctrl.redis

import model.RmConfigTemplateDTO
import model.RmServiceDTO
import model.json.ConfigItems
import model.json.KVPair
import org.segment.web.handler.ChainHandler
import plugin.PluginManager

def h = ChainHandler.instance

h.group('/redis/config-template') {
    h.get('/list') { req, resp ->
        def one = new RmConfigTemplateDTO().noWhere().one()
        if (!one) {
            // add default
            def redis6ConfigTpl = PluginManager.pluginsResourceDirPath() + '/redis/redis6.conf.items.txt'
            def file = new File(redis6ConfigTpl)
            def newOne = new RmConfigTemplateDTO(name: 'redis6', des: 'template', updatedDate: new Date())
            ArrayList<KVPair<String>> items = []
            file.eachLine { line ->
                def trimLine = line.trim()
                if (trimLine.startsWith('#') || trimLine.startsWith(';')) {
                    // comment
                } else if (trimLine.startsWith('include')) {
                    // ignore
                } else {
                    def arr = trimLine.split(' ')
                    if (arr.length == 2) {
                        items << new KVPair<String>(key: arr[0], value: arr[1])
                    }
                }
                return
            }
            newOne.configItems = new ConfigItems(items: items)
            newOne.id = newOne.add()
        }

        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def keyword = req.param('keyword')
        def pager = new RmConfigTemplateDTO().noWhere().
                where(keyword as boolean, '(name like ?) or (des like ?)',
                        '%' + keyword + '%', '%' + keyword + '%').listPager(pageNum, pageSize)

        pager
    }

    h.post('/update') { req, resp ->
        def one = req.bodyAs(RmConfigTemplateDTO)
        int id
        if (!one.id) {
            // check name
            def existOne = new RmConfigTemplateDTO(name: one.name).queryFields('id').one()
            if (existOne) {
                resp.halt(500, 'name already exists')
            }
            id = one.add()
        } else {
            one.update()
            id = one.id
        }
        [id: id]
    }

    h.delete('/delete') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int
        assert id > 0

        // check if exists
        def one = new RmConfigTemplateDTO(id: id).queryFields('id').one()
        if (!one) {
            resp.halt(500, 'not exists')
        }

        // check if is used
        def serviceOne = new RmServiceDTO(configTemplateId: id).queryFields('name,status').one()
        if (serviceOne && serviceOne.status != RmServiceDTO.Status.deleted.name()) {
            resp.halt(500, "this config template is used by service: ${serviceOne.name}")
        }

        new RmConfigTemplateDTO(id: id).delete()
        [flag: true]
    }
}
