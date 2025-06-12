package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import model.json.ClusterSlotsDetail
import rm.RedisManager
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class WaitClusterStateTask extends RmJobTask {
    final RmServiceDTO rmService

    WaitClusterStateTask(RmJob rmJob) {
        this.rmService = rmJob.rmService

        this.job = rmJob
        this.step = new JobStep('wait_cluster_state', 0)
    }

    int tryCount = 0

    @Override
    JobResult doTask() {
        assert rmService

        List<ContainerInfo> allContainerList = []
        def instance = InMemoryAllContainerManager.instance

        for (shard in rmService.clusterSlotsDetail.shards) {
            def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, shard.appId)

            def runningNumber = runningContainerList.size()
            if (runningNumber != rmService.replicas) {
                return JobResult.fail('running containers number: ' + runningNumber + ', expect: ' + rmService.replicas)
            }

            allContainerList.addAll(runningContainerList)

            runningContainerList.each { x ->
                def node = new ClusterSlotsDetail.Node()
                node.ip = x.nodeIp
                node.port = rmService.listenPort(x)
                node.shardIndex = shard.shardIndex
                node.replicaIndex = x.instanceIndex()
                // when first created, the first replica is primary
                node.isPrimary = node.replicaIndex == 0
                shard.nodes << node
            }
        }

        // update after nodes updated
        new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
        log.warn 'update cluster slots detail ok'

        for (x in allContainerList) {
            def lines = rmService.connectAndExe(x) { jedis ->
                jedis.clusterInfo()
            }
            if (!lines || !lines.contains('cluster_state:ok')) {
                Thread.sleep(1000 * 5)
                tryCount++

                if (tryCount > 10) {
                    return JobResult.fail('cluster state not ok')
                } else {
                    return doTask()
                }
            }
        }

        return JobResult.ok('wait cluster state ok')
    }
}
