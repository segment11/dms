# Kafka Manager Review Round 23

## Scope

- Review target: design/plan conformance audit + gap implementation (this working session)
- Stage: close remaining design gaps and deferred review findings
- Previous round: `doc/feat/kafka_manager/review_round22.md`

## Audit Result (design vs implementation)

Conformed: all DTOs, JSON models, KmJobTypes (+UPDATE_CONFIG now), 15 task classes, ServiceCtrl/TopicCtrl/JobCtrl/SnapshotCtrl/MetricCtrl endpoints, KafkaManager metrics, plugin + templates.

## Gaps Implemented This Round

### 1. KmConfigTemplateCtrl (design 8.5 — was missing entirely)

- New `dms/src/ctrl/kafka/ConfigTemplateCtrl.groovy`: `GET /kafka/config-template/list` (keyword filter + pager), `POST /add`, `POST /update`, `DELETE /delete`
- `/delete` refuses templates still referenced by a service (`config_template_id` in-use check)
- TDD: `dms/test/ctrl/kafka/ConfigTemplateCtrlRouteTest.groovy` — 4 HTTP-level route specs over H2 (mock servlet + RouterInit), red first (missing class), then green
- Unblocks e2e 1.7 via API

### 2. Consumer group CLI inspection (plan_cmak_features Tasks 4-5 — diverged in round 20)

Round 20 left consumer endpoints ZK-only; Kafka ≥0.9 consumers commit offsets to `__consumer_offsets`, so e2e 9.1-9.4 could never pass. Now:

- New `KafkaManager.containerExec(clusterId, nodeIp, containerId, cmd)` — agent `container init` exec, checks `flag`+`message`
- Agent `ContainerInit.groovy`: host-process branch now runs `bash -c` (was `Runtime.exec(String)` tokenization — no shell support) and waits up to 10s (parity with docker branch)
- New `dms/src/km/ConsumerGroupParser.groovy` — parseGroupList / parseGroupDescribe / parseGroupLag for `kafka-consumer-groups.sh` output; TDD with realistic output samples (6 specs)
- `ConsumerCtrl` rewritten: CLI-first (resolves `kafka-consumer-groups.sh` inside bitnami container or on host), ZK fallback; response carries `mode: 'cli'|'zk'`; bootstrap addr = `nodeIp:(port + instanceIndex)` matching the per-broker port scheme

### 3. Snapshot import consistency + nodeTags (design 8.7)

- `KmSnapshotManager.importSnapshot` now sets `isRunningUnbox`/`cmd`/`deployFileIdList` like `/add` (imported services no longer diverge into container mode)
- Optional `nodeTags`/`nodeTagsByBrokerIndex` params: controller → import conf + service record
- `KafkaManager.startKafkaDeployFileId()` resolves the start script deploy file by `dest_path` (falls back to id 1); `/add` switched from hardcoded `<< 1`; TDD spec over H2

### 4. Deferred review findings closed

| Finding | Fix |
|---|---|
| R18 F8 `/stop` in-flight jobs | 409 "service has in-flight jobs" when `created`/`processing` jobs exist for the service |
| R18 F10 async `update-config` | New `UPDATE_CONFIG` job type + `UpdateConfigTask` (read-merge-write moved from controller; partial-failure comment added); controller validates and enqueues; TDD spec for `mergeConfig` |
| R18 F11 `/reassign` topic filter | Optional `topicNames` body param → job param → `ReassignPartitionsTask.filterTopicNames` (TDD spec); no match → task fails with clear message |
| R19 F2 `/stop` from `creating` | Guard now allows `running` or `creating`; e2e case 11.2 updated |

## Verification

- TDD red→green for all 5 new specs: ConfigTemplateCtrlRouteTest (4), ConsumerGroupParserTest (6), KafkaManagerDeployFileTest (2), ReassignPartitionsTaskTest (3), UpdateConfigTaskTest (2)
- `gradle buildToRun` (dms) + `gradle :test` (dms_agent) successful; `gradle unitTest` 78 tests, only pre-existing env-dependent `RemoteOneLockTest` fails (fails on clean tree too)
- JaCoCo (`km.*`, `ctrl.kafka.*`): new/changed paths executed — ConfigTemplateCtrl, ConsumerGroupParser, UpdateConfigTask, filterTopicNames, KafkaManager helper

## Remaining Work

- Live E2E run (stack down): consumer CLI mode (9.1-9.4), config template flow (1.7), stop guards (11.2), reassign filter, update-config job (11.3/11.4 now async — verify via `/kafka/job/list`)
- Cluster mode (3-broker) E2E
- Web UI stage (design doc has no web UI spec yet — needs planning)
- Minor accepted deviations: `/export` returns `snapshotDir` not `id`; `updateStatus`/`zkFullConnectString` helpers inlined; scale-up param `brokerCount` vs design `count`
