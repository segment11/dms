package rm.job.task

import com.segment.common.Conf
import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import ex.JobProcessException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import model.cluster.MultiSlotRange
import model.cluster.SlotRange
import model.json.ClusterSlotsDetail
import redis.clients.jedis.Jedis
import redis.clients.jedis.params.MigrateParams
import rm.RedisManager
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class MigrateSlotsTask extends RmJobTask {
    final RmServiceDTO rmService

    final ClusterSlotsDetail.Shard fromShard

    final ClusterSlotsDetail.Shard toShard

    final SlotRange slotRange

    MigrateSlotsTask(RmJob rmJob, ClusterSlotsDetail.Shard fromShard, ClusterSlotsDetail.Shard toShard, SlotRange slotRange) {
        this.rmService = rmJob.rmService
        this.fromShard = fromShard
        this.toShard = toShard
        this.slotRange = slotRange

        this.job = rmJob

        def stepNamePrefix = 'migrate_slots_from_shard_' + fromShard.shardIndex + '_to_shard_' + toShard.shardIndex
        this.step = new JobStep(stepNamePrefix + '_slots_' + slotRange.begin + '-' + slotRange.end, 0)
    }

    @Override
    JobResult doTask() {
        assert rmService && rmService.clusterSlotsDetail && rmService.clusterSlotsDetail.shards
        log.warn step.name + ' start'

        def instance = InMemoryAllContainerManager.instance
        def appId = fromShard.appId
        def primaryNode = fromShard.primary()

        def containerList = instance.getContainerList(RedisManager.CLUSTER_ID, appId)
        def primaryX = containerList.find { x -> x.instanceIndex() == primaryNode.replicaIndex }

        assert primaryX.nodeIp == primaryNode.ip && rmService.listenPort(primaryX) == primaryNode.port

        def fromUuid = primaryNode.uuid()
        def toPrimaryNode = toShard.primary()
        def toNodeId = toPrimaryNode.nodeId()

        rmService.connectAndExe(primaryX) { jedis ->
            def jedisTo = new Jedis(toPrimaryNode.ip, toPrimaryNode.port)
            try {
                def fromMultiSlotRange = MultiSlotRange.fromClusterSlots(jedis.clusterSlots(), primaryNode.nodeId())
                def toMultiSlotRange = MultiSlotRange.fromClusterSlots(jedisTo.clusterSlots(), toNodeId)

                for (slot in slotRange.begin..slotRange.end) {
                    // can redo this task
                    if (!fromMultiSlotRange.contains(slot)) {
                        if (toMultiSlotRange.contains(slot)) {
                            log.warn 'slot already migrate: {}', slot
                        } else {
                            log.warn 'slot fail migrate: {}, from ip/port: {}:{}, to ip/port: {}:{}, both has not this slot',
                                    slot, primaryNode.ip, primaryNode.port, toPrimaryNode.ip, toPrimaryNode.port
                        }
                        continue
                    }

                    def resultImport = jedisTo.clusterSetSlotImporting(slot, primaryNode.nodeId())
                    log.debug 'prepare import slot: {}, result: {}', slot, resultImport

                    def result = jedis.clusterSetSlotMigrating(slot, toNodeId)
                    log.debug 'prepare migrate slot: {}, result: {}', slot, result

                    waitUntilMigrateOneSlotSuccess(jedis, slot, toPrimaryNode.ip, toPrimaryNode.port, fromUuid)

                    def resultSetSlotFrom = jedis.clusterSetSlotNode(slot, toNodeId)
                    log.debug 'migrate slot: {}, from node result: {}', slot, resultSetSlotFrom
                    def resultSetSlotTo = jedisTo.clusterSetSlotNode(slot, toNodeId)
                    log.debug 'migrate slot: {}, to node result: {}', slot, resultSetSlotTo

                    if (slot % 100 == 0) {
                        log.info 'done migrate slot: {}, from node: {}, to node: {}',
                                slot, fromUuid, toPrimaryNode.ip + ':' + toPrimaryNode.port
                    }
                }
            } catch (Exception e) {
                log.error 'migrate slot fail', e
                return JobResult.fail('migrate slot fail: ' + e.message)
            } finally {
                jedisTo.close()
            }
        }

        // update from / to shards slot range
        fromShard.multiSlotRange.removeMerge(slotRange.begin, slotRange.end)
        toShard.multiSlotRange.addMerge(slotRange.begin, slotRange.end)

        new RmServiceDTO(id: rmService.id, clusterSlotsDetail: rmService.clusterSlotsDetail, updatedDate: new Date()).update()
        log.info 'update cluster slots ok'

        // broadcast to other shards
        for (shard in rmService.clusterSlotsDetail.shards) {
            if (shard.shardIndex != fromShard.shardIndex && shard.shardIndex != toShard.shardIndex) {
                def node = shard.primary()
                rmService.connectAndExe(node) { jedis ->
                    for (slot in slotRange.begin..slotRange.end) {
                        jedis.clusterSetSlotNode(slot, toNodeId)
                    }
                }
                log.info 'done broadcast to shard: {}', shard.shardIndex
            }
        }

        JobResult.ok('migrate slot ok, slots: ' + slotRange)
    }

    private static String[] toStringArray(Collection<String> list) {
        def arr2 = list.toArray()
        String[] arr = new String[arr2.length]
        for (int i = 0; i < arr2.length; i++) {
            arr[i] = arr2[i] as String
        }
        arr
    }

    private void waitUntilMigrateOneSlotSuccess(Jedis jedis, Integer slot, String toIp, Integer toPort, String fromUuid) {
        int onceKeyNumber = Conf.instance.getInt('rm.job.migrateSlots.onceKeyNumber', 1000)

        List<String> keyList
        keyList = jedis.clusterGetKeysInSlot(slot, onceKeyNumber)

        int count = 0
        while (keyList) {
            count += keyList.size()
            def arr = toStringArray(keyList)
            def result = jedis.migrate(toIp, toPort, 0, new MigrateParams().auth(rmService.pass), arr)
            if ('OK' != result) {
                if ('NOKEY' == result) {
                    break
                }

                throw new JobProcessException('migrate slot fail, result: ' + result +
                        ', this node: ' + fromUuid + ', slot: ' + slot)
            }
        }
        log.debug 'done migrate slot: {}, key number: {}, from node: {}, to node: {}',
                slot, count, fromUuid, toIp + ':' + toPort
        log.debug 'done migrate slot: {}', slot
    }
}
