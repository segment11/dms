# DMS Agent Guidelines

This file provides guidance for coding agents working in this repository.

## Build And Test

Each subproject builds from its own directory.

```bash
# Server
cd dms && gradle buildToRun

# Focused dms TDD loop
cd dms && gradle unitTest
cd dms && gradle unitTest --tests "model.job.RmBackupTemplateDTOTest"
cd dms && gradle unitTest --tests "support.AuthTokenCacheHolderTest"
cd dms && gradle jacocoUnitTestReport

# Other modules
cd dms_common && gradle test
cd dms_agent && gradle test
```

## TDD Rules

- Write the Spock spec first.
- Run the focused spec and confirm it fails for the expected reason before touching production code.
- Write the smallest code change that makes the spec pass.
- Rerun the focused spec, then rerun adjacent relevant specs.
- After the relevant `dms` run, inspect `dms/build/reports/jacocoUnitTestHtml/` and confirm the changed path was executed.
- Prefer `cd dms && gradle unitTest --tests "ExactSpecName"` for backend work. Do not default to `gradle test` during the red-green loop.
- Keep the default TDD loop deterministic. Do not mix real MySQL, ZooKeeper, Redis, or remote-node dependencies into focused unit specs.
- Put reusable helpers in `dms/test/support/`.
- Follow existing sample specs for new work:
  - `dms/test/model/job/RmBackupTemplateDTOTest.groovy`
  - `dms/test/support/AuthTokenCacheHolderTest.groovy`
  - `dms/test/ctrl/FirstCtrlRouteTest.groovy`
  - `dms/test/plugin/redis/RedisManagerBackupPluginTest.groovy`
  - `dms/test/metric/SimpleGaugeTest.groovy`

## Spock Conventions

- Extend `Specification`
- Use descriptive string method names
- Prefer `given/when/then` or `expect/where`
- Keep one behavior per test
- Use package-aligned test paths under `dms/test/`

## Notes

- `dms/build.gradle` defines `unitTest` to avoid sibling-project `--tests` filter failures.
- `jacocoUnitTestReport` is the `dms` coverage entry point.
- Dynamic Groovy controller scripts can produce JaCoCo class-mismatch warnings. For those routes, treat the HTTP-level assertion as primary and coverage as secondary.
