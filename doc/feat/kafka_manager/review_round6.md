# Kafka Manager Review Round 6

## Scope

- Review target: `doc/feat/kafka_manager/backend_design.md`
- Stage: pre-web-API design review
- Previous round: `doc/feat/kafka_manager/review_round5.md`

## Findings

1. High: the plan models JSON columns as plain `Map<String, String>` DTO fields, which does not match this repository's DB JSON serialization contract.

In this repo, the DB layer serializes embedded JSON fields when the value implements `JSONFiled`. The plan currently defines `KmServiceDTO.configOverrides` and `KmTopicDTO.configOverrides` as plain `Map<String, String>`, and `KmSnapshotContent.configOverrides` likewise as a map. That is inconsistent with both the DB layer behavior and the updated modeling guidance that says new nested/non-record structures should prefer JSON-backed value types. As written, this will either fail to persist cleanly or force ad hoc controller/service serialization logic that the plan never describes.

References:
- `doc/feat/kafka_manager/backend_design.md:41`
- `doc/feat/kafka_manager/backend_design.md:66`
- `doc/feat/kafka_manager/backend_design.md:137`
- `segmentd/src/org/segment/d/D.groovy:221`
- `segmentd/src/org/segment/d/D.groovy:233`
- `doc/web_api_code_conversion.md:344`

2. High: the plan defines `TOPIC_ALTER` and `TOPIC_DELETE` endpoints/jobs, but it never defines the task implementations or task chains needed to execute them.

The controller section says `/kafka/topic/alter` enqueues `TOPIC_ALTER` and `/kafka/topic/delete` enqueues `TOPIC_DELETE`, and `KmJobTypes` includes both job types. But the task table and task-chain section only cover `CreateTopicTask` for topic operations. There is no `AlterTopicTask`, no `DeleteTopicTask`, and no task chain for either job type. If implementation follows this plan literally, those two API endpoints will exist without executable backend behavior.

References:
- `doc/feat/kafka_manager/backend_design.md:228`
- `doc/feat/kafka_manager/backend_design.md:229`
- `doc/feat/kafka_manager/backend_design.md:276`
- `doc/feat/kafka_manager/backend_design.md:287`
- `doc/feat/kafka_manager/backend_design.md:321`
- `doc/feat/kafka_manager/backend_design.md:322`

3. Medium: `KmImportRequest` is used by the snapshot import endpoint, but the plan never defines the request model or where it lives.

The controller section says `POST /kafka/snapshot/import` takes `KmImportRequest`, but there is no DTO/request-model subsection and it is missing from the file creation summary. That leaves a gap in the API contract for one of the core stage-1 endpoints.

References:
- `doc/feat/kafka_manager/backend_design.md:338`
- `doc/kafka_manager_design.md:448`

## Summary

Backend design is not ready for implementation yet.

The implementer should not start the web API stage until the JSON-field modeling is corrected and the missing import/topic-operation execution pieces are added to the plan.

## Resolution

All three findings addressed:

1. Replaced all `Map<String, String>` JSON column fields with proper `JSONFiled` wrapper types:
   - `KmServiceDTO.configOverrides` → `KmConfigOverrides` (new JSON model)
   - `KmTopicDTO.configOverrides` → `KmConfigOverrides`
   - `KmSnapshotContent.configOverrides` → `KmConfigOverrides`
   - `KmSnapshotContent.TopicEntry.configOverrides` → `KmConfigOverrides`

2. Added missing task classes and task chains:
   - `AlterTopicTask` for `TOPIC_ALTER` (partition increase + config alter)
   - `DeleteTopicTask` for `TOPIC_DELETE` (topic deletion)
   - Both added to task table and task chain definitions

3. Defined `KmImportRequest` as a new JSON model under `dms/src/model/json/KmImportRequest.groovy` with fields `snapshotPath`, `zkConnectString`, `zkChroot`, `nodeTags`. Added to file creation summary.
