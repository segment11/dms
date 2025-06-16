package ctrl.redis

import model.job.RmBackupTemplateDTO
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/redis/backup-template') {
    h.get('/simple-list') { req, resp ->
        def list = new RmBackupTemplateDTO().noWhere().queryFields('id,name').list()
        [list: list.collect {
            [id: it.id, name: it.name]
        }]
    }

    h.get('/list') { req, resp ->
        def list = new RmBackupTemplateDTO().noWhere().list()
        list.each {
            it.targetBucket.accessKey = '******'
            it.targetBucket.secretKey = '******'
        }
        [list: list]
    }

    h.post('/update') { req, resp ->
        def one = req.bodyAs(RmBackupTemplateDTO)
        int id
        if (!one.id) {
            // check name
            def existOne = new RmBackupTemplateDTO(name: one.name).queryFields('id').one()
            if (existOne) {
                resp.halt(409, 'name already exists')
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
        def one = new RmBackupTemplateDTO(id: id).queryFields('id').one()
        if (!one) {
            resp.halt(404, 'backup template not exists')
        }

        new RmBackupTemplateDTO(id: id).delete()
        [flag: true]
    }
}
