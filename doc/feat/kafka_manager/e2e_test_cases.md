# Kafka Manager E2E Test Cases

Comprehensive operations for managing multiple Kafka standalone servers and clusters, including broken-recovery scenarios.

## Prerequisites

- DMS server running with KafkaPlugin initialized
- At least 3 worker nodes registered and healthy
- External ZooKeeper ensemble running (e.g., `zk1:2181,zk2:2181,zk3:2181`)
- KmConfigTemplate created (optional, for template-based tests)

---

## 1. Standalone Service Lifecycle

### 1.1 Create standalone service
- POST `/kafka/service/add` with `mode=standalone`, `brokers=1`, `zkConnectString`, `kafkaVersion=2.8.2`
- **Verify:** service status transitions `creating → running`, single broker registered in ZK, `brokerDetail` populated

### 1.2 Create topic on standalone
- POST `/kafka/topic/add` with `partitions=4`, `replicationFactor=1`
- **Verify:** topic exists in ZK `/brokers/topics/{name}`, `KmTopicDTO` status = `active`

### 1.3 List topics
- GET `/kafka/topic/list?serviceId=X`
- **Verify:** returns the created topic with correct partition count

### 1.4 Get topic detail
- GET `/kafka/topic/one?serviceId=X&name=Y`
- **Verify:** returns topic with partitions, replicationFactor

### 1.5 Delete topic (soft)
- POST `/kafka/topic/delete` with `serviceId`, `name`
- **Verify:** `KmTopicDTO` status = `deleted`

### 1.6 Delete standalone service
- POST `/kafka/service/delete`
- **Verify:** containers stopped, ZK chroot deleted, service status = `deleted`

### 1.7 Create standalone with config template
- Create `KmConfigTemplateDTO` with custom `log.retention.hours=72`
- POST `/kafka/service/add` with `configTemplateId`
- **Verify:** `server.properties.template.tpl` used, custom config rendered into container mount

---

## 2. Cluster Service Lifecycle

### 2.1 Create 3-broker cluster
- POST `/kafka/service/add` with `mode=cluster`, `brokers=3`
- **Verify:** 3 broker containers running, all registered in ZK, controller elected, `brokerDetail` has 3 nodes

### 2.2 Create replicated topic on cluster
- POST `/kafka/topic/add` with `partitions=8`, `replicationFactor=3`
- **Verify:** each partition has 3 replicas spread across 3 brokers (via PartitionBalancer)

### 2.3 Create multiple topics
- Create 3 topics with varying partitions (4, 8, 16) and replication factors (1, 2, 3)
- **Verify:** all topics active, partition assignments use actual broker IDs

### 2.4 Get service detail
- GET `/kafka/service/one?id=X`
- **Verify:** returns broker topology, container running status, connection string info

### 2.5 List services with filters
- GET `/kafka/service/list?mode=cluster&status=running`
- **Verify:** returns only matching services

### 2.6 Delete cluster service
- POST `/kafka/service/delete`
- **Verify:** all 3 containers stopped, ZK chroot recursively deleted

---

## 3. Broker Scaling

### 3.1 Scale up: 3 → 5 brokers
- POST `/kafka/service/scale-up` with `brokerCount=2`
- **Verify:** job chain completes (AddBrokers → WaitRunning → WaitRegistered → Reassign → WaitReassign), 5 brokers running, partitions redistributed to include new brokers

### 3.2 Scale up validation: exceed max brokers
- POST `/kafka/service/scale-up` with `brokerCount=30` (total > 32)
- **Verify:** 409 rejected

### 3.3 Scale down: 5 → 3 brokers
- POST `/kafka/service/scale-down` with `brokerCount=2`
- **Verify:** partitions moved off removed brokers first, then containers stopped and ZK cleaned, 3 brokers remain, `brokerDetail` updated

### 3.4 Scale down validation: below minimum
- POST `/kafka/service/scale-down` on cluster to `brokers < 3`
- **Verify:** 409 rejected

### 3.5 Scale down standalone to 0
- POST `/kafka/service/scale-down` on standalone with `brokerCount=1`
- **Verify:** 409 rejected (minimum 1 for standalone)

### 3.6 Sequential scale: up then down then up
- Scale 3 → 5 → 3 → 4
- **Verify:** partition reassignment works correctly at each step, no orphaned partitions, all topics remain accessible

---

## 4. Controller Failover

### 4.1 Failover on running cluster
- POST `/kafka/service/failover` (requires wiring — currently via direct FailoverTask)
- **Verify:** old controller stopped, new controller elected with different `brokerid`, old controller restarted, `brokerDetail.isController` updated

### 4.2 Failover when controller already stopped
- Stop controller container manually, then trigger failover
- **Verify:** returns "controller broker already stopped"

### 4.3 Preferred replica election
- Create 3-broker cluster with topic (partitions=6, replicationFactor=3)
- Stop broker 0 (leader for some partitions), wait for leader election, restart broker 0
- At this point some partitions have broker 1 or 2 as leader instead of preferred broker 0
- POST `/kafka/service/preferred-replica-election`
- **Verify:** ZK path `/admin/preferred_replica_election` written then deleted by Controller, partition leaders reset to preferred replicas, job status = `ok`

### 4.4 Preferred replica election — election already in progress
- Trigger preferred replica election twice rapidly
- **Verify:** second call returns job with `failed` status, "preferred replica election already in progress"

---

## 5. Snapshot Export / Import

### 5.1 Export running cluster snapshot
- POST `/kafka/snapshot/export` with `serviceId`
- **Verify:** directory created with `snapshot.json`, `topics.json`, `server.properties`; snapshot.json contains brokers, topics, configItems, configOverrides, defaultPartitions, defaultReplicationFactor, heapMb

### 5.2 Export standalone snapshot
- Export a standalone service
- **Verify:** `snapshot.json` has `mode=standalone`, 1 broker entry

### 5.3 List snapshots
- GET `/kafka/snapshot/list?serviceId=X`
- **Verify:** returns snapshot records with status = `done`

### 5.4 Import snapshot to new ZK
- Delete original service, then POST `/kafka/snapshot/import` with new `zkConnectString`
- **Verify:** new service created with same config, brokers start, topics recreated with same partition/replication settings

### 5.5 Import snapshot with custom chroot
- POST `/kafka/snapshot/import` with explicit `zkChroot=/kafka/imported_cluster`
- **Verify:** service uses provided chroot, not the snapshot's original chroot

### 5.6 Import snapshot — name conflict
- Try importing while original service still exists
- **Verify:** error "service name already exists"

### 5.7 Import snapshot — invalid path
- POST `/kafka/snapshot/import` with non-existent `snapshotPath`
- **Verify:** error "snapshot.json not found"

### 5.8 Round-trip fidelity
- Export cluster A, import as cluster B, export cluster B
- **Verify:** snapshot B's `defaultPartitions`, `defaultReplicationFactor`, `heapMb`, topics match snapshot A

---

## 6. Monitoring Setup

### 6.1 Init exporters
- GET `/kafka/metric/init-exporters?targetNodeIp=X&serviceId=Y`
- **Verify:** `km_prometheus` and `km_kafka_exporter` apps created, exporter has correct `KAFKA_SERVER` env with real broker addresses

### 6.2 Init exporters — duplicate
- Call init-exporters again
- **Verify:** 409 "already exists"

### 6.3 Init exporters — invalid node
- Call with non-existent nodeIp
- **Verify:** 404 "node not exists"

---

## 7. Multi-Service Management

### 7.1 Create multiple services sharing one ZK ensemble
- Create standalone `svc_a` and cluster `svc_b` pointing to same `zkConnectString` but different auto-generated chroots
- **Verify:** `/kafka/svc_a` and `/kafka/svc_b` are isolated, no metadata interference

### 7.2 Service list with mixed statuses
- Create services in various states (running, stopped, deleted)
- GET `/kafka/service/list` with different `status` filters
- **Verify:** correct filtering

### 7.3 Simple list for dropdowns
- GET `/kafka/service/simple-list`
- **Verify:** returns only running services with id + name

### 7.4 Delete one service without affecting others
- Delete `svc_a`, verify `svc_b` brokers and topics unaffected
- **Verify:** `svc_a` chroot deleted, `svc_b` chroot intact

---

## 8. Broken Recovery Scenarios

### 8.1 Broker container crash recovery
- Kill a broker container externally (docker stop)
- **Verify:** DMS detects container not running via `InMemoryAllContainerManager`, service `/one` shows broker as not running; if app status is `auto`, Guardian reschedules the container

### 8.2 Broker container crash — cluster stays available
- Kill 1 of 3 brokers in a cluster with `replicationFactor=3` topics
- **Verify:** remaining 2 brokers serve reads/writes, ZK still has 2 registered brokers, topics with `replicationFactor ≤ 2` remain fully available

### 8.3 ZooKeeper unreachable during service creation
- Provide a non-reachable `zkConnectString`
- **Verify:** `ValidateZookeeperTask` fails, job status = `failed`, service stays in `creating` status

### 8.4 ZooKeeper unreachable during scale-up
- Start scale-up, then make ZK unreachable
- **Verify:** `ReassignPartitionsTask` or `WaitBrokersRegisteredTask` fails gracefully, job reports error

### 8.5 Snapshot import with unreachable ZK
- POST `/kafka/snapshot/import` with unreachable `zkConnectString`
- **Verify:** `ValidateZookeeperTask` fails, snapshot status = `failed`

### 8.6 Service delete with unreachable ZK
- Delete a service when ZK is down
- **Verify:** containers are still stopped, ZK chroot cleanup fails silently (caught exception), service marked `deleted`

### 8.7 Partial scale-down failure
- Start scale-down; if `ReassignPartitionsTask` succeeds but `RemoveBrokersTask` fails (e.g., agent unreachable)
- **Verify:** job reports failure, partitions already moved but containers still running — operator can retry or manually clean up

### 8.8 Duplicate chroot detection
- Create service A with chroot `/kafka/test`
- Try creating service B with the same name (which would generate same chroot)
- **Verify:** 409 "name already exists" from ServiceCtrl before reaching ZK

### 8.9 Existing cluster data in chroot
- Manually create broker nodes under a chroot path in ZK
- Create a new service that generates that chroot
- **Verify:** `ValidateZookeeperTask` fails with "cluster already exists, brokers found"

### 8.10 Recover deleted service from snapshot
- Create cluster, add topics, export snapshot, delete service
- Import snapshot with same or new ZK
- **Verify:** new service running with same topology, all topics recreated (no message data — expected)

### 8.11 Node failure during cluster operation
- Worker node goes offline during `WaitInstancesRunningTask`
- **Verify:** task retries (max 10, 10s interval), eventually fails with "running containers number: X, expect: Y"

### 8.12 Failover timeout
- Stop controller in a 1-broker standalone (no other broker to elect)
- **Verify:** FailoverTask polls 15 times, returns "new controller election timeout"

### 8.13 Port conflict on service creation
- Create service A on port 9092, then create service B on same port
- **Verify:** KafkaPlugin `before` checker returns false, container creation blocked

---

## 9. Consumer Group Inspection

### 9.1 List consumer groups
- Produce messages to topic, start a Kafka consumer group (e.g., `test-group` with 2 members)
- GET `/kafka/consumer/list?serviceId=X`
- **Verify:** returns `test-group` in the list

### 9.2 Consumer group detail
- GET `/kafka/consumer/one?serviceId=X&groupId=test-group`
- **Verify:** returns members (client ID, host), assigned topic-partitions, current offsets

### 9.3 Consumer lag
- Produce 1000 messages, consume only 100
- GET `/kafka/consumer/lag?serviceId=X&groupId=test-group`
- **Verify:** lag ≈ 900 for the consumed topic, log-end-offset and current offset reported per partition

### 9.4 Consumer lag — no active consumers
- Stop all consumers, GET `/kafka/consumer/lag`
- **Verify:** group listed with no active members, lag reflects last committed offset vs current log-end-offset

### 9.5 Consumer groups — stopped service
- GET `/kafka/consumer/list?serviceId=X` on a stopped service
- **Verify:** 409 "service must be running"

---

## 10. Job and Task Observability

### 10.1 Job history
- GET `/kafka/job/list?serviceId=X`
- **Verify:** returns all jobs (create, scale-up, scale-down, topic-create) with status and costMs

### 10.2 Task logs for a job
- GET `/kafka/job/task/list?jobId=X`
- **Verify:** returns step-by-step task execution log with individual costMs

### 10.3 Failed job visibility
- Trigger a job that will fail (e.g., unreachable ZK)
- **Verify:** job status = `failed`, result contains error message, task log shows which step failed

---

## 11. Additional Endpoint Coverage

### 11.1 Service stop/start round-trip
- POST `/kafka/service/stop` on a running service
- **Verify:** containers stopped, status = `stopped`
- POST `/kafka/service/start`
- **Verify:** status transitions to `creating`, Guardian restarts containers, `/one` eventually shows `running`

### 11.2 Stop a stuck service
- Start a service that fails to reach `running` (stuck in `creating`)
- POST `/kafka/service/stop`
- **Verify:** 409 "service must be running" — operator cannot stop a stuck service (known limitation)

### 11.3 Update broker config
- POST `/kafka/service/update-config` with `{configOverrides: {num.io.threads: "8"}}`
- **Verify:** ZK `/config/brokers/{id}` updated for all brokers, DB `config_overrides` merged, config persists after broker restart

### 11.4 Update config — merge behavior
- Set `{a: "1"}` then `{b: "2"}`
- **Verify:** ZK has both `{a: "1", b: "2"}`, DB has both

### 11.5 Topic alter — partition increase
- Create topic with partitions=4, POST `/kafka/topic/alter` with `partitions=8`
- **Verify:** ZK `/brokers/topics/{name}` has 8 partition entries, KmTopicDTO updated

### 11.6 Topic alter — config update
- POST `/kafka/topic/alter` with `{configOverrides: {retention.ms: "86400000"}}`
- **Verify:** ZK `/config/topics/{name}` merged (existing configs preserved)

### 11.7 Topic alter — status guard
- Attempt to alter a `creating` topic
- **Verify:** 409 "topic must be active"

### 11.8 Topic delete — status guard
- Delete a topic, then attempt to delete again
- **Verify:** second call returns 409 "topic must be active" (topic is now `deleted`)

---

## Implementation Gap Analysis

### Previously identified gaps (G1-G8) — all resolved

All gaps G1-G8 from the original analysis have been implemented and reviewed (rounds 18-19).

### New features from CMAK comparison

| Test Case | API Exists | Logic Implemented | Gap |
|-----------|-----------|-------------------|-----|
| 4.3 Preferred replica election | **No** | **No task** | `PREFERRED_REPLICA_ELECTION` job type and `PreferredReplicaElectionTask` not yet implemented |
| 4.4 Preferred replica election conflict | **No** | **No** | Same gap |
| 9.1–9.5 Consumer group inspection | **No** | **No** | `KmConsumerCtrl` and consumer group read endpoints not yet implemented |

### Gaps Found

| # | Gap | Severity | Detail |
|---|-----|----------|--------|
| G9 | **No preferred replica election** | Medium | CMAK exposes `KMPreferredReplicaElectionFeature` as a first-class feature. KM has no endpoint or task. Implementation is simple: write empty JSON to ZK `/admin/preferred_replica_election`. Useful after broker restarts or scale-down to restore balanced leader distribution. |
| G10 | **No consumer group inspection** | Medium | CMAK provides consumer list + lag views. KM has no visibility into consumer groups. Requires `KmConsumerCtrl` with read-only endpoints using `kafka-consumer-groups.sh`. kafka_exporter already exposes lag metrics to Prometheus, but the API provides on-demand human-readable inspection. |

### Additional missing E2E test cases for existing endpoints

These test cases cover endpoints that exist but were not in the original E2E doc:

| Test Case | Endpoint | Description |
|-----------|----------|-------------|
| 10.1 | `POST /kafka/service/stop` + `POST /kafka/service/start` | Stop a running service, verify status = `stopped`, start it, verify status transitions to `creating` then `running` |
| 10.2 | `POST /kafka/service/update-config` | Update broker config, verify persists in ZK and DB, survives broker restart |
| 10.3 | `POST /kafka/topic/alter` — partition increase | Increase topic partitions, verify new partitions assigned to brokers via PartitionBalancer |
| 10.4 | `POST /kafka/topic/alter` — config update | Update topic config (e.g., `retention.ms`), verify merged with existing config in ZK |
| 10.5 | `POST /kafka/topic/alter` — status guard | Attempt to alter a `creating` topic, verify 409 |
| 10.6 | `POST /kafka/topic/delete` — status guard | Attempt to delete an already `deleting` topic, verify 409 |
