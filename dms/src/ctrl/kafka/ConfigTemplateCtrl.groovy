package ctrl.kafka

import model.KmConfigTemplateDTO
import model.KmServiceDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/config-template') {
    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def keyword = req.param('keyword')
        def dto = new KmConfigTemplateDTO().noWhere()
        dto.where(keyword as boolean, '(name like ?)', '%' + keyword + '%')

        dto.listPager(pageNum, pageSize)
    }

    h.post('/add') { req, resp ->
        def one = req.bodyAs(KmConfigTemplateDTO)

        if (!one.name) {
            resp.halt(409, 'name is required')
        }

        def existOne = new KmConfigTemplateDTO(name: one.name).queryFields('id').one()
        if (existOne) {
            resp.halt(409, 'name already exists')
        }

        one.updatedDate = new Date()
        def id = one.add()

        [id: id]
    }

    h.post('/update') { req, resp ->
        def one = req.bodyAs(KmConfigTemplateDTO)

        if (!one.id) {
            resp.halt(409, 'id is required')
        }

        def existOne = new KmConfigTemplateDTO(id: one.id).queryFields('id,name').one()
        if (!existOne) {
            resp.halt(404, 'config template not found')
        }

        if (one.name && one.name != existOne.name) {
            def dupOne = new KmConfigTemplateDTO(name: one.name).queryFields('id').one()
            if (dupOne) {
                resp.halt(409, 'name already exists')
            }
        }

        one.updatedDate = new Date()
        one.update()

        [flag: true]
    }

    h.delete('/delete') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int
        assert id > 0

        def existOne = new KmConfigTemplateDTO(id: id).queryFields('id').one()
        if (!existOne) {
            resp.halt(404, 'config template not found')
        }

        def inUse = new KmServiceDTO().noWhere().
                where('config_template_id = ?', id).
                queryFields('id,name').list()
        if (inUse) {
            resp.halt(409, 'config template in use by service: ' + inUse.collect { it.name }.join(','))
        }

        new KmConfigTemplateDTO(id: id).delete()
        [flag: true]
    }
}
