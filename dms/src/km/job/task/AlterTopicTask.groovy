package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.PartitionBalancer
import km.job.KmJob
import km.CuratorPoolHolder
import km.job.KmJobTask
import model.KmTopicDTO
import org.segment.d.json.DefaultJsonTransformer

@CompileStatic
@Slf4j
class AlterTopicTask extends KmJobTask {
    final String topicName
    final int newPartitions
    final Map<String, String> configOverrides

    AlterTopicTask(KmJob kmJob, String topicName, int newPartitions, Map<String, String> configOverrides) {
        this.topicName = topicName
        this.newPartitions = newPartitions
        this.configOverrides = configOverrides
        this.job = kmJob
        this.step = new JobStep('alter_topic_' + topicName, 0)
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {
            def json = new DefaultJsonTransformer()

            def topicPath = '/brokers/topics/' + topicName
            if (client.checkExists().forPath(topicPath) == null) {
                return JobResult.fail('topic not found: ' + topicName)
            }

            def topicData = new String(client.getData().forPath(topicPath), 'UTF-8')
            def topicJson = json.read(topicData, Map.class) as Map
            def partitionsMap = topicJson['partitions'] as Map<String, List<Integer>>

            def currentPartitions = partitionsMap ? partitionsMap.size() : 0

            if (newPartitions > 0 && newPartitions > currentPartitions) {
                def brokerDetail = kmService.brokerDetail
                if (!brokerDetail?.brokers) {
                    return JobResult.fail('no broker detail found')
                }

                def brokerIds = brokerDetail.brokers.collect { it.brokerId }.sort()
                def brokerCount = brokerIds.size()
                def replicationFactor = partitionsMap ? (partitionsMap.values().first()?.size() ?: 1) : 1

                def addCount = newPartitions - currentPartitions
                def rawAssignment = PartitionBalancer.assignReplicas(brokerCount, addCount, replicationFactor)
                def assignment = rawAssignment.collect { replicas ->
                    replicas.collect { idx -> brokerIds[idx] }
                }

                assignment.eachWithIndex { List<Integer> replicas, int i ->
                    partitionsMap.put(String.valueOf(currentPartitions + i), replicas)
                }

                topicJson['partitions'] = partitionsMap
                def updatedJson = json.json(topicJson)
                client.setData().forPath(topicPath, updatedJson.getBytes('UTF-8'))

                def existingTopic = new KmTopicDTO().where('service_id = ? and name = ?', kmService.id, topicName).one()
                if (existingTopic) {
                    new KmTopicDTO(id: existingTopic.id, partitions: newPartitions, updatedDate: new Date()).update()
                }
            }

            if (configOverrides) {
                def configPath = '/config/topics/' + topicName
                Map existingConfig = [:]
                if (client.checkExists().forPath(configPath) != null) {
                    def existingData = new String(client.getData().forPath(configPath), 'UTF-8')
                    def existingJson = json.read(existingData, Map.class) as Map
                    existingConfig = (existingJson['config'] as Map) ?: [:]
                }
                existingConfig.putAll(configOverrides)
                def configData = [version: 1, config: existingConfig]
                def configJson = json.json(configData)

                if (client.checkExists().forPath(configPath) != null) {
                    client.setData().forPath(configPath, configJson.getBytes('UTF-8'))
                } else {
                    client.create().creatingParentsIfNeeded().forPath(configPath, configJson.getBytes('UTF-8'))
                }
            }

            JobResult.ok('topic altered: ' + topicName)
        } catch (Exception e) {
            log.error('alter topic error', e)
            JobResult.fail('alter topic error: ' + e.message)
        }
    }
}
