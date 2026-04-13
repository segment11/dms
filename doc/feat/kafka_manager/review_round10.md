# Kafka Manager Review Round 10

## Scope

- Review target: commits `8d868e4`..`ecb72e5` (KafkaPlugin, cluster creation task chain, template fix, ServiceCtrl, TopicCtrl, scaling/failover tasks, SnapshotManager/SnapshotCtrl, MetricCtrl/JobCtrl)
- Stage: controllers + task chains + plugin + snapshot
- Previous round: `doc/feat/kafka_manager/review_round9.md`

## Round 9 Blocker Status

Round 9 flagged three PartitionBalancer issues as blockers. Checking `dms/src/km/PartitionBalancer.groovy`:

- **Not fixed.** `reassignForScale()` still ignores `newBrokerIds` and rebuilds as dense range (line 22-27).
- **Not fixed.** `reassignForDecommission()` still produces duplicate replicas when `remaining.size() < replicationFactor` (line 43-46).
- **Not fixed.** Both methods still crash on empty `currentAssignment` input.

These remain blockers. The scaling task chains (`BROKER_SCALE_UP`, `BROKER_SCALE_DOWN`) depend on correct reassignment logic.

## New Findings

### Critical

1. **ServiceCtrl `/add` uses wrong image group — plugin won't match the app.**

   ServiceCtrl creates apps with `conf.group = 'library'` and `conf.image = 'kafka'` (line 189-190). KafkaPlugin's `canUseTo()` only matches `bitnami/kafka` or `confluentinc/cp-kafka`. The plugin's checkers, template rendering, and init logic will not activate for these apps.

   Fix: use `conf.group = 'bitnami'` to match the plugin.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:189`
   - `dms/plugins/plugin/demo2/KafkaPlugin.groovy:137-143`

2. **ServiceCtrl `/add` template params don't match template variables.**

   ServiceCtrl passes `zkConnect` (pre-merged) in the FileVolumeMount paramList (line 226), but both `ServerPropertiesTpl.groovy` and `ServerPropertiesUseTemplateTpl.groovy` expect separate `zkConnectString` and `zkChroot` variables. Additionally, `defaultPartitions`, `defaultReplicationFactor`, and `brokerCount` are expected by the templates but never passed.

   Templates will fail at render time with missing binding properties.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:222-229`
   - `dms/plugins_resources/kafka/ServerPropertiesTpl.groovy:3-8`

3. **ServiceCtrl `/add` mounts to wrong container path.**

   The FileVolumeMount `dist` is `/opt/kafka/config/server.properties` (line 221), but the bitnami/kafka image and the KafkaPlugin both use `/opt/bitnami/kafka/config/server.properties`. The broker will start with default config, not the rendered template.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:221`
   - `dms/plugins/plugin/demo2/KafkaPlugin.groovy:86,97`

4. **ValidateZookeeperTask connects with chroot but cannot create it.**

   The task connects to `zkConnectString + zkChroot` (line 32). If the chroot path doesn't exist in ZooKeeper yet (the normal case for new services), the ZK client session will fail because the namespace doesn't exist. Per the design: the task should connect to the bare `zkConnectString` first, create the chroot path, then validate it. The ZK client cannot create its own chroot from within a chroot'd session.

   References:
   - `dms/src/km/job/task/ValidateZookeeperTask.groovy:31-32`
   - Design doc: "Open a ZooKeeper client session (Curator) to verify connectivity and create the chroot path"

5. **Scale-up and scale-down jobs have no tasks.**

   Both `/scale-up` (line 352-366) and `/scale-down` (line 397-415) create a `KmJob` and call `kmJob.run()`, but neither adds any tasks to `kmJob.taskList`. The job runs with an empty task list — effectively a no-op.

   Per design, scale-up should chain: `AddBrokersTask` -> `WaitInstancesRunningTask` -> `WaitBrokersRegisteredTask` -> `ReassignPartitionsTask` -> `WaitReassignmentCompleteTask`. Scale-down should chain: `ReassignPartitionsTask` -> `WaitReassignmentCompleteTask` -> `DecommissionBrokerTask` -> `RemoveBrokersTask`.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:352-366`
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:397-415`

### High

6. **Most task implementations are stubs.**

   `CreateTopicTask`, `AddBrokersTask`, `RemoveBrokersTask`, `ReassignPartitionsTask`, `WaitReassignmentCompleteTask`, `DecommissionBrokerTask`, and `FailoverTask` all return `JobResult.ok(...)` with no actual logic. These need real implementations before the corresponding features can work.

   While stubs are acceptable as a scaffolding step, the controllers that reference them (`/add` for topics, `/scale-up`, `/scale-down`) are wired to actually run these jobs — meaning the API appears to succeed but does nothing.

   References:
   - `dms/src/km/job/task/CreateTopicTask.groovy:28-29`
   - `dms/src/km/job/task/AddBrokersTask.groovy:24-25`
   - All other stub tasks

7. **KmSnapshotManager.exportSnapshot writes no files.**

   `exportSnapshot()` creates a DB record and returns a directory path, but never writes `snapshot.json`, `server.properties`, or `topics.json` to disk. The design specifies these files as the core snapshot output.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:12-33`

8. **KmSnapshotManager.importSnapshot doesn't parse or reconstruct.**

   `importSnapshot()` creates a DB record but never reads the snapshot, creates a service, starts brokers, or recreates topics. It returns the snapshot DB id as the "service id", which is semantically wrong.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:35-52`

9. **WaitBrokersRegisteredTask leaks Curator connections on retry.**

   The task uses recursion for retrying. Each recursive `doTask()` call creates a new Curator client (line 37-38) inside the try block, but the outer call's `finally` only runs after recursion unwinds. This means up to 20 simultaneous ZooKeeper connections can be held open during retries.

   Refactor to an iterative loop with a single client, or close and reconnect on each iteration.

   References:
   - `dms/src/km/job/task/WaitBrokersRegisteredTask.groovy:34-80`

10. **MetricCtrl creates kafka_exporter without broker addresses.**

    The design says kafka_exporter should be configured with `KAFKA_SERVER=broker1:9092,broker2:9092` via env vars. The code creates the app but never sets `conf.envList` with broker connection info. The exporter will start but have nothing to scrape.

    References:
    - `dms/src/ctrl/kafka/MetricCtrl.groovy:75-90`

### Medium

11. **ServiceCtrl `/list` mutates DB state in a GET request.**

    The list endpoint updates service status to `running` or marks as `unhealthy` as a side effect of listing (lines 66-79). Health reconciliation should be a separate background process (like Guardian), not embedded in a read endpoint. Concurrent list requests could race on status updates.

    References:
    - `dms/src/ctrl/kafka/ServiceCtrl.groovy:64-79`

12. **ServiceCtrl `/delete` doesn't guard against root chroot deletion.**

    `ValidateZookeeperTask.isValidChroot` only runs during creation. The delete path (lines 303-318) does `client.delete().deletingChildrenIfNeeded().forPath('/')` without re-validating `zkChroot`. If data is corrupted and `zkChroot` is empty/null, the condition `one.zkChroot` on line 303 passes for any non-null string, including `/`.

    References:
    - `dms/src/ctrl/kafka/ServiceCtrl.groovy:303-318`

13. **KafkaPlugin checker is a no-op.**

    The `before` checker always returns `true` (line 127). The design specifies port conflict detection, directory pre-creation, ZooKeeper connectivity validation, and broker ID uniqueness validation.

    References:
    - `dms/plugins/plugin/demo2/KafkaPlugin.groovy:120-134`

14. **WaitInstancesRunningTask uses unbounded recursion for retry.**

    Retries via recursive `doTask()` calls risk StackOverflow at high retry counts. Should be refactored to an iterative loop. Same pattern issue as WaitBrokersRegisteredTask but without the connection leak.

    References:
    - `dms/src/km/job/task/WaitInstancesRunningTask.groovy:26-48`

15. **ValidateZookeeperTaskTest is minimal.**

    Only tests `isValidChroot` static method. Does not test the actual `doTask()` ZooKeeper interaction, even with a mock. The core validation logic (chroot creation, existing-cluster detection) has no test coverage.

    References:
    - `dms/test/km/job/task/ValidateZookeeperTaskTest.groovy`

### Low

16. **WaitBrokersRegisteredTask creates a new ObjectMapper per broker.**

    Inside the `each` loop (line 60-67), a new Jackson ObjectMapper is instantiated for every broker ID. Move it outside the loop.

    References:
    - `dms/src/km/job/task/WaitBrokersRegisteredTask.groovy:60`

17. **SnapshotCtrl `/export` reads `name` from body but never uses it.**

    Line reads `def name = body.name as String` but does not pass it to `exportSnapshot()`.

    References:
    - `dms/src/ctrl/kafka/SnapshotCtrl.groovy:20`

18. **KafkaManager.groovy uses `var` instead of `def`.**

    Line 28 uses `var labelValues = List.of(...)` which violates the Groovy style guide. Should use `def`.

    References:
    - `dms/src/km/KafkaManager.groovy:28`

## Summary

This round covers a large surface area (plugin, 4 controllers, 11 task classes, snapshot manager). The scaffolding structure is well-aligned with the design document. However, there are several critical issues that would prevent the system from working end-to-end:

**Must fix before proceeding:**

1. Fix the app group to `bitnami` so the KafkaPlugin activates (finding 1)
2. Fix template parameter names and add missing params (finding 2)
3. Fix the container mount path (finding 3)
4. Fix ValidateZookeeperTask to connect without chroot first, create chroot, then validate (finding 4)
5. Wire task chains into scale-up/scale-down jobs (finding 5)
6. Fix the PartitionBalancer issues from round 9 — still blocking

**Should fix before feature review signoff:**

7. Implement real logic in stub tasks, or clearly mark the endpoints as not-yet-functional (finding 6)
8. Implement snapshot export/import file I/O (findings 7-8)
9. Fix Curator connection leak in WaitBrokersRegisteredTask (finding 9)
10. Add broker addresses to kafka_exporter env config (finding 10)
