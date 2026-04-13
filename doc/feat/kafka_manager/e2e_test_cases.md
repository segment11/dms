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

## 9. Job and Task Observability

### 9.1 Job history
- GET `/kafka/job/list?serviceId=X`
- **Verify:** returns all jobs (create, scale-up, scale-down, topic-create) with status and costMs

### 9.2 Task logs for a job
- GET `/kafka/job/task/list?jobId=X`
- **Verify:** returns step-by-step task execution log with individual costMs

### 9.3 Failed job visibility
- Trigger a job that will fail (e.g., unreachable ZK)
- **Verify:** job status = `failed`, result contains error message, task log shows which step failed

---

## Implementation Gap Analysis

Comparing the test cases above against current implementation:

| Test Case | API Exists | Logic Implemented | Gap |
|-----------|-----------|-------------------|-----|
| 1.1–1.7 Standalone lifecycle | Yes | Yes | — |
| 2.1–2.6 Cluster lifecycle | Yes | Yes | — |
| 3.1–3.6 Scaling | Yes | Yes | — |
| 4.1 Failover | **No endpoint** | Task exists | ServiceCtrl has no `/failover` route that creates a job with FailoverTask |
| 4.2 Failover edge case | — | Task handles it | Same endpoint gap |
| 5.1–5.8 Snapshot | Yes | Yes | `/download` is stub |
| 6.1–6.3 Monitoring | Yes | Yes | — |
| 7.1–7.4 Multi-service | Yes | Yes | — |
| 8.1 Container crash | Implicit | Guardian handles | — |
| 8.2 Cluster availability | Implicit | Kafka built-in | — |
| 8.3–8.5 ZK unreachable | Yes | Tasks handle | — |
| 8.6 Delete with ZK down | Yes | Caught exception | — |
| 8.7 Partial scale failure | Yes | Job reports fail | — |
| 8.8–8.9 Chroot conflict | Yes | ValidateZK handles | — |
| 8.10 Recover from snapshot | Yes | Yes | — |
| 8.11–8.13 Timeout cases | Yes | Task retries | — |
| 8.13 Port conflict | Implicit | Plugin checker | — |
| 9.1–9.3 Observability | Yes | Yes | — |

### Gaps Found

| # | Gap | Severity | Detail |
|---|-----|----------|--------|
| G1 | **No `/kafka/service/failover` endpoint** | High | Design doc specifies `POST /kafka/service/failover`. `FailoverTask` exists with real logic but no controller route wires it into a job. Cannot trigger failover via API. |
| G2 | **`POST /kafka/topic/alter` is a stub** | High | Design doc specifies "Increase partitions or update topic config." Implementation returns `{id}` without altering anything. No `TOPIC_ALTER` job chain exists. |
| G3 | **`POST /kafka/topic/delete` is soft-only** | Medium | Updates `KmTopicDTO.status = deleted` in DB but does not delete the topic from ZooKeeper. The topic remains active on brokers. Should also delete `/brokers/topics/{name}` and `/config/topics/{name}` in ZK. |
| G4 | **`POST /kafka/topic/reassign` is a stub** | Medium | Design doc specifies per-topic reassignment. Implementation returns `{id}` without triggering `REASSIGN_PARTITIONS` job. |
| G5 | **`POST /kafka/service/update-config` is a stub** | Medium | Design doc specifies "Update broker runtime config via `kafka-configs.sh --alter`." Implementation only validates service exists. No ZK config update or `kafka-configs.sh` integration. |
| G6 | **`GET /kafka/snapshot/download` is a stub** | Low | Returns "not implemented." Snapshot files exist on disk but cannot be downloaded via API. |
| G7 | **No `TOPIC_DELETE` job with ZK cleanup** | Medium | Related to G3. `KmJobTypes.TOPIC_DELETE` exists but no task class deletes the topic from ZK. |
| G8 | **No service stop/start endpoints** | Low | Design doc lists `stopped` as a valid status. No endpoint to stop a running service (without deleting it) or restart a stopped service. `KafkaManager.stopContainers()` exists but is only called from `/delete`. |
