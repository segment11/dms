# Kafka Manager - Backend Design

Covers the web API stage sub-steps per CLAUDE.md feature implement workflow.
Reference: `doc/kafka_manager_design.md`

## Sub-step Order

1. DTO models
2. JSON models
3. KafkaManager (singleton hub)
4. KmJobExecutor (thread pool)
5. KmSnapshotManager
6. PartitionBalancer
7. Job/task framework (KmJob, KmJobTask, KmTaskLog, KmJobTypes, task classes)
8. ChainHandler router controllers
9. KafkaPlugin + config templates

---

## 1. DTO Models

All under `dms/src/model/` (table-mapped, extend `BaseRecord<Self>`).

### 1.1 KmServiceDTO

Maps to `km_service`. Fields match DDL columns in camelCase:

```
id, name, des, mode, kafkaVersion, configTemplateId, configOverrides,
zkConnectString, zkChroot, appId, port, brokers,
defaultReplicationFactor, defaultPartitions, heapMb, pass,
isSaslOn, isTlsOn, nodeTags, nodeTagsByBrokerIndex, logPolicy,
status, extendParams, brokerDetail, lastUpdatedMessage,
createdDate, updatedDate
```

Inner enums:
- `Mode` — `standalone`, `cluster`
- `Status` — `creating`, `running`, `scaling_up`, `scaling_down`, `stopped`, `deleted`, `unhealthy`

JSON-backed fields (use `JSONFiled` types):
- `configOverrides` → `ExtendParams` (reuse existing from `dms_common` — generic key-value JSON, per web_api_code_conversion.md reuse guidance)
- `brokerDetail` → `BrokerDetail` (new JSON model)
- `logPolicy` → `LogPolicy` (reuse existing)
- `extendParams` → `ExtendParams` (reuse existing from `dms_common`)

Helper methods:
- `updateStatus(Status status, String message)` — same pattern as `RmServiceDTO`
- `zkFullConnectString()` — returns `zkConnectString + zkChroot`

### 1.2 KmConfigTemplateDTO

Maps to `km_config_template`. Fields: `id, name, des, configItems, updatedDate`.

`configItems` → `ConfigItems` (reuse existing `model.json.ConfigItems`).

### 1.3 KmTopicDTO

New file at `dms/src/model/job/KmTopicDTO.groovy`.

Maps to `km_topic`. Fields: `id, serviceId, name, partitions, replicationFactor, configOverrides, status, createdDate, updatedDate`.

Inner enum:
- `Status` — `creating`, `active`, `deleting`, `deleted`

`configOverrides` → `ExtendParams` (reuse existing from `dms_common`).

### 1.4 KmJobDTO

New file at `dms/src/model/job/KmJobDTO.groovy`.

Maps to `km_job`. Same shape as `RmJobDTO`:
`id, busiId, type, status, result, content, failedNum, costMs, createdDate, updatedDate`.

### 1.5 KmTaskLogDTO

New file at `dms/src/model/job/KmTaskLogDTO.groovy`.

Maps to `km_task_log`. Same shape as `RmTaskLogDTO`:
`id, jobId, step, jobResult, costMs, createdDate, updatedDate`.

### 1.6 KmSnapshotDTO

New file at `dms/src/model/job/KmSnapshotDTO.groovy`.

Maps to `km_snapshot`. Fields: `id, name, serviceId, snapshotDir, status, message, costMs, createdDate, updatedDate`.

Inner enum:
- `Status` — `created`, `failed`, `done`

---

## 2. JSON Models

All under `dms/src/model/json/`, implement `JSONFiled`.

### 2.1 BrokerDetail

Cluster topology stored in `km_service.broker_detail`.

```groovy
class BrokerDetail implements JSONFiled {
    List<BrokerNode> brokers

    BrokerNode findByBrokerId(int brokerId)
    BrokerNode findByIpPort(String ip, int port)
    List<BrokerNode> activeBrokers()

    static class BrokerNode implements JSONFiled {
        int brokerId
        int brokerIndex
        boolean isController
        String ip
        int port
        String rackId

        String uuid() { "${ip}:${port}" }
    }
}
```

### 2.2 KmSnapshotContent

Snapshot payload, stored as `snapshot.json` file (not a DB column).

```groovy
class KmSnapshotContent implements JSONFiled {
    String serviceName
    String mode
    String kafkaVersion
    Date snapshotDate
    List<BrokerEntry> brokers
    String zkConnectString
    String zkChroot
    List<TopicEntry> topics
    List<KVPair<String>> configItems
    ExtendParams configOverrides

    static class BrokerEntry implements JSONFiled {
        int brokerId
        String host
        int port
        String rackId
        String logDirs
    }

    static class TopicEntry implements JSONFiled {
        String name
        int partitions
        int replicationFactor
        ExtendParams configOverrides
    }
}
```

---

## 3. KafkaManager

File: `dms/src/km/KafkaManager.groovy`

Singleton utility class (not a Spring-style service). Modeled after `RedisManager`:

- Constants: `CLUSTER_ID = 1`, `MAX_BROKERS = 32`, `MAX_PARTITIONS_PER_TOPIC = 256`
- Static methods for:
  - Password encode/decode (reuse same XOR-based encoding as RedisManager)
  - Data directory paths: `/data/kafka/data`, `/data/kafka/logs`
  - Container stop/remove by appId (delegates to `InMemoryAllContainerManager`)
  - Prometheus gauge registration: `km_cluster_count`, `km_broker_count`, `km_service_status`
- `init()` method called at server startup to register metrics

---

## 4. KmJobExecutor

File: `dms/src/km/KmJobExecutor.groovy`

`@Singleton`, modeled after `RmJobExecutor`:

- Fixed thread pool, size = `Runtime.runtime.availableProcessors()`
- Named threads: `km-job-{n}`
- `submit(KmJob job)` — executes job task chain in thread pool
- `runCreatingAppJob(...)` — creates DMS app for Kafka broker containers (delegates to existing app creation infrastructure)

---

## 5. KmSnapshotManager

File: `dms/src/km/KmSnapshotManager.groovy`

Handles metadata-only snapshot export/import:

- `export(KmServiceDTO service)` → creates snapshot dir, writes `snapshot.json`, `server.properties`, `topics.json`
- `importFrom(String snapshotPath, String zkConnectString, String zkChroot, String nodeTags)` → reconstructs cluster from snapshot

Snapshot dir pattern: `snapshots/{service_name}_{yyyyMMdd_HHmmss}/`

---

## 6. PartitionBalancer

File: `dms/src/km/PartitionBalancer.groovy`

Utility for partition replica assignment:

- `assignReplicas(int brokerCount, int partitionCount, int replicationFactor)` → `List<List<Integer>>` (partition → broker ID list)
- `reassignForScale(List<List<Integer>> current, List<Integer> newBrokerIds)` → updated assignment
- `reassignForDecommission(List<List<Integer>> current, List<Integer> removeBrokerIds)` → assignment without removed brokers

Uses round-robin with broker-rack awareness when `rackId` is present.

---

## 7. Job/Task Framework

### 7.1 KmJobTypes

File: `dms/src/km/job/KmJobTypes.groovy`

Static `JobType` constants:

| Constant | Value |
|----------|-------|
| `STANDALONE_CREATE` | `standalone_create` |
| `CLUSTER_CREATE` | `cluster_create` |
| `BROKER_SCALE_UP` | `broker_scale_up` |
| `BROKER_SCALE_DOWN` | `broker_scale_down` |
| `TOPIC_CREATE` | `topic_create` |
| `TOPIC_ALTER` | `topic_alter` |
| `TOPIC_DELETE` | `topic_delete` |
| `REASSIGN_PARTITIONS` | `reassign_partitions` |
| `FAILOVER` | `failover` |
| `SNAPSHOT` | `snapshot` |
| `IMPORT` | `import` |

### 7.2 KmJob

File: `dms/src/km/job/KmJob.groovy`

Extends framework `Job`, maps to `KmJobDTO` for persistence. Same pattern as `RmJob`:
- Constructor takes `KmServiceDTO` + job type + content
- Persists to DB on creation, updates status on completion
- Holds reference to service DTO for task access

### 7.3 KmJobTask

File: `dms/src/km/job/KmJobTask.groovy`

Abstract base for all KM tasks. Same pattern as `RmJobTask`:
- Extends framework `JobTask`
- Constructor takes `KmJob` + step name
- `doTask()` returns `JobResult`
- Persists task log to `KmTaskLogDTO` on completion

### 7.4 KmTaskLog

File: `dms/src/km/job/KmTaskLog.groovy`

Static helper to persist task log, same pattern as `rm.job.RmTaskLog`.

### 7.5 Task Classes

All under `dms/src/km/job/task/`:

| Task | Job Types Used In | Responsibility |
|------|-------------------|----------------|
| `ValidateZookeeperTask` | STANDALONE_CREATE, CLUSTER_CREATE | Curator client to verify ZK connectivity, create chroot, reject root/empty chroot |
| `RunCreatingAppJobTask` | STANDALONE_CREATE, CLUSTER_CREATE | Create Kafka broker app via DMS app infrastructure |
| `WaitInstancesRunningTask` | STANDALONE_CREATE, CLUSTER_CREATE, BROKER_SCALE_UP | Poll until all broker containers are running |
| `WaitBrokersRegisteredTask` | STANDALONE_CREATE, CLUSTER_CREATE, BROKER_SCALE_UP | Verify brokers registered in ZK `/brokers/ids` |
| `AddBrokersTask` | BROKER_SCALE_UP | Increase app instance count, update broker_detail |
| `ReassignPartitionsTask` | BROKER_SCALE_UP, BROKER_SCALE_DOWN, REASSIGN_PARTITIONS | Generate and execute reassignment JSON |
| `WaitReassignmentCompleteTask` | BROKER_SCALE_UP, BROKER_SCALE_DOWN, REASSIGN_PARTITIONS | Poll until reassignment done |
| `DecommissionBrokerTask` | BROKER_SCALE_DOWN | Stop and remove target broker containers |
| `RemoveBrokersTask` | BROKER_SCALE_DOWN | Update broker detail after removal |
| `CreateTopicTask` | TOPIC_CREATE | Execute `kafka-topics.sh --create` |
| `AlterTopicTask` | TOPIC_ALTER | Execute `kafka-topics.sh --alter` for partition increase and `kafka-configs.sh --alter` for topic config |
| `DeleteTopicTask` | TOPIC_DELETE | Execute `kafka-topics.sh --delete` |
| `FailoverTask` | FAILOVER | Kill controller broker, wait for re-election |

### 7.6 Task Chain Definitions

| Job Type | Task Chain |
|----------|-----------|
| STANDALONE_CREATE | ValidateZookeeperTask → RunCreatingAppJobTask → WaitInstancesRunningTask → WaitBrokersRegisteredTask |
| CLUSTER_CREATE | ValidateZookeeperTask → RunCreatingAppJobTask → WaitInstancesRunningTask → WaitBrokersRegisteredTask |
| BROKER_SCALE_UP | AddBrokersTask → WaitInstancesRunningTask → WaitBrokersRegisteredTask → ReassignPartitionsTask → WaitReassignmentCompleteTask |
| BROKER_SCALE_DOWN | ReassignPartitionsTask → WaitReassignmentCompleteTask → DecommissionBrokerTask → RemoveBrokersTask |
| TOPIC_CREATE | CreateTopicTask |
| TOPIC_ALTER | AlterTopicTask |
| TOPIC_DELETE | DeleteTopicTask |
| REASSIGN_PARTITIONS | ReassignPartitionsTask → WaitReassignmentCompleteTask |
| FAILOVER | FailoverTask |
| SNAPSHOT | (handled directly in KmSnapshotManager) |
| IMPORT | (handled directly in KmSnapshotManager) |

---

## 8. ChainHandler Router Controllers

All under `dms/src/ctrl/kafka/`, package `ctrl.kafka`.

Each controller is a Groovy script using `ChainHandler.instance` DSL.

Input style follows conversion doc 3.1:
- Simple GET/DELETE inputs: `req.param('x')`
- Simple POST/PUT inputs with few fields: `req.bodyAs(HashMap)`
- Larger or reused payloads: typed DTO/JSONFiled classes (e.g. `KmServiceDTO`, `KmTopicDTO`, `KmImportRequest`)

### 8.1 KmServiceCtrl — `/kafka/service`

| Method | Path | Input | Output | Description |
|--------|------|-------|--------|-------------|
| GET | `/list` | `pageNum`, `pageSize`, `mode`, `status`, `keyword` | pager of KmServiceDTO | Paginated service list with filters |
| GET | `/one` | `id` | KmServiceDTO with brokerDetail | Service detail |
| POST | `/add` | KmServiceDTO body | `[id: id]` | Create service, validate ZK, enqueue create job |
| POST | `/scale-up` | `{id, count}` | `[flag: true]` | Add brokers, enqueue scale-up job |
| POST | `/scale-down` | `{id, brokerIds}` | `[flag: true]` | Remove brokers, enqueue scale-down job |
| POST | `/failover` | `{id}` | `[flag: true]` | Enqueue failover job |
| POST | `/update-config` | `{id, configOverrides}` | `[flag: true]` | Update broker runtime config |
| POST | `/delete` | `id` | `[flag: true]` | Stop all brokers, delete ZK chroot, mark deleted |

### 8.2 KmTopicCtrl — `/kafka/topic`

| Method | Path | Input | Output | Description |
|--------|------|-------|--------|-------------|
| GET | `/list` | `serviceId` | list of KmTopicDTO | List topics for service |
| GET | `/one` | `serviceId`, `name` | topic detail with partitions/ISR | Topic detail from broker metadata |
| POST | `/add` | KmTopicDTO body | `[id: id]` | Create topic, enqueue TOPIC_CREATE job |
| POST | `/alter` | `{serviceId, name, partitions, configOverrides}` | `[flag: true]` | Alter topic, enqueue TOPIC_ALTER job |
| POST | `/delete` | `serviceId`, `name` | `[flag: true]` | Delete topic, enqueue TOPIC_DELETE job |
| POST | `/reassign` | `{serviceId, topicName}` | `[flag: true]` | Enqueue REASSIGN_PARTITIONS job |

### 8.3 KmJobCtrl — `/kafka/job`

| Method | Path | Input | Output | Description |
|--------|------|-------|--------|-------------|
| GET | `/list` | `serviceId`, `pageNum`, `pageSize` | pager of KmJobDTO | Job history by service |
| GET | `/task/list` | `jobId` | list of KmTaskLogDTO | Task logs for a job |

### 8.4 KmSnapshotCtrl — `/kafka/snapshot`

| Method | Path | Input | Output | Description |
|--------|------|-------|--------|-------------|
| GET | `/list` | `serviceId` | list of KmSnapshotDTO | List snapshots for service |
| POST | `/export` | `{serviceId}` | `[id: snapshotId]` | Export cluster metadata |
| POST | `/import` | KmImportRequest body | `[id: serviceId]` | Reconstruct cluster from snapshot |
| GET | `/download` | `id` | file stream | Download snapshot zip |

### 8.5 KmConfigTemplateCtrl — `/kafka/config-template`

| Method | Path | Input | Output | Description |
|--------|------|-------|--------|-------------|
| GET | `/list` | `pageNum`, `pageSize` | pager of KmConfigTemplateDTO | Paginated list |
| POST | `/add` | KmConfigTemplateDTO body | `[id: id]` | Create template |
| POST | `/update` | KmConfigTemplateDTO body | `[flag: true]` | Update template |
| DELETE | `/delete` | `id` | `[flag: true]` | Delete template |

### 8.6 KmMetricCtrl — `/kafka/metric`

| Method | Path | Input | Output | Description |
|--------|------|-------|--------|-------------|
| GET | `/init-exporters` | `targetNodeIp` | `[prometheusAppId, exporterAppId]` | Create Prometheus + kafka_exporter apps |

### 8.7 KmImportRequest

File: `dms/src/model/json/KmImportRequest.groovy`

Request model for `POST /kafka/snapshot/import`. Not a DB-mapped DTO — implements `JSONFiled` for JSON deserialization from request body.

```groovy
class KmImportRequest implements JSONFiled {
    String snapshotPath        // path to snapshot dir or zip
    String zkConnectString     // required, e.g. zk1:2181,zk2:2181
    String zkChroot            // optional, default: /kafka/{serviceName}
    String nodeTags            // optional, override target node placement
}
```

---

## 9. KafkaPlugin + Config Templates

### 9.1 KafkaPlugin

File: `dms/plugins/plugin/demo2/KafkaPlugin.groovy`

Extends `BasePlugin`:
- `name()` → `'kafka'`
- `image()` → `'bitnami/kafka:2.8.2'`
- `group()` → `'kafka'`
- `init()` → register image templates, checkers (port conflict, dir creation, ZK validation)

### 9.2 Config Templates

Under `dms/plugins_resources/kafka/`:

- `ServerPropertiesTpl.groovy` — `server.properties.tpl` with placeholders: `broker.id`, `listeners`, `advertised.listeners`, `zookeeper.connect`, `log.dirs`, `num.partitions`, `default.replication.factor`, `offsets.topic.replication.factor`, `transaction.state.log.replication.factor`
- `ServerPropertiesUseTemplateTpl.groovy` — `server.properties.template.tpl` from KmConfigTemplate items

---

## File Creation Summary

### New Files

| # | Path | Type |
|---|------|------|
| 1 | `dms/src/model/KmServiceDTO.groovy` | DTO |
| 2 | `dms/src/model/KmConfigTemplateDTO.groovy` | DTO |
| 3 | `dms/src/model/job/KmTopicDTO.groovy` | DTO |
| 4 | `dms/src/model/job/KmJobDTO.groovy` | DTO |
| 5 | `dms/src/model/job/KmTaskLogDTO.groovy` | DTO |
| 6 | `dms/src/model/job/KmSnapshotDTO.groovy` | DTO |
| 7 | `dms/src/model/json/BrokerDetail.groovy` | JSON model |
| 8 | `dms/src/model/json/KmSnapshotContent.groovy` | JSON model |
| 9 | `dms/src/model/json/KmImportRequest.groovy` | JSON model (request) |
| 10 | `dms/src/km/KafkaManager.groovy` | Manager |
| 11 | `dms/src/km/KmJobExecutor.groovy` | Executor |
| 12 | `dms/src/km/KmSnapshotManager.groovy` | Snapshot |
| 13 | `dms/src/km/PartitionBalancer.groovy` | Utility |
| 14 | `dms/src/km/job/KmJobTypes.groovy` | Job types |
| 15 | `dms/src/km/job/KmJob.groovy` | Job |
| 16 | `dms/src/km/job/KmJobTask.groovy` | Abstract task |
| 17 | `dms/src/km/job/KmTaskLog.groovy` | Task log helper |
| 18 | `dms/src/km/job/task/ValidateZookeeperTask.groovy` | Task |
| 19 | `dms/src/km/job/task/RunCreatingAppJobTask.groovy` | Task |
| 20 | `dms/src/km/job/task/WaitInstancesRunningTask.groovy` | Task |
| 21 | `dms/src/km/job/task/WaitBrokersRegisteredTask.groovy` | Task |
| 22 | `dms/src/km/job/task/AddBrokersTask.groovy` | Task |
| 23 | `dms/src/km/job/task/ReassignPartitionsTask.groovy` | Task |
| 24 | `dms/src/km/job/task/WaitReassignmentCompleteTask.groovy` | Task |
| 25 | `dms/src/km/job/task/DecommissionBrokerTask.groovy` | Task |
| 26 | `dms/src/km/job/task/RemoveBrokersTask.groovy` | Task |
| 27 | `dms/src/km/job/task/CreateTopicTask.groovy` | Task |
| 28 | `dms/src/km/job/task/AlterTopicTask.groovy` | Task |
| 29 | `dms/src/km/job/task/DeleteTopicTask.groovy` | Task |
| 30 | `dms/src/km/job/task/FailoverTask.groovy` | Task |
| 31 | `dms/src/ctrl/kafka/KmServiceCtrl.groovy` | Controller |
| 32 | `dms/src/ctrl/kafka/KmTopicCtrl.groovy` | Controller |
| 33 | `dms/src/ctrl/kafka/KmJobCtrl.groovy` | Controller |
| 34 | `dms/src/ctrl/kafka/KmSnapshotCtrl.groovy` | Controller |
| 35 | `dms/src/ctrl/kafka/KmConfigTemplateCtrl.groovy` | Controller |
| 36 | `dms/src/ctrl/kafka/KmMetricCtrl.groovy` | Controller |
| 37 | `dms/plugins/plugin/demo2/KafkaPlugin.groovy` | Plugin |
| 38 | `dms/plugins_resources/kafka/ServerPropertiesTpl.groovy` | Template |
| 39 | `dms/plugins_resources/kafka/ServerPropertiesUseTemplateTpl.groovy` | Template |

### Reused Existing Types

- `model.json.ConfigItems` — for `KmConfigTemplateDTO.configItems`
- `model.json.LogPolicy` — for `KmServiceDTO.logPolicy`
- `model.json.ExtendParams` (from `dms_common`) — for `KmServiceDTO.extendParams` and `KmServiceDTO.configOverrides` and `KmTopicDTO.configOverrides`
- `org.segment.d.json.JSONFiled` — marker interface for all JSON models
- `com.segment.common.job.chain.*` — framework for Job, JobTask, JobResult

### Pattern Reuse Note

Per web_api_code_conversion.md section 3.6.5, the KM job/task framework reuses the RM pattern (`RmJob`, `RmJobTask`, `RmTaskLog`, `RmJobExecutor`) with feature-specific naming (`KmJob`, `KmJobTask`, `KmTaskLog`, `KmJobExecutor`). No behavioral divergence from the RM pattern.
