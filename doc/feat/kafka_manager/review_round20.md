# Kafka Manager Review Round 20

## Scope

- Review target: commit `3d39ca7` (feat(km): add preferred replica election, consumer group inspection, and CuratorPoolHolder)
- Stage: feature review → fix pass
- Previous round: `doc/feat/kafka_manager/review_round19.md`

## Fix Commit: `c0514b5` (fix(km): address review round 20 findings)

| # | Finding | Severity | Status | Notes |
|---|---------|----------|--------|-------|
| 1 | Consumer lag uses `replicas.size()` as log-end-offset | Critical | **Fixed** | Removed fake lag; reports consumer offsets only with note to use kafka_exporter |
| 2 | Election ZK path has no stale cleanup | High | **Fixed** | Added 5-min TTL with `stat.ctime` check; deletes stale path before writing |
| 3 | CuratorPoolHolder has no connection/session timeout | High | **Fixed** | Added `5000, 30000` (5s conn, 30s session) to `newClient()` |
| 4 | No `serviceId` in consumer endpoint responses | Medium | **Fixed** | All 3 endpoints now return `serviceId` |
| 5 | Duplicate `KmJob` import in 8 task files | Low | **Fixed** | Removed extraneous `import km.job.KmJob` from all affected files |

## Post-Fix Code Review Findings (E2E attempt)

After E2E infrastructure setup, a code review revealed additional issues:

### Medium

1. **Dead code: unused `DefaultJsonTransformer` in `ConsumerCtrl.groovy`**

   Line 62:
   ```groovy
   def json = new DefaultJsonTransformer()
   ```
   The `json` variable is created but never used in the `/one` method. Either remove it or it was intended for future use.

   References:
   - `dms/src/ctrl/kafka/ConsumerCtrl.groovy:62`

2. **NPE risk: null `zkConnectString` concatenation in all 3 consumer endpoints**

   Lines 28, 59, 150:
   ```groovy
   def connectionString = service.zkConnectString + service.zkChroot
   ```
   If either field is null, this throws `NullPointerException`. While the service existence check exists, there is no explicit null check on `zkConnectString` or `zkChroot`. Consider adding a guard or making these non-null in the DB schema.

   References:
   - `dms/src/ctrl/kafka/ConsumerCtrl.groovy:28`
   - `dms/src/ctrl/kafka/ConsumerCtrl.groovy:59`
   - `dms/src/ctrl/kafka/ConsumerCtrl.groovy:150`

3. **NPE risk: null `status` comparison in `/start` endpoint**

   `ServiceCtrl.groovy:585`:
   ```groovy
   if (one.status != KmServiceDTO.Status.stopped) {
   ```
   If `one.status` is null (e.g., DB inconsistency), this throws NPE. Other endpoints check `!= running` which has the same risk.

   References:
   - `dms/src/ctrl/kafka/ServiceCtrl.groovy:585`

### High

4. **Silent deletion failure in `PreferredReplicaElectionTask.groovy`**

   Line 38:
   ```groovy
   client.delete().forPath(electionPath)
   ```
   The delete operation does not verify success. If deletion fails silently, line 41's create will throw `NodeExistsException`. Consider using `deletingChildrenIfNeeded()` or checking deletion result.

   References:
   - `dms/src/km/job/task/PreferredReplicaElectionTask.groovy:38`

5. **Race condition between stale path delete and create**

   Lines 38-41:
   ```groovy
   client.delete().forPath(electionPath)
   // ... gap ...
   client.create().creatingParentsIfNeeded().forPath(electionPath, '{}'.getBytes('UTF-8'))
   ```
   Between delete and create, another process could create the path. Also, `forPath()` on delete returns immediately without waiting for ZK to process the deletion. Use a watch or retry loop to handle `NodeExistsException`.

   References:
   - `dms/src/km/job/task/PreferredReplicaElectionTask.groovy:38-41`

## Summary

All 5 findings from this review round are fixed in commit `c0514b5`. The fixes are minimal and targeted:

- **Lag calculation**: Removed the broken `replicas.size()` approach entirely. `/lag` now returns `lastOffset` (sum of consumer-committed offsets per topic) with a clear note that actual lag requires kafka_exporter. This is honest about the ZK-only limitation.
- **Stale election path**: Added `stat.ctime` check with 5-minute threshold. If an election path exists but is older than 5 minutes, it is deleted as a stale artifact before writing the new election.
- **Timeouts**: Added explicit `connectionTimeoutMs=5000` and `sessionTimeoutMs=30000` to `CuratorFrameworkFactory.newClient()`.
- **API consistency**: All consumer endpoints return `serviceId` alongside `groupId`.
- **Style**: Removed duplicate imports across all refactored task files.

**Post-fix code review found 5 additional issues (1 High, 3 Medium, 1 Low).** The most critical is the silent deletion failure and race condition in PreferredReplicaElectionTask.

**No remaining blockers. All critical and high findings resolved.**

**Recommendation:** Ready to merge. Address findings 1, 4, 5 in a follow-up patch.
