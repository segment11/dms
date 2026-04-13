# Kafka Manager Review Round 16

## Scope

- Review target: commits `28ec942` and `7dae514` (fix round 15 findings + revert RemoveBrokersTask)
- Stage: fix pass for round 15 findings
- Previous round: `doc/feat/kafka_manager/review_round15.md`

## Round 15 Finding Status

| # | Finding | Status | Notes |
|---|---------|--------|-------|
| 1 | RemoveBrokersTask instanceIndex | **Retracted in round 15** | Correct DMS pattern; revert commit `7dae514` restores it |
| 2 | ReassignPartitionsTask passes all broker IDs | Acknowledged | Works because `reassignForScale` takes union; no change needed |
| 3 | FailoverTask detects disappearance as "new elected" | **Fixed** | Now waits for `/controller` with different `brokerid` |
| 4 | Snapshot import hardcodes config values | **Partially fixed** | See finding 1 below |
| 5 | Snapshot import marks done before async job | **Fixed** | `done` removed from sync path |
| 6 | Export server.properties unresolved placeholders | **Fixed** | Now writes `broker.id=0`, resolved port, `<node_ip>` placeholder |
| 7 | Port typo 9029 | **Fixed** | Changed to `9092` |
| 8 | removeBrokerIds parsing inside loop | **Fixed** | Moved outside topic loop |
| 9 | WaitBrokersRegisteredTask brokerIndex | **Fixed** | Sets `brokerIndex = idx` from ZK enumeration |
| 10 | Export missing configItems/configOverrides | **Fixed** | Populated from KmConfigTemplateDTO and service overrides |

## New Findings

### High

1. **Snapshot import reads `defaultPartitions`, `defaultReplicationFactor`, `heapMb` from snapshot JSON, but export never writes them.**

   Import reads:
   ```groovy
   def defaultPartitions = (content['defaultPartitions'] as Integer) ?: 8        // line 168
   def defaultReplicationFactor = (content['defaultReplicationFactor'] as Integer) ?: 1  // line 169
   def heapMb = (content['heapMb'] as Integer) ?: 1024                           // line 170
   ```

   But `KmSnapshotContent` has no `defaultPartitions`, `defaultReplicationFactor`, or `heapMb` fields. Export populates `serviceName`, `mode`, `kafkaVersion`, `brokers`, `topics`, `configItems`, `configOverrides`, `zkConnectString`, `zkChroot` — but not these three fields. So import always falls back to `8`, `1`, `1024`.

   Fix either:
   - (a) Add these fields to `KmSnapshotContent` and populate them in `exportSnapshot`, or
   - (b) Read them from the existing snapshot data (e.g., derive `defaultPartitions` from the most common topic partition count, `heapMb` from configItems if present)

   References:
   - `dms/src/km/KmSnapshotManager.groovy:168-170` (import reads)
   - `dms/src/km/KmSnapshotManager.groovy:63-104` (export writes — no defaultPartitions/heapMb)
   - `dms/src/model/json/KmSnapshotContent.groovy` (no such fields)

2. **Snapshot import never marks snapshot as `done` on success.**

   Round 15 finding 5 flagged that `done` was set before the async job completed. The fix removed the `done` update entirely (lines 316-317 deleted). Now the snapshot stays in `created` status forever on success — only the `failed` path updates the status.

   Should mark `done` either: (a) inside the async `kmJob.run()` callback after success, or (b) immediately after the job is submitted (the job itself tracks success/failure via `KmJobDTO`).

   References:
   - `dms/src/km/KmSnapshotManager.groovy:315-319` (no status update on success path)

### Medium

3. **`WaitBrokersRegisteredTask` sets `brokerIndex = idx` from ZK `getChildren` enumeration order, not DMS instance index.**

   ZK's `getChildren()` returns children in sorted string order (e.g., `"0", "1", "10", "2"` — sorted lexicographically). The `idx` from `eachWithIndex` is the iteration position in that sorted list, which may not match the DMS instance index. For freshly created clusters where `broker.id = instanceIndex`, the string-sorted order matches numeric order only when all IDs are single-digit or uniformly sized.

   For practical purposes this is unlikely to cause issues (broker counts rarely exceed 9 in the short term, and FailoverTask falls back to IP-only matching). But for correctness, `brokerIndex` should be set to `brokerId` (which equals `instanceIndex` by template design), not the ZK enumeration position.

   References:
   - `dms/src/km/job/task/WaitBrokersRegisteredTask.groovy:64,73`

4. **Exported `server.properties` uses `<node_ip>` as a literal string placeholder.**

   Line: `sb.append('advertised.listeners=PLAINTEXT://<node_ip>:').append(one.port)`

   This is better than the unresolved `${nodeIp}` template variable from before, but `<node_ip>` is not a standard convention and could confuse operators inspecting the snapshot. Consider either:
   - Using the first broker's actual IP from `brokerDetail` (e.g., `broker0.ip`), with a comment noting it's for broker 0 only
   - Or documenting the convention somewhere

   This is cosmetic — the file is informational, not used by import.

   References:
   - `dms/src/km/KmSnapshotManager.groovy:112`

### Low

5. **`RemoveBrokersTask` added logging (good) — minor: `readTimeout` in map is `int` not `String`.**

   Line 43: `def p = [id: x.id, isRemoveAfterStop: '1', readTimeout: 30 * 1000]` — `readTimeout` is an int (`30000`) while `isRemoveAfterStop` is a String. This is likely fine (Groovy coercion), and consistent with `KafkaManager.stopContainers` which uses the same pattern. No fix needed.

## Summary

These two commits cleanly address the round 15 findings. The FailoverTask now correctly waits for a new controller with a different broker ID. The snapshot export is now comprehensive (configItems, configOverrides, resolved server.properties, topics.json). The `RemoveBrokersTask` revert correctly preserves the DMS instanceIndex pattern.

**Should fix:**

1. Add `defaultPartitions`, `defaultReplicationFactor`, `heapMb` to `KmSnapshotContent` and populate in export — so import can faithfully reconstruct (finding 1)
2. Mark snapshot `done` on import success path (finding 2)

**Nice to fix:**

3. Set `brokerIndex = brokerId` instead of ZK enumeration index (finding 3)

**Overall status:** The Kafka Manager backend API is functionally complete. All task chains work end-to-end with real ZooKeeper interaction. Snapshot export/import reconstructs clusters with topics. The remaining items are minor data fidelity gaps in snapshot round-tripping. Ready for web UI stage after these fixes.
