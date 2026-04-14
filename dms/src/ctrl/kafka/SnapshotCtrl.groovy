package ctrl.kafka

import km.KmSnapshotManager
import model.KmSnapshotDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/snapshot') {
    h.get('/list') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        assert serviceIdStr
        def serviceId = serviceIdStr as int

        new KmSnapshotDTO(serviceId: serviceId).list()
    }

    h.post('/export') { req, resp ->
        def body = req.bodyAs(Map)

        def serviceId = body.serviceId as int

        def service = new model.KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        def snapshotDir = KmSnapshotManager.instance.exportSnapshot(serviceId)

        [snapshotDir: snapshotDir]
    }

    h.post('/import') { req, resp ->
        def body = req.bodyAs(Map)

        def snapshotPath = body.snapshotPath as String
        def zkConnectString = body.zkConnectString as String
        def zkChroot = body.zkChroot as String

        if (!snapshotPath) {
            resp.halt(409, 'snapshotPath is required')
        }
        if (!zkConnectString) {
            resp.halt(409, 'zkConnectString is required')
        }

        def serviceId = KmSnapshotManager.instance.importSnapshot(snapshotPath, zkConnectString, zkChroot)

        [serviceId: serviceId]
    }

    h.get('/download') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new KmSnapshotDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'snapshot not found')
        }

        def snapshotDir = one.snapshotDir
        def snapshotFile = new File(snapshotDir + '/snapshot.json')
        if (!snapshotFile.exists()) {
            resp.halt(404, 'snapshot file not found on disk')
        }

        resp.download(new FileInputStream(snapshotFile), one.name + '.json')
    }
}
