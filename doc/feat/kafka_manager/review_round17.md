# Kafka Manager Review Round 17

## Scope

- Review target: commit `e9badb2` (fix(km): address review round 16 findings)
- Stage: fix pass for round 16 findings
- Previous round: `doc/feat/kafka_manager/review_round16.md`

## Round 16 Finding Status

| # | Finding | Status | Notes |
|---|---------|--------|-------|
| 1 | Snapshot round-trip gap (defaultPartitions etc.) | **Fixed** | Fields added to `KmSnapshotContent`, populated in export |
| 2 | Import snapshot never marked done | **Fixed** | `done` set after job submission |
| 3 | brokerIndex = ZK enumeration index | **Fixed** | Changed to `brokerIndex = brokerId` |
| 4 | `<node_ip>` placeholder in server.properties | **Fixed** | Uses first broker's actual IP |
| 5 | readTimeout int vs String | Acknowledged | No fix needed |

## New Findings

### Low

1. **Import snapshot `done` is set before async job completes (revisited).**

   Line 324 marks the snapshot `done` immediately after `KmJobExecutor.instance.execute { kmJob.run() }`. The job runs asynchronously, so the cluster may not be fully reconstructed yet.

   However, this is an acceptable pattern: snapshot `done` means "import request processed and job submitted," while the `KmJobDTO` status tracks whether the actual cluster creation succeeded. This matches how export works — `done` means "files written" regardless of cluster state. No fix needed, but worth documenting this semantic in a code comment if clarity matters.

2. **`KmSnapshotContent` new fields use primitive `int` — default to 0 when not present in old snapshots.**

   The new fields `defaultPartitions`, `defaultReplicationFactor`, `heapMb` are declared as `int` (primitive, defaults to 0). When deserializing a snapshot created before this change, these fields will be 0. Import already handles this with `?: 8`, `?: 1`, `?: 1024` fallbacks, so no runtime issue. But if any code ever reads these fields directly from `KmSnapshotContent` without the Elvis fallback, it will get 0.

   Consider using `Integer` (nullable) or `int` with explicit defaults:
   ```groovy
   int defaultPartitions = 8
   int defaultReplicationFactor = 1
   int heapMb = 1024
   ```

   References:
   - `dms/src/model/json/KmSnapshotContent.groovy:22-24`

## Summary

All round 16 findings are resolved. This is a clean, minimal commit that addresses exactly what was needed.

**No blockers. No critical or high findings.**

The Kafka Manager backend API stage is complete:
- Cluster creation (standalone + cluster) with ZK validation and broker registration
- Topic management (create via ZK with PartitionBalancer assignment)
- Broker scaling (up/down with real partition reassignment)
- Controller failover with restart and brokerDetail update
- Snapshot export (snapshot.json, topics.json, server.properties with resolved values)
- Snapshot import (full cluster reconstruction with topic recreation)
- KafkaPlugin checker (port conflict detection, directory pre-creation)
- Prometheus + kafka_exporter monitoring setup
- Job history and task log tracking

**Recommendation:** Proceed to web UI stage. The only optional cleanup is setting defaults on the new `KmSnapshotContent` fields (finding 2).
