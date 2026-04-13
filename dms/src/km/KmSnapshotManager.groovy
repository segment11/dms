package km

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.KmServiceDTO
import model.KmSnapshotDTO
import model.KmTopicDTO
import model.json.KmSnapshotContent
import org.segment.d.json.DefaultJsonTransformer

import java.nio.file.Files
import java.nio.file.Paths

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
        def baseDir = dataDir() + '/snapshots'
        def snapshotDir = baseDir + '/' + one.name + '_' + timestamp

        def snapshot = new KmSnapshotDTO()
        snapshot.name = one.name + '_' + timestamp
        snapshot.serviceId = serviceId
        snapshot.snapshotDir = snapshotDir
        snapshot.status = KmSnapshotDTO.Status.created
        snapshot.createdDate = new Date()
        snapshot.updatedDate = new Date()

        try {
            def dir = Paths.get(snapshotDir)
            Files.createDirectories(dir)

            def content = new KmSnapshotContent()
            content.serviceName = one.name
            content.mode = one.mode?.name()
            content.kafkaVersion = one.kafkaVersion
            content.snapshotDate = new Date()
            content.zkConnectString = one.zkConnectString
            content.zkChroot = one.zkChroot

            if (one.brokerDetail?.brokers) {
                one.brokerDetail.brokers.each { b ->
                    content.brokers << new KmSnapshotContent.BrokerEntry(
                            brokerId: b.brokerId, host: b.ip, port: b.port,
                            rackId: b.rackId, logDirs: '/data/kafka/data')
                }
            }

            def topicList = new KmTopicDTO(serviceId: serviceId).list()
            topicList?.findAll { it.status == KmTopicDTO.Status.active }?.each { t ->
                content.topics << new KmSnapshotContent.TopicEntry(
                        name: t.name, partitions: t.partitions,
                        replicationFactor: t.replicationFactor,
                        configOverrides: t.configOverrides?.params ?: [:])
            }

            def json = new DefaultJsonTransformer().json(content)
            Files.write(Paths.get(snapshotDir + '/snapshot.json'), json.getBytes('UTF-8'))

            def snapshotDTO = snapshot
            def id = snapshotDTO.add()
            snapshotDTO.id = id

            def costMs = System.currentTimeMillis() - beginT
            new KmSnapshotDTO(id: id, status: KmSnapshotDTO.Status.done, costMs: costMs as int, updatedDate: new Date()).update()

            snapshotDir
        } catch (Exception e) {
            def id = snapshot.add()
            new KmSnapshotDTO(id: id, status: KmSnapshotDTO.Status.failed, message: e.message, updatedDate: new Date()).update()
            throw e
        }
    }

    String importSnapshot(String snapshotPath, String zkConnectString, String zkChroot) {
        if (!zkConnectString) {
            throw new IllegalArgumentException('zkConnectString is required')
        }

        def snapshotFile = Paths.get(snapshotPath + '/snapshot.json')
        if (!Files.exists(snapshotFile)) {
            throw new IllegalArgumentException('snapshot.json not found at: ' + snapshotPath)
        }

        def json = new String(Files.readAllBytes(snapshotFile), 'UTF-8')
        def content = new DefaultJsonTransformer().read(json, Map.class)

        def beginT = System.currentTimeMillis()

        def snapshot = new KmSnapshotDTO()
        snapshot.name = (content.serviceName as String) + '_import'
        snapshot.serviceId = 0
        snapshot.snapshotDir = snapshotPath
        snapshot.status = KmSnapshotDTO.Status.created
        snapshot.message = 'imported'
        snapshot.createdDate = new Date()
        snapshot.updatedDate = new Date()
        def id = snapshot.add()

        def costMs = System.currentTimeMillis() - beginT
        new KmSnapshotDTO(id: id, status: KmSnapshotDTO.Status.done, costMs: costMs as int, updatedDate: new Date()).update()

        id as String
    }

    static String dataDir() {
        KafkaManager.dataDir()
    }
}
