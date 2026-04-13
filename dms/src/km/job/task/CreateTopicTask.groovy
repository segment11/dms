package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.PartitionBalancer
import km.job.KmJob
import km.job.KmJobTask
import model.KmTopicDTO
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.segment.d.json.DefaultJsonTransformer

@CompileStatic
@Slf4j
class CreateTopicTask extends KmJobTask {
    final String topicName
    final int partitions
    final int replicationFactor

    CreateTopicTask(KmJob kmJob, String topicName, int partitions, int replicationFactor) {
        this.topicName = topicName
        this.partitions = partitions
        this.replicationFactor = replicationFactor
        this.job = kmJob
        this.step = new JobStep('create_topic_' + topicName, 0)
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString,
                new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            def topicsPath = '/brokers/topics/' + topicName
            if (client.checkExists().forPath(topicsPath) != null) {
                return JobResult.fail('topic already exists: ' + topicName)
            }

            def brokerDetail = kmService.brokerDetail
            if (!brokerDetail?.brokers) {
                return JobResult.fail('no broker detail found')
            }

            def brokerIds = brokerDetail.brokers.collect { it.brokerId }.sort()
            def brokerCount = brokerIds.size()
            if (replicationFactor > brokerCount) {
                return JobResult.fail('replicationFactor ' + replicationFactor + ' > broker count ' + brokerCount)
            }

            def rawAssignment = PartitionBalancer.assignReplicas(brokerCount, partitions, replicationFactor)
            def assignment = rawAssignment.collect { replicas ->
                replicas.collect { idx -> brokerIds[idx] }
            }

            Map<String, List<Integer>> partitionMap = [:]
            assignment.eachWithIndex { List<Integer> replicas, int p ->
                partitionMap.put(String.valueOf(p), replicas)
            }

            def topicData = [
                    version   : 2,
                    partitions: partitionMap
            ]

            def topicJson = new DefaultJsonTransformer().json(topicData)
            client.create().creatingParentsIfNeeded().forPath(topicsPath, topicJson.bytes)

            def existingTopic = new KmTopicDTO().where('service_id = ? and name = ?', kmService.id, topicName).one()
            if (existingTopic) {
                new KmTopicDTO(id: existingTopic.id, status: KmTopicDTO.Status.active, updatedDate: new Date()).update()
            } else {
                new KmTopicDTO(
                        serviceId: kmService.id,
                        name: topicName,
                        partitions: partitions,
                        replicationFactor: replicationFactor,
                        status: KmTopicDTO.Status.active,
                        createdDate: new Date(),
                        updatedDate: new Date()
                ).add()
            }

            JobResult.ok('topic created: ' + topicName + ', partitions: ' + partitions)
        } catch (Exception e) {
            log.error('create topic error', e)
            return JobResult.fail('create topic error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
