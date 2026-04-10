# DMS Admin Web Pages Code Conversion Guide

This document explains how the legacy admin frontend under `dms/www/admin/` is organized and how to convert each page safely and consistently.

## Scope

Reviewed source:

- `dms/www/admin/index.html`
- `dms/www/admin/index.js`
- `dms/www/admin/login.html`
- `dms/www/admin/pages/*/*.html`
- `dms/www/admin/pages/*/*.js`

The admin frontend contains:

- One standalone login page: `login.html`
- One SPA shell: `index.html` + `index.js`
- 27 routed page pairs under `pages/<domain>/<name>.html` and `pages/<domain>/<name>.js`

## 1. Current Frontend Architecture

### 1.1 Entry points

- `login.html` is a standalone form page with plain DOM scripting.
- `index.html` is the admin shell. It renders header, sidebar, and main content container.
- `index.js` owns:
  - sidebar menu construction
  - hash routing
  - dynamic page HTML loading
  - dynamic page JS loading
  - AngularJS bootstrap for each loaded page
  - shared helpers, directives, and filters

### 1.2 Route to file mapping

Routes use hash URLs like:

```text
#/page/cluster_overview
#/page/redis_service
#/page/setting_admin-password-reset
```

The loader converts the route key with:

```js
page = page.replace(/\_/, '/');
```

That means:

- `cluster_overview` -> `pages/cluster/overview.*`
- `redis_service` -> `pages/redis/service.*`
- `setting_admin-password-reset` -> `pages/setting/admin-password-reset.*`

Important rule:

- Only the first underscore is converted to `/`
- Hyphens in the file name are preserved
- The Angular module name must be `module_<dir>/<file>`

Example:

- route key: `redis_sentinel-service`
- HTML: `pages/redis/sentinel-service.html`
- JS: `pages/redis/sentinel-service.js`
- module name: `module_redis/sentinel-service`

### 1.3 Page loading lifecycle

Each routed page follows this runtime sequence:

1. Router matches `#/page/:page`
2. `Page.open(page)` loads `pages/<dir>/<name>.js` once via `$LAB`
3. `Page.renderContent(page)` fetches `pages/<dir>/<name>.html`
4. The HTML fragment is inserted into `#main`
5. Angular bootstraps the fragment with `angular.bootstrap(..., ['module_' + page])`

Implication for conversion:

- A legacy page is not a full HTML document
- It is an HTML fragment plus a matching JS controller module
- The shell owns route dispatch and frame layout

## 2. Legacy Page Contract

Every page pair follows the same baseline contract.

### 2.1 HTML fragment shape

Most page templates start with:

```html
<div ng-controller="MainCtrl">
```

Common structure inside:

- `container-fluid`
- `row-fluid`
- one or more `widget-box` blocks
- `widget-title`
- `widget-content`
- `table.table.table-bordered.table-striped`

Many CRUD pages also embed dialog templates directly inside the same HTML file with:

```html
<script type="text/x-template" ui-dialog="{...}">
```

### 2.2 JavaScript module shape

Most page scripts follow:

```js
var md = angular.module('module_xxx/yyy', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.editOne = {};
});
```

Common state buckets:

- `$scope.tmp`: transient UI state, filters, tab index, form refs, timestamps
- `$scope.ctrl`: booleans that show/hide dialogs
- `$scope.editOne`: the current editable record
- `$scope.confOne`: nested editable config in large forms
- `$scope.ll`: list data
- `$scope.pager`: pagination metadata

### 2.3 Shared dependencies from `base`

The `base` Angular module in `index.js` provides:

- HTTP response handling and login redirect on `403`
- validation patterns via `uiValid`
- chart directives
- filters such as `toThousands`, `shortView`, `timeAgo`

Custom directives used by pages include:

- `ui-dialog`
- `ui-tabs`
- `ui-pagi`
- `ui-valid`
- `ui-dropdown`
- `ui-date`
- `ui-chart-pie`
- `ui-chart-line`
- `ui-mind`

## 3. Page Inventory

### 3.1 Cluster pages

| Route key | Files | Notes |
|---|---|---|
| `cluster_overview` | `pages/cluster/overview.*` | dashboard, charts, dialogs, navigation |
| `cluster_list` | `pages/cluster/list.*` | CRUD dialog page |
| `cluster_deploy` | `pages/cluster/deploy.*` | multi-action operational page |
| `cluster_namespace` | `pages/cluster/namespace.*` | CRUD dialog page |
| `cluster_app` | `pages/cluster/app.*` | largest page, tabs, large nested form |
| `cluster_permit` | `pages/cluster/permit.*` | CRUD dialog page |
| `cluster_lookup` | `pages/cluster/lookup.*` | lookup/result page |
| `cluster_container` | `pages/cluster/container.*` | tabs, polling, dialogs, charts, logs |

### 3.2 Image pages

| Route key | Files | Notes |
|---|---|---|
| `image_registry` | `pages/image/registry.*` | CRUD dialog page |
| `image_volume` | `pages/image/volume.*` | CRUD dialog page with pagination |
| `image_tpl` | `pages/image/tpl.*` | CRUD dialog page with nested params |
| `image_env` | `pages/image/env.*` | CRUD dialog page |
| `image_port` | `pages/image/port.*` | CRUD dialog page |

### 3.3 Script, plugin, setting pages

| Route key | Files | Notes |
|---|---|---|
| `script_list` | `pages/script/list.*` | list + dialog + persisted keyword |
| `script_pull-log` | `pages/script/pull-log.*` | read-only list |
| `plugin_list` | `pages/plugin/list.*` | list page with prompt/confirm flows |
| `setting_admin-password-reset` | `pages/setting/admin-password-reset.*` | simple form page |

### 3.4 Redis pages

| Route key | Files | Notes |
|---|---|---|
| `redis_overview` | `pages/redis/overview.*` | refreshable summary page |
| `redis_service` | `pages/redis/service.*` | list page, navigate to detail/create |
| `redis_sentinel-service` | `pages/redis/sentinel-service.*` | CRUD dialog page |
| `redis_config-template` | `pages/redis/config-template.*` | CRUD dialog page with nested config items |
| `redis_backup-template` | `pages/redis/backup-template.*` | CRUD dialog page with conditional target fields |
| `redis_setting` | `pages/redis/setting.*` | form page |
| `redis_add` | `pages/redis/add.*` | large creation form |
| `redis_one` | `pages/redis/one.*` | detail page, actions, tabs, charts, dialogs |
| `redis_jobs` | `pages/redis/jobs.*` | detail jobs page with dialog |
| `redis_backups` | `pages/redis/backups.*` | detail backups page |

## 4. Repeated Legacy Patterns That Must Be Accounted For

### 4.1 CRUD list + dialog pattern

This is the dominant pattern in:

- `cluster/list`
- `cluster/namespace`
- `cluster/permit`
- `image/registry`
- `image/volume`
- `image/tpl`
- `image/env`
- `image/port`
- `script/list`
- `redis/config-template`
- `redis/backup-template`
- `redis/sentinel-service`

Typical behavior:

1. Load table data with `queryLl()`
2. Open dialog by setting `ctrl.isShowAdd = true`
3. Copy row data into `editOne`
4. Validate `tmp.addForm`
5. `POST` update
6. Re-query list or patch local list
7. `DELETE` after `uiTips.confirm`

Conversion rule:

- Treat the list page, dialog form, and API layer as separate units
- Do not keep the form template inline if the target stack supports real components
- Keep the same field defaults and validation gates

### 4.2 Cross-page navigation with transient params

Pages pass state via `Page.go()` and recover it with `Page.params()`.

Examples:

- `cluster/app` -> `cluster/container`
- `cluster/overview` -> `cluster/container`
- `redis/service` -> `redis/one`
- `redis/one` -> `redis/jobs`
- `redis/one` -> `redis/backups`

Conversion rule:

- Replace this with real route params, query params, or typed navigation state
- Preserve fallback behavior where the legacy page also reads from storage

### 4.3 Session storage wrappers

`index.js` defines `LocalStore`, used by several pages to preserve filters or the last selected entity.

Observed keys include:

- `script_list_keyword`
- `plugin_keyword`
- `image_volume_keyword`
- `image_tpl_keyword`
- `cluster_app_old`
- `cluster_container_old`
- `deploy_choose_node_ip`
- `redis_service_one_id`

Conversion rule:

- Inventory storage keys before converting a page
- Decide whether each key should become:
  - route state
  - query string state
  - local/session storage
  - in-memory cache
- Do not silently drop persisted behavior

### 4.4 Polling / refresh registration

Some pages register polling through:

```js
Page.registerIntervalFunc(document.location.hash, 'refreshList');
```

Used in complex operational pages such as:

- `cluster/container`
- `redis/one`

Conversion rule:

- Move polling into page-local lifecycle code
- Ensure cleanup on unmount
- Preserve refresh cadence and active-tab conditions

### 4.5 Direct DOM and dialog usage

The codebase uses jQuery DOM updates in addition to Angular bindings, for example:

- `$('#main').html(...)`
- `$('#jobLogList').html(data.str)`
- `$.dialog(...)`
- `setTimeout(...)` for dialog recentering

Conversion rule:

- Identify all direct DOM writes before converting
- Replace them with declarative rendering where possible
- If raw HTML still must be rendered, isolate and sanitize it explicitly

### 4.6 Shared chart primitives

Charts are not page-local implementations. `index.js` defines:

- `uiChartPie`
- `uiChartLine`
- `uiMind`

Pages only pass data and config via attributes.

Conversion rule:

- Recreate shared chart wrappers once
- Then migrate pages to those wrappers
- Do not hand-roll chart setup in every page

## 5. Recommended Conversion Order Per Page

For each page pair, convert in this order.

### Step 1: Inventory the legacy page

Capture:

- route key
- HTML file
- JS file
- Angular module name
- API endpoints
- navigation targets
- storage keys
- dialogs
- tabs
- charts
- pagination
- validation rules
- filters used in the template

### Step 2: Separate page responsibilities

Split the page into:

- route entry
- page container
- API client/service
- table or detail sections
- modal/dialog components
- shared form helpers

Large pages such as `cluster/app`, `cluster/container`, `cluster/deploy`, `cluster/overview`, `redis/add`, and `redis/one` should never be converted as a single giant file.

### Step 3: Port page state intentionally

Legacy scope buckets should map to named state slices.

Suggested mapping:

- `$scope.tmp` -> transient page UI state
- `$scope.ctrl` -> modal visibility state
- `$scope.editOne` -> draft entity state
- `$scope.ll` -> fetched list state
- `$scope.pager` -> pagination state

Avoid carrying forward the ambiguous `tmp` naming into the new code.

### Step 4: Port API flows

Most pages follow this request pattern:

- `GET` for initial data
- `POST` for create/update/action
- `DELETE` for removal
- `.success(...)` branch expects server-shaped success payloads

Conversion rule:

- Keep request and response shapes unchanged first
- Add response normalization in the API layer, not in the view
- Preserve user feedback timing around loading, success, and failure

### Step 5: Port validation and conditional fields

A large amount of behavior is encoded in `ui-valid`, `ng-show`, and `$watch`.

Examples:

- redis mode changes shard/replica defaults
- backup forms enable/disable fields by toggle
- app dialogs load tab-specific remote options
- nested arrays require add/remove controls

Conversion rule:

- Treat validation and conditional rendering as behavior, not just presentation
- Port the conditions before restyling the form

### Step 6: Port dialogs and tabs

Legacy dialogs are inline templates controlled by booleans in `ctrl`.

Conversion rule:

- Convert each dialog to a dedicated modal component
- Keep open/close state page-local
- Preserve per-tab lazy loading logic where the legacy page only fetches data after tab activation

### Step 7: Verify parity

Before considering a page done, verify:

- it opens from the same menu entry
- table/list loads correctly
- forms preserve defaults
- validations block invalid submission
- destructive actions still prompt
- navigation still passes required context
- stored filters/selection still work
- polling still starts and stops correctly
- charts render with equivalent data

## 6. Conversion Priorities By Complexity

### Low complexity

Good first conversions:

- `setting/admin-password-reset`
- `script/pull-log`
- `redis/overview`
- `cluster/lookup`

### Medium complexity

Convert after shared form and modal patterns exist:

- `cluster/list`
- `cluster/namespace`
- `cluster/permit`
- `image/registry`
- `image/volume`
- `image/tpl`
- `image/env`
- `image/port`
- `script/list`
- `plugin/list`
- `redis/config-template`
- `redis/backup-template`
- `redis/sentinel-service`
- `redis/service`
- `redis/jobs`
- `redis/backups`

### High complexity

Convert only after routing, storage, modal, chart, and API wrappers are settled:

- `cluster/overview`
- `cluster/deploy`
- `cluster/app`
- `cluster/container`
- `redis/add`
- `redis/one`

## 7. Page Conversion Checklist

Use this checklist for every page:

- Confirm route key to file mapping
- Confirm Angular module name
- List every API endpoint used by the page
- List dialogs and tab panels
- List storage keys read/written
- List navigation targets reached through `Page.go()`
- List polling behavior and refresh triggers
- List custom directives used in the template
- List validation rules and dynamic required fields
- Port list/detail rendering
- Port create/update/delete/action flows
- Port loading, success, and error feedback
- Verify parity with the legacy page

## 8. Special Cases

### 8.1 Login page

`login.html` is not part of the SPA shell.

It has:

- server POST form submit to `/dms/login`
- inline `check()` validation
- URL-based error display

Convert it separately from the routed admin pages.

### 8.2 Admin shell

`index.html` and `index.js` are not just another page. They define:

- application frame
- menu model
- dynamic page loader
- base Angular module
- shared directives and filters

Do not start with page conversions before deciding what replaces this shell-level runtime.

### 8.3 First underscore split rule

This is easy to miss and will break routing if ignored.

Route naming convention is:

```text
<dir>_<file-name>
```

Where:

- the first underscore becomes `/`
- the remaining characters stay unchanged

Examples:

- `cluster_container` -> `cluster/container`
- `script_pull-log` -> `script/pull-log`
- `setting_admin-password-reset` -> `setting/admin-password-reset`

## 9. Recommended Shared Building Blocks Before Bulk Conversion

Before converting many pages, create shared replacements for:

- application shell and route registry
- typed page navigation helper
- storage helper
- API client wrapper
- confirm / alert / prompt / loading UI
- modal component
- pagination component
- tabs component
- validation utilities
- chart wrappers for pie and line charts

Without these shared pieces, converted pages will drift and duplicate logic.

## 10. Practical Execution Strategy

Recommended sequence:

1. Replace or wrap shell-level routing and page mounting
2. Build shared modal, pagination, validation, chart, and storage helpers
3. Convert one low-complexity page end to end
4. Convert CRUD pages in batches by pattern
5. Convert detail/operational pages last
6. Remove legacy page loader only after all routed pages are migrated

This order matches the actual structure of `dms/www/admin/` and minimizes rework.
