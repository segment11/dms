# Kafka Manager Review Round 20

## Scope

- Review target: commit `3d39ca7` (feat(km): add preferred replica election, consumer group inspection, and CuratorPoolHolder)
- Stage: feature review
- Previous round: `doc/feat/kafka_manager/review_round19.md`

## Round 19 Finding Status

No findings from round 19 were actioned in this commit — this is a new feature commit.

## New Findings

### Critical

1. **Consumer lag calculation is fundamentally wrong.**

   `ConsumerCtrl.groovy:190-191`:
   ```groovy
   def replicas = partitionsMap[partitionId] as List
   long logEndOffset = replicas ? replicas.size() as long : 0L
   ```

   The ZK path `/brokers/topics/{topic}` stores the replica assignment list (e.g., `[0, 1, 2]` for partition 0), not the log end offset. Using `replicas.size()` as the log end offset means lag is computed as `number_of_replicas - consumer_offset`, which is meaningless.

   Log end offset is a Kafka broker runtime value stored in the partition segment files, not in ZK. To get the real log end offset you must query Kafka directly (e.g., `kafka-consumer-groups.sh --describe` or the `__consumer_offsets` topic), not ZK.

   **Fix options** (pick one):
   - Use `kafka-consumer-groups.sh --describe` output which provides `LOG-END-OFFSET` directly (consistent with how `/one` and `/list` work)
   - If ZK-only is acceptable for the MVP use case, at minimum add a comment that this is an approximation based on replica count, not actual lag

   References:
   - `dms/src/ctrl/kafka/ConsumerCtrl.groovy:190-191`

### High

2. **`/admin/preferred_replica_election` ZK path never gets cleaned up on failure.**

   `PreferredReplicaElectionTask.groovy:29-36`: The task creates the election path and returns OK. If the Controller fails to process or auto-delete the path, subsequent calls will always fail with "preferred replica election already in progress" (line 31).

   The path `/admin/preferred_replica_election` is supposed to be auto-deleted by the Kafka Controller after it processes the election. However, there is no mechanism to:
   - Detect that the Controller has finished (success or failure)
   - Clean up a stale path from a failed Controller
   - Provide a timeout or retry window

   Consider adding a comment documenting this limitation. For a production system, consider a TTL-based cleanup (delete the path if it exists for > N minutes).

   References:
   - `dms/src/km/job/task/PreferredReplicaElectionTask.groovy:29-36`

3. **`CuratorPoolHolder.instance.create()` has no connection timeout.**

   `CuratorPoolHolder.groovy:19-26`: The `CuratorFrameworkFactory.newClient()` uses default Curator timeouts. For `ExponentialBackoffRetry(1000, 3)`, the retry policy only handles connection failures, not session/operation timeouts. If a ZK server becomes unreachable, operations will hang until the socket timeout fires.

   This is pre-existing behavior (it was the same with the old per-call `newClient`), so not a regression. But the singleton nature of CuratorPoolHolder means one stuck client can affect all subsequent operations for that connection string.

   Consider: add a `connectionTimeoutMs` parameter to `newClient()`.

   References:
   - `dms/src/km/CuratorPoolHolder.groovy:19-26`

### Medium

4. **`ConsumerCtrl` has no route for stopped service — inconsistent with other endpoints.**

   `ConsumerCtrl.groovy:35-37` and `146-148`: The 3 consumer endpoints all check `service.status != running → halt 409`. But unlike other endpoints (e.g., `/delete` at `ServiceCtrl.groovy:307`), there is no explicit branch for `stopped` services. The existing E2E test case `9.5 Consumer groups — stopped service` expects 409, which matches this behavior, so this is consistent.

   Marking as medium for documentation clarity — the test case should be updated to explicitly test `stopped` service behavior.

   References:
   - `dms/src/ctrl/kafka/ConsumerCtrl.groovy:35-37, 146-148`
   - `doc/feat/kafka_manager/e2e_test_cases.md:306`

### Low

5. **Duplicate import in `AlterTopicTask.groovy`.**

   `AlterTopicTask.groovy:6-8`:
   ```groovy
   import km.CuratorPoolHolder
   import km.job.KmJob
   import km.job.KmJob
   ```

   `KmJob` is imported twice. Minor style issue.

   References:
   - `dms/src/km/job/task/AlterTopicTask.groovy:6-8`

6. **Duplicate import in `CreateTopicTask.groovy`, `DecommissionBrokerTask.groovy`, `DeleteTopicTask.groovy`, `FailoverTask.groovy`, `ReassignPartitionsTask.groovy`.**

   Same `import km.job.KmJob` duplicate appears in multiple files that were refactored to use CuratorPoolHolder. Low severity.

   References:
   - `dms/src/km/job/task/CreateTopicTask.groovy:6-8`
   - `dms/src/km/job/task/DecommissionBrokerTask.groovy:5-7`
   - `dms/src/km/job/task/DeleteTopicTask.groovy:5-7`
   - `dms/src/km/job/task/FailoverTask.groovy:6-8`
   - `dms/src/km/job/task/ReassignPartitionsTask.groovy:6-8`

7. **Missing `serviceId` in `/one` response.**

   `ConsumerCtrl.groovy:128`: The `/one` response includes `groupId` but not `serviceId`. Other endpoints (e.g., `/lag`) also only return `groupId`. For consistency with the request params, consider including `serviceId` in the response.

   References:
   - `dms/src/ctrl/kafka/ConsumerCtrl.groovy:128`

## Summary

The CuratorPoolHolder refactor is clean and achieves its goal of eliminating per-call client creation overhead. The preferred replica election implementation is correct for the happy path.

**One critical bug** — the consumer lag calculation uses replica list size instead of actual log end offset, making the lag values meaningless. This must be fixed before the feature can be considered functional.

**One high finding** — the election ZK path has no cleanup mechanism for stale paths, which can permanently block future elections.

**The refactor is correct in intent** — replacing 16 per-call `newClient/start/close` sites with a singleton pool is the right pattern. The client lifecycle is now managed by CuratorPoolHolder, and all tasks correctly call `CuratorPoolHolder.instance.create()` without explicit `close()`.

**Recommendation:** Do not merge. Fix finding #1 (lag calculation) and address finding #2 (stale path) before re-review.
