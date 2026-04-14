# Kafka Manager Review Round 19

## Scope

- Review target: commit `1cd881a` (fix(km): address review round 18 findings)
- Stage: fix pass for round 18 findings
- Previous round: `doc/feat/kafka_manager/review_round18.md`

## Round 18 Finding Status

| # | Finding | Severity | Status | Notes |
|---|---------|----------|--------|-------|
| 1 | `update-config` only targets broker 0 | Critical | **Fixed** | Iterates all broker IDs from `brokerDetail.brokers` |
| 2 | `update-config` replaces entire ZK config | Critical | **Fixed** | Read-merge-write: reads existing config, merges new values, writes back |
| 3 | `AlterTopicTask` replaces entire topic config | Critical | **Fixed** | Same read-merge-write pattern |
| 4 | `DeleteTopicTask` combines conflicting strategies | High | **Fixed** | Removed admin marker strategy, manual ZK delete only |
| 5 | `/start` immediately marks `running` | High | **Fixed** | Changed to `creating`; `/one` reconciliation promotes to `running` |
| 6 | No status guard on `/alter` | High | **Fixed** | Added `one.status != active → halt 409` |
| 7 | No status guard on `/delete` | High | **Fixed** | Same guard added |
| 8 | `/stop` no guard against in-flight jobs | Medium | Not addressed | Acknowledged — deferred |
| 9 | `FileInputStream` leak in download | Medium | **Fixed** | Wrapped in try/finally with `stream.close()` |
| 10 | `update-config` synchronous in HTTP handler | Medium | Not addressed | Acknowledged — deferred |
| 11 | `/reassign` no topic filter | Low | Not addressed | Acknowledged — deferred |
| 12 | `/start` doesn't verify ZK connectivity | Low | Not addressed | Acknowledged — consistent with Guardian pattern |

## New Findings

### Medium

1. **`update-config` partial failure leaves brokers with inconsistent config.**

   `ServiceCtrl.groovy:498-515`: The fix iterates all brokers and writes config to each. If the write to broker 2 fails (ZK exception), brokers 0 and 1 may already have the updated config, but the `catch` block halts with 500 and the DB update at line 521 is skipped. Result: ZK for brokers 0-1 has the new config, broker 2 doesn't, and the DB has no record of the change.

   This is inherent to synchronous multi-broker ZK writes and was pre-existing (the original single-broker version had the same risk profile). Making it transactional would require a compensating rollback. For now, the operator can retry the call — the read-merge-write pattern is idempotent for the same input.

   No fix required, but consider adding a comment noting the partial-failure behavior.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:498-515`

### Low

2. **`/stop` only allows stopping from `running` — cannot stop a service stuck in `creating`.**

   `ServiceCtrl.groovy:541-543`: `one.status != running → halt 409`. If `/start` sets status to `creating` (round 18 fix, finding 5) and the start fails or hangs, the service is stuck in `creating` with no way to stop it via the API. The operator would need to either wait for reconciliation or delete the service.

   Consider also allowing stop from `creating` status. The `KafkaManager.stopContainers()` call works regardless of service status.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:541-543`
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:570` (start sets `creating`)

## Summary

All 7 actionable findings from round 18 are fixed. The fixes are clean and minimal:

- **Config write path** (findings 1-3): Correctly iterates all brokers and uses read-merge-write to preserve existing config keys. The ZK and DB are now consistent on successful writes.
- **Delete strategy** (finding 4): Manual ZK delete only — consistent with the ZK-direct pattern used throughout KM.
- **Start status** (finding 5): `creating` is correct; the `/one` reconciliation handles promotion to `running`.
- **Topic guards** (findings 6-7): Prevent concurrent modification of topics in non-active states.
- **Resource leak** (finding 9): Properly closed.

The 3 deferred items (findings 8, 10, 11) are acknowledged non-blocking improvements.

**No blockers. No critical or high findings.**

**Recommendation:** Ready to proceed to web UI stage. The backend API stage is complete with all E2E gaps filled and reviewed.
