# Kafka Manager E2E Test Setup

## Overview

ZK 3-node ensemble on localhost → DMS API creates/manages Kafka services.

## Quick Start

```bash
cd /home/kerry/ws/dms

# Build (one-time)
cd dms && gradle buildToRun -q && cd ..

# Run all tests
./doc/feat/kafka_manager/run-e2e.sh

# Run specific test
./doc/feat/kafka_manager/run-e2e.sh 2.1    # create cluster
./doc/feat/kafka_manager/run-e2e.sh 4.3     # preferred replica election
./doc/feat/kafka_manager/run-e2e.sh 9      # consumer group tests
```

## What Gets Started

| Component | Port | Role |
|-----------|------|------|
| Zookeeper-1 | 2181 | Part of 3-node ensemble |
| Zookeeper-2 | 2182 | Part of 3-node ensemble |
| Zookeeper-3 | 2183 | Part of 3-node ensemble |
| DMS server | 5010 | Manages Kafka via API |
| DMS agent | 5010 | Runs on same node, registers as worker node |

**ZK connection string:** `localhost:2181,localhost:2182,localhost:2183`

Kafka brokers are created by DMS API (not pre-started) as Docker containers.

## Manual Commands

```bash
# ZK only
./doc/feat/kafka_manager/start-local.sh
./doc/feat/kafka_manager/stop-local.sh

# Server logs
tail -f /tmp/dms_server.log
tail -f /tmp/dms_agent.log
```

## Auth

API uses `MD5(secret + host)` for auth:
- Cluster secret: `1`
- Host: `localhost`
- Token: computed automatically by the script

## Test Coverage

| Group | Tests | Description |
|-------|-------|-------------|
| 1 | 1.1-1.5, 1.7 | Standalone service lifecycle |
| 2 | 2.1-2.5 | Cluster service lifecycle |
| 4 | 4.3, 4.3b | Preferred replica election |
| 9 | 9.1-9.4 | Consumer group inspection |
| 10 | 10.1 | Job observability |
| 11 | 11.1, 11.3-11.4 | Stop + update-config |

## Troubleshooting

### Server won't start
```bash
tail -50 /tmp/dms_server.log
# Check port
ss -tlnp | grep 5010
```

### ZK not ready
```bash
for port in 2181 2182 2183; do
  echo -n "port $port: "
  echo "ruok" | nc localhost $port
done
```

### Node not registering
```bash
curl -s http://localhost:5010/dms/api/node/list \
  -H "X-Auth-Token: $(echo -n '1127.0.0.1' | md5sum | cut -d' ' -f1)" \
  -H "X-Cluster-Id: 1"
```

### Cluster creation stuck
```bash
# Check jobs
curl -s "http://localhost:5010/dms/api/kafka/job/list?serviceId=<ID>" \
  -H "X-Auth-Token: $(echo -n '1localhost' | md5sum | cut -d' ' -f1)" \
  -H "X-Cluster-Id: 1"
```
