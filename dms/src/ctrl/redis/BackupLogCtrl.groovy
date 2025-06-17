package ctrl.redis

import model.job.RmBackupLogDTO
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/redis/backup-log') {
    h.get('/list') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        assert serviceIdStr
        def serviceId = serviceIdStr as int

        def list = new RmBackupLogDTO(serviceId: serviceId).list()
        [list: list]
    }

    h.delete('/delete') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int
        assert id > 0

        // check if exists
        def one = new RmBackupLogDTO(id: id).queryFields('id').one()
        if (!one) {
            resp.halt(404, 'backup log not exists')
        }

        new RmBackupLogDTO(id: id).delete()
        [flag: true]
    }
}
