# Kafka Manager Review Round 13

## Scope

- Review target: commit `3aba1a8` (fix(km): address review round 12 findings)
- Stage: fix pass for round 12 findings
- Previous round: `doc/feat/kafka_manager/review_round12.md`

## Round 12 Finding Status

| # | Finding | Status | Notes |
|---|---------|--------|-------|
| 1 | Scale-down broker selection non-deterministic | **Fixed** | Sorts by `brokerId` desc, takes top N |
| 2 | `configTemplateId` defaults to `'0'` | **Fixed** | Only added to paramList when non-null |
| 3 | Prometheus scrape config missing kafka_exporter | **Fixed** | New template + wired via `configTplName` |
| 4 | `ImageTplDTO` lookup crashes with AssertionError | **Fixed** | Changed to `resp.halt(409, ...)` |
| 5 | Stub tasks (carry-over) | **Not fixed** | Deferred |
| 6 | Snapshot export/import stubs (carry-over) | **Not fixed** | Deferred |
| 7 | KafkaPlugin checker no-op (carry-over) | **Not fixed** | Deferred |
| 8 | kafka_exporter missing `registryId` | **Fixed** | Uses `KafkaManager.preferRegistryId()` |
| 9 | ValidateZookeeperTaskTest minimal (carry-over) | **Not fixed** | Deferred |

## New Findings

### Medium

1. **`PrometheusKafkaExporterYmlTpl.groovy` has unused imports compared to Redis variant.**

   The Redis exporter template imports `server.InMemoryAllContainerManager` and `server.InMemoryCacheSupport` because it iterates all Redis apps and their containers. The Kafka template only uses `ContainerMountTplHelper`, which is correct for its simpler use case. No issue here — just noting the template is well-scoped.

   However, the Kafka template does not include a `node-exporter` scrape job. The Redis exporter template scrapes both `redis_exporter` and `node-exporter` targets. If Kafka Manager's Prometheus instance should also monitor node-level metrics (CPU, memory, disk) for Kafka broker hosts, a `node-exporter` job should be added. This is a feature gap, not a bug.

   References:
   - `dms/plugins_resources/prometheus/PrometheusKafkaExporterYmlTpl.groovy`
   - `dms/plugins_resources/prometheus/PrometheusRedisExporterYmlTpl.groovy:13-19`

2. **`init-exporters` creates one Prometheus + one kafka_exporter globally, not per service.**

   The app names `km_prometheus` and `km_kafka_exporter` are hardcoded constants. Calling `init-exporters` for service A creates these apps; calling it again for service B returns 409 "already exists." This means only one Kafka cluster can be monitored by the exporter.

   If this is intentional (single Prometheus monitors all Kafka clusters), then the kafka_exporter's `KAFKA_SERVER` env should include brokers from all running services, not just the one passed via `serviceId`. If per-service monitoring is intended, the app names should include the service name (e.g., `km_prometheus_{serviceName}`).

   The design doc says "One-click setup: creates Prometheus app + kafka_exporter app as separate DMS applications" — which implies a single global pair. But the `KAFKA_SERVER` env currently only contains brokers from the requested `serviceId`.

   References:
   - `dms/src/ctrl/kafka/MetricCtrl.groovy:56,85` (hardcoded names)
   - `dms/src/ctrl/kafka/MetricCtrl.groovy:38-42` (single-service broker list)

3. **`KafkaManager.dataDir()` queries `DynConfigDTO` on every call — no caching.**

   `dataDir()` does a DB query each time it's invoked. Currently it's only called once during `init-exporters`, so this is not a performance concern. But if future code calls it in a loop (like metric collection), it would be inefficient. Consider caching or documenting that it's intended for infrequent use.

   Severity: low — only called once currently.

   References:
   - `dms/src/km/KafkaManager.groovy:26-29`

### Low

4. **Prometheus `nodeDir` defaults to `/data/openobserve` in `PrometheusPlugin.demoApp()` when not set.**

   Line 170-172 of PrometheusPlugin: `if (!nodeDir) { nodeDir = '/data/openobserve' }`. The MetricCtrl correctly overrides this to `KafkaManager.dataDir() + '/prometheus'`, so the fallback doesn't apply here. But the default path name `openobserve` is misleading for Prometheus — this appears to be a pre-existing naming issue in PrometheusPlugin, not introduced by the Kafka changes.

   References:
   - `dms/plugins/plugin/demo2/PrometheusPlugin.groovy:170-172`

5. **`PrometheusKafkaExporterYmlTpl.groovy` produces a YAML with leading whitespace before `global:`.**

   Both the empty-targets and full templates return strings with leading newline and whitespace due to the `"""` multiline string starting on the next line (lines 18, 31). This is cosmetic — Prometheus parses it fine — but the Redis template has the same pattern, so it's consistent.

   References:
   - `dms/plugins_resources/prometheus/PrometheusKafkaExporterYmlTpl.groovy:18-22,31-38`

## Summary — Carry-Over Tracker

This commit cleanly addresses all the actionable findings from round 12. The Prometheus scrape configuration is well-implemented following the existing Redis exporter pattern. The scale-down broker selection is now deterministic. The `ImageTplDTO` guard provides a clear user-facing error.

**No new blockers.** The cluster creation path is now complete end-to-end: plugin init, template lookup, config rendering, ZK chroot creation, broker registration, and monitoring setup all have correct wiring.

**Remaining deferred items (unchanged since round 12):**

| Item | First Flagged | Status |
|------|---------------|--------|
| 7 stub task implementations | Round 10 (#6) | Scaffolding only — no real logic |
| KmSnapshotManager export/import file I/O | Round 10 (#7-8) | DB records only, no files written/read |
| KafkaPlugin `before` checker | Round 10 (#13) | Always returns `true` |
| ValidateZookeeperTask test coverage | Round 10 (#15) | Only `isValidChroot` tested |
| Multi-service exporter gap | This round (#2) | Single exporter pair for all clusters |

**Recommendation:** The controller + wiring layer is solid. The next implementation pass should focus on the stub tasks — particularly `CreateTopicTask`, `AddBrokersTask`, and `ReassignPartitionsTask` — as these are the most impactful for making the API actually functional. Snapshot file I/O and the KafkaPlugin checker can follow after that.
