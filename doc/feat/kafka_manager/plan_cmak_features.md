# CMAK Feature Parity: Preferred Replica Election + Consumer Group Inspection

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add two CMAK-inspired features to Kafka Manager: (1) preferred replica election, (2) consumer group inspection with lag visibility.

**Architecture:** Preferred replica election is a single-step async job that writes to a ZK path. Consumer group inspection is a set of synchronous read-only endpoints that shell into a broker container to run `kafka-consumer-groups.sh`. Both follow existing KM patterns (KmJobTask for jobs, ChainHandler routes for controllers).

**Tech Stack:** Groovy, Curator (ZK), AgentCaller (container exec), Kafka 2.8.2 CLI tools inside bitnami/kafka container

---

### Task 1: Add `PREFERRED_REPLICA_ELECTION` job type

**Files:**
- Modify: `dms/src/km/job/KmJobTypes.groovy`

**Step 1: Add the job type constant**

Add after the `FAILOVER` line:

```groovy
static final JobType PREFERRED_REPLICA_ELECTION = new JobType('preferred_replica_election')
```

**Step 2: Verify it compiles**

Run: `cd dms && gradle compileGroovy`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add dms/src/km/job/KmJobTypes.groovy
git commit -m "feat(km): add PREFERRED_REPLICA_ELECTION job type"
```

---

### Task 2: Implement `PreferredReplicaElectionTask`

**Files:**
- Create: `dms/src/km/job/task/PreferredReplicaElectionTask.groovy`

**Step 1: Write the task**

Follow the FailoverTask pattern — extends KmJobTask, uses Curator ZK client, single doTask() method.

```groovy
package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

@CompileStatic
@Slf4j
class PreferredReplicaElectionTask extends KmJobTask {

    PreferredReplicaElectionTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('preferred_replica_election', 0)
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString,
                new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            def electionPath = '/admin/preferred_replica_election'
            if (client.checkExists().forPath(electionPath) != null) {
                return JobResult.fail('preferred replica election already in progress')
            }

            def json = '{}'
            client.create().creatingParentsIfNeeded().forPath(electionPath, json.getBytes('UTF-8'))

            JobResult.ok('preferred replica election triggered')
        } catch (Exception e) {
            log.error('preferred replica election error', e)
            JobResult.fail('preferred replica election error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
```

**Step 2: Verify it compiles**

Run: `cd dms && gradle compileGroovy`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add dms/src/km/job/task/PreferredReplicaElectionTask.groovy
git commit -m "feat(km): add PreferredReplicaElectionTask"
```

---

### Task 3: Wire `/kafka/service/preferred-replica-election` endpoint

**Files:**
- Modify: `dms/src/ctrl/kafka/ServiceCtrl.groovy`

**Step 1: Add the endpoint**

Insert after the `/failover` endpoint block, before `/update-config`. Follow the failover endpoint pattern exactly.

```groovy
    h.post('/preferred-replica-election') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        if (one.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        if (one.mode == KmServiceDTO.Mode.standalone) {
            resp.halt(409, 'preferred replica election not supported for standalone mode')
        }

        def kmJob = new KmJob()
        kmJob.kmService = one
        kmJob.type = KmJobTypes.PREFERRED_REPLICA_ELECTION
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmServiceId', id.toString())

        kmJob.taskList << new PreferredReplicaElectionTask(kmJob)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: id]
    }
```

Also add `import km.job.task.PreferredReplicaElectionTask` if needed (the existing `import km.job.task.*` should cover it).

**Step 2: Verify it compiles**

Run: `cd dms && gradle compileGroovy`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add dms/src/ctrl/kafka/ServiceCtrl.groovy
git commit -m "feat(km): add /kafka/service/preferred-replica-election endpoint"
```

---

### Task 4: Add container exec helper to KafkaManager

**Files:**
- Modify: `dms/src/km/KafkaManager.groovy`

**Step 1: Add a static `containerExec` helper method**

Following the EtcdPlugin pattern. Add to KafkaManager class:

```groovy
static String containerExec(int clusterId, String nodeIp, String containerId, String cmd) {
    def r = server.AgentCaller.instance.agentScriptExe(clusterId, nodeIp,
            'container init', [id: containerId, initCmd: cmd])
    def message = r?.getString('message')
    if (!message) {
        throw new RuntimeException('container exec failed, cmd: ' + cmd)
    }
    message
}
```

**Step 2: Verify it compiles**

Run: `cd dms && gradle compileGroovy`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add dms/src/km/KafkaManager.groovy
git commit -m "feat(km): add containerExec helper to KafkaManager"
```

---

### Task 5: Create `KmConsumerCtrl` with consumer group endpoints

**Files:**
- Create: `dms/src/ctrl/kafka/ConsumerCtrl.groovy`

**Step 1: Write the controller**

Three synchronous GET endpoints. Uses `kafka-consumer-groups.sh` via container exec for new-style consumers, with ZK fallback path for completeness.

```groovy
package ctrl.kafka

import km.KafkaManager
import model.KmServiceDTO
import org.json.JSONObject
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import server.InMemoryAllContainerManager

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/consumer') {
    h.get('/list') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        assert serviceIdStr
        def serviceId = serviceIdStr as int

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (service.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def containerInfo = findRunningContainer(service)
        if (!containerInfo) {
            resp.halt(409, 'no running broker available')
        }

        def brokerAddr = "${containerInfo.nodeIp}:${service.port}"
        def cmd = "/opt/bitnami/kafka/bin/kafka-consumer-groups.sh --bootstrap-server ${brokerAddr} --list"

        try {
            def output = KafkaManager.containerExec(KafkaManager.CLUSTER_ID,
                    containerInfo.nodeIp, containerInfo.containerId, cmd)
            def groups = output.readLines().findAll { it.trim() && !it.startsWith('Note:') && !it.startsWith('All') }
            [list: groups]
        } catch (Exception e) {
            log.error('list consumer groups error', e)
            resp.halt(500, 'list consumer groups error: ' + e.message)
        }
    }

    h.get('/one') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        def groupId = req.param('groupId')
        assert serviceIdStr && groupId
        def serviceId = serviceIdStr as int

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (service.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def containerInfo = findRunningContainer(service)
        if (!containerInfo) {
            resp.halt(409, 'no running broker available')
        }

        def brokerAddr = "${containerInfo.nodeIp}:${service.port}"
        def cmd = "/opt/bitnami/kafka/bin/kafka-consumer-groups.sh --bootstrap-server ${brokerAddr} --describe --group ${groupId}"

        try {
            def output = KafkaManager.containerExec(KafkaManager.CLUSTER_ID,
                    containerInfo.nodeIp, containerInfo.containerId, cmd)
            parseConsumerGroupDescribe(output, groupId)
        } catch (Exception e) {
            log.error('describe consumer group error', e)
            resp.halt(500, 'describe consumer group error: ' + e.message)
        }
    }

    h.get('/lag') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        def groupId = req.param('groupId')
        assert serviceIdStr && groupId
        def serviceId = serviceIdStr as int

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (service.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def containerInfo = findRunningContainer(service)
        if (!containerInfo) {
            resp.halt(409, 'no running broker available')
        }

        def brokerAddr = "${containerInfo.nodeIp}:${service.port}"
        def cmd = "/opt/bitnami/kafka/bin/kafka-consumer-groups.sh --bootstrap-server ${brokerAddr} --describe --group ${groupId}"

        try {
            def output = KafkaManager.containerExec(KafkaManager.CLUSTER_ID,
                    containerInfo.nodeIp, containerInfo.containerId, cmd)
            parseConsumerLag(output, groupId)
        } catch (Exception e) {
            log.error('consumer lag error', e)
            resp.halt(500, 'consumer lag error: ' + e.message)
        }
    }
}

private Map<String, Object> findRunningContainer(KmServiceDTO service) {
    def instance = InMemoryAllContainerManager.instance
    def containerList = instance.getContainerList(KafkaManager.CLUSTER_ID, service.appId)
    def running = containerList.find { it.running() }
    if (!running) {
        return null
    }
    [containerId: running.id, nodeIp: running.nodeIp]
}

private Map<String, Object> parseConsumerGroupDescribe(String output, String groupId) {
    def lines = output.readLines().findAll { it.trim() && !it.startsWith('Note:') }

    if (lines.size() < 2) {
        return [groupId: groupId, partitions: [], state: 'NO_OFFSET']
    }

    def headerLine = lines[0]
    def headers = headerLine.split(/\s+/)
    def partitions = []

    for (int i = 1; i < lines.size(); i++) {
        def cols = lines[i].split(/\s+/)
        if (cols.size() >= headers.size()) {
            def entry = [:]
            for (int j = 0; j < headers.size(); j++) {
                entry[headers[j]] = cols[j]
            }
            partitions << entry
        }
    }

    [groupId: groupId, partitions: partitions]
}

private Map<String, Object> parseConsumerLag(String output, String groupId) {
    def lines = output.readLines().findAll { it.trim() && !it.startsWith('Note:') }

    if (lines.size() < 2) {
        return [groupId: groupId, totalLag: 0, topics: []]
    }

    def headerLine = lines[0]
    def headers = headerLine.split(/\s+/).toList()
    def topicIdx = headers.indexOf('TOPIC')
    def lagIdx = headers.indexOf('LAG')
    def offsetIdx = headers.indexOf('CURRENT_OFFSET')
    def logEndIdx = headers.indexOf('LOG_END_OFFSET')

    if (topicIdx < 0 || lagIdx < 0) {
        return [groupId: groupId, totalLag: -1, topics: [], raw: lines]
    }

    long totalLag = 0
    Map<String, Long> topicLag = [:]
    Map<String, Integer> topicPartitionCount = [:]

    for (int i = 1; i < lines.size(); i++) {
        def cols = lines[i].split(/\s+/)
        if (cols.size() > Math.max(topicIdx, lagIdx)) {
            def topic = cols[topicIdx]
            def lag = cols[lagIdx] as long
            totalLag += lag
            topicLag[topic] = (topicLag[topic] ?: 0L) + lag
            topicPartitionCount[topic] = (topicPartitionCount[topic] ?: 0) + 1
        }
    }

    def topics = topicLag.collect { topic, lag ->
        [topic: topic, lag: lag, partitionCount: topicPartitionCount[topic]]
    }

    [groupId: groupId, totalLag: totalLag, topics: topics]
}
```

**Step 2: Verify it compiles**

Run: `cd dms && gradle compileGroovy`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add dms/src/ctrl/kafka/ConsumerCtrl.groovy
git commit -m "feat(km): add KmConsumerCtrl with consumer group list, detail, and lag endpoints"
```

---

### Task 6: Build and verify

**Step 1: Full build**

Run: `cd dms && gradle buildToRun`
Expected: BUILD SUCCESSFUL

**Step 2: Run unit tests**

Run: `cd dms && gradle unitTest`
Expected: All tests pass (no new tests should break)

**Step 3: Commit any fixes if needed**
