package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import rm.RedisManager
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class WaitInstancesRunningTask extends RmJobTask {
    final RmServiceDTO rmService

    final int shardIndex

    WaitInstancesRunningTask(RmJob rmJob, int shardIndex) {
        this.rmService = rmJob.rmService
        this.shardIndex = shardIndex

        this.job = rmJob
        this.step = new JobStep('wait_instances_running_for_shard_' + shardIndex, shardIndex)
    }

    int tryCount = 0

    @Override
    JobResult doTask() {
        assert rmService

        def targetShardAppId = rmService.mode == RmServiceDTO.Mode.cluster ?
                rmService.clusterSlotsDetail.shards.find { it.shardIndex == shardIndex }.appId :
                rmService.appId

        def instance = InMemoryAllContainerManager.instance
        def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, targetShardAppId)
        if (!runningContainerList) {
            Thread.sleep(10 * 1000)
            tryCount++

            if (tryCount > 10) {
                return JobResult.fail('no containers found for app id: ' + targetShardAppId)
            } else {
                return doTask()
            }
        }

        def runningNumber = runningContainerList.size()
        log.info 'running containers number: {}, app id: {}', runningNumber, targetShardAppId
        if (runningNumber == rmService.replicas) {
            return JobResult.ok('running containers number: ' + runningNumber)
        }

        Thread.sleep(10 * 1000)
        tryCount++

        if (tryCount > 10) {
            return JobResult.fail('running containers number: ' + runningNumber + ', expect: ' + rmService.replicas)
        } else {
            return doTask()
        }
    }
}
