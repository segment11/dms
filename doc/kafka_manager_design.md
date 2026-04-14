# Kafka Manager Design Document

Multi-cluster Kafka 2.8.2 lifecycle management for DMS, modeled after the existing Redis Manager (`dms/src/rm/`).

## Overview

Kafka Manager (KM) manages multiple Kafka clusters within DMS. It handles broker lifecycle (creation, scaling, failover), topic management, partition reassignment, and monitoring. KM does **not** manage ZooKeeper — it connects to an existing ZooKeeper ensemble that is already running (created via the existing DMS ZookeeperPlugin or managed externally).

Kafka 2.8.2 uses ZooKeeper for metadata management (KRaft mode is experimental in this version and not supported here).

## Cluster Modes

| Mode | Description | Min Brokers |
|------|-------------|-------------|
| `standalone` | Single broker, connects to existing ZooKeeper | 1 |
| `cluster` | Multi-broker, connects to existing ZooKeeper ensemble | 3 |

Unlike Redis Manager's three modes (standalone/sentinel/cluster), Kafka has two modes. Both require an existing ZooKeeper ensemble — KM only manages the Kafka brokers. Multiple Kafka clusters can share one ZooKeeper by using different `chroot` paths. Every service **must** use a dedicated non-root chroot (auto-generated as `/kafka/{name}`, UNIQUE constraint enforced) — this ensures safe cleanup on delete and prevents cross-cluster metadata interference.

## Architecture

```
┌─────────────────────────────────────────────────┐
│                  DMS Server                      │
│                                                  │
│  ┌──────────────┐  ┌────────────┐  ┌──────────┐ │
│  │ KmServiceCtrl│  │ KmTopicCtrl│  │KmJobCtrl │ │
│  └──────┬───────┘  └─────┬──────┘  └────┬─────┘ │
│  ┌──────┴───────┐  ┌─────┴──────┐              │
│  │KmConsumerCtrl│  │KmSnapshot  │              │
│  └──────┬───────┘  │    Ctrl    │              │
│         │          └─────┬──────┘              │
│  ┌──────▼────────────────▼───────────────▼─────┐ │
│  │              KafkaManager                    │ │
│  │  ┌──────────────┐  ┌────────────────────┐   │ │
│  │  │ BackupManager│  │ KmJobExecutor      │   │ │
│  │  └──────────────┘  └────────────────────┘   │ │
│  │  ┌──────────────┐  ┌────────────────────┐   │ │
│  │  │PartitionBal. │  │ KmJob + Tasks      │   │ │
│  │  └──────────────┘  └────────────────────┘   │ │
│  └──────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
         │
    ┌────▼────┐
    │  Kafka  │
    │ Brokers │──────────► Existing ZooKeeper
    └─────────┘
```

### Core Components (under `dms/src/km/`)

| Class | Responsibility |
|-------|---------------|
| `KafkaManager` | Singleton hub. Manages data dirs, password encoding, metrics (Prometheus gauges), container stop/remove, plugin init. Constants: `CLUSTER_ID = 1`, max brokers (32), max partitions per topic (256). |
| `KmJobExecutor` | Thread pool (fixed = CPU count), named threads `km-job-*`. Executes long-running ops (cluster create, scale, reassignment) asynchronously. |
| `KmSnapshotManager` | Exports/imports cluster metadata snapshots (broker topology, topics, configs) to dir/zip. No message data — Kafka replication handles durability. |
| `PartitionBalancer` | Utility for partition replica assignment across brokers. Methods: `assignReplicas(brokerCount, partitionCount, replicationFactor)`, `reassignForScale(currentAssignment, newBrokerIds)`, `reassignForDecommission(currentAssignment, removeBrokerIds)`. |

### Read-only Inspection Components

These components query live cluster state without modifying it. They provide CMAK-style visibility into cluster internals.

| Class | Responsibility |
|-------|---------------|
| `PreferredReplicaElectionTask` | Triggers preferred replica election by writing to ZK `/admin/preferred_replica_election`. Single-step job. The Kafka Controller processes the election asynchronously. |

### Directory Layout

```
dms/src/km/
├── KafkaManager.groovy
├── KmSnapshotManager.groovy
├── KmJobExecutor.groovy
├── PartitionBalancer.groovy
└── job/
    ├── KmJobTypes.groovy
    ├── KmJob.groovy
    ├── KmJobTask.groovy
    ├── KmTaskLog.groovy
    └── task/
        ├── RunCreatingAppJobTask.groovy
        ├── WaitInstancesRunningTask.groovy
        ├── ValidateZookeeperTask.groovy
        ├── WaitBrokersRegisteredTask.groovy
        ├── CreateTopicTask.groovy
        ├── ReassignPartitionsTask.groovy
        ├── WaitReassignmentCompleteTask.groovy
        ├── DecommissionBrokerTask.groovy
        ├── FailoverTask.groovy
        ├── AddBrokersTask.groovy
        ├── RemoveBrokersTask.groovy
        └── PreferredReplicaElectionTask.groovy
```

## Data Model

### Database Tables

**KM_SERVICE** — Main Kafka service/cluster definition

| Column | Type | Description |
|--------|------|-------------|
| `id` | int, PK | |
| `name` | varchar(50), UNIQUE | Cluster display name |
| `des` | varchar(200) | Description |
| `mode` | varchar(20) | `standalone` / `cluster` |
| `kafka_version` | varchar(20) | e.g. `2.8.2` |
| `config_template_id` | int, FK | → km_config_template |
| `config_overrides` | varchar(2000) | JSON — broker dynamic config changes applied via `/update-config` (`kafka-configs.sh --alter`). Persisted here so stopped-cluster snapshots remain faithful. |
| `zk_connect_string` | varchar(500) | ZooKeeper host list without chroot, e.g. `zk1:2181,zk2:2181,zk3:2181` |
| `zk_chroot` | varchar(200), NOT NULL | Service-owned chroot path. Auto-generated as `/kafka/{name}` on create. Must be non-root and unique across all services — enforced by UNIQUE index. Brokers connect with `{zk_connect_string}{zk_chroot}`. |
| `app_id` | int | DMS app ID for broker containers (both standalone and cluster modes) |
| `port` | int | Broker listener port (default 9092) |
| `brokers` | int | Number of broker nodes |
| `default_replication_factor` | int | Default replication factor for new topics |
| `default_partitions` | int | Default partition count for new topics |
| `heap_mb` | int | Broker JVM heap size |
| `pass` | varchar(200) | SASL password (encoded), nullable |
| `is_sasl_on` | bit | SASL authentication enabled |
| `is_tls_on` | bit | TLS encryption enabled |
| `node_tags` | varchar(100) | Target node constraint tags |
| `node_tags_by_broker_index` | varchar(500) | Per-broker node placement |
| `log_policy` | varchar(200) | JSON (LogPolicy) |
| `status` | varchar(20) | See status enum below |
| `extend_params` | varchar(2000) | JSON (ExtendParams) |
| `broker_detail` | varchar(4000) | JSON (BrokerDetail) — broker topology |
| `last_updated_message` | varchar(200) | |
| `created_date` | timestamp | |
| `updated_date` | timestamp | |

Service status enum: `creating`, `running`, `scaling_up`, `scaling_down`, `stopped`, `deleted`, `unhealthy`

**KM_CONFIG_TEMPLATE** — Reusable broker configuration templates

| Column | Type | Description |
|--------|------|-------------|
| `id` | int, PK | |
| `name` | varchar(50), UNIQUE | |
| `des` | varchar(200) | |
| `config_items` | text | JSON (ConfigItems) — server.properties overrides |
| `updated_date` | timestamp | |

**KM_TOPIC** — Topic definitions per service

| Column | Type | Description |
|--------|------|-------------|
| `id` | int, PK | |
| `service_id` | int, FK | → km_service |
| `name` | varchar(200) | Topic name |
| `partitions` | int | |
| `replication_factor` | int | |
| `config_overrides` | varchar(2000) | JSON — topic-level config (retention.ms, cleanup.policy, etc.) |
| `status` | varchar(20) | creating/active/deleting/deleted |
| `created_date` | timestamp | |
| `updated_date` | timestamp | |

**KM_JOB** — Async job tracking (same pattern as rm_job)

| Column | Type | Description |
|--------|------|-------------|
| `id` | int, PK | |
| `busi_id` | int | Service ID |
| `type` | varchar(20) | See job types below |
| `status` | varchar(20) | created/running/ok/failed |
| `result` | varchar(500) | |
| `content` | text | Job parameters JSON |
| `failed_num` | int | Retry count |
| `cost_ms` | int | |
| `created_date` | timestamp | |
| `updated_date` | timestamp | |

**KM_TASK_LOG** — Per-step execution log (same pattern as rm_task_log)

| Column | Type | Description |
|--------|------|-------------|
| `id` | int, PK | |
| `job_id` | int, FK | → km_job |
| `step` | varchar(100) | Task class name |
| `job_result` | text | Step result JSON |
| `cost_ms` | int | |
| `created_date` | timestamp | |
| `updated_date` | timestamp | |

**KM_SNAPSHOT** — Cluster metadata snapshots

| Column | Type | Description |
|--------|------|-------------|
| `id` | int, PK | |
| `name` | varchar(100) | Snapshot name (auto: `{service_name}_{yyyyMMdd_HHmmss}`) |
| `service_id` | int, FK | → km_service |
| `snapshot_dir` | varchar(500) | Path to snapshot dir or zip file |
| `status` | varchar(20) | created/failed/done |
| `message` | varchar(200) | |
| `cost_ms` | int | |
| `created_date` | timestamp | |

### JSON Model Classes (under `dms/src/model/json/`)

**BrokerDetail** — Cluster topology

```groovy
class BrokerDetail {
    List<BrokerNode> brokers

    BrokerNode findByBrokerId(int brokerId)
    BrokerNode findByIpPort(String ip, int port)
    List<BrokerNode> activeBrokers()

    static class BrokerNode {
        int brokerId
        int brokerIndex        // 0-based index in DMS app instances
        boolean isController   // current controller broker
        String ip
        int port
        String rackId          // optional rack awareness

        String uuid() { "${ip}:${port}" }
    }
}
```

**KmSnapshotContent** — What gets saved in a snapshot

```groovy
class KmSnapshotContent {
    // cluster identity
    String serviceName
    String mode                        // standalone / cluster
    String kafkaVersion
    Date snapshotDate

    // broker topology
    List<BrokerEntry> brokers          // host:port, broker.id, rack, data dirs

    // ZooKeeper connection (not managed by KM)
    String zkConnectString             // e.g. zk1:2181,zk2:2181
    String zkChroot                    // e.g. /kafka/my_cluster

    // topics
    List<TopicEntry> topics            // name, partitions, replicationFactor, config overrides

    // broker config (materialized — does not depend on template ID)
    List<KVPair<String>> configItems   // server.properties key-value pairs (from config template)
    Map<String, String> configOverrides // runtime dynamic config changes from /update-config

    static class BrokerEntry {
        int brokerId
        String host
        int port
        String rackId
        String logDirs                 // e.g. /data/kafka/data
    }

    static class TopicEntry {
        String name
        int partitions
        int replicationFactor
        Map<String, String> configOverrides  // retention.ms, cleanup.policy, etc.
    }
}
```

### DTO Classes (under `dms/src/model/`)

| DTO | Table | Key Fields |
|-----|-------|------------|
| `KmServiceDTO` | km_service | mode, kafkaVersion, brokers, brokerDetail (JSON), zkConnectString, zkChroot, status |
| `KmConfigTemplateDTO` | km_config_template | configItems (JSON) |
| `KmTopicDTO` | km_topic | serviceId, partitions, replicationFactor, configOverrides (JSON) |
| `KmJobDTO` | km_job | busiId, type, status, result, content |
| `KmTaskLogDTO` | km_task_log | jobId, step, jobResult, costMs |
| `KmSnapshotDTO` | km_snapshot | serviceId, snapshotDir, status |

## Job Types and Task Chains

### KmJobTypes

| Type | Description |
|------|-------------|
| `STANDALONE_CREATE` | Create single-broker Kafka, connect to existing ZooKeeper |
| `CLUSTER_CREATE` | Create multi-broker cluster, connect to existing ZooKeeper |
| `BROKER_SCALE_UP` | Add brokers to existing cluster |
| `BROKER_SCALE_DOWN` | Remove brokers (decommission + reassign partitions) |
| `TOPIC_CREATE` | Create a new topic |
| `TOPIC_ALTER` | Modify topic partitions or config |
| `TOPIC_DELETE` | Delete a topic |
| `REASSIGN_PARTITIONS` | Rebalance partition replicas across brokers |
| `PREFERRED_REPLICA_ELECTION` | Trigger preferred replica election — reset partition leaders to their first (preferred) replica |
| `FAILOVER` | Force controller re-election |
| `SNAPSHOT` | Export cluster metadata to dir/zip |
| `IMPORT` | Recover/recreate cluster from a snapshot |

### Container Config Injection Model

DMS injects configuration **before** a container starts, not after. The flow in `CreateProcessor` is:

1. **`before` checkers** — plugin validates ports, creates host dirs
2. **Template rendering** — `fileVolumeList` templates (e.g. `server.properties.tpl`) are rendered via `/dms/api/container/create/tpl` with `instanceIndex`, `nodeIp`, `nodeIpList`, etc. The rendered file is written to the host and bind-mounted into the container
3. **Container create** — Docker container is created with config files already mounted
4. **`beforeStart` checkers** — additional pre-start validation
5. **Container start** — process starts with correct config from mount
6. **`init` checkers** — shell commands executed inside the running container (`ContainerInit.groovy`)
7. **`after` checkers** — post-start hooks

This means `broker.id`, `listeners`, `advertised.listeners`, and `zookeeper.connect` are all resolved at template render time (step 2) using `instanceIndex`, `nodeIp`, etc. There is no post-start config step — the broker must start with correct config already in place.

### Task Chains by Job Type

**STANDALONE_CREATE**
1. `ValidateZookeeperTask` — Open a ZooKeeper client session (using Apache Curator, already in `dms/build.gradle`) to verify connectivity and create the chroot path. Rejects root `/` or empty chroot. Fails early if ZooKeeper is unreachable or the chroot already contains another cluster's metadata.
2. `RunCreatingAppJobTask` — Create Kafka broker app (1 instance). `server.properties.tpl` is rendered with `broker.id=${instanceIndex}`, `listeners=PLAINTEXT://${nodeIp}:${port}`, `zookeeper.connect=${zkConnectString}${zkChroot}`. The `before` checker validates port availability and creates log dirs. Container starts with config mounted.
3. `WaitInstancesRunningTask` — Wait broker container running
4. `WaitBrokersRegisteredTask` — Verify broker registered in ZooKeeper (`/brokers/ids`)

**CLUSTER_CREATE**
1. `ValidateZookeeperTask` — Open a ZooKeeper client session (Curator) to verify connectivity and create the chroot path. Rejects root `/` or empty chroot. Does not rely on four-letter commands (`ruok`), which may be disabled on hardened/external ZooKeeper clusters.
2. `RunCreatingAppJobTask` — Create Kafka broker app (N instances). Each instance's `server.properties.tpl` is rendered with `broker.id=${instanceIndex}`, `listeners=PLAINTEXT://${nodeIp}:${port}`, `advertised.listeners`, and `zookeeper.connect=${zkConnectString}${zkChroot}`. The `before` checker validates port availability and creates data dirs. Container starts with config mounted — no post-start config step needed.
3. `WaitInstancesRunningTask` — Wait all broker containers running
4. `WaitBrokersRegisteredTask` — Verify all brokers registered, controller elected

**BROKER_SCALE_UP**
1. `AddBrokersTask` — Increase app instance count. New instances get `server.properties.tpl` rendered with their `instanceIndex` as `broker.id`, correct `listeners` for their assigned `nodeIp`, and the existing `zkConnectString`. The `before` checker validates ports and creates dirs. Containers start with config mounted.
2. `WaitInstancesRunningTask` — Wait new broker containers running
3. `WaitBrokersRegisteredTask` — Verify new brokers registered in ZooKeeper
4. `ReassignPartitionsTask` — Generate reassignment plan, move partition replicas to include new brokers
5. `WaitReassignmentCompleteTask` — Poll `kafka-reassign-partitions.sh --verify` until done

**BROKER_SCALE_DOWN**
1. `ReassignPartitionsTask` — Move all partition replicas off target brokers
2. `WaitReassignmentCompleteTask` — Wait until no partitions remain on target brokers
3. `DecommissionBrokerTask` — Stop and remove target broker containers
4. `RemoveBrokersTask` — Update broker detail

**TOPIC_CREATE**
1. `CreateTopicTask` — Execute `kafka-topics.sh --create` with partitions, replication-factor, and config overrides

**REASSIGN_PARTITIONS**
1. `ReassignPartitionsTask` — Generate and execute reassignment JSON
2. `WaitReassignmentCompleteTask` — Poll until all partitions are in-sync

**PREFERRED_REPLICA_ELECTION**
1. `PreferredReplicaElectionTask` — Write empty JSON `{}` to ZK path `/admin/preferred_replica_election`. If the path already exists (a previous election is in progress), fail with a conflict error. The Kafka Controller watches this ZK path and upon seeing the notification, will re-elect the preferred (first) replica as leader for all partitions where it is not currently the leader. After processing, the Controller deletes the ZK path. No task chain — single-step job.

**FAILOVER**
1. `FailoverTask` — Kill the current controller broker container, wait for ZooKeeper to elect a new controller, update `brokerDetail`

## Controllers and API Endpoints

### KmServiceCtrl (`/kafka/service`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/list` | Paginated service list, filter by mode/status/keyword |
| GET | `/one` | Service detail with broker topology, connection string |
| POST | `/add` | Create new Kafka service (validates node capacity, port conflicts) |
| POST | `/scale-up` | Add brokers to cluster |
| POST | `/scale-down` | Remove brokers from cluster (triggers partition reassignment first) |
| POST | `/failover` | Force controller re-election |
| POST | `/preferred-replica-election` | Trigger preferred replica election — reset partition leaders to preferred replicas |
| POST | `/update-config` | Update broker runtime config via `kafka-configs.sh --alter` |
| POST | `/delete` | Stop all brokers, recursively delete the service-owned `zk_chroot` from ZooKeeper, mark deleted |

### KmTopicCtrl (`/kafka/topic`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/list` | List topics for a service (from broker metadata) |
| GET | `/one` | Topic detail: partitions, replicas, ISR, leader distribution, config |
| POST | `/add` | Create topic |
| POST | `/alter` | Increase partitions or update topic config |
| POST | `/delete` | Delete topic |
| POST | `/reassign` | Trigger partition reassignment across brokers |

### KmConsumerCtrl (`/kafka/consumer`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/list` | List consumer groups for a service (from ZK `/consumers` and `__consumer_offsets` topic via kafka-consumer-groups.sh) |
| GET | `/one` | Consumer group detail: members, assigned partitions, offset per partition |
| GET | `/lag` | Consumer lag: per-topic end-of-log offset minus consumer offset. Uses kafka-consumer-groups.sh `--describe` to fetch lag data |

Consumer group inspection is read-only — KM does not manage consumer groups (no create, alter, delete). This provides visibility into which consumers are connected, how far behind they are, and which topics they consume.

**Implementation note:** Consumer group data is read directly from ZooKeeper via Curator — no container exec or CLI tools needed. This is the same ZK-direct pattern used by all KM tasks. No async job is needed for read-only queries.

### KmJobCtrl (`/kafka/job`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/list` | Job history by service |
| GET | `/task/list` | Task logs for a job |

### KmSnapshotCtrl (`/kafka/snapshot`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/list` | List snapshots for a service |
| POST | `/export` | Export cluster metadata to snapshot dir/zip |
| POST | `/import` | Reconstruct cluster from snapshot (requires `zkConnectString` to existing ZooKeeper) |
| GET | `/download` | Download snapshot zip file |

### KmMetricCtrl (`/kafka/metric`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/init-exporters` | One-click setup: creates Prometheus app + kafka_exporter app as separate DMS applications |

## Plugin

### KafkaPlugin (`dms/plugins/plugin/demo2/KafkaPlugin.groovy`)

**Image**: `bitnami/kafka:2.8.2` or `confluentinc/cp-kafka:7.0.x` (wrapping Kafka 2.8.2)

**Config templates** (in `dms/plugins_resources/`):
- `server.properties.tpl` — Main broker config with placeholders for broker.id, listeners, zookeeper.connect, log.dirs
- `server.properties.template.tpl` — Config from KmConfigTemplate

**Checkers**:
- Port conflict detection across all Kafka apps (broker port)
- Directory pre-creation on target nodes (`/data/kafka/logs`, `/data/kafka/data`)
- ZooKeeper connectivity validation before broker start (Curator client session, not four-letter commands)
- Broker ID uniqueness validation

**Key server.properties parameters** managed per instance:
```properties
broker.id=${instanceIndex}
listeners=PLAINTEXT://${nodeIp}:${port}
advertised.listeners=PLAINTEXT://${nodeIp}:${port}
zookeeper.connect=${zkConnectString}${zkChroot}
log.dirs=/data/kafka/data
num.partitions=${defaultPartitions}
default.replication.factor=${defaultReplicationFactor}
offsets.topic.replication.factor=${min(3, brokerCount)}
transaction.state.log.replication.factor=${min(3, brokerCount)}
```

## Snapshot / Import

KM provides a lightweight **metadata-only snapshot** — it captures cluster structure (broker topology, topic definitions, broker configs) but **not** message data or log segments. This is strictly a cluster reconstruction tool: it can rebuild an empty cluster with the same topology so that producers/consumers can reconnect, but any messages stored on lost broker disks are gone.

### Snapshot (export)

Collects cluster metadata and saves to a directory or zip file:

```
snapshots/{service_name}_{yyyyMMdd_HHmmss}/
├── snapshot.json          # KmSnapshotContent — full cluster definition (self-contained, no template ID dependency)
├── server.properties      # materialized broker config (config template items + runtime overrides merged)
└── topics.json            # all topics: name, partitions, replicationFactor, config overrides
```

**How it works:**
1. Query `KmServiceDTO` for cluster topology, `zkConnectString`, config template, and `config_overrides`
2. If the cluster is running: connect to a live broker and run `kafka-topics.sh --describe` to dump all topics with configs, and `kafka-configs.sh --describe` for current broker dynamic configs
3. If the cluster is stopped: use the last known state from DB — topics from `KM_TOPIC` table, broker config from `config_template_id` + `config_overrides`
4. Serialize everything into `KmSnapshotContent` JSON
5. Write files to snapshot dir, optionally zip

### Import (recover)

Reconstructs an empty cluster from a snapshot — useful when a cluster is broken or was shut down and needs to be rebuilt on new nodes. This recreates the cluster structure (brokers, topics, configs) but does not recover any message data.

The caller must provide a `zkConnectString` pointing to an existing ZooKeeper ensemble (the same one as before, or a new one — KM does not manage ZooKeeper lifecycle).

**How it works:**
1. Parse `snapshot.json` to get `KmSnapshotContent`
2. Validate `zkConnectString` is reachable (Curator session). Validate `zkChroot` is non-root and unique across existing services.
3. Create a new `KmServiceDTO` with the same config (mode, version, broker count, ports) and the provided `zkConnectString` / `zkChroot`. Recreate a `KmConfigTemplateDTO` from the snapshot's `configItems` if no matching template exists in this DMS instance.
4. Create Kafka brokers → wait for registration
5. Recreate all topics with original partitions, replication factor, and config overrides
6. Update service status to `running`

#### Import API parameters

```groovy
// POST /kafka/snapshot/import
class KmImportRequest {
    String snapshotPath        // path to snapshot dir or zip
    String zkConnectString     // required, e.g. zk1:2181,zk2:2181
    String zkChroot            // optional, default: /kafka/{serviceName} — must be non-root, validated unique
    String nodeTags            // optional, override target node placement
}
```

**Important:** Import is metadata-only reconstruction. It rebuilds cluster structure (brokers, topics, configs) so producers/consumers can reconnect, but does not restore any message data. Messages on lost disks are unrecoverable through this mechanism — use Kafka's built-in partition replication (`replication.factor >= 2`) to protect against broker-disk or single-node failures during normal operation.

## Metrics and Monitoring

Same pattern as Redis Manager (`redis/MetricCtrl`): Prometheus and kafka_exporter are created as **separate DMS applications** via `init-exporters`.

### init-exporters flow (in `KmMetricCtrl`)

`GET /kafka/metric/init-exporters?targetNodeIp=x.x.x.x` creates two DMS apps:

1. **Prometheus app** — `AppDTO` with Prometheus config template whose scrape targets point to the kafka_exporter endpoint. Uses `BasePlugin.tplApp()` and `PrometheusPlugin.demoApp()`. Submitted via `KmJobExecutor.runCreatingAppJob()`.

2. **kafka_exporter app** — `AppDTO` using `danielqsj/kafka-exporter` image, same role as `oliver006/redis_exporter` in Redis Manager. Configured via `AppConf.envList` with `KAFKA_SERVER=broker1:9092,broker2:9092`. Runs in `host` network mode, exposes metrics port (default 9308). Prometheus scrapes this endpoint for topic/partition/consumer-group metrics. Submitted via `KmJobExecutor.runCreatingAppJob()`.

### Cluster-level gauges (registered in `KafkaManager`)
- `km_cluster_count` — Total Kafka clusters
- `km_broker_count` — Total brokers across all clusters
- `km_service_status` — Per-service health state

## Preferred Replica Election

Over time, partition leaders can drift away from their preferred (first) replica due to broker restarts, failovers, or reassignments. This causes uneven leader distribution — some brokers handle more leader partitions than others, creating hot spots.

CMAK exposes this as a first-class feature (`KMPreferredReplicaElectionFeature`). KM provides the same capability.

### How it works

1. `POST /kafka/service/preferred-replica-election` validates the service is running and in cluster mode.
2. Creates a `PREFERRED_REPLICA_ELECTION` job with a single `PreferredReplicaElectionTask`.
3. The task connects to ZK and checks if `/admin/preferred_replica_election` already exists. If so, a previous election is still in progress — returns fail.
4. Writes an empty JSON `{}` to `/admin/preferred_replica_election`. The Kafka Controller watches this znode and triggers leader election for all partitions where the current leader is not the preferred replica.
5. After processing, the Controller deletes the znode. KM does not need to poll — the election is asynchronous.

### When to use

- After broker restarts (leaders may have moved)
- After scale-down (remaining partitions may have non-preferred leaders)
- Periodically as a maintenance task for large clusters

## Consumer Group Inspection

CMAK provides visibility into consumer groups — which groups exist, their members, what topics they consume, and how far behind they are (consumer lag). KM provides the same read-only inspection capability.

### Data sources

Kafka 0.8-style consumers store group metadata directly in ZooKeeper under `/consumers/{group}/`. KM reads from these ZK paths — no container exec or CLI tools required. This is consistent with the ZK-direct pattern used across all KM tasks.

- `/consumers/{group}/ids/` — consumer member IDs
- `/consumers/{group}/offsets/{topic}/{partition}` — committed offset per partition
- `/consumers/{group}/owners/{topic}/{partition}` — which consumer owns which partition
- `/brokers/topics/{topic}` — partition replica assignment (used to compute log-end-offset for lag)

For new-style consumers (0.9+) that store offsets in the `__consumer_offsets` internal topic, kafka_exporter (set up via `/kafka/metric/init-exporters`) already exposes `kafka_consumergroup_lag` metrics to Prometheus.

### API behavior

- `GET /kafka/consumer/list?serviceId=X` — Lists all consumer groups. For new-style consumers, runs `kafka-consumer-groups.sh --bootstrap-server` against the first live broker from `brokerDetail`.
- `GET /kafka/consumer/one?serviceId=X&groupId=Y` — Detail for one group: members (client ID, host), assigned topic-partitions, current offset, log-end offset, lag.
- `GET /kafka/consumer/lag?serviceId=X&groupId=Y` — Focused lag view: per-topic lag summary (total lag, partition count, consumer count).

### Implementation approach

Since these are read-only queries that shell out to `kafka-consumer-groups.sh`, they execute synchronously in the HTTP handler (same as `/one` reads). No async job is needed. The command timeout should be bounded (e.g., 10 seconds) to prevent hanging on unresponsive brokers.

If the cluster is stopped or no broker is reachable, these endpoints return an appropriate error (e.g., 409 "cluster not running" or 503 "no broker available").

### Consumer lag monitoring

The kafka_exporter (set up via `/kafka/metric/init-exporters`) exposes `kafka_consumergroup_lag` metrics to Prometheus for new-style (0.9+) consumers. The API endpoints above provide on-demand ZK-based consumer inspection for old-style (0.8) consumers.

## Web UI Pages (under `dms/www/admin/pages/kafka/`)

| Page | Description |
|------|-------------|
| `service.html/js` | Service list and management |
| `one.html/js` | Service detail: broker list, controller status, connection string |
| `add.html/js` | Create Kafka service form |
| `topic.html/js` | Topic list with partition/replica info, ISR status |
| `topic-one.html/js` | Topic detail: per-partition leader, replicas, offsets |
| `consumer.html/js` | Consumer group list with lag per topic |
| `consumer-one.html/js` | Consumer group detail: members, assigned partitions, offset timeline |
| `jobs.html/js` | Job history |
| `snapshot.html/js` | Snapshot list, export, import/recover |
| `config-template.html/js` | Broker config template management |
| `overview.html/js` | Dashboard: cluster count, broker health, under-replicated partitions |

## Differences from Redis Manager

| Aspect | Redis Manager | Kafka Manager |
|--------|--------------|---------------|
| Coordination | Optional (Sentinel/ZooKeeper) | Requires existing ZooKeeper (not managed by KM) |
| Data distribution | Slot-based (16384 slots) | Partition-based (per topic) |
| Scaling unit | Shard (group of primary+replicas) | Broker (individual node) |
| Rebalancing | Slot migration between shards | Partition reassignment between brokers |
| Balancer class | `SlotBalancer` | `PartitionBalancer` |
| Backup | RDB snapshot (single file) | Metadata snapshot only (topics, configs, topology) — message data protected by replication |
| Modes | standalone/sentinel/cluster | standalone/cluster |
| Engine variants | redis/valkey/engula/kvrocks/velo | Single engine (Apache Kafka 2.8.2) |
| Config hot-reload | `CONFIG SET` command | `kafka-configs.sh --alter` (dynamic configs only) |
| Health check | `PING`, `CLUSTER INFO` | Broker registration in ZK, ISR state, controller alive |
| Client library | Jedis | kafka-clients 2.8.2 (or shell commands via `kafka-*.sh`) |

## Implementation Order

1. **Data model** — DDL in `init_h2.sql`, DTO classes, JSON model classes
2. **KafkaManager + KmJobExecutor** — Singleton hub, thread pool, metrics
3. **KafkaPlugin + config templates** — Container image, server.properties templating
4. **Cluster creation jobs** — STANDALONE_CREATE, CLUSTER_CREATE task chains
5. **Controllers** — KmServiceCtrl with create/list/one/delete
6. **Topic management** — KmTopicCtrl, CreateTopicTask
7. **Scaling** — BROKER_SCALE_UP/DOWN, PartitionBalancer, ReassignPartitionsTask
8. **Failover** — FailoverTask, controller re-election
9. **Snapshot/Import** — KmSnapshotManager, metadata export to dir/zip, cluster recovery from snapshot
10. **Monitoring** — kafka_exporter + Prometheus as separate DMS apps, KmMetricCtrl
11. **Preferred replica election** — PreferredReplicaElectionTask, KmServiceCtrl endpoint
12. **Consumer group inspection** — KmConsumerCtrl, read-only lag/group queries via kafka-consumer-groups.sh
13. **Web UI** — Pages following existing Redis Manager UI patterns
