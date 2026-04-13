package km

import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTypes
import km.KmJobExecutor
import km.job.task.ValidateZookeeperTask
import km.job.task.RunCreatingAppJobTask
import km.job.task.WaitInstancesRunningTask
import km.job.task.WaitBrokersRegisteredTask
import km.job.task.CreateTopicTask
import model.AppDTO
import model.ImageTplDTO
import model.KmServiceDTO
import model.KmSnapshotDTO
import model.KmTopicDTO
import model.NamespaceDTO
import model.NodeVolumeDTO
import model.json.AppConf
import model.json.DirVolumeMount
import model.json.ExtendParams
import model.json.FileVolumeMount
import model.json.KVPair
import model.json.KmSnapshotContent
import model.json.PortMapping
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
                Map<String, String> overrides = [:]
                if (t.configOverrides?.params) {
                    t.configOverrides.params.each { k, v -> overrides.put(k as String, v as String) }
                }
                content.topics << new KmSnapshotContent.TopicEntry(
                        name: t.name, partitions: t.partitions,
                        replicationFactor: t.replicationFactor,
                        configOverrides: overrides)
            }

            def json = new DefaultJsonTransformer()
            Files.write(Paths.get(snapshotDir + '/snapshot.json'), json.json(content).getBytes('UTF-8'))

            def topicsJson = json.json(content.topics)
            Files.write(Paths.get(snapshotDir + '/topics.json'), topicsJson.getBytes('UTF-8'))

            def sb = new StringBuilder()
            sb.append('broker.id=${brokerId}').append('\n')
            sb.append('listeners=PLAINTEXT://0.0.0.0:${port}').append('\n')
            sb.append('advertised.listeners=PLAINTEXT://${nodeIp}:${port}').append('\n')
            sb.append('zookeeper.connect=').append(one.zkConnectString).append(one.zkChroot).append('\n')
            sb.append('log.dirs=/data/kafka/data').append('\n')
            sb.append('num.partitions=').append(one.defaultPartitions).append('\n')
            sb.append('default.replication.factor=').append(one.defaultReplicationFactor).append('\n')
            def minReplication = Math.min(3, one.brokers)
            sb.append('offsets.topic.replication.factor=').append(minReplication).append('\n')
            sb.append('transaction.state.log.replication.factor=').append(minReplication).append('\n')
            sb.append('log.retention.hours=168').append('\n')
            sb.append('log.segment.bytes=1073741824').append('\n')
            sb.append('num.io.threads=8').append('\n')
            sb.append('num.network.threads=3').append('\n')
            sb.append('socket.send.buffer.bytes=102400').append('\n')
            sb.append('socket.receive.buffer.bytes=102400').append('\n')
            sb.append('socket.request.max.bytes=104857600').append('\n')
            Files.write(Paths.get(snapshotDir + '/server.properties'), sb.toString().getBytes('UTF-8'))

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

        def json = new DefaultJsonTransformer()
        def snapshotJson = new String(Files.readAllBytes(snapshotFile), 'UTF-8')
        def content = json.read(snapshotJson, Map.class)

        def serviceName = content.serviceName as String
        def mode = content.mode as String
        def kafkaVersion = content.kafkaVersion as String
        def brokersList = (content.brokers as List<Map>) ?: []
        def brokers = brokersList.size() ?: 1
        def port = brokersList ? (brokersList[0]['port'] as Integer ?: 9029) : 9092
        if (port == 9029) port = 9092

        def beginT = System.currentTimeMillis()

        def snapshot = new KmSnapshotDTO()
        snapshot.name = serviceName + '_import'
        snapshot.serviceId = 0
        snapshot.snapshotDir = snapshotPath
        snapshot.status = KmSnapshotDTO.Status.created
        snapshot.message = 'imported'
        snapshot.createdDate = new Date()
        snapshot.updatedDate = new Date()
        def snapshotId = snapshot.add()

        try {
            def existOne = new KmServiceDTO(name: serviceName).queryFields('id').one()
            if (existOne) {
                throw new IllegalStateException('service name already exists: ' + serviceName)
            }

            if (!zkChroot) {
                zkChroot = '/kafka/' + serviceName
            }

            def namespaceId = NamespaceDTO.createIfNotExist(KafkaManager.CLUSTER_ID, 'kafka')

            def app = new AppDTO()
            app.clusterId = KafkaManager.CLUSTER_ID
            app.namespaceId = namespaceId
            app.name = 'km_' + serviceName
            app.status = AppDTO.Status.auto
            app.updatedDate = new Date()

            def conf = new AppConf()
            conf.containerNumber = brokers
            conf.registryId = KafkaManager.preferRegistryId()
            conf.group = 'bitnami'
            conf.image = 'kafka'
            conf.tag = kafkaVersion
            conf.memReservationMB = 1024
            conf.memMB = 2048
            conf.cpuFixed = 1.0
            conf.networkMode = 'host'
            conf.portList << new PortMapping(privatePort: port, publicPort: port)

            final String dataDir = '/data'
            def serviceDataDir = dataDir + '/kafka_data_app_' + '_${appId}_${instanceIndex}'
            def nodeVolumeId = new NodeVolumeDTO(imageName: 'bitnami/kafka', name: 'for imported service ' + serviceName,
                    dir: serviceDataDir, clusterId: KafkaManager.CLUSTER_ID, des: 'data dir for imported kafka service').add()
            def dirOne = new DirVolumeMount(
                    dir: serviceDataDir, dist: '/kafka/logs', mode: 'rw',
                    nodeVolumeId: nodeVolumeId)
            conf.dirVolumeList << dirOne

            def tplOne = new ImageTplDTO(imageName: 'bitnami/kafka', name: 'server.properties.tpl').one()
            if (!tplOne) {
                throw new IllegalStateException('KafkaPlugin not initialized - image templates not found')
            }
            def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content,
                    dist: '/opt/bitnami/kafka/config/server.properties')
            mountOne.isParentDirMount = false
            mountOne.paramList << new KVPair<String>('brokerId', '${instanceIndex}')
            mountOne.paramList << new KVPair<String>('port', '' + port)
            mountOne.paramList << new KVPair<String>('dataDir', '/kafka/logs')
            mountOne.paramList << new KVPair<String>('zkConnectString', zkConnectString)
            mountOne.paramList << new KVPair<String>('zkChroot', zkChroot)
            mountOne.paramList << new KVPair<String>('defaultPartitions', '8')
            mountOne.paramList << new KVPair<String>('defaultReplicationFactor', '1')
            mountOne.paramList << new KVPair<String>('brokerCount', '' + brokers)
            mountOne.paramList << new KVPair<String>('heapMb', '1024')
            conf.fileVolumeList << mountOne

            app.conf = conf
            def appId = app.add()
            app.id = appId

            def one = new KmServiceDTO()
            one.name = serviceName
            one.mode = mode == 'standalone' ? KmServiceDTO.Mode.standalone : KmServiceDTO.Mode.cluster
            one.kafkaVersion = kafkaVersion
            one.zkConnectString = zkConnectString
            one.zkChroot = zkChroot
            one.appId = appId
            one.port = port
            one.brokers = brokers
            one.heapMb = 1024
            one.defaultReplicationFactor = 1
            one.defaultPartitions = 8
            one.status = KmServiceDTO.Status.creating
            one.extendParams = new ExtendParams()
            one.createdDate = new Date()
            one.updatedDate = new Date()

            def id = one.add()
            one.id = id

            app.extendParams = new ExtendParams()
            app.extendParams.put('kmServiceId', id.toString())
            new AppDTO(id: app.id, extendParams: app.extendParams).update()

            snapshot.serviceId = id
            new KmSnapshotDTO(id: snapshotId, serviceId: id, updatedDate: new Date()).update()

            def kmJob = new KmJob()
            kmJob.kmService = one
            kmJob.type = mode == 'standalone' ? KmJobTypes.STANDALONE_CREATE : KmJobTypes.CLUSTER_CREATE
            kmJob.status = JobStatus.created
            kmJob.params = new JobParams()
            kmJob.params.put('kmServiceId', id.toString())

            kmJob.taskList << new ValidateZookeeperTask(kmJob)
            kmJob.taskList << new RunCreatingAppJobTask(kmJob, app)
            kmJob.taskList << new WaitInstancesRunningTask(kmJob)
            kmJob.taskList << new WaitBrokersRegisteredTask(kmJob)

            def topics = content.topics as List<Map>
            if (topics) {
                topics.each { t ->
                    def topicName = t.name as String
                    def partitions = (t.partitions as Integer) ?: 8
                    def replicationFactor = (t.replicationFactor as Integer) ?: 1
                    new KmTopicDTO(
                            serviceId: id,
                            name: topicName,
                            partitions: partitions,
                            replicationFactor: replicationFactor,
                            status: KmTopicDTO.Status.creating,
                            createdDate: new Date(),
                            updatedDate: new Date()
                    ).add()
                    kmJob.taskList << new CreateTopicTask(kmJob, topicName, partitions, replicationFactor)
                }
            }

            kmJob.createdDate = new Date()
            kmJob.updatedDate = new Date()
            kmJob.save()

            KmJobExecutor.instance.execute {
                kmJob.run()
            }

            def costMs = System.currentTimeMillis() - beginT
            new KmSnapshotDTO(id: snapshotId, status: KmSnapshotDTO.Status.done, costMs: costMs as int, updatedDate: new Date()).update()

            id as String
        } catch (Exception e) {
            new KmSnapshotDTO(id: snapshotId, status: KmSnapshotDTO.Status.failed, message: e.message, updatedDate: new Date()).update()
            throw e
        }
    }

    static String dataDir() {
        KafkaManager.dataDir()
    }
}
