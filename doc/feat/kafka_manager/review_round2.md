# Kafka Manager Review Round 2

## Scope

- Review target: Kafka manager follow-up DDL fix commit `b98f058`
- Previous round: `doc/feat/kafka_manager/review_round1.md`

## Findings

1. Medium: the migration still does not repair databases that already created `km_service` from the earlier bad DDL.

`dms/ddl_update.sql` now creates the Kafka manager tables for existing deployments, but it only uses `create table if not exists`. That helps when the tables are missing. It does not change an existing `km_service` table that was already created with `node_tags_by_broker_index varchar(100)`. In that case the column remains undersized even though `init_h2.sql` and the new-create path now use `varchar(500)`.

References:
- `dms/ddl_update.sql:3`
- `dms/ddl_update.sql:24`
- `dms/init_h2.sql:405`

2. Low: the design doc is now stale and disagrees with the committed schema.

The design doc still says `node_tags_by_broker_index` is `varchar(100)`, while both schema files now use `varchar(500)`. This is not a runtime blocker, but it will create confusion for later API and web page implementation work.

References:
- `doc/kafka_manager_design.md:110`
- `dms/init_h2.sql:405`
- `dms/ddl_update.sql:24`

## Summary

Round 1 feedback is only partially closed. The missing-table upgrade path is fixed, but there is still no explicit migration for environments that may already have the earlier `km_service` definition, and the design doc should be updated to match the schema.

## Resolution

Both findings addressed:

1. Added `alter table km_service alter column node_tags_by_broker_index varchar(500)` before the `create table if not exists` block in `ddl_update.sql`. If the table exists with the old `varchar(100)`, the ALTER widens it. If the table does not exist, the ALTER fails harmlessly (caught by the same try/catch in RunServer.groovy) and the CREATE follows.

2. Updated `doc/kafka_manager_design.md` line 110 to reflect `varchar(500)` for `node_tags_by_broker_index`.
