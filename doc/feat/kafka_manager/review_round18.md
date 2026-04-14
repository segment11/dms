# Kafka Manager Review Round 18

## Scope

- Review target: commit `1a52563` (feat(km): implement E2E gap items G1-G8)
- Stage: implement all 8 gaps identified in E2E gap analysis
- Previous round: `doc/feat/kafka_manager/review_round17.md`

## Gap Item Status

| # | Gap | Status | Notes |
|---|-----|--------|-------|
| G1 | `/kafka/service/failover` endpoint | **Implemented** | Wires FailoverTask into job chain, standalone guard |
| G2 | `/kafka/topic/alter` with AlterTopicTask | **Implemented** | Partition increase + config update via ZK. See findings 3, 6 |
| G3 | `/kafka/topic/delete` with ZK cleanup | **Implemented** | DeleteTopicTask deletes from ZK. See finding 4 |
| G4 | `/kafka/topic/reassign` job chain | **Implemented** | Triggers REASSIGN_PARTITIONS with ReassignPartitionsTask |
| G5 | `/kafka/service/update-config` ZK write | **Implemented** | Writes broker config to ZK. See findings 1, 2 |
| G6 | `/kafka/snapshot/download` file serving | **Implemented** | Serves snapshot.json. See finding 7 |
| G7 | DeleteTopicTask with ZK cleanup | **Implemented** | Same as G3 |
| G8 | Service stop/start endpoints | **Implemented** | See findings 5, 8 |

## Findings

### Critical

1. **`update-config` writes only broker 0's config — not cluster-wide.**

   `ServiceCtrl.groovy:493` hardcodes `def configPath = '/config/brokers/0'`. In Kafka's ZK layout, `/config/brokers/{id}` is per-broker — each broker has its own config node. In a 3-broker cluster, only broker 0 receives the update; brokers 1 and 2 are unaffected.

   The design doc says "Update broker runtime config" (plural), and the KM_SERVICE table stores a single `config_overrides` JSON for the whole service. The intent is clearly cluster-wide config application.

   Fix: iterate all broker IDs from `brokerDetail.brokers`, writing each broker's ZK config path. Or, if only broker 0 should receive configs (e.g., as a default), document this limitation explicitly.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:493`

2. **`update-config` replaces entire ZK config node — silently drops existing config keys.**

   `ServiceCtrl.groovy:494`: `[version: 1, config: configOverrides]` overwrites the full config map at the ZK path. If the broker previously had `{a: "1", b: "2"}` in ZK and the API call sends `{c: "3"}`, keys `a` and `b` are lost from ZK.

   Meanwhile the DB merge at line 506 is additive: `configOverrides.each { k, v -> one.configOverrides.put(k, v) }`. This causes **ZK and DB to diverge** — the DB believes `a`, `b`, and `c` are all set, but ZK only has `c`.

   Fix: read existing config from ZK first, merge new values into it, then write back:
   ```groovy
   def existingData = new String(client.getData().forPath(configPath), 'UTF-8')
   def existingJson = JSON.parseObject(existingData, Map.class)
   def existingConfig = existingJson['config'] as Map ?: [:]
   existingConfig.putAll(configOverrides)
   def configData = [version: 1, config: existingConfig]
   ```

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:494-501`

3. **Same config-replace bug in `AlterTopicTask` — overwrites topic config in ZK.**

   `AlterTopicTask.groovy:85`: `[version: 1, config: configOverrides]` replaces the entire topic config node in ZK. If a topic previously had `retention.ms=604800000` and the alter request sends `{cleanup.policy: "compact"}`, the retention config is wiped from ZK.

   The controller class `TopicCtrl.groovy` similarly passes only the new overrides map — there's no merge of existing values. This is the same pattern as finding 2.

   Fix: read-merge-write, same as finding 2.

   References:
   - `dms/src/km/job/task/AlterTopicTask.groovy:84-92`

### High

4. **`DeleteTopicTask` combines two conflicting delete strategies — manual ZK delete + admin delete marker.**

   `DeleteTopicTask.groovy:35-51`: The task first manually deletes `/brokers/topics/{name}` and `/config/topics/{name}` (lines 36-43), then also creates `/admin/delete_topics/{name}` (lines 45-51).

   In Kafka 2.8.2 with `delete.topic.enable=true` (the default), the Controller watches `/admin/delete_topics/` and will attempt to clean up — but the topic data is already gone. The Controller may log errors trying to process a delete for a non-existent topic path.

   Pick one strategy:
   - (a) **Manual ZK delete only** — remove lines 45-51. This is the simpler, more direct approach and matches the ZK-direct pattern used everywhere else in KM (CreateTopicTask, AlterTopicTask, ReassignPartitionsTask).
   - (b) **Admin marker only** — remove lines 36-43. Let the Controller handle deletion. More idiomatic but slower and depends on `delete.topic.enable=true`.

   References:
   - `dms/src/km/job/task/DeleteTopicTask.groovy:35-51`

5. **`/start` immediately marks service `running` before containers are actually up.**

   `ServiceCtrl.groovy:555-556`: Sets `AppDTO.Status.auto` and `KmServiceDTO.Status.running` synchronously in the HTTP handler. The containers haven't started yet — the `auto` status tells the Guardian scheduler to reconcile, but the service falsely reports `running` in the interim.

   The `/one` endpoint has `canChangeToRunningWhenInstancesRunningOk()` reconciliation logic (line 86), which would eventually fix this on next GET. But the initial state is misleading for any caller that checks status immediately after `/start`.

   Consider: set status to `creating` or a transitional state, and let the `/one` reconciliation promote it to `running`.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:555-556`
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:86-93` (reconciliation in `/one`)

6. **No status guard on `/kafka/topic/alter` — allows altering a `creating` topic.**

   `TopicCtrl.groovy:110-158`: No check on the topic's current status before submitting the alter job. If a topic is still in `creating` status (its `TOPIC_CREATE` job hasn't completed yet), an alter request will submit a competing `TOPIC_ALTER` job. Both jobs could write to the same ZK paths concurrently.

   The service-level endpoints consistently guard status (e.g., `one.status != running → halt 409`). Add: `if (one.status != KmTopicDTO.Status.active) { resp.halt(409, 'topic must be active') }`.

   References:
   - `dms/src/ctrl/kafka/TopicCtrl.groovy:110-158`

7. **No status guard on `/kafka/topic/delete` — allows double-delete or deleting a `creating` topic.**

   Same issue as finding 6. `TopicCtrl.groovy:160-195`: no check that the topic is in `active` status. A topic already in `deleting` status can have another delete job submitted.

   References:
   - `dms/src/ctrl/kafka/TopicCtrl.groovy:160-195`

### Medium

8. **`/stop` does not guard against in-flight jobs.**

   `ServiceCtrl.groovy:531`: No check for running jobs before stopping containers. If a scale-up or reassignment job is in progress, stopping containers mid-reassignment can leave partition assignments incomplete or under-replicated.

   Consider querying `KmJobDTO` for any jobs with `status = running` on this service, and rejecting the stop request if found.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:531`

9. **`FileInputStream` leak in snapshot download.**

   `SnapshotCtrl.groovy:71`: `resp.download(new FileInputStream(snapshotFile), one.name + '.json')` — the framework's `download()` method does not close the InputStream. File descriptors accumulate under load.

   The `output()` method in the same framework class properly closes its InputStream. This is an inconsistency in the framework, but the KM code should work around it.

   Fix: wrap in a closeable pattern:
   ```groovy
   def stream = new FileInputStream(snapshotFile)
   try {
       resp.download(stream, one.name + '.json')
   } finally {
       stream.close()
   }
   ```

   References:
   - `dms/src/ctrl/kafka/SnapshotCtrl.groovy:71`

10. **`update-config` runs synchronous ZK I/O in the HTTP handler — inconsistent with async job pattern.**

    `ServiceCtrl.groovy:487-515`: Every other mutating endpoint (failover, scale-up/down, topic create/alter/delete, reassign) submits a job to `KmJobExecutor` and returns immediately. `update-config` performs blocking ZK client operations directly in the HTTP thread. On ZK latency spikes, this blocks an HTTP thread.

    Consider wrapping in a `KmJob` with a new task type (e.g., `UpdateConfigTask`) for consistency and resilience.

    References:
    - `dms/src/ctrl/kafka/ServiceCtrl.groovy:487-515`

### Low

11. **`/reassign` takes no topic filter — reassigns all topics across all brokers.**

    `TopicCtrl.groovy:197-229`: The endpoint accepts only `serviceId`. `ReassignPartitionsTask` reassigns all topics across all brokers. The design doc's API table says `POST /kafka/topic/reassign` (singular "topic"), implying per-topic control. Consider adding an optional `topicNames` parameter for selective reassignment.

    References:
    - `dms/src/ctrl/kafka/TopicCtrl.groovy:197-229`
    - Design doc: `KmTopicCtrl` — "POST /reassign — Trigger partition reassignment across brokers"

12. **`/start` does not wait for containers or verify ZK connectivity before reporting success.**

    `ServiceCtrl.groovy:555-556`: Sets app to `auto` status and returns. The Guardian will eventually start containers, but there's no verification that ZK is reachable or that the chroot still exists. If ZK was wiped while the service was stopped, brokers will fail to start.

    This is a best-effort pattern consistent with the Redis Manager's Guardian-based recovery, so not blocking — but worth noting for operator awareness.

## Summary

This commit fills all 8 E2E gaps in a single pass. The failover, reassign, snapshot download, and topic delete endpoints are cleanly wired. The main concerns are in the config write path:

**Must fix before merging:**

1. Config-replace-overwrites-existing: read-merge-write in `update-config` and `AlterTopicTask` (findings 2, 3)
2. `update-config` only targets broker 0 — iterate all brokers (finding 1)
3. Pick one delete strategy in `DeleteTopicTask`, don't combine both (finding 4)

**Should fix:**

4. Add topic status guards on `/alter` and `/delete` (findings 6, 7)
5. Don't immediately mark `running` on `/start` (finding 5)
6. Close `FileInputStream` in snapshot download (finding 9)

**Nice to fix:**

7. Guard `/stop` against in-flight jobs (finding 8)
8. Make `update-config` async like other mutating endpoints (finding 10)
