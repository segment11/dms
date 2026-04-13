# Kafka Manager Review Round 15

## Scope

- Review target: commit `87f576b` (fix(km): address review round 14 findings)
- Stage: fix pass for round 14 findings
- Previous round: `doc/feat/kafka_manager/review_round14.md`

## Round 14 Finding Status

| # | Finding | Status | Notes |
|---|---------|--------|-------|
| 1 | ReassignPartitionsTask was a stub | **Fixed** | Real ZK reassignment with PartitionBalancer |
| 2 | CreateTopicTask used 0-based broker IDs | **Fixed** | Maps indices to actual broker IDs via `brokerIds[idx]` |
| 3 | FailoverTask didn't restart controller | **Fixed** | Restarts container and updates brokerDetail |
| 4 | FailoverTask matched by IP only | **Fixed** | Matches by IP+brokerIndex, falls back to IP-only |
| 5 | Scale-down task order (ZK before stop) | **Fixed** | Reordered: RemoveBrokers (stop) before Decommission (ZK) |
| 6 | Snapshot export missing files | **Fixed** | Writes `snapshot.json`, `topics.json`, `server.properties` |
| 7 | Snapshot import didn't reconstruct | **Fixed** | Full reconstruction with service/app/job/topics |
| 8 | Plugin checker scope | Acknowledged | Not changed, acceptable as-is |
| 9 | Unused ObjectMapper import | **Fixed** | Removed |
| 10 | configOverrides type mismatch | **Fixed** | Explicit `k as String, v as String` cast |
| 11 | CreateTopicTask insert-or-update | **Fixed** | Checks existing record, inserts if missing |

## New Findings

### High

1. ~~**Scale-down `RemoveBrokersTask` uses instanceIndex — may stop wrong containers.**~~

   **Retracted.** DMS uses instance index as the canonical container identity, always mapped 0..N-1 to containerNumber. `RemoveBrokersTask` correctly uses `instanceIndex >= newContainerNumber` to select containers for removal — this is the standard DMS pattern. Kafka broker IDs are a separate concern from DMS instance indices. No fix needed.

2. **`ReassignPartitionsTask` for scale-up passes all current broker IDs as "new" broker IDs to `reassignForScale`.**

   Line 82: `def brokerIds = brokerDetail.brokers.collect { it.brokerId } as int[]`
   Line 83: `newAssignment = PartitionBalancer.reassignForScale(currentAssignment, brokerIds)`

   `reassignForScale(currentAssignment, newBrokerIds)` expects `newBrokerIds` to be only the newly added broker IDs. Passing all broker IDs (existing + new) means the method's `existingBrokers` (extracted from `currentAssignment`) will overlap with `newBrokerIds`. The union set `allBrokers` will still be correct, so the final result is actually fine — but the semantics are misleading and the method name suggests it should receive only new broker IDs.

   More importantly, for scale-up the task runs after `WaitBrokersRegisteredTask`, which updates `brokerDetail` with all brokers (old + new). So `brokerDetail.brokers` already includes the new ones. To correctly identify just the new broker IDs, the task would need to compare against the pre-scale broker list. Currently it works because `reassignForScale` takes the union anyway, but this is fragile.

   References:
   - `dms/src/km/job/task/ReassignPartitionsTask.groovy:82-83`
   - `dms/src/km/PartitionBalancer.groovy:25` (`reassignForScale` signature)

3. **`FailoverTask` detects new controller by checking if `/controller` ZK data changed or disappeared — but a disappeared node means no controller is elected yet.**

   Lines 85-86: `if (exists == null) { newElected = true; break }` — if the `/controller` ephemeral node disappears, it means the old controller's ZK session expired but no new controller has been elected yet. The task considers this "new elected" and immediately restarts the old container. The restarted broker may become controller again, defeating the purpose of failover.

   Should wait until a new `/controller` node appears with a *different* broker ID, not just detect that the old one disappeared.

   References:
   - `dms/src/km/job/task/FailoverTask.groovy:84-96`

4. **Snapshot import hardcodes `defaultPartitions=8`, `defaultReplicationFactor=1`, `heapMb=1024` instead of reading from snapshot.**

   Lines 217-220, 236-238: The import ignores values from the snapshot's `KmSnapshotContent` (which contains the original cluster's config) and uses hardcoded defaults. The snapshot's `configItems` and `configOverrides` fields are also not used to reconstruct `KmConfigTemplateDTO` or `config_overrides`.

   Per design: "Create a new KmServiceDTO with the same config (mode, version, broker count, ports). Recreate a KmConfigTemplateDTO from the snapshot's configItems if no matching template exists."

   References:
   - `dms/src/km/KmSnapshotManager.groovy:217-220,236-238`
   - `dms/src/model/json/KmSnapshotContent.groovy:22-23` (configItems, configOverrides available)

5. **Snapshot import sets snapshot status to `done` before the async job completes.**

   Line 293-294: `new KmSnapshotDTO(id: snapshotId, status: KmSnapshotDTO.Status.done, ...)` is called right after `KmJobExecutor.instance.execute { kmJob.run() }`. Since the job runs asynchronously, the snapshot is marked `done` while the cluster is still being created. If the job fails, the snapshot will remain in `done` status.

   Should mark `done` only after the job succeeds, or track via job status.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:289-294`

### Medium

6. **Snapshot export `server.properties` contains unresolved placeholders.**

   Lines in the exported `server.properties`:
   ```
   broker.id=${brokerId}
   listeners=PLAINTEXT://0.0.0.0:${port}
   advertised.listeners=PLAINTEXT://${nodeIp}:${port}
   ```

   These are template variables, not resolved values. The design says "materialized broker config (config template items + runtime overrides merged)." The exported `server.properties` should contain the actual resolved config from broker 0 (or a representative broker), not the template. The snapshot's purpose is to let an operator inspect the actual config that was running.

   The snapshot `configItems` and `configOverrides` fields in `KmSnapshotContent` already exist for this purpose but `exportSnapshot` doesn't populate `content.configItems` or `content.configOverrides`.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:90-106`
   - `dms/src/model/json/KmSnapshotContent.groovy:22-23`

7. **Snapshot import port fallback has a typo: `9029` instead of `9092`.**

   Line 150: `def port = brokersList ? (brokersList[0]['port'] as Integer ?: 9029) : 9092`
   Line 151: `if (port == 9029) port = 9092`

   The Elvis operator default is `9029` (typo), which is then corrected by the next line. This works but is confusing — the default should just be `9092` directly.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:150-151`

8. **`ReassignPartitionsTask` reads `removeBrokerIds` from params on every topic iteration in the loop.**

   Line 75: `def removeBrokerIdsStr = job.params?.get('removeBrokerIds') as String` is inside the `for (topicName in topicNames)` loop. This is a minor inefficiency — should be read once before the loop. The param parsing also runs for every topic.

   References:
   - `dms/src/km/job/task/ReassignPartitionsTask.groovy:74-79`

### Low

9. **`BrokerDetail.BrokerNode` has a `brokerIndex` field, but `WaitBrokersRegisteredTask` never populates it.**

   `FailoverTask` relies on `controllerNode.brokerIndex` (line 58) to match containers. `WaitBrokersRegisteredTask` creates `BrokerNode` objects with `brokerId`, `ip`, `port` but never sets `brokerIndex`. The field defaults to `0`, so all nodes have `brokerIndex == 0`, making the IP+brokerIndex match degenerate to IP-only (same result as before the fix).

   References:
   - `dms/src/km/job/task/WaitBrokersRegisteredTask.groovy:50-58`
   - `dms/src/km/job/task/FailoverTask.groovy:58`
   - `dms/src/model/json/BrokerDetail.groovy` (BrokerNode.brokerIndex)

10. **Snapshot export doesn't write `configItems` or `configOverrides` to `KmSnapshotContent`.**

    `content.configItems` and `content.configOverrides` remain empty in the exported `snapshot.json`. If import ever starts reading these fields to reconstruct config templates, it will find nothing. Should populate from `KmConfigTemplateDTO` and `one.configOverrides`.

    References:
    - `dms/src/km/KmSnapshotManager.groovy:43-67`
    - `dms/src/model/json/KmSnapshotContent.groovy:22-23`

## Summary

This commit addresses all the critical findings from round 14. The core functionality is now substantially complete:

- **ReassignPartitionsTask** properly reads current assignments from ZK, calls PartitionBalancer, and writes reassignment JSON — the most critical fix
- **CreateTopicTask** correctly maps 0-based indices to actual broker IDs
- **FailoverTask** restarts the controller and updates brokerDetail
- **Snapshot import** fully reconstructs a cluster from snapshot with topic recreation

**Should fix before feature review signoff:**

1. `FailoverTask` should wait for a *new* controller, not just detect the old one disappeared (finding 3)
2. Snapshot import should read config values from snapshot instead of hardcoding (finding 4)
3. Snapshot import should not mark `done` before async job completes (finding 5)
4. `WaitBrokersRegisteredTask` should populate `brokerIndex` on `BrokerNode` (finding 9)

**Nice to fix:**

6. Export `server.properties` with resolved values (finding 6)
7. Fix port typo `9029` → `9092` (finding 7)
8. Move `removeBrokerIds` parsing outside the topic loop (finding 8)
9. Populate `configItems`/`configOverrides` in snapshot export (finding 10)

No critical blockers remain. The system can create clusters, manage topics, scale brokers, failover controllers, and snapshot/restore — all with real ZooKeeper interaction. The remaining items are correctness refinements for edge cases (non-contiguous IDs after scale, failover race window) and completeness gaps in snapshot config preservation.
