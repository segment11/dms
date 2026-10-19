# Kafka Manager Review Round 22

## Scope

- Review target: follow-up patch for round 20 post-fix findings + round 21 E2E fixes (this working session)
- Stage: round 20 follow-up patch
- Previous round: `doc/feat/kafka_manager/review_round21.md`

## Round 20 Post-Fix Finding Status

| # | Finding | Severity | Status | Notes |
|---|---------|----------|--------|-------|
| 1 | Dead code: unused `DefaultJsonTransformer` in `ConsumerCtrl` | Medium | **Fixed** | Line removed along with its import |
| 2 | NPE risk: null `zkConnectString` concat in 3 consumer endpoints | Medium | **Fixed** | New script-level `zkConnectionString(service, resp)` helper guards null fields, halts 500 |
| 3 | NPE risk: null `status` comparison in `/start` | Medium | **Fixed** | Explicit `!one.status ||` guard added. Note: dynamic Groovy `!=` is already null-safe, so this is intent clarity rather than a real NPE fix |
| 4 | Silent deletion failure in `PreferredReplicaElectionTask` | High | **Fixed** | Delete now uses `guaranteed().deletingChildrenIfNeeded()` |
| 5 | Race condition between stale path delete and create | High | **Fixed** | Create wrapped in `MAX_ATTEMPTS=3` retry loop catching `KeeperException.NodeExistsException`; fresh path from a racing writer returns `fail('already in progress')` on next stat check |

## Changes

### PreferredReplicaElectionTask (`dms/src/km/job/task/PreferredReplicaElectionTask.groovy`)

- Extracted testable constants `ELECTION_PATH`, `STALE_THRESHOLD_MS` (5 min), `MAX_ATTEMPTS` (3)
- Extracted `static boolean isStaleElection(Stat stat, long nowMs)` — null stat returns false (caller keeps the in-progress guard)
- `doTask()` loop: check stat → stale check → `delete().guaranteed().deletingChildrenIfNeeded()` → create; on `NodeExistsException` retry, up to 3 attempts, then fail with conflict message

### ConsumerCtrl (`dms/src/ctrl/kafka/ConsumerCtrl.groovy`)

- Removed dead `def json = new DefaultJsonTransformer()` in `/one` and the `DefaultJsonTransformer` import
- Added `zkConnectionString(KmServiceDTO, Resp)` helper; all 3 endpoints use it (follows the script-method pattern of `ctrl/redis/MetricCtrl.groovy:457`)

### ServiceCtrl (`dms/src/ctrl/kafka/ServiceCtrl.groovy`)

- `/start` guard: `if (!one.status || one.status != KmServiceDTO.Status.stopped)`

### KafkaPlugin health checker (`dms/plugins/plugin/demo2/KafkaPlugin.groovy`)

- Same null-zk-field NPE class as round 20 finding 2: concat moved inside the try with a null-field guard returning false (unhealthy)

## Verification

- TDD: new spec `dms/test/km/job/task/PreferredReplicaElectionTaskTest.groovy` (6 cases) — red confirmed first (`MissingPropertyException: STALE_THRESHOLD_MS`), then green
- `gradle buildToRun` — successful
- `gradle unitTest` — 61 tests, 60 pass; `RemoteOneLockTest.Exe` fails identically on a clean tree (pre-existing, environment-dependent, unrelated to KM)
- `gradle jacocoUnitTestReport` (km/model filter) — report includes `km.job.task` package; `PreferredReplicaElectionTask` executed

## Round 21 Open Item

Round 21's recommendation listed "Host process mode — requires CreateProcessor and InMemoryAllContainerManager changes" as open, but its own Status Summary and verification show host-process Kafka working end-to-end (service 9 `creating` → `running` after the health checker fix): `CreateProcessor.filterNodeList` already excludes docker agents for `isRunningUnbox`, `HostProcessSupport.startOneProcess` wraps processes as containers, and `ContainerHelper.generateProcessAsContainerId` provides the id scheme. Treating the recommendation line as stale; no code change made.

## Remaining Work

- E2E verification with live ZK/Kafka/DMS (environment currently down): health checker across services 8/9, topic ops, consumer group inspection, preferred replica election (see `todo_next.md`)
- Cluster mode (3-broker) E2E test
- Web UI stage (per round 19 recommendation)
- Deferred round 18 items: `/stop` in-flight job guard (F8), async `update-config` (F10), `/reassign` topic filter (F11)
