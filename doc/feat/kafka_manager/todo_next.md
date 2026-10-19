# Kafka Manager - Next Steps

## Status

- Round 23 done: design audit + all gaps closed (config template CRUD, consumer CLI inspection, import consistency + nodeTags, deferred R18/R19 findings)
- Backend API stage complete and reviewed (rounds 17-23)

## Todo

1. **E2E verification with live environment** (ZK + Kafka + DMS server currently down)
   - Restart local stack: see `doc/feat/kafka_manager/start-local.sh`, `start-zk-node.sh`
   - New this round: consumer CLI endpoints 9.1-9.4 (`mode: 'cli'`), config template flow 1.7, stop guards 11.2, `/reassign` topicNames filter, async update-config 11.3/11.4 (verify via `/kafka/job/list`)
   - Verify health checker for existing Kafka services (8, 9): `creating` → `running` transition

2. **Test cluster mode** (3 brokers, not standalone)

3. **Web UI stage** — no design doc yet; needs planning (page list, mockups, segmentweb work)

## Done This Session (round 23)

- `ConfigTemplateCtrl`: 4 CRUD routes at `/kafka/config-template` (+ in-use delete guard), route-tested over H2
- Consumer group CLI: `KafkaManager.containerExec`, agent `ContainerInit` host-process `bash -c`, `ConsumerGroupParser` (6 specs), `ConsumerCtrl` CLI-first with ZK fallback
- Import: host-process conf parity with `/add`, optional `nodeTags`/`nodeTagsByBrokerIndex`, `startKafkaDeployFileId()` dest-path lookup (spec)
- `/stop`: in-flight job guard + allow stop from `creating` (R18 F8, R19 F2)
- `/update-config`: async via new `UPDATE_CONFIG` job + `UpdateConfigTask` with read-merge-write (R18 F10, spec for mergeConfig)
- `/reassign`: optional `topicNames` filter (R18 F11, spec)
