# DMS Web API Code Conversion Guide

This document explains how the backend HTTP layer under `dms/src/ctrl/` is organized today and how to convert it safely and consistently.

## Scope

Reviewed controller files:

- `dms/src/ctrl/*.groovy`
- `dms/src/ctrl/redis/*.groovy`
- `dms/src/ctrl/push/*.groovy`

Current scope:

- 27 controller scripts
- about 5209 lines of controller code

Notable large controllers:

- `dms/src/ctrl/redis/ServiceCtrl.groovy`
- `dms/src/ctrl/redis/MetricCtrl.groovy`
- `dms/src/ctrl/NodeCtrl.groovy`
- `dms/src/ctrl/ContainerManageCtrl.groovy`
- `dms/src/ctrl/AppCtrl.groovy`

## 1. Current Backend Architecture

### 1.1 Controller model

This is not a Spring-style annotated controller layer.

The backend uses `org.segment.web.handler.ChainHandler` directly:

```groovy
def h = ChainHandler.instance

h.group('/namespace') {
    h.get('/list') { req, resp -> ... }
}.post('/update') { req, resp -> ... }
```

Important implications:

- controllers are Groovy scripts, not classes with methods
- route registration happens at file load time
- `h.group(...)` builds route families
- `h.before(...)` and `h.afterAfter(...)` act like middleware
- some files define standalone routes outside any group

### 1.2 Global entry and error behavior

`dms/src/ctrl/First.groovy` provides:

- a global exception handler
- `/route/list`
- `/hz`
- `/leader/hz`

The exception handler writes status `500` and returns either `t.message` or the stack trace string. That is part of the current error contract.

### 1.3 Global auth and request filtering

`dms/src/ctrl/Filter.groovy` is the real gateway for request policy.

It defines two distinct auth layers:

- `/api/**`
  - expects `authToken` header and `clusterId` header
  - skips `/gw/`
  - validates token from cluster secret + request host
- non-`/api/**`
  - expects cookie `Auth-Token`
  - refreshes cookie TTL
  - resolves user from cache or JWT
  - injects `req.attr('user', user)`

Conversion rule:

- do not convert route handlers without first deciding how this filter behavior will be preserved
- auth, session refresh, and agent token validation are cross-cutting runtime concerns, not controller-local details

## 2. Route Families

### 2.1 Admin/UI-facing route roots

Observed route families:

- `/app`
- `/app/job`
- `/permit`
- `/cluster`
- `/container/manage`
- `/container`
- `/deploy-file`
- `/event`
- `/image/config`
- `/namespace`
- `/node`
- `/plugin`
- `/push`
- `/redis`
- `/redis/service`
- `/redis/sentinel-service`
- `/redis/config`
- `/redis/config-template`
- `/redis/backup-template`
- `/redis/backup-log`
- `/redis/job`
- `/redis/metric`

Also notable standalone admin routes:

- `/login`
- `/logout`
- `/login/user`
- `/setting/admin-password-reset`
- `/guard/toggle`
- `/manage/...`
- `/deploy/node-file/list`
- `/deploy/begin`
- `/agent/auth`
- `/agent/image/init/*`

### 2.2 Agent/internal route roots

Internal or agent-facing routes include:

- `/api/**`
- `/api/app/*`
- `/api/job/*`
- `/api/container/*`
- `/api/agent/script/pull`
- `/api/...` from `NodeCtrl`

Conversion rule:

- split public/admin routes and agent/internal routes explicitly
- they use different auth mechanisms and should not be treated as the same API surface

## 3. Common Controller Patterns

### 3.1 Request input style

Controllers mostly read input in three ways:

- query params with `req.param('x')`
- JSON body maps with `req.bodyAs(HashMap)` or `req.bodyAs()`
- typed DTO bodies with `req.bodyAs(SomeDTO)`

Typical examples:

```groovy
def id = req.param('id')
def map = req.bodyAs(HashMap)
def one = req.bodyAs(AppDTO)
```

Current validation style is usually:

- `assert` for required fields
- explicit `resp.halt(...)` for business validation

Conversion rule:

- separate syntax validation from business validation
- keep the required-field contract, but do not rely on `assert` in the new stack as the main validation mechanism
- do not create a request class only to receive a simple HTTP request with few params and no reuse elsewhere
- for simple GET/DELETE inputs, prefer `req.param(...)`
- for simple POST/PUT inputs, prefer `req.bodyAs(HashMap)` or `req.bodyAs()`
- introduce a typed request class only when the payload is larger, reused, or important enough to deserve an explicit contract

### 3.2 Response style

Current handlers return several different response shapes:

- map/list objects returned directly, usually serialized as JSON
- pagers returned directly
- plain strings like `'ok'`
- redirects with `resp.redirect(...)`
- raw text or HTML via `resp.end(...)`
- hard failures via `resp.halt(status, message)`

Examples:

- login redirects to `/admin/index.html` or `/admin/login.html?error=1`
- `AppJobCtrl` returns rendered HTML fragments in `data.str`
- `ManageCtrl` returns HTML for `/manage/sql/set/view`
- `EventCtrl` proxies remote event endpoints with raw string bodies

Conversion rule:

- inventory endpoint response types before changing them
- several endpoints are not JSON APIs even though the frontend treats most routes as JSON

### 3.3 Status code convention

Observed status usage:

- `403` for auth/permission problems
- `404` for missing resources
- `409` for validation/conflict/state errors
- `500` for operational failure or legacy generic failure

Conversion rule:

- preserve these codes first
- only normalize later with explicit compatibility review

### 3.4 DTO-driven persistence

Most controllers use DTOs directly as the data access layer:

- `one()`
- `list()`
- `listPager()`
- `add()`
- `update()`
- `delete()`
- `queryFields(...)`
- `where(...)`
- `whereIn(...)`
- `noWhere()`
- `orderBy(...)`

Examples:

```groovy
new NamespaceDTO(clusterId: clusterId as int).listPager(pageNum, pageSize)
new RmServiceDTO(id: id).one()
new ImageRegistryDTO().noWhere().list()
```

Conversion rule:

- controllers currently mix HTTP, validation, and persistence directly
- when converting, move DTO query logic behind services/repositories first if possible
- preserve query semantics such as `queryFields`, paging defaults, and ad hoc filters

### 3.5 Inline permission checks

Permissions are checked inside handlers with `User` methods such as:

- `isAdmin()`
- `isImageManager()`
- `isAccessCluster(id)`
- `isAccessNamespace(id)`
- `isAccessApp(id)`

Some route trees also add a `before` hook, for example `PluginCtrl` and `ImageConfCtrl`.

Conversion rule:

- centralize authorization by capability/resource when rewriting
- but preserve the exact resource-scoped checks before changing behavior

### 3.6 Database CRUD

The code under `dms/src/model/**/*` is not the whole application DTO layer. It is mostly the Redis manager's model layer plus embedded/support classes used by that subsystem.

Important boundary:

- persistent CRUD base lives in `dms_common/src/model/BaseRecord.groovy`
- `dms/src/model/**/*` builds Redis-specific records and embedded value objects on top of that base
- many non-Redis DTOs used by controllers, such as `AppDTO`, `ClusterDTO`, `NamespaceDTO`, `NodeDTO`, live in `dms_common/src/model/*`

#### 3.6.1 Shared CRUD base

`BaseRecord<V>` extends `org.segment.d.Record<V>` and defines:

- primary key name as `id`
- datasource selection through `dms_server_ds`
- dialect switching between MySQL and PostgreSQL

That base is the reason the DTOs in `dms/src/model` support the same fluent CRUD/query API used elsewhere:

- `one()`
- `list()`
- `listPager()`
- `add()`
- `update()`
- `delete()`
- `deleteAll()`
- `queryFields(...)`
- `where(...)`
- `whereIn(...)`
- `noWhere()`
- `orderBy(...)`

Conversion rule:

- document the `BaseRecord` contract before rewriting any DTO call sites
- if moving to repositories or ORM entities, preserve query semantics first, especially `queryFields`, pager defaults, and `deleteAll`

#### 3.6.2 Persistent record classes in `dms/src/model`

These are the actual database CRUD records under `dms/src/model`.

| Class | Role | Main usage sites |
|---|---|---|
| `model.RmServiceDTO` | Redis service record | `ctrl/redis/ServiceCtrl.groovy`, `ConfigCtrl.groovy`, `MainCtrl.groovy`, `MetricCtrl.groovy`, `JobCtrl.groovy`, `SentinelServiceCtrl.groovy`, `rm/BackupManager.groovy`, `rm/RedisManager.groovy`, `rm/job/task/*` |
| `model.RmSentinelServiceDTO` | Sentinel cluster record | `ctrl/redis/SentinelServiceCtrl.groovy`, `ServiceCtrl.groovy`, `rm/RedisManager.groovy`, `rm/job/RmJob.groovy` |
| `model.RmConfigTemplateDTO` | Redis config template record | `ctrl/redis/ConfigTemplateCtrl.groovy`, `ServiceCtrl.groovy` |
| `model.job.RmJobDTO` | Redis async job header record | `ctrl/redis/JobCtrl.groovy`, `rm/job/RmJob.groovy` |
| `model.job.RmTaskLogDTO` | Redis async job step log record | `ctrl/redis/JobCtrl.groovy`, `rm/job/RmJobTask.groovy`, `rm/job/RmTaskLog.groovy` |
| `model.job.RmBackupTemplateDTO` | Backup target/template record | `ctrl/redis/BackupTemplateCtrl.groovy`, `ServiceCtrl.groovy`, `rm/BackupManager.groovy` |
| `model.job.RmBackupLogDTO` | Backup execution record | `ctrl/redis/BackupLogCtrl.groovy`, `rm/BackupManager.groovy` |

The main persistent model in this tree is `RmServiceDTO`.

It is not just a schema holder. It also contains domain logic such as:

- status transitions
- computed listen port rules
- Redis connection helpers
- running container lookup
- node/cluster validation logic

Conversion rule:

- do not blindly map `RmServiceDTO` to a passive entity
- separate persistence concerns from domain/runtime methods deliberately
- identify whether each method belongs in:
  - repository
  - domain service
  - infrastructure adapter
  - entity/value object

#### 3.6.3 Embedded JSON/value classes in `dms/src/model/json`

These classes implement `org.segment.d.json.JSONFiled` and are stored as embedded JSON fields inside the persistent records rather than separate tables.

| Class | Stored in / used by | Main usage sites |
|---|---|---|
| `BackupPolicy` | `RmServiceDTO.backupPolicy` | `ctrl/redis/ServiceCtrl.groovy`, `rm/BackupManager.groovy` |
| `ClusterSlotsDetail` | `RmServiceDTO.clusterSlotsDetail` | `ctrl/redis/ServiceCtrl.groovy`, `JobCtrl.groovy`, `MainCtrl.groovy`, `rm/job/task/*` |
| `PrimaryReplicasDetail` | `RmServiceDTO.primaryReplicasDetail` | `ctrl/redis/ServiceCtrl.groovy`, `JobCtrl.groovy`, `rm/job/task/*` |
| `ConfigItems` | `RmConfigTemplateDTO.configItems` | `ctrl/redis/ConfigTemplateCtrl.groovy` |
| `LogPolicy` | `RmServiceDTO.logPolicy` | `RmServiceDTO` and Redis manager flows |
| `TargetBucket` | nested in `RmBackupTemplateDTO.targetBucket` | `ctrl/redis/BackupTemplateCtrl.groovy`, `rm/BackupManager.groovy` |

Important characteristic:

- these are not CRUD records themselves
- they shape the JSON columns and drive controller/runtime logic directly

Conversion rule:

- treat JSON-backed types as part of the database contract
- preserve their serialized field names and nesting first
- for new feature modeling, prefer a JSON field when a structure is not clearly a business DTO/persistent record
- for new feature modeling, prefer a JSON field when a structure has many nested properties and would otherwise create a noisy or low-value table split
- for generic key-value JSON payloads, prefer reusing `dms_common/src/model/json/ExtendParams.groovy` before creating a new wrapper type
- for config template item lists, log-policy structures, or similar existing shapes, prefer reusing existing `JSONFiled` types such as `ConfigItems`, `LogPolicy`, `BackupPolicy`, `PrimaryReplicasDetail`, or `ClusterSlotsDetail` when the semantics already match
- normalize them into separate tables only with explicit migration planning

#### 3.6.4 Cluster support classes in `dms/src/model/cluster`

These are helper/domain classes, not `BaseRecord` CRUD records:

- `ClusterNode`
- `MessageReader`
- `MultiSlotRange`
- `SlotNode`
- `SlotRange`

They are used to model Redis cluster topology and slot ownership, especially in:

- `model.json.ClusterSlotsDetail`
- `ctrl/redis/JobCtrl.groovy`
- `rm/job/task/MigrateSlotsTask.groovy`
- `rm/job/task/MeetNodesWhenScaleUpTask.groovy`
- `rm/job/task/WaitClusterStateTask.groovy`
- other Redis cluster job tasks under `dms/src/rm/job/task/*`

`MultiSlotRange` and `SlotRange` contain real behavior:

- set/range conversion
- merge/remove operations
- total slot calculations
- cluster node argument generation
- conversion from Redis `clusterSlots()` output

Conversion rule:

- do not misclassify these as database entities
- they are domain support types used by job orchestration and cluster-state reconciliation

#### 3.6.5 Reuse Existing Backend Patterns First

Before introducing a new backend abstraction, check whether an existing Redis-manager or plugin pattern already fits with renaming and small adaptations.

Good reuse candidates in this repo:

- `dms/src/rm/job/RmJob.groovy`, `dms/src/rm/job/RmJobTask.groovy`, `dms/src/rm/job/RmTaskLog.groovy`, `dms/src/rm/RmJobExecutor.groovy` for async job/task execution patterns
- `dms/src/plugin/BasePlugin.groovy` for DMS plugin lifecycle, template registration, and checker integration
- existing DTO placement patterns under `dms/src/model/` and `dms/src/model/job/`

Conversion rule:

- prefer reusing an existing pattern with feature-specific naming over creating a parallel framework with slightly different behavior
- if a new feature diverges from Redis-manager patterns, document the reason explicitly in the design doc before implementation

#### 3.6.6 How the CRUD models are used today

The persistent classes under `dms/src/model` are used in two layers:

- controller layer
  - mostly `dms/src/ctrl/redis/*`
  - list, detail, update, delete, validation, and conflict checks
- runtime/job layer
  - `dms/src/rm/*`
  - backup execution, service orchestration, sentinel/cluster reconciliation, failover, scale up/down, task logging

This means the same DTO often acts as:

- database record
- serialization shape
- domain object
- runtime state holder

That coupling is strongest for:

- `RmServiceDTO`
- `RmBackupTemplateDTO`
- `RmBackupLogDTO`
- `RmJobDTO` and `RmTaskLogDTO`

Conversion rule:

- split read/write persistence concerns from orchestration logic gradually
- if you move controller code first but keep the runtime layer unchanged, preserve the current DTO contract at the boundary
- if you redesign the model layer, convert `ctrl/redis/*` and `rm/*` together because they share the same record types directly

#### 3.6.6 Practical migration guidance for this model tree

- Inventory which classes are true records versus embedded JSON types versus helper/domain classes.
- Keep `BaseRecord` query behavior compatible before replacing it.
- Migrate `RmServiceDTO` last within this tree, because it is the highest-coupled record.
- Treat `ctrl/redis/*` and `rm/*` as a shared migration unit for all `Rm*` classes.
- Avoid splitting JSON-backed fields like `ClusterSlotsDetail` or `PrimaryReplicasDetail` into separate persistence models unless you also migrate the Redis job/task code that mutates them.
- Verify both controller usage and runtime/job usage for each model change, not just compile success.

#### 3.6.7 Prefer multi-step DTO queries over join-table SQL

A recurring pattern in the current codebase is:

- query the primary DTO first
- query related DTOs in separate steps
- connect them in Groovy with `find`, `collect`, `groupBy`, `transfer`, or `whereIn`

When using `BaseRecord` subclasses, the existing code usually does not prefer hand-written join-table SQL.

Representative examples:

- permit enrichment in [AuthCtrl.groovy](/home/kerry/ws/dms/dms/src/ctrl/AuthCtrl.groovy)
  - first query `UserPermitDTO` pager
  - then query `ClusterDTO` and `NamespaceDTO` lists
  - then resolve `resourceName` in memory
  - then query `AppDTO` with `whereIn` for app permits
- app option assembly in [AppCtrl.groovy](/home/kerry/ws/dms/dms/src/ctrl/AppCtrl.groovy)
  - separate queries for `ClusterDTO`, `NamespaceDTO`, `ImageRegistryDTO`, `NodeDTO`, `AppDTO`, `DeployFileDTO`
  - then compute derived response fields in memory
- deploy node/file response building in [DeployCtrl.groovy](/home/kerry/ws/dms/dms/src/ctrl/DeployCtrl.groovy)
  - first page `NodeKeyPairDTO`
  - then query matching `NodeDTO` rows with `whereIn`
  - then merge tags and heartbeat state in `pager.transfer`
- container list enrichment in [ContainerManageCtrl.groovy](/home/kerry/ws/dms/dms/src/ctrl/ContainerManageCtrl.groovy)
  - get container runtime state first
  - then query `AppDTO`
  - then attach app metadata to each container in memory

This style shows up for both admin CRUD flows and runtime/orchestration code.

Why the codebase does this:

- it keeps each query anchored on one DTO/table
- it reuses `BaseRecord` methods consistently
- it avoids pushing complex cross-table coupling into ad hoc SQL strings
- it lets the code mix database rows with runtime state before assembling the final response

Conversion rule:

- when converting `BaseRecord`-based code, prefer preserving this multi-step DTO pattern first
- do not introduce join-table SQL just to compress the code unless there is a measured need and the behavior is unchanged
- if a response needs related data from multiple tables, prefer:
  1. query the main record set
  2. batch-fetch related records with additional DTO queries, often `whereIn`
  3. enrich/merge in memory
- only replace this with joins after verifying:
  - field coverage is identical
  - permission filtering stays correct
  - runtime-derived fields still work
  - pagination semantics do not change

Practical guidance:

- use joins cautiously around paged endpoints, because the current code often pages the main table first and enriches afterward
- keep `whereIn` plus in-memory merge as the default migration strategy when porting `BaseRecord` CRUD code
- treat raw SQL joins as an optimization or redesign step, not the default translation target
- for performance, prefer one batched `whereIn(...)` query over doing `one()` or small `list()` queries inside a `for` loop
- if related rows must be loaded for many parent records, collect the ids first, query once with `whereIn`, then map in memory

## 4. Repeated API Patterns That Must Be Accounted For

### 4.1 Standard CRUD pattern

Many routes follow this exact shape:

- `GET /list`
- `GET /list/simple`
- `POST /update`
- `DELETE /delete`

Seen in:

- `/namespace`
- `/permit`
- `/image/config/*`
- `/deploy-file`
- `/redis/config-template`
- `/redis/backup-template`
- `/redis/backup-log`
- `/redis/sentinel-service`
- parts of `/redis/service`

Typical rules:

- page size defaults to `10`
- keyword filters use SQL `like`
- create/update share the same endpoint
- create returns `[id: newId]`
- delete returns `[flag: true]`

Conversion rule:

- treat this as the baseline compatibility contract
- if splitting create and update into separate endpoints, provide a compatibility layer or migrate callers together

### 4.2 Operational action endpoints

A large part of the API is action-oriented rather than CRUD-oriented.

Examples:

- `/container/manage/start`
- `/container/manage/stop`
- `/container/manage/remove`
- `/node/agent/init`
- `/node/agent/start`
- `/deploy/begin`
- `/redis/service/cluster-scale-up`
- `/redis/service/failover`
- `/redis/metric/init-exporters`

These endpoints often:

- mutate remote infrastructure
- call agents
- enqueue jobs
- return step logs or boolean flags

Conversion rule:

- separate “resource CRUD” from “command/action” APIs in the new design
- actions usually deserve explicit request/response types and better job tracking

### 4.3 Async job pattern

There are two major async-job styles:

- app jobs under `/app/job`
- Redis manager jobs under `/redis/job`

Patterns observed:

- list jobs
- list task logs
- create jobs from action endpoints
- execute asynchronously through executors such as `RmJobExecutor`
- frontend polls or refreshes job lists

Conversion rule:

- standardize job creation, status, and log retrieval if you redesign
- but preserve job lifecycle semantics first, especially for long-running infra operations

### 4.4 Side effects on GET

The current API uses `GET` for many mutating operations.

Examples include:

- `/guard/toggle`
- `/cluster/guard/toggle`
- `/node/tag/update`
- `/manage/conf/set`
- `/plugin/demo/create`
- `/api/app/scale`
- debug/status update endpoints under `/redis/job`

Conversion rule:

- mark these explicitly during migration
- if you normalize them to `POST` or `PATCH`, you must update all callers and any automation depending on the old method

### 4.5 Mixed JSON and rendered HTML/text

Not every endpoint is machine-clean JSON.

Examples:

- `AppJobCtrl` builds HTML strings for job log tables
- `ManageCtrl` returns HTML pages
- `AuthCtrl` redirects instead of returning JSON
- `EventCtrl` sometimes returns remote raw string payloads
- some endpoints return plain `'ok'`

Conversion rule:

- classify each endpoint as one of:
  - JSON resource API
  - JSON action API
  - redirect endpoint
  - raw text endpoint
  - HTML endpoint
  - proxy/pass-through endpoint

## 5. Important Runtime Dependencies Hidden Inside Controllers

The controllers are thin only in appearance. They rely on a lot of runtime state:

- `InMemoryCacheSupport`
- `InMemoryAllContainerManager`
- `Guardian`
- `AgentCaller`
- `PluginManager`
- `RedisManager`
- `RmJobExecutor`
- `AuthTokenCacheHolder`

This means many handlers are coupled to:

- in-memory cluster/app caches
- live node heartbeat state
- scheduler state
- agent reachability
- plugin lifecycle
- remote command execution

Conversion rule:

- do not treat controllers as isolated HTTP adapters
- model these dependencies explicitly in the new architecture before moving route code

## 6. Special Backend Cases

### 6.1 Auth and login routes

`AuthCtrl.groovy` is not a simple auth API.

It contains:

- cookie/JWT login for admin UI
- password reset
- current-user lookup
- logout
- agent auth token exchange
- permit management

It mixes:

- redirect flows
- session issuance
- admin-only settings
- resource CRUD

Convert this area carefully and split responsibilities if possible.

### 6.2 Filter middleware

`Filter.groovy` is effectively part of the API contract.

It decides:

- which routes need login
- how `/api/**` auth works
- which paths are skipped
- how request user context is injected

Any rewrite that ignores this file will break behavior even if the route handlers are ported perfectly.

### 6.3 Redis manager family

The Redis controllers under `dms/src/ctrl/redis/` form a subsystem, not just a few endpoints.

They include:

- overview/settings
- service lifecycle
- sentinel service lifecycle
- templates
- backups
- metrics
- job orchestration
- live config mutation

This family is the largest backend API surface and should be migrated as a coherent module.

### 6.4 Push API family

`push/PushCtrl.groovy` behaves more like a command/event bridge than CRUD.

Patterns:

- long-poll style fetch of pending events
- push-to-client with timeout
- completion callback
- client registration/update

It should not be merged into normal REST CRUD migration work without explicit design.

## 7. Recommended Conversion Unit

Do not convert file by file blindly.

Use these units instead:

### Unit A: platform/runtime

- `First.groovy`
- `Filter.groovy`
- auth/session/token infrastructure
- exception mapping

### Unit B: admin CRUD APIs

- namespace
- permit
- image config
- deploy file
- plugin list/load/delete

### Unit C: cluster and node operations

- cluster
- node
- deploy
- container manage
- app
- app job
- event

### Unit D: Redis subsystem

- `redis/*`

### Unit E: push/agent/internal APIs

- `push/*`
- `/api/**`
- agent sync/pull endpoints

This grouping matches the real coupling in the codebase better than a per-file migration.

## 8. Recommended Conversion Order

1. Recreate middleware behavior from `First.groovy` and `Filter.groovy`
2. Define shared request validation, auth, and error-mapping conventions
3. Extract DTO/database logic behind services or repositories
4. Convert low-risk CRUD route families
5. Convert operational action endpoints with job handling
6. Convert Redis subsystem as its own bounded migration
7. Convert push and agent/internal APIs last or as a dedicated track

## 9. Per-Endpoint Conversion Checklist

For every endpoint, capture:

- HTTP method
- full path
- whether it is admin UI, internal `/api/**`, or push/agent traffic
- auth source: cookie/JWT, route `before`, or header token
- query params
- body type: map or DTO
- response type: JSON map/list, pager, text, HTML, redirect, raw pass-through
- status codes used
- DTOs read/written
- cache/runtime dependencies touched
- side effects on remote systems, jobs, or in-memory state

Do not move an endpoint until that checklist is complete.

## 10. Practical Rules For Conversion

- Preserve route paths first. Path changes should be deliberate and separately tracked.
- Preserve response status codes first. Several frontend pages depend on them.
- Preserve permission semantics first. Inline user checks are part of behavior.
- Preserve mixed response formats first. Some endpoints are intentionally not JSON.
- Isolate remote execution calls. `AgentCaller` and job executors deserve adapters.
- Replace `assert`-style validation with explicit input validation, but keep the same required fields and constraints.
- Identify every GET endpoint with side effects and decide whether to preserve or normalize it.
- Treat `listPager()` defaults as API behavior, not an implementation detail.
- Split admin web APIs from internal `/api/**` APIs early.

## 11. Highest-Risk Areas

Most conversion risk sits in:

- middleware and auth flow
- Redis service lifecycle and metrics
- container/node/deploy operational endpoints
- HTML/text endpoints disguised as APIs
- GET endpoints that mutate state
- endpoints that depend on in-memory runtime state instead of only the database

Those areas should be reviewed first before any broad mechanical rewrite.
