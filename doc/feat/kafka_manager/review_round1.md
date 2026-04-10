# Kafka Manager Review Round 1

## Scope

- Review target: Kafka manager DDL commit `2aad9c7`
- Related design reference: `doc/kafka_manager_design.md`

## Findings

1. High: existing deployments will not receive the new `km_*` tables.

`RunServer.groovy` only executes `init_h2.sql` on first boot when the `CLUSTER` table does not exist. For existing deployments it looks for `ddl_update.sql` instead. This DDL commit only changes `dms/init_h2.sql`, and the repository currently has no `ddl_update.sql`, so upgraded environments will restart without the Kafka Manager schema.

References:
- `dms/src/RunServer.groovy:70`
- `dms/src/RunServer.groovy:86`
- `dms/init_h2.sql:383`

2. Medium: `km_service.node_tags_by_broker_index` is too small for the documented topology.

The design says Kafka Manager supports up to 32 brokers and uses `node_tags_by_broker_index` for per-broker node placement. The committed DDL keeps this field at `varchar(100)`, which is not enough for a practical 32-entry mapping even with a compact serialized format. This creates an avoidable truncation risk once larger clusters are supported.

References:
- `doc/kafka_manager_design.md:51`
- `doc/kafka_manager_design.md:110`
- `dms/init_h2.sql:405`

## Summary

The DDL direction matches the design at a high level, but it is not yet safe for rollout. The upgrade path issue should block merge; the per-broker placement field sizing should be corrected in the same round.

## Resolution

Both findings addressed:

1. Created `dms/ddl_update.sql` with all 6 `km_*` tables using `create table if not exists` and `create index if not exists` for safe idempotent execution on existing deployments. RunServer.groovy already handles this file at line 86.

2. Changed `km_service.node_tags_by_broker_index` from `varchar(100)` to `varchar(500)` in both `init_h2.sql` and `ddl_update.sql` to accommodate up to 32 broker entries.
