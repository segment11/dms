package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import ex.JobProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import model.RmServiceDTO
import model.json.ClusterSlotsDetail
import model.json.PrimaryReplicasDetail
import rm.RedisManager
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager
import server.scheduler.processor.CreateProcessor

@CompileStatic
@Slf4j
class AddReplicasTask extends RmJobTask {
    final RmServiceDTO rmService

    final List<Integer> replicaIndexList

    AddReplicasTask(RmJob rmJob, List<Integer> replicaIndexList) {
        this.rmService = rmJob.rmService
        this.replicaIndexList = replicaIndexList

        this.job = rmJob
        this.step = new JobStep('add_replicas', 0)
    }

    private void runCreateAppJob(AppDTO app) {
        // replicas already updated
        if (app.conf.containerNumber >= rmService.replicas) {
            return
        }
        app.conf.containerNumber = rmService.replicas

        List<Integer> needRunInstanceIndexList = []
        replicaIndexList.each {
            needRunInstanceIndexList << it
        }

        def appJob = new AppJobDTO(
                appId: app.id,
                failNum: 0,
                status: AppJobDTO.Status.created,
                jobType: AppJobDTO.JobType.create,
                createdDate: new Date(),
                updatedDate: new Date()).
                needRunInstanceIndexList(needRunInstanceIndexList)
        int appJobId = appJob.add()
        appJob.id = appJobId

        try {
            new CreateProcessor().process(appJob, app, [])
            new AppJobDTO(id: appJobId, status: AppJobDTO.Status.done, updatedDate: new Date()).update()
            log.warn('start application create job done, job id: {}', job.id)
        } catch (Exception e) {
            log.error('start application create job error', e)
            throw new JobProcessException('start application create job error')
        }

        // wait dms agent send containers info
        Thread.sleep(1000 * 5)

        if (rmService.mode == RmServiceDTO.Mode.standalone) {
            // do replica of manually, if sentinel mode, RedisPlugin will do replica of
            def runningContainerList = rmService.runningContainerList()
            def primaryX = runningContainerList.find { x ->
                'master' == rmService.connectAndExe(x) { jedis ->
                    jedis.role()[0] as String
                }
            }
            assert primaryX

            def newReplicasContainerList = runningContainerList.findAll { x -> x.instanceIndex() in replicaIndexList }
            if (newReplicasContainerList.size() != replicaIndexList.size()) {
                throw new JobProcessException('new replicas container list size not match')
            }

            def primaryNodeIp = primaryX.nodeIp
            def primaryNodePort = rmService.listenPort(primaryX)

            newReplicasContainerList.each { x ->
                rmService.connectAndExe(x) { jedis ->
                    def r = jedis.replicaof(primaryNodeIp, primaryNodePort)
                    log.warn 'replicaof of {} to {}:{}, result: {}', x.name(), primaryNodeIp, primaryNodePort, r
                }
            }
        } else {
            def runningContainerList = rmService.runningContainerList()

            def newReplicasContainerList = runningContainerList.findAll { x -> x.instanceIndex() in replicaIndexList }
            if (newReplicasContainerList.size() != replicaIndexList.size()) {
                throw new JobProcessException('new replicas container list size not match')
            }

            newReplicasContainerList.each { x ->
                def node = new PrimaryReplicasDetail.Node()
                node.ip = x.nodeIp
                node.port = rmService.listenPort(x)
                node.replicaIndex = x.instanceIndex()
                node.isPrimary = false
                rmService.primaryReplicasDetail.nodes << node
            }

            // update after nodes updated
            new RmServiceDTO(id: rmService.id, primaryReplicasDetail: rmService.primaryReplicasDetail, updatedDate: new Date()).update()
            log.warn 'update primary replicas nodes ok'
        }

        new AppDTO(id: app.id, conf: app.conf).update()
    }

    @Override
    JobResult doTask() {
        assert rmService

        // todo
        if (rmService.mode == RmServiceDTO.Mode.standalone || rmService.mode == RmServiceDTO.Mode.sentinel) {
            def app = new AppDTO(id: rmService.appId).one()
            assert app

            runCreateAppJob(app)
        } else {
            for (shard in rmService.clusterSlotsDetail.shards) {
                def app = new AppDTO(id: shard.appId).one()
                assert app

                runCreateAppJob(app)
            }

            // meet nodes
            def instance = InMemoryAllContainerManager.instance
            for (shard in rmService.clusterSlotsDetail.shards) {
                def appId = shard.appId

                def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, appId)
                def primaryX = runningContainerList.find { x -> x.instanceIndex() == shard.primary().replicaIndex }

                def newReplicasContainerList = runningContainerList.findAll { x -> x.instanceIndex() in replicaIndexList }
                if (newReplicasContainerList.size() != replicaIndexList.size()) {
                    return JobResult.fail('running new replicas container list size not match')
                }

                newReplicasContainerList.each { x ->
                    def node = new ClusterSlotsDetail.Node()
                    node.ip = x.nodeIp
                    node.port = rmService.listenPort(x)
                    node.shardIndex = shard.shardIndex
                    node.replicaIndex = x.instanceIndex()
                    node.isPrimary = false
                    shard.nodes << node
                }

                rmService.connectAndExe(primaryX) { jedis ->
                    for (x in newReplicasContainerList) {
                        def listenPort2 = rmService.listenPort(x)
                        def r = jedis.clusterMeet(x.nodeIp, listenPort2)
                        log.warn 'meet node, new node: {}, old node: {}, shard index:{}, app id: {}, result: {}',
                                x.nodeIp + ':' + listenPort2,
                                primaryX.nodeIp + ':' + rmService.listenPort(primaryX),
                                shard.shardIndex,
                                appId,
                                r
                    }
                }
            }

            log.warn('meet nodes when scale done, wait 5 seconds')
            Thread.sleep(1000 * 5)

            // update after nodes updated
            new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
            log.warn 'update cluster nodes ok'

            // replica of
            for (shard in rmService.clusterSlotsDetail.shards) {
                def appId = shard.appId

                def primaryNode = shard.primary()
                def primaryNodeId = primaryNode.nodeId()

                def runningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, appId)
                runningContainerList.findAll { xx -> xx.instanceIndex() in replicaIndexList }.each { x ->
                    rmService.connectAndExe(x) { jedis ->
                        def r = jedis.clusterReplicas(primaryNodeId)
                        log.warn 'replicaof of {} to {}:{}, result: {}', x.name(), primaryNode.ip, primaryNode.port, r
                    }
                }
            }

            log.warn('replica of done')
        }

        JobResult.ok('add replicas ok')
    }
}
