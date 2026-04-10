# Kafka Manager Review Round 3

## Scope

- Review target: Kafka manager follow-up DDL fix commit `708d0f8`
- Previous round: `doc/feat/kafka_manager/review_round2.md`

## Findings

1. Medium: the new column-widening migration is not portable to the repository's PostgreSQL startup path.

`RunServer.groovy` still applies `ddl_update.sql` when `db.driver` indicates PostgreSQL, so the new widening statement needs to work on that path too. The added statement uses `alter table km_service alter column node_tags_by_broker_index varchar(500)`, which is valid for H2-style syntax, but PostgreSQL requires `ALTER COLUMN ... TYPE ...`. If PostgreSQL deployments are intended to be supported here, this migration will fail on that backend and the old column width will remain in place.

References:
- `dms/src/RunServer.groovy:63`
- `dms/src/RunServer.groovy:86`
- `dms/ddl_update.sql:4`
- PostgreSQL `ALTER TABLE` docs: https://www.postgresql.org/docs/current/sql-altertable.html

## Summary

Round 2 closed the earlier H2 upgrade gap and the design-doc mismatch. The remaining issue is backend compatibility: the new `ALTER TABLE` statement should be made portable, or the repository should explicitly scope this migration path to H2-only environments.

## Resolution

The interim dual-ALTER approach from this round was later removed.

Final handling moved the backend-specific repair into `support/DDLPostChecker.groovy`, so `ddl_update.sql` no longer depends on intentional statement failures to support both H2 and PostgreSQL.
