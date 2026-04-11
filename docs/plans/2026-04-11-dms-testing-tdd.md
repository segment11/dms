# DMS Testing TDD Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make `dms` support a fast, deterministic TDD loop by separating unit tests from infra/manual checks, introducing test seams for external systems, and making coverage verification mandatory.

**Architecture:** Keep the existing multi-module layout, but make `dms` behave more like `velo`: one reliable fast-test entry point, explicit test categories, reusable in-memory fakes, and JaCoCo verification on touched paths. The first refactor targets build/test workflow and one infrastructure-heavy vertical slice rather than the whole module at once.

**Tech Stack:** Gradle 8, Java 21, Groovy 4, Spock 2.3, JUnit Platform, JaCoCo

---

### Task 1: Stabilize the fast test command

**Files:**
- Modify: `dms/build.gradle`

**Step 1: Write the failing test/workflow expectation**

Document the expected commands:

```bash
cd /home/kerry/ws/dms/dms
./gradlew :test --tests "rm.SlotBalancerTest"
./gradlew :test --tests "rm.SlotBalancerTest.splitAvgTest"
```

Expected:
- only `dms` tests run
- sibling module test tasks are not broken by the filter

**Step 2: Run command to verify current failure**

Run:

```bash
cd /home/kerry/ws/dms/dms
./gradlew test --tests "rm.SlotBalancerTest"
```

Expected: FAIL because `--tests` leaks into sibling tasks like `:dms_common:test`.

**Step 3: Write minimal build fix**

Adjust `dms/build.gradle` so the fast path is explicit and isolated. Prefer adding a dedicated `unitTest` task aliasing the local `test` task behavior.

**Step 4: Run focused verification**

Run:

```bash
cd /home/kerry/ws/dms/dms
./gradlew :test --tests "rm.SlotBalancerTest"
```

Expected: PASS and only local `dms` tests are relevant.

**Step 5: Commit**

```bash
git add dms/build.gradle docs/plans/2026-04-11-dms-testing-tdd.md
git commit -m "build: isolate dms fast test entry point"
```

### Task 2: Separate unit, integration, and manual test code

**Files:**
- Modify: `dms/build.gradle`
- Create: `dms/integrationTest/...`
- Create: `dms/manual/...`
- Move: `dms/test/server/lock/RemoteOneLockTest.groovy`
- Move: `dms/test/server/TestLeaderLock.groovy`
- Move: `dms/test/server/dns/TestDmsDnsServer.groovy`
- Move: `dms/test/proxy/ProxyServer.groovy`
- Move: `dms/test/push/*`
- Move: `dms/test/tools/*`

**Step 1: Write the failing classification rule**

Define:
- `test/` = deterministic automated specs only
- `integrationTest/` = real DB/ZK/network
- `manual/` = scripts and developer runners

**Step 2: Add build support**

Add an `integrationTest` source set and task. Keep it out of default `test`.

**Step 3: Move files to the right locations**

Move ad hoc scripts and infra-bound cases out of `test/`.

**Step 4: Run verification**

Run:

```bash
cd /home/kerry/ws/dms/dms
./gradlew :test
./gradlew integrationTest
```

Expected:
- `:test` is fast and deterministic
- `integrationTest` is opt-in

**Step 5: Commit**

```bash
git add dms/build.gradle dms/test dms/integrationTest dms/manual
git commit -m "refactor: split dms test types"
```

### Task 3: Standardize Spock test structure

**Files:**
- Modify: `dms/test/rm/SlotBalancerTest.groovy`
- Modify: `dms/test/GroovyStyleRefer.groovy`
- Create: `dms/test/support/...`

**Step 1: Write the failing style target**

Define that all automated specs should:
- extend `Specification`
- use descriptive string test names
- use `given/when/then`, `expect`, and `where` consistently

**Step 2: Convert one existing test to the target style**

Use `SlotBalancerTest` as the first model spec.

**Step 3: Add shared test helpers**

Create `test/support` helpers for fixtures/builders/assertions.

**Step 4: Run focused verification**

Run:

```bash
cd /home/kerry/ws/dms/dms
./gradlew :test --tests "rm.SlotBalancerTest"
```

Expected: PASS with cleaner structure.

**Step 5: Commit**

```bash
git add dms/test/rm/SlotBalancerTest.groovy dms/test/GroovyStyleRefer.groovy dms/test/support
git commit -m "test: standardize dms spock style"
```

### Task 4: Introduce test seams for external collaborators

**Files:**
- Modify: locking, DNS, proxy, Redis-related classes under `dms/src`
- Create: `dms/src/...` small adapter interfaces/factories
- Create: `dms/test/support/fake/...`

**Step 1: Write the failing test for one infra-heavy slice**

Choose one vertical slice, recommended:
- leader/lock flow first

Write a spec that asserts behavior using fake collaborators rather than real MySQL/ZooKeeper.

**Step 2: Run test to verify it fails**

Run the focused spec and confirm current code is too tightly coupled.

**Step 3: Write minimal implementation**

Extract thin seams around:
- DB lock access
- Curator/ZooKeeper client access
- DNS lifecycle
- Jetty/proxy boot
- Redis/Jedis access where needed

**Step 4: Add in-memory fakes**

Follow the `velo` pattern of first-class fake implementations for unit tests.

**Step 5: Run focused verification**

Run only the new focused spec.

Expected: PASS without external infrastructure.

**Step 6: Commit**

```bash
git add dms/src dms/test/support/fake dms/test
git commit -m "refactor: add dms test seams for infra collaborators"
```

### Task 5: Convert one real infra test into unit plus integration coverage

**Files:**
- Modify/create in the chosen subsystem
- Likely touch:
  - `dms/integrationTest/...`
  - `dms/test/...`
  - related source under `dms/src`

**Step 1: Write the unit-level failing test**

Cover business logic with fakes in `test/`.

**Step 2: Write the integration-level failing test**

Keep the real infra path in `integrationTest/` if still needed.

**Step 3: Implement minimal changes**

Keep business logic in the unit-testable layer and external wiring in adapters.

**Step 4: Run verification**

Run:

```bash
cd /home/kerry/ws/dms/dms
./gradlew :test --tests "*Lock*"
./gradlew integrationTest --tests "*Lock*"
```

Expected:
- unit spec passes quickly
- integration spec remains opt-in

**Step 5: Commit**

```bash
git add dms/src dms/test dms/integrationTest
git commit -m "test: split lock coverage into unit and integration"
```

### Task 6: Make JaCoCo a required verification step

**Files:**
- Modify: `dms/build.gradle`

**Step 1: Add coverage support**

Add `jacoco` plugin and `jacocoTestReport`.

**Step 2: Wire the workflow**

Make focused test runs followed by coverage inspection part of completion criteria.

**Step 3: Run verification**

Run:

```bash
cd /home/kerry/ws/dms/dms
./gradlew :test --tests "rm.SlotBalancerTest"
./gradlew jacocoTestReport
```

Expected:
- report is generated
- touched lines in the changed class are visibly covered

**Step 4: Commit**

```bash
git add dms/build.gradle
git commit -m "build: require jacoco verification for dms tests"
```

### Task 7: Document the testing contract

**Files:**
- Modify: `CLAUDE.md`
- Create or modify: `dms/README.md` or `dms/docs/testing.md`

**Step 1: Write documentation**

Document:
- fast test commands
- unit vs integration vs manual placement
- required Spock style
- failing fast spec first rule
- JaCoCo inspection as a hard requirement

**Step 2: Verify docs against commands**

Run the documented commands exactly.

**Step 3: Commit**

```bash
git add CLAUDE.md dms/README.md dms/docs/testing.md
git commit -m "docs: define dms testing workflow"
```

### Task 8: Final verification and commit

**Files:**
- Inspect: `dms/build/reports/jacoco/...` or configured HTML output
- Commit touched files

**Step 1: Run fast-suite verification**

```bash
cd /home/kerry/ws/dms/dms
./gradlew :test
```

**Step 2: Run integration verification**

```bash
cd /home/kerry/ws/dms/dms
./gradlew integrationTest
```

**Step 3: Inspect JaCoCo**

Confirm changed paths are actually executed.

**Step 4: Commit**

```bash
git add dms/build.gradle dms/test dms/integrationTest dms/manual CLAUDE.md dms/README.md dms/docs/testing.md docs/plans/2026-04-11-dms-testing-tdd.md
git commit -m "refactor: make dms tests tdd-friendly"
```
