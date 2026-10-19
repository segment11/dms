# Kafka Manager Review Round 24

## Scope

- Review target: web UI stage (step 13 of `doc/kafka_manager_design.md`)
- Stage: implement all 11 Kafka pages + menu registration
- Previous round: `doc/feat/kafka_manager/review_round23.md`

## Pages Implemented

All under `dms/www/admin/pages/kafka/`, AngularJS 1.x fragments + matching controller module (route key = `kafka_<file>`, first underscore becomes `/`):

| Page | Route | Backend APIs used |
|------|-------|-------------------|
| `service.html/js` | `kafka_service` | service list/simple-list/add? no - list, simple-list, delete, stop, start, metric/init-exporters |
| `add.html/js` | `kafka_add` | service add, config-template list |
| `one.html/js` | `kafka_one` | service one, scale-up, scale-down, failover, preferred-replica-election, update-config, stop, start, delete |
| `topic.html/js` | `kafka_topic` | topic list/add/alter/delete |
| `topic-one.html/js` | `kafka_topic-one` | topic one/alter/delete/reassign |
| `consumer.html/js` | `kafka_consumer` | consumer list/lag |
| `consumer-one.html/js` | `kafka_consumer-one` | consumer one/lag |
| `jobs.html/js` | `kafka_jobs` | job list, job task/list |
| `snapshot.html/js` | `kafka_snapshot` | snapshot list/export/import/download |
| `config-template.html/js` | `kafka_config-template` | config-template list/add/update/delete |
| `overview.html/js` | `kafka_overview` | service list (dashboard computed client-side) |

Menu: `dms/www/admin/index.js` — new "Kafka" group (Overview, Kafka Service, Config Template); sub-pages reachable from the service detail page.

## Design Conformance

Implementation follows the existing Redis Manager frontend exactly — no new tech:

- Same page contract: HTML fragment + `ng-controller="MainCtrl"`, `container-fluid`/`row-fluid`/`widget-box`/`widget-title`/`widget-content`, `table table-bordered table-striped`
- Same dialog pattern: inline `<script type="text/x-template" ui-dialog="{showModel: 'ctrl.isShowAdd', ...}">` with `ng-form="tmp.xxxForm"`
- Same state buckets: `$scope.tmp` / `$scope.ctrl` / `$scope.editOne` / `$scope.ll` / `$scope.pager`
- Same request style: `$http.get(...).success(...)` / `$http.post(...)` / `$http.delete(...)` — no `.error()` handlers (base module owns 403 handling)
- Same feedback helpers only: `uiTips.loading/alert/confirm/prompt/tips`, `uiLog.i` — no `uiTips.unloading` (the shell's `Page.renderContent` already calls `$.dialog.unloading`)
- Same validation rules: `ui-valid="r"`, `ui-valid="r int minval:int:1 maxval:int:256"`, `ui-valid="int"` — text inputs, no `type="number"`
- Same polling pattern: `Page.registerIntervalFunc(document.location.hash, 'refresh')` in the detail page
- Same storage pattern: `LocalStore` keeps `kafka_service_one_id` for the detail page
- Same navigation helper: `Page.go('/page/kafka_xxx', {...})`, params via `Page.params()`
- Same helper library: vendored underscore 1.5.1 only (`_.each`, `_.clone`, `_.indexOf`) — API parity with the Redis pages verified mechanically (no Kafka-only helper APIs)

## Verification

- `node --check` on all 11 page scripts — syntax OK
- Mechanical sweep over all page pairs: module name == file name, every `ng-click` handler defined, every `ng-form` referenced by the controller, all API URLs under `/dms/kafka/`, no ES6 syntax, no Redis-only idioms
- API contract cross-check: every `$http` call in the pages resolves to a real route in `dms/src/ctrl/kafka/*.groovy` (0 broken calls; only `/kafka/snapshot/download` is used via `window.open` instead of `$http`)
- Route key -> file mapping verified against the shell loader rule (first underscore only)
- `gradle buildToRun` (dms) — successful

## Notes / Accepted Limits

- The overview dashboard is computed client-side from `/kafka/service/list` (no dedicated Kafka overview endpoint exists; the Redis equivalent has its own `/redis/overview` endpoint — adding `/kafka/overview` would be a follow-up if a real dashboard is needed)
- Consumer pages render both response shapes: `mode: 'cli'` (kafka-consumer-groups.sh output) and `mode: 'zk'` (old-style ZK consumers)
- Topic detail shows the DB-side topic record; per-partition leader/ISR is not exposed by the current API (`/kafka/topic/one` returns the KmTopicDTO) — the design mentions ISR in the topic page, which would need a backend extension

## Remaining Work

- Live browser check of all pages against a running DMS + Kafka stack (environment currently down)
- Optional backend extension: per-partition leader/ISR for the topic detail page
- Optional backend extension: dedicated `/kafka/overview` endpoint for the dashboard
