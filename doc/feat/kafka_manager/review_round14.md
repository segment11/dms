# Kafka Manager Review Round 14

## Scope

- Review target: commit `d97b880` (feat(km): implement real task logic, snapshot file I/O, and plugin checker)
- Stage: implementation of all deferred items from round 13
- Previous round: `doc/feat/kafka_manager/review_round13.md`

## Round 13 Deferred Item Status

| Item | Status | Notes |
|------|--------|-------|
| 7 stub task implementations | **Implemented** | See findings below for quality |
| KmSnapshotManager export file I/O | **Implemented** | Writes `snapshot.json`; missing `server.properties` and `topics.json` |
| KmSnapshotManager import reconstruction | **Partially implemented** | Reads/validates `snapshot.json`, but doesn't create service/brokers/topics |
| KafkaPlugin `before` checker | **Implemented** | Port conflict + dir pre-creation |
| ValidateZookeeperTask test coverage | **Not addressed** | Still only `isValidChroot` tested |

## Findings

### Critical

1. **`ReassignPartitionsTask` does not actually reassign partitions — it's still effectively a stub.**

   The task connects to ZooKeeper, reads topic names, checks broker detail exists, then returns `JobResult.ok('reassignment submitted for N topics')` without performing any reassignment. It never:
   - Reads current partition assignments from ZK (`/brokers/topics/{name}`)
   - Calls `PartitionBalancer.reassignForScale()` or `reassignForDecommission()`
   - Writes the reassignment JSON to `/admin/reassign_partitions`

   This means both `BROKER_SCALE_UP` and `BROKER_SCALE_DOWN` job chains will report success for the reassignment step without actually moving partitions. For scale-down, this is dangerous: the subsequent `DecommissionBrokerTask` will remove broker ZK nodes and `RemoveBrokersTask` will stop containers — but partitions that were on those brokers were never moved, causing data loss and under-replicated partitions.

   References:
   - `dms/src/km/job/task/ReassignPartitionsTask.groovy:25-56`
   - Design doc: "Generate reassignment plan, move partition replicas to include new brokers"

2. **`CreateTopicTask` uses `PartitionBalancer.assignReplicas(brokerCount, ...)` with `brokerCount` but actual broker IDs may not be 0-based contiguous.**

   Line 56: `PartitionBalancer.assignReplicas(brokerCount, partitions, replicationFactor)` generates replica assignments using broker IDs `0, 1, 2, ..., brokerCount-1`. But after scale-up/scale-down, actual broker IDs may be non-contiguous (e.g., `[0, 2, 5]`). ZooKeeper requires that the partition assignment references actual registered broker IDs. Writing assignments with non-existent broker IDs will cause topic creation to fail or produce unassignable partitions.

   Fix: pass actual broker IDs from `brokerDetail.brokers` to the assignment logic, or map the 0-based output to actual broker IDs.

   References:
   - `dms/src/km/job/task/CreateTopicTask.groovy:56`
   - `dms/src/km/PartitionBalancer.groovy:8` (`assignReplicas` always uses 0-based IDs)

### High

3. **`FailoverTask` stops the controller container but never restarts it.**

   The task stops the controller broker container (line 65), waits for ZK to elect a new controller, then returns success. The stopped broker is never restarted. After failover, the cluster runs with one fewer broker permanently. The design says "Kill the current controller broker container, wait for ZooKeeper to elect a new controller, update `brokerDetail`" — the `brokerDetail` update is also missing.

   The task should either restart the container after the new controller is elected, or at minimum update `brokerDetail` to mark the old controller as non-controller and identify the new controller.

   References:
   - `dms/src/km/job/task/FailoverTask.groovy:62-77`
   - Design doc: "FailoverTask — Kill the current controller broker container, wait for ZooKeeper to elect a new controller, update brokerDetail"

4. **`FailoverTask` matches controller to container by IP only — ambiguous when multiple brokers share a node.**

   Line 57-60: `containerList.find { x -> controllerNode.ip == x.nodeIp }`. When multiple Kafka broker containers run on the same node (same IP, different ports), this returns the first match, which may not be the controller. Should also match by port or instance index.

   References:
   - `dms/src/km/job/task/FailoverTask.groovy:57-60`

5. **`RemoveBrokersTask` and `DecommissionBrokerTask` have overlapping responsibilities — containers are stopped by both.**

   In the `BROKER_SCALE_DOWN` chain: `DecommissionBrokerTask` removes broker ZK nodes and updates `brokerDetail`, then `RemoveBrokersTask` stops and removes containers. However, `DecommissionBrokerTask` only deletes ZK nodes — the actual broker process is still running. A running broker will immediately re-register in ZK after its ephemeral node is deleted. The ZK delete is ineffective unless the broker process is stopped first.

   The correct order should be: stop containers first, then clean up ZK. Currently `DecommissionBrokerTask` runs before `RemoveBrokersTask`, so ZK cleanup happens while brokers are still alive.

   References:
   - `dms/src/km/job/task/DecommissionBrokerTask.groovy:37-42` (ZK delete while broker running)
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:406-409` (task order: Decommission before Remove)

6. **`KmSnapshotManager.exportSnapshot` only writes `snapshot.json` — missing `server.properties` and `topics.json`.**

   The design specifies three files in the snapshot directory:
   - `snapshot.json` — written (good)
   - `server.properties` — not written (materialized broker config from template + overrides)
   - `topics.json` — not written (topics are embedded in `snapshot.json` as `content.topics`, but the design calls for a separate file)

   The `server.properties` file is important for import: it lets the operator inspect and verify broker config before reconstructing the cluster.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:39-68`
   - Design doc: "snapshot.json, server.properties, topics.json"

7. **`KmSnapshotManager.importSnapshot` reads the snapshot but doesn't reconstruct anything.**

   The method validates the file exists, reads the JSON, creates a `KmSnapshotDTO` record, and returns. It does not:
   - Create a `KmServiceDTO` from the snapshot
   - Create an app / start broker containers
   - Recreate topics
   - Validate ZK connectivity or chroot uniqueness

   Per the design, import should "Create a new KmServiceDTO with the same config, Create Kafka brokers → wait for registration, Recreate all topics with original partitions, replication factor, and config overrides."

   References:
   - `dms/src/km/KmSnapshotManager.groovy:85-114`
   - Design doc: Import section

### Medium

8. **KafkaPlugin port conflict checker compares ports across apps but not across instances on the same node.**

   The checker compares the configured port of the current app against other Kafka apps' ports. If two different Kafka services use port 9092, it correctly detects the conflict. But it doesn't check whether this specific node already has a different app (non-Kafka) using the same port. The design calls for "Port conflict detection across all Kafka apps" — so this scope is acceptable, but worth noting it doesn't protect against non-Kafka port collisions.

   Also, the port comparison is app-level, not node-level. Two Kafka services on different nodes using the same port should not conflict, but the current check would flag it. Since brokers use `networkMode: 'host'`, port conflicts are only relevant per-node.

   References:
   - `dms/plugins/plugin/demo2/KafkaPlugin.groovy:126-148`

9. **`KmSnapshotManager` has unused `ObjectMapper` import.**

   Line 3: `import com.fasterxml.jackson.databind.ObjectMapper` — never used. `DefaultJsonTransformer` is used instead.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:3`

10. **`KmSnapshotManager.exportSnapshot` type mismatch: `ExtendParams.params` is `Map<String, Object>` but `TopicEntry.configOverrides` expects `Map<String, String>`.**

    Line 64: `configOverrides: t.configOverrides?.params ?: [:]` assigns `Map<String, Object>` to a `Map<String, String>` field. Groovy's dynamic typing allows this at runtime, but values that are not strings (e.g., integer retention.ms values) will serialize differently. Should explicitly cast values to strings.

    References:
    - `dms/src/km/KmSnapshotManager.groovy:64`
    - `dms_common/src/model/json/ExtendParams.groovy:10` (`Map<String, Object>`)

11. **`CreateTopicTask` updates `KmTopicDTO` with `.where().update()` but the record may not exist yet.**

    Lines 71-76: The task tries to update an existing `KmTopicDTO` matching `service_id` and `name`. But `CreateTopicTask` is called from the `TOPIC_CREATE` job — the `KmTopicDTO` record with status `creating` should already exist (created by `TopicCtrl` before the job runs). If the record doesn't exist, the `.update()` call will silently update zero rows, leaving the topic in `creating` status permanently.

    Should verify the record exists or use insert-or-update logic.

    References:
    - `dms/src/km/job/task/CreateTopicTask.groovy:71-76`

## Summary

This is a substantial implementation commit that replaces all seven stub tasks with real logic and adds snapshot file I/O and plugin checker functionality. The overall direction is correct and the ZooKeeper interaction patterns are consistent.

**Must fix before proceeding:**

1. Implement actual partition reassignment in `ReassignPartitionsTask` — this is the most critical gap since scale-down depends on it for data safety (finding 1)
2. Fix `CreateTopicTask` to use actual broker IDs, not 0-based indices (finding 2)
3. Reorder or fix the scale-down chain so containers are stopped before ZK nodes are deleted (finding 5)

**Should fix before feature review signoff:**

4. Restart the controller container after failover, or update `brokerDetail` (finding 3)
5. Fix failover container matching to include port, not just IP (finding 4)
6. Complete snapshot export with `server.properties` and `topics.json` files (finding 6)
7. Implement real import reconstruction logic (finding 7)
