# Kafka Manager - Next Steps

## Status

- Round 24 done: web UI stage complete — 11 Kafka pages under `dms/www/admin/pages/kafka/` + Kafka menu group, following the Redis Manager frontend patterns exactly
- Backend API stage complete and reviewed (rounds 17-23)

## Todo

1. **Live browser verification of the web UI** (DMS + ZK + Kafka stack currently down)
   - Walk through: Kafka menu -> Overview -> Service list -> Add service -> Detail (scale/failover/update-config) -> Topics -> Topic detail -> Consumer groups -> Jobs -> Snapshots -> Config template
   - Same for the E2E API suite (see `doc/feat/kafka_manager/e2e_test_cases.md`)

2. **E2E verification with live environment**
   - Restart local stack: `doc/feat/kafka_manager/start-local.sh`, `start-zk-node.sh`
   - Consumer CLI mode 9.1-9.4, config template flow 1.7, stop guards 11.2, `/reassign` topicNames filter, async update-config 11.3/11.4 (check `/kafka/job/list`)

3. **Test cluster mode** (3 brokers, not standalone)

4. Optional backend extensions surfaced by the web UI
   - `/kafka/topic/one` could expose per-partition leader/ISR (design mentions ISR on the topic page)
   - Dedicated `/kafka/overview` endpoint (dashboard currently computed client-side)

## Done This Session (round 24)

- 11 page pairs: service, add, one, topic, topic-one, consumer, consumer-one, jobs, snapshot, config-template, overview
- Menu group "Kafka" in `dms/www/admin/index.js`
- Redis Manager parity: no `.error()` handlers, no `uiTips.unloading`, `ui-valid` int/minval rules on text inputs, `Page.registerIntervalFunc` polling on the detail page, `LocalStore` for the selected service, underscore-only helpers
- Mechanical verification: module/file naming, ng-click handlers, ng-form wiring, API contract cross-check against the backend routes, `node --check` syntax pass
