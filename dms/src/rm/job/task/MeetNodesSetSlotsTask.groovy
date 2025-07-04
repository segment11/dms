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
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class MeetNodesSetSlotsTask extends RmJobTask {
    final RmServiceDTO rmService

    MeetNodesSetSlotsTask(RmJob rmJob) {
        this.rmService = rmJob.rmService

        this.job = rmJob
        this.step = new JobStep('meet_nodes_set_slots', 0)
    }

    @Override
    JobResult doTask() {
        assert rmService

        List<ContainerInfo> allContainerList = []
        def instance = InMemoryAllContainerManager.instance

        for (shard in rmService.clusterSlotsDetail.shards) {
            def appId = shard.appId

            def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, appId)

            def runningNumber = runningContainerList.size()
            if (runningNumber != rmService.replicas) {
                return JobResult.fail('running containers number: ' + runningNumber + ', expect: ' + rmService.replicas)
            }

            allContainerList.addAll(runningContainerList)
        }

        for (x in allContainerList) {
            rmService.connectAndExe(x) { jedis ->
                for (x2 in allContainerList) {
                    if (x != x2) {
                        def listenPort2 = rmService.listenPort(x2)
                        def r = jedis.clusterMeet(x2.nodeIp, listenPort2)
                        log.warn 'meet node, new node: {}, old node: {}, result: {}',
                                x2.nodeIp + ':' + listenPort2,
                                x.nodeIp + ':' + rmService.listenPort(x),
                                r
                    }
                }
            }
        }

        log.warn('meet nodes done, wait 5 seconds')
        Thread.sleep(1000 * 5)

        // set slots to the first replica
        for (x in allContainerList) {
            def instanceIndex = x.instanceIndex()
            // only primary set slots
            if (instanceIndex != 0) {
                continue
            }

            def appId = x.appId()
            def shard = rmService.clusterSlotsDetail.shards.find { it.appId == appId }
            def slotsBegin = shard.multiSlotRange.list[0].begin
            def slotsEnd = shard.multiSlotRange.list[0].end
            int[] slots = new int[slotsEnd - slotsBegin + 1]
            for (int i = 0; i < slots.length; i++) {
                slots[i] = slotsBegin + i
            }

            rmService.connectAndExe(x) { jedis ->
                def r = jedis.clusterAddSlots(slots)
                log.warn('add slots: {}-{}, host: {}, port: {}, result: {}', slotsBegin, slotsEnd, x.nodeIp, rmService.listenPort(x), r)
            }
        }

        log.warn('set slots done')

        // replica of
        for (x in allContainerList) {
            def instanceIndex = x.instanceIndex()
            // skip primary
            if (instanceIndex == 0) {
                continue
            }

            def xPrimary = allContainerList.find {
                it.appId() == x.appId() && it.instanceIndex() == 0
            }
            def primaryNodeId = rmService.connectAndExe(xPrimary) { jedis ->
                jedis.clusterMyId()
            }

            rmService.connectAndExe(x) { jedis ->
                def r = jedis.clusterReplicate(primaryNodeId)
                log.warn('replicate node: {}, host: {}, port: {}, result: {}', primaryNodeId, x.nodeIp, rmService.listenPort(x), r)
            }
        }

        log.warn('replica of done')

        return JobResult.ok('meet nodes set slots ok')
    }
}
