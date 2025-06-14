package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import ex.JobProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import model.RmServiceDTO
import rm.RedisManager
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager
import server.scheduler.processor.RemoveProcessor

@CompileStatic
@Slf4j
class RemoveReplicasTask extends RmJobTask {
    final RmServiceDTO rmService

    final List<Integer> replicaIndexList

    RemoveReplicasTask(RmJob rmJob, List<Integer> replicaIndexList) {
        this.rmService = rmJob.rmService
        this.replicaIndexList = replicaIndexList

        this.job = rmJob
        this.step = new JobStep('remove_replicas', 0)
    }

    private void runRemoveAppJob(AppDTO app) {
        // replicas already updated
        if (app.conf.containerNumber <= rmService.replicas) {
            log.warn 'app conf container number {} already <= expect replicas {}, skip', app.conf.containerNumber, rmService.replicas
            return
        }

        def appJob = new AppJobDTO(
                appId: app.id,
                failNum: 0,
                status: AppJobDTO.Status.created,
                jobType: AppJobDTO.JobType.remove,
                createdDate: new Date(),
                updatedDate: new Date()).
                addParam('toContainerNumber', rmService.replicas)
        int appJobId = appJob.add()
        appJob.id = appJobId

        def instance = InMemoryAllContainerManager.instance
        def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, app.id)

        try {
            new RemoveProcessor().process(appJob, app, runningContainerList)
            new AppJobDTO(id: appJobId, status: AppJobDTO.Status.done, updatedDate: new Date()).update()
            log.warn('start application remove job done, job id: {}', job.id)
        } catch (Exception e) {
            log.error('start application remove job error', e)
            throw new JobProcessException('start application remove job error')
        }

        // wait dms agent send containers info
        Thread.sleep(1000 * 5)

        if (rmService.mode == RmServiceDTO.Mode.sentinel) {
            rmService.primaryReplicasDetail.nodes.removeIf { node ->
                node.replicaIndex in replicaIndexList
            }

            // update after nodes updated
            new RmServiceDTO(id: rmService.id, primaryReplicasDetail: rmService.primaryReplicasDetail, updatedDate: new Date()).update()
            log.warn 'update primary replicas nodes ok'
        }

        app.conf.containerNumber = rmService.replicas
        new AppDTO(id: app.id, conf: app.conf).update()
    }

    @Override
    JobResult doTask() {
        assert rmService

        // todo
        if (rmService.mode == RmServiceDTO.Mode.standalone || rmService.mode == RmServiceDTO.Mode.sentinel) {
            def app = new AppDTO(id: rmService.appId).one()
            assert app

            runRemoveAppJob(app)
        } else {
            // forget nodes
            def instance = InMemoryAllContainerManager.instance
            for (shard in rmService.clusterSlotsDetail.shards) {
                def appId = shard.appId

                def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, appId)
                def primaryX = runningContainerList.find { x -> x.instanceIndex() == shard.primary().replicaIndex }

                def needRemoveReplicasContainerList = runningContainerList.findAll { x -> x.instanceIndex() in replicaIndexList }
                if (needRemoveReplicasContainerList.size() != replicaIndexList.size()) {
                    log.warn 'running need remove replicas container list size not match'
//                    return JobResult.fail('running need remove replicas container list size not match')
                }

                rmService.connectAndExe(primaryX) { jedis ->
                    for (n in shard.nodes.findAll { node -> node.replicaIndex in replicaIndexList }) {
                        def r = jedis.clusterForget(n.nodeId())
                        log.warn 'forget node: {}, result: {}', n.nodeId(), r
                    }
                }

                shard.nodes.removeIf { node -> node.replicaIndex in replicaIndexList }
            }

            log.warn('forget nodes when update replicas done, wait 5 seconds')
            Thread.sleep(1000 * 5)

            // update after nodes updated
            new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
            log.warn 'update cluster nodes ok'

            for (shard in rmService.clusterSlotsDetail.shards) {
                def app = new AppDTO(id: shard.appId).one()
                assert app

                runRemoveAppJob(app)
            }
        }

        JobResult.ok('add replicas ok')
    }
}
