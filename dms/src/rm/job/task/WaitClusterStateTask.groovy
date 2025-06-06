package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ha.JedisPoolHolder
import model.RmServiceDTO
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
            def containerList = instance.getContainerList(1, shard.appId)

            def runningNumber = containerList.findAll { x -> x.running() }.size()
            if (runningNumber != rmService.replicas) {
                return JobResult.fail('running containers number: ' + runningNumber + ', expect: ' + rmService.replicas)
            }

            allContainerList.addAll(containerList)
        }

        for (x in allContainerList) {
            def jedisPool = rmService.connect(x)
            def lines = JedisPoolHolder.exe(jedisPool) { jedis ->
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
