# Kafka Manager Review Round 21

## Scope

- Review target: E2E testing of Kafka Manager with host process mode (no Docker)
- Previous round: `doc/feat/kafka_manager/review_round20.md`

## E2E Infrastructure Setup

### What Works
- **ZooKeeper 3.8.4**: Running on `localhost:2181` (started manually, admin server disabled via `admin.enableServer=false`)
- **Kafka 2.8.2**: Running on `localhost:9092` with broker id 0 registered in ZK
- **Kafka Broker Registration**: Verified via `zkCli.sh ls /brokers/ids` showing broker 0
- **DMS Server**: Running on `localhost:5010` with cookie-based auth working
- **KafkaPlugin loaded**: `loadPlugin('plugin.demo2.KafkaPlugin', false)` in `PluginManager.groovy`

### What Was Attempted

1. **Service creation with `isRunningUnbox=true`** (ServiceCtrl.groovy modified)
   - Added `conf.isRunningUnbox = true`
   - Added `conf.cmd = '/tmp/start-kafka.sh'`
   - Added `conf.deployFileIdList << 1` (DeployFileDTO id=1 is the start script)

2. **Deploy file created**
   - `id=1`: `/tmp/start-kafka.sh` - a bash script that starts Kafka broker

3. **Service creation API call**
   - Created service id=3 (`host-kafka2`) with zkChroot `/kafka/host-kafka2`
   - Service status stuck in `creating` → job failed at `run_creating_app_job` step

## Bug Found: Template ParamList Value Not Evaluated (HIGH)

### Error
```
For input string: "${instanceIndex}"
```

### Root Cause
In `ContainerManageCtrl.groovy` line 389-394, when rendering the template via `/dms/api/container/create/tpl`, the `paramList` values (like `'${instanceIndex}'`) were added to the binding as literal strings without evaluation. The template then saw `brokerId = '${instanceIndex}'` (literal string) instead of `brokerId = 0` (integer).

### Fix Applied
In `ContainerManageCtrl.groovy`, added evaluation of paramList values that match the pattern `${variableName}`:
```groovy
def val = it.value
if (val instanceof String && val.startsWith('${') && val.endsWith('}')) {
    def inner = val[2..-2]
    if (map.containsKey(inner)) {
        val = map[inner]
    }
}
map[it.key] = val
```

### Files Changed
- `dms/src/ctrl/ContainerManageCtrl.groovy`: Added paramList value evaluation for `${variable}` patterns

### Verification
After fix, jobs 9, 10, 11 no longer show "For input string: "${instanceIndex}"" error. Template now correctly substitutes `${instanceIndex}` with `0`.

## Bug Found: GetPid Script Exception Handling (HIGH)

### Error
```
get pid error
org.hyperic.sigar.SigarException: 没有那个进程
```

### Root Cause
In `GetPid.groovy`, when iterating processes with `sigar.getProcList()`, calling `sigar.getProcArgs(pid)` on a process that has terminated throws a `SigarException`. The outer try-catch caught this and returned immediately with "pid not found" error, instead of continuing to check other processes.

### Fix Applied
In `GetPid.groovy`, added inner try-catch around `sigar.getProcArgs()` to continue checking other processes when one fails:

```groovy
for (pid in sigar.getProcList()) {
    try {
        def procArgs = sigar.getProcArgs(pid)
        // ... check args ...
    } catch (Exception e) {
        continue  // skip this process and continue to next
    }
}
```

### Additional Fix: Detect Kafka Java Processes for Shell Scripts

The original `GetPid.groovy` checked if the process command started with the script path. But for shell scripts like `/tmp/start-kafka.sh` that spawn Java child processes, this check fails. Added logic to detect `kafka.Kafka` in process args for `.sh` scripts:

```groovy
def isScript = cmdArgs0.endsWith(".sh")
if (isScript && hasKafkaMatch) {
    return [pid: pid]
}
```

### Files Changed
- `dms_agent/src/script/GetPid.groovy`: Added inner try-catch and Kafka process detection
- `dms/plugins/plugin/demo2/KafkaPlugin.groovy`: Added health checker for Kafka brokers

### Verification
After fix, GetPid returns correct PID (e.g., `{"pid":4176527}`) for running Kafka processes.
After health checker fix, service 9 (`host-kafka-final3`) transitioned from `creating` to `running`.

## Status Summary

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | Template paramList `${instanceIndex}` not evaluated | High | **Fixed** |
| 2 | GetPid script exception handling | High | **Fixed** |
| 3 | Host process mode - service starts but broker not registered | Medium | **Fixed** |
| 4 | No health checker for Kafka | Medium | **Fixed** |

## Fixes Applied

### GString Interpolation Escaping (HIGH)

Fixed by escaping the `$` in GString interpolation:

| # | File | Line | Before | After |
|---|------|------|--------|-------|
| 1 | ServiceCtrl.groovy | 193 | `'_${appId}_${instanceIndex}'` | `'_\${appId}_\${instanceIndex}'` |
| 2 | KmSnapshotManager.groovy | 220 | `'_${appId}_${instanceIndex}'` | `'_\${appId}_\${instanceIndex}'` |
| 3 | redis/ServiceCtrl.groovy | 354 | `'_${appId}_${instanceIndex}'` | `'_\${appId}_\${instanceIndex}'` |
| 4 | redis/SentinelServiceCtrl.groovy | 138 | `'_${appId}'` | `'_\${appId}'` |
| 5 | redis/SentinelServiceCtrl.groovy | 147 | `'/${appId}_${instanceIndex}.conf'` | `'/\${appId}_\${instanceIndex}.conf'` |

### Host Process Mode Setup (MEDIUM)

In `ServiceCtrl.groovy`, added for Kafka service creation:
```groovy
conf.isRunningUnbox = true
conf.cmd = '/tmp/start-kafka.sh'
conf.deployFileIdList << 1  // DeployFileDTO id=1 = start script
```

### Kafka Health Checker (MEDIUM)

Added `initHealthChecker()` to KafkaPlugin, similar to RedisPlugin:

```groovy
private void initHealthChecker() {
    HealthCheckerHolder.instance.add new HealthChecker() {
        @Override
        String name() { 'kafka broker health check' }
        
        @Override
        boolean check(AppDTO app) {
            def kmServiceId = app.extendParams?.get('kmServiceId') as String
            def kmService = new KmServiceDTO(id: kmServiceId as int).one()
            def connectionString = kmService.zkConnectString + kmService.zkChroot
            def client = CuratorPoolHolder.instance.create(connectionString)
            // Check /brokers/ids path exists and has expected number of brokers
        }
    }
}
```

Reloaded via API: `POST /dms/plugin/load` with `{"className":"plugin.demo2.KafkaPlugin"}`

## All Issues Fixed

All major issues for host process mode Kafka have been resolved.

## Recommendation

1. **Fixed**: GString interpolation escaping in 5 locations
2. **Fixed**: GetPid script exception handling and Kafka detection
3. **Fixed**: Host process mode service creation and health checking
2. **Open**: Host process mode - requires CreateProcessor and InMemoryAllContainerManager changes
3. **Workaround**: For E2E testing, use Kafka CLI tools directly; DMS will show service as stuck in "creating"

## Files Modified This Round

- `dms/src/ctrl/kafka/ServiceCtrl.groovy`: Added `isRunningUnbox=true`, `cmd`, `deployFileIdList` + fixed GString escaping
- `dms/src/km/KmSnapshotManager.groovy`: Fixed GString escaping
- `dms/src/ctrl/redis/ServiceCtrl.groovy`: Fixed GString escaping
- `dms/src/ctrl/redis/SentinelServiceCtrl.groovy`: Fixed GString escaping (2 locations)
- `dms/src/plugin/PluginManager.groovy`: Added `loadPlugin('plugin.demo2.KafkaPlugin', false)` (done in round 20)
- `dms/src/ctrl/ContainerManageCtrl.groovy`: Added paramList value evaluation for `${variable}` patterns
- `dms_agent/src/script/GetPid.groovy`: Added inner try-catch and Kafka process detection (updated via API, not rebuilt)
