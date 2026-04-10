# Kafka Manager Review Round 4

## Scope

- Review target: Kafka manager follow-up DDL fix commit `9fb4d1e`
- Previous round: `doc/feat/kafka_manager/review_round3.md`

## Findings

1. Low: the portable migration now guarantees startup error logs on every backend.

`ddl_update.sql` contains two backend-specific `ALTER TABLE` statements and depends on `RunServer.groovy`'s per-statement `try/catch` to ignore the one that does not match the active database. That makes the migration functionally portable, but it also means one statement will fail and be logged as `ddl update fail` on every restart for every existing deployment. If `km_service` is still missing, both ALTER statements will fail before the `CREATE TABLE` runs. This is noisy and makes real DDL failures harder to spot in production logs.

References:
- `dms/ddl_update.sql:5`
- `dms/ddl_update.sql:6`
- `dms/src/RunServer.groovy:94`
- `dms/src/RunServer.groovy:97`

## Summary

The functional portability issue from round 3 is addressed, but the current implementation pays for it with intentional startup error noise. A backend-aware migration path or conditional existence/type check would avoid masking real DDL problems in logs.

## Resolution

This round's concern is addressed by the final startup flow:

1. `ddl_update.sql` no longer contains intentionally failing backend-specific `ALTER TABLE` statements.
2. `RunServer.groovy` now runs `DDLPostChecker.instance.check(d, isPG)` after successful DDL update execution.
3. `support/DDLPostChecker.groovy` performs the Kafka manager post-DDL repair and verification, including widening `km_service.node_tags_by_broker_index` with backend-specific SQL only when the schema is actually undersized.
4. Any DDL update or post-check failure now stops startup instead of being logged and ignored.
