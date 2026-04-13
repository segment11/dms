# Kafka Manager Review Round 12

## Scope

- Review target: commit `9d74d6e` (fix(km): address review round 11 findings)
- Stage: fix pass for round 11 findings
- Previous round: `doc/feat/kafka_manager/review_round11.md`

## Round 11 Finding Status

| # | Finding | Status | Notes |
|---|---------|--------|-------|
| 1 | FileVolumeMount missing `imageTplId`/`content` | **Fixed** | Looks up `ImageTplDTO` by name, sets both fields |
| 1b | `configTemplateId` not passed to template-aware variant | **Fixed** | Re-added to paramList, defaults to `'0'` when null |
| 2 | MetricCtrl hardcoded `broker1:9092` | **Fixed** | Accepts `serviceId`, builds real broker addresses from `brokerDetail` |
| 3 | Stub tasks remain no-ops | **Not fixed** | Acknowledged carry-over, no change |
| 4 | `containerNumber = 1` missing for kafka_exporter | **Fixed** | Restored |
| 5 | `removeBrokerIds` assumes contiguous IDs | **Fixed** | Derives from `brokerDetail` when available |
| 6 | Snapshot export/import stubs | **Not fixed** | No change |
| 7 | KafkaPlugin checker no-op | **Not fixed** | No change |
| 8 | PartitionBalancer missing non-contiguous ID tests | **Fixed** | Two new test cases added |
| 9 | Unused `tryCount` in WaitReassignmentCompleteTask | **Fixed** | Removed |
| 10 | NodeVolumeDTO `imageName` = `library/kafka` | **Fixed** | Changed to `bitnami/kafka` |

## New Findings

### High

1. **Scale-down `removeBrokerIds` from `brokerDetail` uses negative indexing that may select wrong brokers.**

   Line 395: `one.brokerDetail.brokers[-brokerCount..-1].collect { it.brokerId }`

   This takes the last `brokerCount` entries from the `brokerDetail.brokers` list, assuming the tail entries are the ones to remove. However, `brokerDetail.brokers` is populated by `WaitBrokersRegisteredTask` by iterating ZooKeeper `/brokers/ids` children — the order depends on ZooKeeper's `getChildren()` return order, which is not guaranteed to match creation order. If ZK returns `[2, 0, 5, 3]`, the "last 2" would be `[5, 3]` which may not be the intended brokers to remove.

   The scale-down endpoint should either: (a) accept explicit broker IDs to remove from the caller, or (b) sort `brokerDetail.brokers` by `brokerId` descending and take from the top, which at least gives a deterministic policy of "remove highest-ID brokers first."

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:394-396`
   - `dms/src/km/job/task/WaitBrokersRegisteredTask.groovy:50-58` (population order)

2. **`configTemplateId` defaults to `'0'` when null — `ServerPropertiesUseTemplateTpl.groovy` will query `KmConfigTemplateDTO(id: 0)` and fail.**

   Line 216: `mountOne.paramList << new KVPair<String>('configTemplateId', configTemplateId ? '' + configTemplateId : '0')`

   When no config template is provided, the code selects `server.properties.tpl` (correct), but still passes `configTemplateId=0` in the paramList. The base template (`ServerPropertiesTpl.groovy`) doesn't read this param, so it's harmless for the base case. However, the param is always present — if future code changes or template logic ever reads it, `id: 0` will cause a lookup that returns null. Consider simply omitting the param when `configTemplateId` is null, since the template selection already handles this via `tplName`.

   Severity: low in practice (base template ignores it), but the intent is unclear.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:216`

3. **Prometheus app scrape config does not include kafka_exporter target.**

   `MetricCtrl` creates both a Prometheus app and a kafka_exporter app, but the Prometheus app is initialized via `PrometheusPlugin.demoApp()` which sets up default scrape targets. There's no code to add the kafka_exporter endpoint (`targetNodeIp:9308`) to Prometheus's scrape configuration. The Prometheus instance will run but won't scrape Kafka metrics.

   The Redis Manager equivalent (`MetricCtrl` in `dms/src/ctrl/redis/`) presumably configures Prometheus scrape targets to point at the redis_exporter endpoint. The Kafka MetricCtrl should do the same for kafka_exporter.

   References:
   - `dms/src/ctrl/kafka/MetricCtrl.groovy:64-81` (Prometheus app creation)
   - Design doc: "Prometheus config template whose scrape targets point to the kafka_exporter endpoint"

4. **`ImageTplDTO` lookup at `/add` time will fail if KafkaPlugin has not been initialized.**

   Line 202-204: The code queries `ImageTplDTO(imageName: 'bitnami/kafka', name: tplName).one()` and asserts non-null. These records are created by `KafkaPlugin.initImageConfig()` during plugin initialization. If the plugin hasn't run yet (e.g., first server start, plugin load order issue, or plugin disabled), the assert fails and the entire `/add` request crashes with an `AssertionError`.

   Other controllers (Redis) also rely on plugin init, so this may be an accepted pattern. But it would be more robust to provide a clear error message: `if (!tplOne) { resp.halt(409, 'KafkaPlugin not initialized — image templates not found') }`.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:202-204`
   - `dms/plugins/plugin/demo2/KafkaPlugin.groovy:90-101` (where templates are created)

### Medium (carry-over, not addressed)

5. **All seven task implementations remain stubs.** (Round 11 finding 3)

   `CreateTopicTask`, `AddBrokersTask`, `RemoveBrokersTask`, `ReassignPartitionsTask`, `WaitReassignmentCompleteTask`, `DecommissionBrokerTask`, `FailoverTask` all return `JobResult.ok(...)` with no logic. The wiring is correct now, but operations silently succeed without doing anything.

6. **KmSnapshotManager export/import are still stubs.** (Round 11 finding 6)

   `exportSnapshot()` creates a DB record but writes no files. `importSnapshot()` creates a DB record but doesn't parse or reconstruct.

7. **KafkaPlugin `before` checker is still a no-op.** (Round 11 finding 7)

   Always returns `true`. Port conflict detection, directory pre-creation, and broker ID validation are all missing.

### Low

8. **MetricCtrl kafka_exporter uses `conf.registryId` default (0) — may pull from wrong registry.**

   ServiceCtrl now uses `KafkaManager.preferRegistryId()` to set the correct Docker registry for the `bitnami/kafka` image. The kafka_exporter app in MetricCtrl doesn't set `registryId`, so it defaults to 0. If `danielqsj/kafka-exporter` is not available from the default registry, the pull will fail. Consider setting `conf.registryId = KafkaManager.preferRegistryId()` or the appropriate registry for the exporter image.

   References:
   - `dms/src/ctrl/kafka/MetricCtrl.groovy:97-104`
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:170` (pattern)

9. **`ValidateZookeeperTaskTest` still only tests `isValidChroot` static method.** (Round 11 finding carry-over from round 10)

   No `doTask()` test coverage even with mocks. The core chroot creation and existing-cluster detection logic remains untested.

## Summary

This fix commit cleanly addresses all the "must fix" items from round 11. The critical `imageTplId`/`content` blocker is resolved, `containerNumber` is restored, real broker addresses are used for the exporter, and `removeBrokerIds` now derives from actual broker topology.

The remaining open items fall into two categories:

**Should fix before declaring the backend API stage complete:**

1. Clarify or fix scale-down broker selection order (finding 1)
2. Add Prometheus scrape target configuration for kafka_exporter (finding 3)
3. Guard `ImageTplDTO` lookup with a user-facing error (finding 4)

**Deferred — acceptable to implement in a follow-up pass:**

4. Implement real task logic in the 7 stub tasks (finding 5)
5. Implement snapshot file I/O (finding 6)
6. Implement KafkaPlugin checker logic (finding 7)

No new critical blockers. The cluster creation path (`/add`) should now work end-to-end for the first time: plugin activates, template renders, config mounts correctly, ZooKeeper chroot is created, and brokers register.
