package km

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.KmServiceDTO
import model.KmSnapshotDTO
import model.json.KmSnapshotContent

@CompileStatic
@Slf4j
@Singleton
class KmSnapshotManager {

    String exportSnapshot(int serviceId) {
        def one = new KmServiceDTO(id: serviceId).one()
        if (!one) {
            throw new IllegalStateException('service not found, id: ' + serviceId)
        }

        def beginT = System.currentTimeMillis()
        def timestamp = new Date().format('yyyyMMddHHmmss')
        def snapshotDir = 'snapshots/' + one.name + '_' + timestamp

        def snapshot = new KmSnapshotDTO()
        snapshot.name = one.name + '_' + timestamp
        snapshot.serviceId = serviceId
        snapshot.snapshotDir = snapshotDir
        snapshot.status = KmSnapshotDTO.Status.created
        snapshot.createdDate = new Date()
        snapshot.updatedDate = new Date()

        def id = snapshot.add()
        snapshot.id = id

        def costMs = System.currentTimeMillis() - beginT
        new KmSnapshotDTO(id: id, status: KmSnapshotDTO.Status.done, costMs: costMs as int, updatedDate: new Date()).update()

        snapshotDir
    }

    String importSnapshot(String snapshotPath, String zkConnectString, String zkChroot) {
        if (!zkConnectString) {
            throw new IllegalArgumentException('zkConnectString is required')
        }

        def snapshot = new KmSnapshotDTO()
        snapshot.name = snapshotPath
        snapshot.snapshotDir = snapshotPath
        snapshot.status = KmSnapshotDTO.Status.created
        snapshot.message = 'imported'
        snapshot.createdDate = new Date()
        snapshot.updatedDate = new Date()

        def id = snapshot.add()
        snapshot.id = id

        new KmSnapshotDTO(id: id, status: KmSnapshotDTO.Status.done, updatedDate: new Date()).update()

        id as String
    }
}
