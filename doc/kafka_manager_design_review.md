# Kafka Manager Design Review

Review target: `doc/kafka_manager_design.md`

## Current Findings

1. The implementation plan still refers to JMX exporter integration, but the monitoring design now clearly uses `kafka_exporter`.

The document’s main monitoring sections have converged on `kafka_exporter` as a separate DMS application plus Prometheus, but the implementation order still says "JMX exporter integration". That item is stale and could send implementation in the wrong direction.

References:
- `doc/kafka_manager_design.md:377`
- `doc/kafka_manager_design.md:458`
- `doc/kafka_manager_design.md:516`

2. The `KM_SERVICE.app_id` field description is misleading.

The schema comment says `app_id` is the DMS app ID for broker containers in standalone mode, but the task chains model Kafka brokers as one DMS app in both standalone and cluster modes. Since there is no separate cluster-mode broker app ID field, this description should be widened to cover both modes.

References:
- `doc/kafka_manager_design.md:100`
- `doc/kafka_manager_design.md:298`
- `doc/kafka_manager_design.md:304`

## Resolved Since Earlier Review

These issues appear fixed in the latest revision:
- snapshot/import wording now clearly says metadata-only reconstruction, not message recovery
- import API is now consistent around `zkConnectString`
- stopped-cluster export now has a persisted `config_overrides` field
- ZooKeeper validation now uses a Curator client session instead of four-letter commands
- ZooKeeper chroot ownership is now explicit, non-root, unique, and validated
- snapshot import now specifies recreating a config template from materialized snapshot config when needed

## Summary

The design is in good shape at the document level. The remaining issues are consistency cleanups rather than architectural problems.

Residual implementation risks to verify with tests:
- create/import validation for unique non-root `zk_chroot`
- recursive chroot cleanup safety on delete
- stopped-cluster snapshot export using persisted `config_overrides`
- config-template recreation from snapshot materialized config in a fresh DMS environment
