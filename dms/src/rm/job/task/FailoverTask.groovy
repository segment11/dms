package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ha.JedisPoolHolder
import model.RmServiceDTO
import redis.clients.jedis.args.ClusterFailoverOption
import rm.RedisManager
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

@CompileStatic
@Slf4j
class FailoverTask extends RmJobTask {
    final RmServiceDTO rmService

    final int shardIndex

    final int replicaIndex

    FailoverTask(RmJob rmJob, int shardIndex, int replicaIndex) {
        this.rmService = rmJob.rmService
        this.shardIndex = shardIndex
        this.replicaIndex = replicaIndex

        this.job = rmJob
        this.step = new JobStep('failover', 0)
    }

    @Override
    JobResult doTask() {
        assert rmService

        def runningContainerList = rmService.runningContainerList()
        def instance = InMemoryAllContainerManager.instance
        if (rmService.mode == RmServiceDTO.Mode.standalone) {
            def primaryX = runningContainerList.find { x ->
                'master' == rmService.connectAndExe(x) { jedis ->
                    jedis.role()[0] as String
                }
            }
            if (primaryX) {
                if (primaryX.instanceIndex() == replicaIndex) {
                    log.warn 'target instance is primary, ignore'
                    return JobResult.ok('target instance is primary, ignore')
                }
            }

            def containerX = runningContainerList.find { x -> x.instanceIndex() == replicaIndex }
            if (!containerX) {
                log.warn 'target instance not exist, ignore'
                return JobResult.ok('target instance not exist, ignore')
            }

            rmService.connectAndExe(containerX) { jedis ->
                def r = jedis.replicaofNoOne()
                log.warn 'set target instance, replica of no one, result {}, instance index {}', r, replicaIndex
            }

            def primaryNodeIp = containerX.nodeIp
            def primaryRedisPort = rmService.listenPort(primaryX)

            runningContainerList.findAll { x ->
                x != containerX
            }.each { x ->
                rmService.connectAndExe(containerX) { jedis ->
                    def r = jedis.replicaof(primaryNodeIp, primaryRedisPort)
                    log.warn 'set other instances, replica of {}, result {}, instance index {}', primaryNodeIp, r, x.instanceIndex()
                }
            }

            return JobResult.ok()
        } else if (rmService.mode == RmServiceDTO.Mode.sentinel) {
            def node = rmService.primaryReplicasDetail.nodes.find { n -> n.replicaIndex == replicaIndex }
            assert node
            if (node.isPrimary) {
                log.warn 'target instance is primary, ignore'
                return JobResult.ok('target instance is primary, ignore')
            }

            // set slave-priority
            def containerX = runningContainerList.find { x -> x.instanceIndex() == replicaIndex }
            if (!containerX) {
                log.warn 'target instance not exist, ignore'
                return JobResult.ok('target instance not exist, ignore')
            }

            rmService.connectAndExe(containerX) { jedis ->
                def r = jedis.configSet('slave-priority', '1')
                log.warn 'set target instance, slave-priority 1, result {}, instance index {}', r, replicaIndex
            }

            runningContainerList.findAll { x ->
                x != containerX
            }.each { x ->
                rmService.connectAndExe(containerX) { jedis ->
                    def r = jedis.configSet('slave-priority', '100')
                    log.warn 'set other instances, slave-priority 100, result {}, instance index {}', r, x.instanceIndex()
                }
            }

            def sentinelAppOne = InMemoryCacheSupport.instance.oneApp(rmService.sentinelAppId)
            def sentinelRunningContainerList = instance.getRunningContainerList(RedisManager.CLUSTER_ID, sentinelAppOne.id)
            if (!sentinelRunningContainerList) {
                log.warn 'sentinel not running'
                return JobResult.ok('sentinel not running')
            }

            def sentinelX = sentinelRunningContainerList[0]

            def sentinelConfOne = sentinelAppOne.conf.fileVolumeList.find { it.dist.contains('/sentinel') }
            def sentinelPort = sentinelConfOne.paramValue('port') as int
            def sentinelPassword = sentinelConfOne.paramValue('password') as String
            def isSentinelSingleNode = 'true' == sentinelConfOne.paramValue('isSingleNode')

            def masterName = 'redis-app-' + rmService.appId

            def thisInstanceSentinelPort = sentinelPort + (isSentinelSingleNode ? sentinelX.instanceIndex() : 0)
            def jedisPoolSentinel = JedisPoolHolder.instance.create(sentinelX.nodeIp, thisInstanceSentinelPort, sentinelPassword)
            JedisPoolHolder.instance.exe(jedisPoolSentinel) { jedis ->
                def r = jedis.sentinelFailover(masterName)
                log.warn 'sentinel failover, result {}, master name {}', r, masterName
            }

            def oldPrimaryNode = rmService.primaryReplicasDetail.nodes.find { n -> n.isPrimary }
            oldPrimaryNode.isPrimary = false
            node.isPrimary = true

            new RmServiceDTO(id: rmService.id, primaryReplicasDetail: rmService.primaryReplicasDetail, updatedDate: new Date()).update()
            log.warn 'update primary replicas nodes ok'

            return JobResult.ok()
        } else {
            // cluster mode
            assert rmService.clusterSlotsDetail && rmService.clusterSlotsDetail.shards
            def shard = rmService.clusterSlotsDetail.shards.find { shard -> shard.shardIndex == shardIndex }
            assert shard

            def node = shard.nodes.find { n -> n.replicaIndex == replicaIndex }
            assert node
            if (node.isPrimary) {
                log.warn 'target instance is primary, ignore'
                return JobResult.ok('target instance is primary, ignore')
            }

            rmService.connectAndExe(node) { jedis ->
                def r = jedis.clusterFailover(ClusterFailoverOption.TAKEOVER)
                log.warn 'cluster failover takeover, result {}, instance index {}', r, node.replicaIndex
            }

            def oldPrimaryNode = shard.nodes.find { n -> n.isPrimary }
            oldPrimaryNode.isPrimary = false
            node.isPrimary = true

            new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
            log.warn 'update cluster nodes ok'

            return JobResult.ok()
        }
    }
}
