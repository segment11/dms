# Kafka Manager Review Round 11

## Scope

- Review target: commit `62bf43a` (fix(km): address review round 10 findings)
- Stage: fix pass for round 10 findings
- Previous round: `doc/feat/kafka_manager/review_round10.md`

## Round 10 Finding Status

| # | Finding | Status | Notes |
|---|---------|--------|-------|
| 1 | App group `bitnami` | **Fixed** | `ServiceCtrl.groovy:170` |
| 2 | Template params mismatch | **Partially fixed** | See finding 1 below |
| 3 | Container mount path | **Fixed** | `ServiceCtrl.groovy:203` |
| 4 | ValidateZookeeperTask chroot connect | **Fixed** | Connects to bare `zkConnectString`, creates chroot path |
| 5 | Scale-up/scale-down empty task lists | **Fixed** | Task chains wired correctly |
| R9 | PartitionBalancer blockers | **Fixed** | Empty input, duplicate replicas, actual broker IDs all addressed |
| 6 | Stub tasks | **Not fixed** | All task implementations remain stubs (see finding 3) |
| 7 | Snapshot export writes no files | **Not fixed** | `KmSnapshotManager.exportSnapshot()` still creates DB record only |
| 8 | Snapshot import doesn't parse/reconstruct | **Not fixed** | `KmSnapshotManager.importSnapshot()` still creates DB record only |
| 9 | Curator connection leak (WaitBrokersRegistered) | **Fixed** | Refactored to iterative loop with single client |
| 10 | MetricCtrl kafka_exporter missing broker addresses | **Partially fixed** | See finding 2 below |
| 11 | GET /list mutates DB state | **Fixed** | Status mutation removed from list endpoint |
| 12 | Root chroot deletion guard | **Fixed** | `isValidChroot()` check added before delete |
| 13 | KafkaPlugin checker no-op | **Not fixed** | `before` checker still returns `true` |
| 14 | WaitInstancesRunningTask recursion | **Fixed** | Refactored to iterative loop |
| 15 | ValidateZookeeperTaskTest minimal | **Not fixed** | No new test coverage for `doTask()` |
| 16 | ObjectMapper per broker | **Fixed** | Shared static instance |
| 17 | SnapshotCtrl unused name param | **Fixed** | Removed |
| 18 | `var` instead of `def` | **Fixed** | Changed to `def` |

## New Findings

### Critical

1. **ServiceCtrl `/add` does not set `imageTplId` or `content` on `FileVolumeMount` — template rendering will not execute.**

   The `FileVolumeMount` requires `imageTplId` (and optionally `content`) to bind to an `ImageTplDTO` record that contains the Groovy template script. Without it, the framework has no template to render — the `paramList` values go unused, and no `server.properties` file is generated for the container.

   Other controllers (Redis `ServiceCtrl`, `SentinelServiceCtrl`) always look up the `ImageTplDTO` by name and set both `imageTplId` and `content`:
   ```groovy
   // Redis example at dms/src/ctrl/redis/ServiceCtrl.groovy:364
   def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content, dist: '/etc/redis/redis.conf')
   ```

   Kafka ServiceCtrl should do the same — look up the `ImageTplDTO` by `imageName` and `tplName` (either `server.properties.tpl` or `server.properties.template.tpl` depending on whether `configTemplateId` is set), then set `mountOne.imageTplId = tplOne.id` and `mountOne.content = tplOne.content`.

   Additionally, when `configTemplateId` is provided, the template-aware variant (`ServerPropertiesUseTemplateTpl.groovy`) still expects a `configTemplateId` binding variable (line 20), but ServiceCtrl removed it from `paramList` in this fix commit. Either re-add it to `paramList` when `configTemplateId` is set, or restructure so the template doesn't need it.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:202-214`
   - `dms_common/src/model/json/FileVolumeMount.groovy:9` (`imageTplId` field)
   - `dms/src/ctrl/redis/ServiceCtrl.groovy:364` (correct pattern)
   - `dms/plugins_resources/kafka/ServerPropertiesUseTemplateTpl.groovy:20` (expects `configTemplateId`)

### High

2. **MetricCtrl kafka_exporter uses hardcoded `broker1:9092` instead of actual broker addresses.**

   The fix added `KAFKA_SERVER=broker1:9092` (line 83), which is a placeholder, not real broker addresses. The MetricCtrl has access to the service's broker topology (via `KmServiceDTO.brokerDetail`), but the code doesn't look up any service — it doesn't even accept a `serviceId` parameter to know which Kafka cluster to monitor.

   Fix: accept `serviceId` as a parameter, load the `KmServiceDTO`, build the broker list from `brokerDetail` (or from `zkConnectString` + port), and set `KAFKA_SERVER` to the actual comma-separated `host:port` list.

   References:
   - `dms/src/ctrl/kafka/MetricCtrl.groovy:83`
   - Design doc: "configured via `AppConf.envList` with `KAFKA_SERVER=broker1:9092,broker2:9092`"

3. **All seven task stubs remain no-ops — scale-up/scale-down/topic-create/failover will silently succeed without doing anything.**

   The task chains are now wired (finding 5 fixed), but the tasks themselves still return `JobResult.ok(...)` immediately. This means:
   - `POST /scale-up` creates a job, "runs" it, and all tasks report success — but no broker instances are actually added
   - `POST /scale-down` reports success but no partitions are reassigned, no containers stopped
   - `POST /kafka/topic/add` reports success but no topic is created on the Kafka cluster
   - `POST /kafka/service/failover` (if it existed) would report success without killing any container

   Since the controllers set the service status based on job completion (e.g. `scaling_up`), the service status will transition to a post-operation state without the operation actually happening.

   Stub tasks: `CreateTopicTask`, `AddBrokersTask`, `RemoveBrokersTask`, `ReassignPartitionsTask`, `WaitReassignmentCompleteTask`, `DecommissionBrokerTask`, `FailoverTask`.

   References:
   - All files under `dms/src/km/job/task/`

4. **MetricCtrl removed `conf.containerNumber = 1` — kafka_exporter app has no container count.**

   The fix replaced the `containerNumber` line with the env var line instead of adding the env var alongside it. The `containerNumber` field defaults to `0` in `AppConf`, which means the app won't schedule any containers.

   References:
   - `dms/src/ctrl/kafka/MetricCtrl.groovy:83` (was line 80 before)

### Medium

5. **Scale-down `removeBrokerIds` calculation assumes broker IDs are 0-based contiguous integers.**

   Line 391: `def removeBrokerIds = ((one.brokers - brokerCount)..<one.brokers) as int[]`

   This assumes broker IDs are `0, 1, 2, ..., N-1` and that the highest-indexed brokers should be removed. In practice, after a previous scale-up/scale-down cycle, broker IDs may not be contiguous. The code should derive the actual broker IDs to remove from `one.brokerDetail.brokers`, removing from the tail of the broker list.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:391`

6. **KmSnapshotManager export/import are still stubs — no file I/O.**

   `exportSnapshot()` creates a DB record and returns a dir path but never writes `snapshot.json`, `server.properties`, or `topics.json` to disk. `importSnapshot()` creates a DB record but never reads the snapshot, creates a service, or starts brokers. These are round 10 findings 7-8, still unfixed.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:14-38` (export)
   - `dms/src/km/KmSnapshotManager.groovy:41-60` (import)

7. **KafkaPlugin `before` checker is still a no-op.**

   Round 10 finding 13. The checker always returns `true` (line 124). Port conflict detection, directory pre-creation, ZooKeeper connectivity validation, and broker ID uniqueness validation are all missing.

   References:
   - `dms/plugins/plugin/demo2/KafkaPlugin.groovy:120-141`

8. **PartitionBalancer test coverage improved but missing edge cases for the fixed code.**

   The test added cases for empty input and decommission scenarios, which is good. However, no test covers `reassignForScale` with non-contiguous broker IDs (e.g., existing brokers `[0, 2, 5]` with new broker `[7]`). This was the core bug in round 9 — that `reassignForScale` ignored actual broker IDs. A test should verify the fix works for non-contiguous IDs.

   References:
   - `dms/test/km/PartitionBalancerTest.groovy`

### Low

9. **`WaitReassignmentCompleteTask` still has an unused `tryCount` field.**

   The class declares `int tryCount = 0` (line 15) but `doTask()` is a stub that returns immediately without using it. When the task is implemented, this should be cleaned up (either use iterative loop like the other tasks, or remove).

   References:
   - `dms/src/km/job/task/WaitReassignmentCompleteTask.groovy:15`

10. **`NodeVolumeDTO` in ServiceCtrl uses `imageName: 'library/kafka'` instead of `bitnami/kafka`.**

    Line 195 still references `library/kafka` for the node volume image name, inconsistent with the fix to `conf.group = 'bitnami'`.

    References:
    - `dms/src/ctrl/kafka/ServiceCtrl.groovy:195`

## Summary

The fix commit addresses the majority of round 10's critical findings. PartitionBalancer, ValidateZookeeperTask, task chain wiring, mount path, app group, and the retry loop issues are all resolved correctly.

**Must fix before proceeding:**

1. Set `imageTplId` and `content` on `FileVolumeMount` so template rendering actually executes (finding 1) — this is the remaining critical blocker for cluster creation
2. Restore `conf.containerNumber = 1` for kafka_exporter app (finding 4)
3. Fix `NodeVolumeDTO` image name to `bitnami/kafka` (finding 10)

**Should fix before feature review signoff:**

4. Wire `configTemplateId` param when the template-aware variant is used (finding 1, second part)
5. Accept `serviceId` in MetricCtrl and use real broker addresses for kafka_exporter (finding 2)
6. Implement real logic in stub tasks or gate the endpoints (finding 3)
7. Derive `removeBrokerIds` from `brokerDetail` instead of assuming contiguous IDs (finding 5)
8. Implement snapshot export/import file I/O (finding 6)
9. Implement KafkaPlugin checker logic (finding 7)
