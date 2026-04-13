package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.PartitionBalancer
import km.job.KmJob
import km.job.KmJobTask
import km.job.KmJobTypes
import model.KmServiceDTO
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.segment.d.json.DefaultJsonTransformer

@CompileStatic
@Slf4j
class ReassignPartitionsTask extends KmJobTask {
    final KmServiceDTO kmService

    ReassignPartitionsTask(KmJob kmJob) {
        this.kmService = kmJob.kmService
        this.job = kmJob
        this.step = new JobStep('reassign_partitions', 3)
    }

    @Override
    JobResult doTask() {
        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString, new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            def topicsPath = '/brokers/topics'
            if (client.checkExists().forPath(topicsPath) == null) {
                return JobResult.ok('no topics to reassign')
            }

            def topicNames = client.getChildren().forPath(topicsPath)
            if (!topicNames) {
                return JobResult.ok('no topics found')
            }

            def brokerDetail = kmService.brokerDetail
            if (!brokerDetail?.brokers) {
                return JobResult.fail('no broker detail')
            }

            def json = new DefaultJsonTransformer()
            boolean isScaleDown = job.type == KmJobTypes.BROKER_SCALE_DOWN

            int[] removeBrokerIds = null
            if (isScaleDown) {
                def removeBrokerIdsStr = job.params?.get('removeBrokerIds') as String
                if (!removeBrokerIdsStr) {
                    return JobResult.fail('removeBrokerIds param missing for scale down')
                }
                removeBrokerIds = removeBrokerIdsStr.split(',').collect { it as int } as int[]
            }

            List<Map> reassignmentPartitions = []

            for (topicName in topicNames) {
                def topicPath = topicsPath + '/' + topicName
                if (client.checkExists().forPath(topicPath) == null) continue

                def topicData = new String(client.getData().forPath(topicPath), 'UTF-8')
                def topicJson = json.read(topicData, Map.class) as Map
                def partitionsMap = topicJson['partitions'] as Map<String, List<Integer>>
                if (!partitionsMap) continue

                def sortedKeys = partitionsMap.keySet().sort { a, b -> (a as int) <=> (b as int) }
                List<List<Integer>> currentAssignment = []
                sortedKeys.each { p ->
                    def replicas = partitionsMap[p] as List<Integer>
                    if (replicas) {
                        currentAssignment << replicas
                    }
                }
                if (!currentAssignment) continue

                List<List<Integer>> newAssignment
                if (isScaleDown) {
                    newAssignment = PartitionBalancer.reassignForDecommission(currentAssignment, removeBrokerIds)
                } else {
                    def brokerIds = brokerDetail.brokers.collect { it.brokerId } as int[]
                    newAssignment = PartitionBalancer.reassignForScale(currentAssignment, brokerIds)
                }

                newAssignment.eachWithIndex { List<Integer> replicas, int p ->
                    reassignmentPartitions << [
                            topic     : topicName,
                            partition : p,
                            replicas  : replicas
                    ]
                }
            }

            if (!reassignmentPartitions) {
                return JobResult.ok('no reassignment needed')
            }

            def reassignmentData = [
                    version   : 1,
                    partitions: reassignmentPartitions
            ]
            def reassignmentJson = json.json(reassignmentData)

            def reassignPath = '/admin/reassign_partitions'
            if (client.checkExists().forPath(reassignPath) != null) {
                client.delete().forPath(reassignPath)
            }
            client.create().creatingParentsIfNeeded().forPath(reassignPath, reassignmentJson.getBytes('UTF-8'))

            JobResult.ok('reassignment submitted for ' + reassignmentPartitions.size() + ' partition(s) across ' + topicNames.size() + ' topic(s)')
        } catch (Exception e) {
            log.error('reassign partitions error', e)
            JobResult.fail('reassign partitions error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
