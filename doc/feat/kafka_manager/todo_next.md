# Kafka Manager - Next Steps

## Status

- Round 22 done: round 20 post-fix findings all fixed (election race retry, dead code, null guards)
- Round 21 E2E host-process-mode bugs fixed (template eval, GString escaping, GetPid, health checker)
- Backend API stage complete and reviewed (rounds 17-19); preferred replica election + consumer inspection in (round 20)

## Todo

1. **E2E verification with live environment** (ZK + Kafka + DMS server currently down)
   - Restart local stack: see `doc/feat/kafka_manager/start-local.sh`, `start-zk-node.sh`
   - Verify health checker for existing Kafka services (8, 9): `creating` → `running` transition
   - Topic create / produce / consume, consumer group inspection (`/kafka/consumer/*`)
   - Preferred replica election (`/kafka/service/preferred-replica-election`) on a cluster-mode service

2. **Test cluster mode** (3 brokers, not standalone)

3. **Web UI stage** (per round 19 recommendation)

4. Deferred improvement items:
   - `/kafka/service/stop` guard against in-flight jobs (round 18 F8)
   - Make `update-config` async via `UpdateConfigTask` job (round 18 F10)
   - `/kafka/topic/reassign` optional `topicNames` filter (round 18 F11)
   - Allow `/stop` from `creating` status (round 19 F2)

## Done This Session (round 22)

- `PreferredReplicaElectionTask`: `guaranteed().deletingChildrenIfNeeded()` + `NodeExistsException` retry loop (3 attempts) + extracted `isStaleElection` helper with Spock spec
- `ConsumerCtrl`: removed dead `DefaultJsonTransformer`; null zk field guard via `zkConnectionString()` helper in all 3 endpoints
- `ServiceCtrl` `/start`: explicit null status guard
- `KafkaPlugin` health checker: null zk field guard (returns unhealthy instead of NPE)
