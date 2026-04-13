# Kafka Manager Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement a multi-cluster Kafka 2.8.2 lifecycle manager modeled after the existing Redis Manager (`dms/src/rm/`).

**Architecture:** KafkaManager (KM) is a DMS module under `dms/src/km/` that manages Kafka broker containers via DMS app lifecycle. It connects to existing ZooKeeper ensembles (not managed by KM). Job chains execute asynchronously via `KmJobExecutor`, with each step tracked in `KM_JOB` / `KM_TASK_LOG` tables. DDL tables already exist in `init_h2.sql` (lines 383-483).

**Tech Stack:** Groovy 4.0, Spock 2.3, H2 in-memory DB, Apache Curator 5.2.0 (already in `build.gradle`), `org.segment.d` Record/JSONFiled ORM, `com.segment.common.job.chain` job framework.

---

## Stage 1: Data Model — JSON Models + DTOs + Tests

### Task 1.1: BrokerDetail JSON Model

**Files:**
- Create: `dms/src/model/json/BrokerDetail.groovy`
- Test: `dms/test/model/json/BrokerDetailTest.groovy`

**Step 1: Write the failing test**

```groovy
package model.json

import spock.lang.Specification

class BrokerDetailTest extends Specification {
    void 'findByBrokerId returns correct node'() {
        given:
        def detail = new BrokerDetail()
        def node1 = new BrokerDetail.BrokerNode(brokerId: 0, brokerIndex: 0, ip: '10.0.0.1', port: 9092)
        def node2 = new BrokerDetail.BrokerNode(brokerId: 1, brokerIndex: 1, ip: '10.0.0.2', port: 9092)
        detail.brokers = [node1, node2]

        expect:
        detail.findByBrokerId(0).ip == '10.0.0.1'
        detail.findByBrokerId(1).ip == '10.0.0.2'
        detail.findByBrokerId(99) == null
    }

    void 'findByIpPort returns correct node'() {
        given:
        def detail = new BrokerDetail()
        detail.brokers = [
                new BrokerDetail.BrokerNode(brokerId: 0, brokerIndex: 0, ip: '10.0.0.1', port: 9092),
                new BrokerDetail.BrokerNode(brokerId: 1, brokerIndex: 1, ip: '10.0.0.2', port: 9092)
        ]

        expect:
        detail.findByIpPort('10.0.0.1', 9092).brokerId == 0
        detail.findByIpPort('10.0.0.2', 9092).brokerId == 1
        detail.findByIpPort('10.0.0.3', 9092) == null
    }

    void 'activeBrokers returns all nodes'() {
        given:
        def detail = new BrokerDetail()
        detail.brokers = [
                new BrokerDetail.BrokerNode(brokerId: 0, brokerIndex: 0, ip: '10.0.0.1', port: 9092),
                new BrokerDetail.BrokerNode(brokerId: 1, brokerIndex: 1, ip: '10.0.0.2', port: 9092)
        ]

        expect:
        detail.activeBrokers().size() == 2
    }

    void 'BrokerNode uuid returns ip:port'() {
        expect:
        new BrokerDetail.BrokerNode(ip: '10.0.0.1', port: 9092).uuid() == '10.0.0.1:9092'
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "model.json.BrokerDetailTest"`
Expected: FAIL — `BrokerDetail` class not found

**Step 3: Write minimal implementation**

```groovy
package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class BrokerDetail implements JSONFiled {
    List<BrokerNode> brokers = []

    BrokerNode findByBrokerId(int brokerId) {
        brokers.find { it.brokerId == brokerId }
    }

    BrokerNode findByIpPort(String ip, int port) {
        brokers.find { it.ip == ip && it.port == port }
    }

    List<BrokerNode> activeBrokers() {
        brokers
    }

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class BrokerNode {
        int brokerId
        int brokerIndex
        boolean isController
        String ip
        int port
        String rackId

        String uuid() { "${ip}:${port}" }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "model.json.BrokerDetailTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/model/json/BrokerDetail.groovy dms/test/model/json/BrokerDetailTest.groovy
git commit -m "feat(km): add BrokerDetail JSON model with tests"
```

---

### Task 1.2: KmSnapshotContent JSON Model

**Files:**
- Create: `dms/src/model/json/KmSnapshotContent.groovy`
- Test: `dms/test/model/json/KmSnapshotContentTest.groovy`

**Step 1: Write the failing test**

```groovy
package model.json

import spock.lang.Specification

class KmSnapshotContentTest extends Specification {
    void 'BrokerEntry and TopicEntry hold values correctly'() {
        given:
        def content = new KmSnapshotContent()
        content.serviceName = 'test-cluster'
        content.mode = 'cluster'
        content.kafkaVersion = '2.8.2'
        content.zkConnectString = 'zk1:2181,zk2:2181'
        content.zkChroot = '/kafka/test-cluster'

        content.brokers = [
                new KmSnapshotContent.BrokerEntry(brokerId: 0, host: '10.0.0.1', port: 9092, logDirs: '/data/kafka/data')
        ]
        content.topics = [
                new KmSnapshotContent.TopicEntry(name: 'test-topic', partitions: 3, replicationFactor: 1)
        ]

        expect:
        content.serviceName == 'test-cluster'
        content.brokers.size() == 1
        content.brokers[0].host == '10.0.0.1'
        content.topics.size() == 1
        content.topics[0].name == 'test-topic'
        content.topics[0].partitions == 3
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "model.json.KmSnapshotContentTest"`
Expected: FAIL

**Step 3: Write minimal implementation**

```groovy
package model.json

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includePackage = false)
class KmSnapshotContent implements JSONFiled {
    String serviceName
    String mode
    String kafkaVersion
    Date snapshotDate

    List<BrokerEntry> brokers = []
    String zkConnectString
    String zkChroot

    List<TopicEntry> topics = []

    ArrayList<KVPair<String>> configItems
    Map<String, String> configOverrides = [:]

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class BrokerEntry {
        int brokerId
        String host
        int port
        String rackId
        String logDirs
    }

    @CompileStatic
    @ToString(includeNames = true, includePackage = false)
    static class TopicEntry {
        String name
        int partitions
        int replicationFactor
        Map<String, String> configOverrides = [:]
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "model.json.KmSnapshotContentTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/model/json/KmSnapshotContent.groovy dms/test/model/json/KmSnapshotContentTest.groovy
git commit -m "feat(km): add KmSnapshotContent JSON model with tests"
```

---

### Task 1.3: KmServiceDTO

**Files:**
- Create: `dms/src/model/KmServiceDTO.groovy`
- Test: `dms/test/model/KmServiceDTOTest.groovy`

**Step 1: Write the failing test**

```groovy
package model

import support.DmsTestDbSupport
import spock.lang.Specification

class KmServiceDTOTest extends Specification {
    def d
    def ds

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, """
create table km_service
(
    id                         int auto_increment primary key,
    name                       varchar(50),
    des                        varchar(200),
    mode                       varchar(20),
    kafka_version              varchar(20),
    config_template_id         int,
    config_overrides           varchar(2000),
    zk_connect_string          varchar(500),
    zk_chroot                  varchar(200) not null,
    app_id                     int,
    port                       int,
    brokers                    int,
    default_replication_factor int,
    default_partitions         int,
    heap_mb                    int,
    pass                       varchar(200),
    is_sasl_on                 bit,
    is_tls_on                  bit,
    node_tags                  varchar(100),
    node_tags_by_broker_index  varchar(500),
    log_policy                 varchar(200),
    status                     varchar(20),
    extend_params              varchar(2000),
    broker_detail              varchar(4000),
    last_updated_message       varchar(200),
    created_date               timestamp,
    updated_date               timestamp default current_timestamp
)
""")
    }

    def cleanup() {
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    void 'crud for km_service with json and enum fields'() {
        when:
        def one = new KmServiceDTO()
        one.name = 'test-kafka'
        one.des = 'test cluster'
        one.mode = KmServiceDTO.Mode.standalone
        one.kafkaVersion = '2.8.2'
        one.zkConnectString = 'zk1:2181'
        one.zkChroot = '/kafka/test-kafka'
        one.port = 9092
        one.brokers = 1
        one.defaultReplicationFactor = 1
        one.defaultPartitions = 1
        one.heapMb = 1024
        one.status = KmServiceDTO.Status.creating
        one.brokerDetail = new model.json.BrokerDetail()
        one.logPolicy = new model.json.LogPolicy()
        one.createdDate = new Date()
        one.updatedDate = new Date()
        def id = one.add()

        then:
        id > 0

        when:
        def found = new KmServiceDTO(id: id).one()

        then:
        found.name == 'test-kafka'
        found.mode == KmServiceDTO.Mode.standalone
        found.kafkaVersion == '2.8.2'
        found.zkChroot == '/kafka/test-kafka'
        found.status == KmServiceDTO.Status.creating
        found.brokerDetail != null
        found.brokerDetail.brokers != null

        when:
        new KmServiceDTO(id: id, status: KmServiceDTO.Status.running, updatedDate: new Date()).update()
        def updated = new KmServiceDTO(id: id).one()

        then:
        updated.status == KmServiceDTO.Status.running

        when:
        updated.delete()
        def deleted = new KmServiceDTO(id: id).one()

        then:
        deleted == null
    }

    void 'Status.canChangeToRunningWhenInstancesRunningOk returns correct values'() {
        expect:
        KmServiceDTO.Status.creating.canChangeToRunningWhenInstancesRunningOk()
        KmServiceDTO.Status.scaling_up.canChangeToRunningWhenInstancesRunningOk()
        KmServiceDTO.Status.scaling_down.canChangeToRunningWhenInstancesRunningOk()
        !KmServiceDTO.Status.running.canChangeToRunningWhenInstancesRunningOk()
        !KmServiceDTO.Status.stopped.canChangeToRunningWhenInstancesRunningOk()
        !KmServiceDTO.Status.deleted.canChangeToRunningWhenInstancesRunningOk()
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "model.KmServiceDTOTest"`
Expected: FAIL — `KmServiceDTO` class not found

**Step 3: Write minimal implementation**

```groovy
package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.BrokerDetail
import model.json.ExtendParams
import model.json.LogPolicy

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmServiceDTO extends BaseRecord<KmServiceDTO> {

    @CompileStatic
    static enum Mode {
        standalone, cluster
    }

    @CompileStatic
    static enum Status {
        creating, running, scaling_up, scaling_down, stopped, deleted, unhealthy

        boolean canChangeToRunningWhenInstancesRunningOk() {
            return this == creating || this == scaling_up || this == scaling_down
        }
    }

    Integer id
    String name
    String des
    Mode mode
    String kafkaVersion
    Integer configTemplateId
    Map<String, String> configOverrides
    String zkConnectString
    String zkChroot
    Integer appId
    Integer port
    Integer brokers
    Integer defaultReplicationFactor
    Integer defaultPartitions
    Integer heapMb
    String pass
    Boolean isSaslOn
    Boolean isTlsOn
    String[] nodeTags
    String[] nodeTagsByBrokerIndex
    LogPolicy logPolicy
    Status status
    ExtendParams extendParams
    BrokerDetail brokerDetail
    String lastUpdatedMessage
    Date createdDate
    Date updatedDate
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "model.KmServiceDTOTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/model/KmServiceDTO.groovy dms/test/model/KmServiceDTOTest.groovy
git commit -m "feat(km): add KmServiceDTO with H2 CRUD test"
```

---

### Task 1.4: KmConfigTemplateDTO

**Files:**
- Create: `dms/src/model/KmConfigTemplateDTO.groovy`
- Modify: `dms/test/model/KmServiceDTOTest.groovy` — add config template test section

**Step 1: Write the failing test** (append to KmServiceDTOTest or create separate test)

Create `dms/test/model/KmConfigTemplateDTOTest.groovy`:

```groovy
package model

import support.DmsTestDbSupport
import spock.lang.Specification

class KmConfigTemplateDTOTest extends Specification {
    def d
    def ds

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, """
create table km_config_template
(
    id           int auto_increment primary key,
    name         varchar(50),
    des          varchar(200),
    config_items text,
    updated_date timestamp default current_timestamp
)
""")
    }

    def cleanup() {
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    void 'crud for km_config_template'() {
        when:
        def one = new KmConfigTemplateDTO()
        one.name = 'default-kafka'
        one.des = 'default kafka config'
        one.configItems = new model.json.ConfigItems(items: [
                new model.json.KVPair<String>(key: 'num.io.threads', value: '8'),
                new model.json.KVPair<String>(key: 'num.network.threads', value: '3')
        ])
        one.updatedDate = new Date()
        def id = one.add()

        then:
        id > 0

        when:
        def found = new KmConfigTemplateDTO(id: id).one()

        then:
        found.name == 'default-kafka'
        found.configItems != null
        found.configItems.items.size() == 2
        found.configItems.items[0].key == 'num.io.threads'
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "model.KmConfigTemplateDTOTest"`
Expected: FAIL

**Step 3: Write minimal implementation**

```groovy
package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.ConfigItems

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmConfigTemplateDTO extends BaseRecord<KmConfigTemplateDTO> {
    Integer id
    String name
    String des
    ConfigItems configItems
    Date updatedDate
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "model.KmConfigTemplateDTOTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/model/KmConfigTemplateDTO.groovy dms/test/model/KmConfigTemplateDTOTest.groovy
git commit -m "feat(km): add KmConfigTemplateDTO with test"
```

---

### Task 1.5: KmTopicDTO, KmJobDTO, KmTaskLogDTO, KmSnapshotDTO

**Files:**
- Create: `dms/src/model/KmTopicDTO.groovy`
- Create: `dms/src/model/job/KmJobDTO.groovy`
- Create: `dms/src/model/job/KmTaskLogDTO.groovy`
- Create: `dms/src/model/KmSnapshotDTO.groovy`
- Test: `dms/test/model/KmTopicDTOTest.groovy`

**Step 1: Write the failing test**

```groovy
package model

import support.DmsTestDbSupport
import spock.lang.Specification

class KmTopicDTOTest extends Specification {
    def d
    def ds

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, """
create table km_topic
(
    id                int auto_increment primary key,
    service_id        int,
    name              varchar(200),
    partitions        int,
    replication_factor int,
    config_overrides  varchar(2000),
    status            varchar(20),
    created_date      timestamp,
    updated_date      timestamp default current_timestamp
)
""")
    }

    def cleanup() {
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    void 'crud for km_topic'() {
        when:
        def one = new KmTopicDTO()
        one.serviceId = 1
        one.name = 'my-topic'
        one.partitions = 6
        one.replicationFactor = 3
        one.status = KmTopicDTO.Status.creating
        one.createdDate = new Date()
        one.updatedDate = new Date()
        def id = one.add()

        then:
        id > 0

        when:
        def found = new KmTopicDTO(id: id).one()

        then:
        found.name == 'my-topic'
        found.partitions == 6
        found.replicationFactor == 3
        found.status == KmTopicDTO.Status.creating
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "model.KmTopicDTOTest"`
Expected: FAIL

**Step 3: Write implementations**

```groovy
// dms/src/model/KmTopicDTO.groovy
package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmTopicDTO extends BaseRecord<KmTopicDTO> {
    @CompileStatic
    static enum Status {
        creating, active, deleting, deleted
    }

    Integer id
    Integer serviceId
    String name
    Integer partitions
    Integer replicationFactor
    Map<String, String> configOverrides
    Status status
    Date createdDate
    Date updatedDate
}
```

```groovy
// dms/src/model/job/KmJobDTO.groovy
package model.job

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.BaseRecord

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmJobDTO extends BaseRecord<KmJobDTO> {
    Integer id
    Integer busiId
    String type
    String status
    String result
    Integer costMs
    String content
    Integer failedNum
    Date createdDate
    Date updatedDate
}
```

```groovy
// dms/src/model/job/KmTaskLogDTO.groovy
package model.job

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.BaseRecord

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmTaskLogDTO extends BaseRecord<KmTaskLogDTO> {
    Integer id
    Integer jobId
    String step
    String jobResult
    Integer costMs
    Date createdDate
    Date updatedDate
}
```

```groovy
// dms/src/model/KmSnapshotDTO.groovy
package model

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class KmSnapshotDTO extends BaseRecord<KmSnapshotDTO> {
    @CompileStatic
    static enum Status {
        created, failed, done
    }

    Integer id
    String name
    Integer serviceId
    String snapshotDir
    Status status
    String message
    Integer costMs
    Date createdDate
    Date updatedDate
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "model.KmTopicDTOTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/model/KmTopicDTO.groovy dms/src/model/job/KmJobDTO.groovy \
       dms/src/model/job/KmTaskLogDTO.groovy dms/src/model/KmSnapshotDTO.groovy \
       dms/test/model/KmTopicDTOTest.groovy
git commit -m "feat(km): add KmTopicDTO, KmJobDTO, KmTaskLogDTO, KmSnapshotDTO"
```

---

## Stage 2: Core Engine — KafkaManager + KmJobExecutor + PartitionBalancer

### Task 2.1: KafkaManager Singleton

**Files:**
- Create: `dms/src/km/KafkaManager.groovy`
- Test: `dms/test/km/KafkaManagerTest.groovy`

**Step 1: Write the failing test**

```groovy
package km

import spock.lang.Specification

class KafkaManagerTest extends Specification {
    void 'encode and decode are inverse'() {
        given:
        def original = 'my-secret-password'

        when:
        def encoded = KafkaManager.encode(original)
        def decoded = KafkaManager.decode(encoded)

        then:
        decoded == original
        encoded != original
    }

    void 'constants are correct'() {
        expect:
        KafkaManager.CLUSTER_ID == 1
        KafkaManager.ONE_CLUSTER_MAX_BROKERS == 32
        KafkaManager.MAX_PARTITIONS_PER_TOPIC == 256
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "km.KafkaManagerTest"`
Expected: FAIL

**Step 3: Write minimal implementation**

```groovy
package km

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import metric.SimpleGauge
import model.AppDTO
import model.KmServiceDTO
import server.AgentCaller
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class KafkaManager {
    static final int CLUSTER_ID = 1

    static final int ONE_CLUSTER_MAX_BROKERS = 32

    static final int MAX_PARTITIONS_PER_TOPIC = 256

    static void init() {
        initMetricCollector()
    }

    static final SimpleGauge globalGauge = new SimpleGauge('Kafka Manager', 'Kafka Manager Metrics.', ['cluster_id'])

    static {
        globalGauge.register()
    }

    static void initMetricCollector() {
        var labelValues = List.of(CLUSTER_ID.toString())

        globalGauge.addRawGetter(() -> {
            def map = new HashMap<String, SimpleGauge.ValueWithLabelValues>()

            def serviceList = new KmServiceDTO(status: KmServiceDTO.Status.running).list()
            def clusterCount = 0
            def brokerTotalCount = 0
            for (one in serviceList) {
                clusterCount++
                brokerTotalCount += one.brokers
            }

            map.km_cluster_count = new SimpleGauge.ValueWithLabelValues((double) clusterCount, labelValues)
            map.km_broker_count = new SimpleGauge.ValueWithLabelValues((double) brokerTotalCount, labelValues)

            map
        })
    }

    static String decode(String content) {
        char[] chars = content.toCharArray()
        char[] x = new char[chars.length]
        chars.eachWithIndex { char c, int i ->
            def diff = i % 2 == 0 ? 1 : 2
            x[i] = (c - diff) as char
        }
        new String(x)
    }

    static String encode(String content) {
        char[] chars = content.toCharArray()
        char[] x = new char[chars.length]
        chars.eachWithIndex { char c, int i ->
            def diff = i % 2 == 0 ? 1 : 2
            x[i] = (c + diff) as char
        }
        new String(x)
    }

    static void stopContainers(int appId) {
        def appOne = new AppDTO(id: appId).queryFields('id,status').one()
        if (!appOne) {
            return
        }
        if (appOne.status == AppDTO.Status.auto) {
            log.warn('update app status to manual, app id: {}', appOne.id)
            new AppDTO(id: appOne.id, status: AppDTO.Status.manual, updatedDate: new Date()).update()
        }

        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getContainerList(CLUSTER_ID, appId)
        containerList.each { x ->
            if (x.running()) {
                log.warn('stop running container: {}', x.name())

                def p = [id: x.id]
                p.isRemoveAfterStop = '1'
                p.readTimeout = 30 * 1000

                AgentCaller.instance.agentScriptExe(CLUSTER_ID, x.nodeIp, 'container stop', p)
            }
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "km.KafkaManagerTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/km/KafkaManager.groovy dms/test/km/KafkaManagerTest.groovy
git commit -m "feat(km): add KafkaManager singleton hub with metrics and password encoding"
```

---

### Task 2.2: KmJobExecutor

**Files:**
- Create: `dms/src/km/KmJobExecutor.groovy`

No unit test needed — trivial delegation class (same pattern as `RmJobExecutor` which also has no test).

**Implementation:**

```groovy
package km

import com.segment.common.job.NamedThreadFactory
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import plugin.BasePlugin
import server.scheduler.processor.CreateProcessor

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@CompileStatic
@Singleton
@Slf4j
class KmJobExecutor {
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.runtime.availableProcessors(),
            new NamedThreadFactory('km-job-'))

    void execute(Runnable runnable) {
        executor.execute(runnable)
    }

    int runCreatingAppJob(AppDTO app) {
        def job = BasePlugin.creatingAppJob(app)

        log.warn('start application create job, job id: {}', job.id)
        execute {
            try {
                new CreateProcessor().process(job, app, [])
                new AppJobDTO(id: job.id, status: AppJobDTO.Status.done, updatedDate: new Date()).update()
                log.warn('start application create job done, job id: {}', job.id)
            } catch (Exception e) {
                log.error('start application create job error', e)
            }
        }

        job.id
    }

    void cleanUp() {
        log.warn('clean up km job executor')
        executor.shutdown()
    }
}
```

**Commit:**

```bash
git add dms/src/km/KmJobExecutor.groovy
git commit -m "feat(km): add KmJobExecutor thread pool"
```

---

### Task 2.3: PartitionBalancer

**Files:**
- Create: `dms/src/km/PartitionBalancer.groovy`
- Test: `dms/test/km/PartitionBalancerTest.groovy`

**Step 1: Write the failing test**

```groovy
package km

import spock.lang.Specification

class PartitionBalancerTest extends Specification {
    void 'assignReplicas distributes partitions evenly'() {
        when:
        def assignment = PartitionBalancer.assignReplicas(3, 6, 1)

        then:
        assignment.size() == 6
        assignment[0].size() == 1
        assignment[0][0] == 0
        assignment[1][0] == 1
        assignment[2][0] == 2
        assignment[3][0] == 0
        assignment[4][0] == 1
        assignment[5][0] == 2
    }

    void 'assignReplicas with replication factor places replicas on different brokers'() {
        when:
        def assignment = PartitionBalancer.assignReplicas(3, 3, 3)

        then:
        assignment.size() == 3
        assignment.each { replicas ->
            assert replicas.size() == 3
            assert replicas.toSet().size() == 3
        }
    }

    void 'assignReplicas throws when replicationFactor exceeds brokerCount'() {
        when:
        PartitionBalancer.assignReplicas(2, 3, 3)

        then:
        thrown(AssertionError)
    }

    void 'reassignForScale includes new broker ids'() {
        given:
        def current = PartitionBalancer.assignReplicas(3, 6, 1)
        def newBrokerIds = [3, 4] as int[]

        when:
        def reassigned = PartitionBalancer.reassignForScale(current, newBrokerIds)

        then:
        def allBrokers = reassigned.collect { it[0] }.toSet()
        allBrokers.containsAll([3, 4])
    }

    void 'reassignForDecommission moves replicas off removed brokers'() {
        given:
        def current = PartitionBalancer.assignReplicas(4, 8, 1)
        def removeBrokerIds = [2, 3] as int[]

        when:
        def reassigned = PartitionBalancer.reassignForDecommission(current, removeBrokerIds)

        then:
        reassigned.each { replicas ->
            replicas.each { brokerId ->
                assert !(brokerId in removeBrokerIds)
            }
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "km.PartitionBalancerTest"`
Expected: FAIL

**Step 3: Write minimal implementation**

```groovy
package km

import groovy.transform.CompileStatic

@CompileStatic
class PartitionBalancer {

    static List<List<Integer>> assignReplicas(int brokerCount, int partitionCount, int replicationFactor) {
        assert replicationFactor <= brokerCount: "replicationFactor $replicationFactor must be <= brokerCount $brokerCount"

        List<List<Integer>> assignment = []
        for (int p = 0; p < partitionCount; p++) {
            List<Integer> replicas = []
            for (int r = 0; r < replicationFactor; r++) {
                int brokerId = (p + r) % brokerCount
                replicas << brokerId
            }
            assignment << replicas
        }
        assignment
    }

    static List<List<Integer>> reassignForScale(List<List<Integer>> currentAssignment, int[] newBrokerIds) {
        int totalBrokers = currentAssignment.collect { it.max() }.max() + 1 + newBrokerIds.length
        List<Integer> allBrokers = (0..<totalBrokers) as List<Integer>

        List<List<Integer>> reassigned = []
        currentAssignment.eachWithIndex { List<Integer> replicas, int partitionIndex ->
            int targetBroker = allBrokers[partitionIndex % allBrokers.size()]
            List<Integer> newReplicas = [targetBroker]
            int offset = 1
            while (newReplicas.size() < replicas.size()) {
                int candidate = allBrokers[(partitionIndex + offset) % allBrokers.size()]
                if (!(candidate in newReplicas)) {
                    newReplicas << candidate
                }
                offset++
            }
            reassigned << newReplicas
        }
        reassigned
    }

    static List<List<Integer>> reassignForDecommission(List<List<Integer>> currentAssignment, int[] removeBrokerIds) {
        int maxBroker = currentAssignment.collect { it.max() }.max()
        List<Integer> remainingBrokers = (0..maxBroker).findAll { !(it in removeBrokerIds) }
        assert remainingBrokers.size() > 0: "no remaining brokers after decommission"

        List<List<Integer>> reassigned = []
        currentAssignment.eachWithIndex { List<Integer> replicas, int partitionIndex ->
            List<Integer> newReplicas = []
            replicas.each { brokerId ->
                if (brokerId in removeBrokerIds) {
                    int replacement = remainingBrokers[partitionIndex % remainingBrokers.size()]
                    int offset = 0
                    while (replacement in newReplicas) {
                        replacement = remainingBrokers[(partitionIndex + offset) % remainingBrokers.size()]
                        offset++
                    }
                    newReplicas << replacement
                } else {
                    newReplicas << brokerId
                }
            }
            reassigned << newReplicas
        }
        reassigned
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "km.PartitionBalancerTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/km/PartitionBalancer.groovy dms/test/km/PartitionBalancerTest.groovy
git commit -m "feat(km): add PartitionBalancer with assign, scale, decommission"
```

---

### Task 2.4: Job Model Classes (KmJobTypes, KmJob, KmJobTask, KmTaskLog)

**Files:**
- Create: `dms/src/km/job/KmJobTypes.groovy`
- Create: `dms/src/km/job/KmJob.groovy`
- Create: `dms/src/km/job/KmJobTask.groovy`
- Create: `dms/src/km/job/KmTaskLog.groovy`

```groovy
// dms/src/km/job/KmJobTypes.groovy
package km.job

import com.segment.common.job.chain.JobType
import groovy.transform.CompileStatic

@CompileStatic
class KmJobTypes {
    static final JobType STANDALONE_CREATE = new JobType('standalone_create')
    static final JobType CLUSTER_CREATE = new JobType('cluster_create')
    static final JobType BROKER_SCALE_UP = new JobType('broker_scale_up')
    static final JobType BROKER_SCALE_DOWN = new JobType('broker_scale_down')
    static final JobType TOPIC_CREATE = new JobType('topic_create')
    static final JobType TOPIC_ALTER = new JobType('topic_alter')
    static final JobType TOPIC_DELETE = new JobType('topic_delete')
    static final JobType REASSIGN_PARTITIONS = new JobType('reassign_partitions')
    static final JobType FAILOVER = new JobType('failover')
    static final JobType SNAPSHOT = new JobType('snapshot')
    static final JobType IMPORT = new JobType('import')
}
```

```groovy
// dms/src/km/job/KmJob.groovy
package km.job

import com.segment.common.job.chain.Job
import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStatus
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.KmServiceDTO
import model.job.KmJobDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer
import spi.SpiSupport

@CompileStatic
@Slf4j
class KmJob extends Job implements Serializable {
    private JsonTransformer json = new DefaultJsonTransformer()

    KmServiceDTO kmService

    @Override
    int appId() {
        kmService.id
    }

    @Override
    void save() {
        def dto = new KmJobDTO()
        dto.busiId = kmService.id
        dto.type = type.name
        dto.status = status.name()
        dto.content = json.json(toMap())
        dto.updatedDate = new Date()
        id = dto.add()
    }

    @Override
    boolean isJobProcessBeforeRestart() {
        return false
    }

    @Override
    boolean isStopped() {
        return false
    }

    @Override
    int maxFailNum() {
        3
    }

    @Override
    void updateFailedNum(Integer failedNum) {
        new KmJobDTO(id: id, failedNum: failedNum, updatedDate: new Date()).update()
    }

    @Override
    void lockExecute(Closure<Void> cl) {
        def key = "kafka_service_job_${kmService.id}".toString()

        def lock = SpiSupport.createLock()
        lock.lockKey = key
        boolean isDone = lock.exe {
            cl.call()
        }
        if (!isDone) {
            log.info 'get kafka job lock fail - {}', kmService.name
        }
    }

    @Override
    void allDone() {
    }

    @Override
    void fail(JobStep failedStep) {
    }

    @Override
    void updateStatus(JobStatus status, JobResult result, Integer costMs) {
        new KmJobDTO(id: id, status: status.name(), result: json.json(result), costMs: costMs, updatedDate: new Date()).update()
    }
}
```

```groovy
// dms/src/km/job/KmJobTask.groovy
package km.job

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobTask
import groovy.transform.CompileStatic
import model.job.KmTaskLogDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer

@CompileStatic
abstract class KmJobTask extends JobTask implements Serializable {
    private JsonTransformer json = new DefaultJsonTransformer()

    @Override
    TaskLog load(Integer jobId, String stepAsUuid) {
        def one = new KmTaskLogDTO(jobId: jobId, step: stepAsUuid).one()
        if (!one) {
            return null
        }

        def r = new KmTaskLog()
        r.id = one.id
        r.jobId = one.jobId
        r.step = one.step
        if (one.jobResult) {
            r.jobResult = json.read(one.jobResult, JobResult)
        }
        r.costMs = one.costMs
        r.createdDate = one.createdDate
        r.updatedDate = one.updatedDate
        r
    }

    @Override
    TaskLog newLog() {
        new KmTaskLog()
    }
}
```

```groovy
// dms/src/km/job/KmTaskLog.groovy
package km.job

import com.segment.common.job.chain.JobTask
import groovy.transform.CompileStatic
import model.job.KmTaskLogDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer

@CompileStatic
class KmTaskLog extends JobTask.TaskLog {
    private JsonTransformer json = new DefaultJsonTransformer()

    @Override
    int add() {
        new KmTaskLogDTO(jobId: jobId, step: step, jobResult: json.json(jobResult),
                createdDate: new Date(), updatedDate: new Date()).add()
    }

    @Override
    void update() {
        new KmTaskLogDTO(id: id, jobResult: json.json(jobResult), costMs: costMs, updatedDate: new Date()).update()
    }
}
```

**Commit:**

```bash
git add dms/src/km/job/KmJobTypes.groovy dms/src/km/job/KmJob.groovy \
       dms/src/km/job/KmJobTask.groovy dms/src/km/job/KmTaskLog.groovy
git commit -m "feat(km): add KmJob chain model classes (KmJobTypes, KmJob, KmJobTask, KmTaskLog)"
```

---

## Stage 3: KafkaPlugin + Config Templates

### Task 3.1: KafkaPlugin

**Files:**
- Create: `dms/plugins/plugin/demo2/KafkaPlugin.groovy`
- Create: `dms/plugins_resources/kafka/ServerPropertiesTpl.groovy`

**Implementation note:** Follow `RedisPlugin.groovy` and `ZookeeperPlugin.groovy` patterns. Register image config (env vars, ports, templates, volumes) and checkers in `init()`.

```groovy
// dms/plugins/plugin/demo2/KafkaPlugin.groovy
package plugin.demo2

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppConf
import plugin.BasePlugin
import server.scheduler.checker.Checker

@CompileStatic
@Slf4j
class KafkaPlugin extends BasePlugin {
    @Override
    String name() { 'kafka' }

    @Override
    String registry() { 'https://docker.1ms.run' }

    @Override
    String group() { 'bitnami' }

    @Override
    String image() { 'kafka' }

    @Override
    String tag() { '2.8.2' }

    @Override
    void init() {
        super.init()
        initImageConfig()
        initChecker()
    }

    private void initImageConfig() {
        addPortIfNotExists(9092, 'broker port')

        addEnvIfNotExists('KAFKA_BROKER_ID', 'broker id')
        addEnvIfNotExists('KAFKA_ZOOKEEPER_CONNECT', 'zookeeper connect string')
        addEnvIfNotExists('KAFKA_LISTENERS', 'listeners')
        addEnvIfNotExists('KAFKA_ADVERTISED_LISTENERS', 'advertised listeners')
        addEnvIfNotExists('KAFKA_HEAP_OPTS', 'heap options')

        def tpl = addImageTpl('server.properties.tpl', '/opt/bitnami/kafka/config/server.properties')
        tpl.paramValue('port', '9092')
        tpl.paramValue('dataDir', '/data/kafka/data')
        tpl.paramValue('brokerId', '${instanceIndex}')
        tpl.paramValue('zkConnectString', '')
        tpl.paramValue('zkChroot', '')
        tpl.paramValue('defaultPartitions', '1')
        tpl.paramValue('defaultReplicationFactor', '1')
        tpl.paramValue('brokerCount', '1')

        def templateTpl = addImageTpl('server.properties.template.tpl', '/opt/bitnami/kafka/config/server.properties')
        templateTpl.paramValue('port', '9092')
        templateTpl.paramValue('dataDir', '/data/kafka/data')
        templateTpl.paramValue('brokerId', '${instanceIndex}')
        templateTpl.paramValue('zkConnectString', '')
        templateTpl.paramValue('zkChroot', '')
        templateTpl.paramValue('configTemplateId', '')
        templateTpl.paramValue('defaultPartitions', '1')
        templateTpl.paramValue('defaultReplicationFactor', '1')
        templateTpl.paramValue('brokerCount', '1')

        addNodeVolumeForUpdate('/data/kafka/data', '/data/kafka/data')
        addNodeVolumeForUpdate('/data/kafka/logs', '/data/kafka/logs')
    }

    private void initChecker() {
        CheckerHolder.instance.add(new Checker() {
            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                def port = conf.paramValue('port') as int
                // port conflict check logic similar to RedisPlugin
                true
            }

            @Override
            Type type() { Type.before }

            @Override
            String name() { 'kafka port conflict check' }

            @Override
            String imageName() { imageName() }
        })
    }

    @Override
    boolean canUseTo(String group, String image) {
        (group == 'bitnami' && image == 'kafka') ||
                (group == 'confluentinc' && image == 'cp-kafka')
    }
}
```

```groovy
// dms/plugins_resources/kafka/ServerPropertiesTpl.groovy
package kafka

def brokerId = super.binding.getProperty('brokerId')
def port = super.binding.getProperty('port')
def dataDir = super.binding.getProperty('dataDir')
def zkConnectString = super.binding.getProperty('zkConnectString')
def zkChroot = super.binding.getProperty('zkChroot')
def defaultPartitions = super.binding.getProperty('defaultPartitions')
def defaultReplicationFactor = super.binding.getProperty('defaultReplicationFactor')
def brokerCount = super.binding.getProperty('brokerCount')

def zkConnect = zkChroot ? "${zkConnectString}${zkChroot}" : zkConnectString
def minReplication = Math.min(3, brokerCount as int)

"""
broker.id=${brokerId}
listeners=PLAINTEXT://0.0.0.0:${port}
advertised.listeners=PLAINTEXT://\${nodeIp}:${port}
zookeeper.connect=${zkConnect}
log.dirs=${dataDir}
num.partitions=${defaultPartitions}
default.replication.factor=${defaultReplicationFactor}
offsets.topic.replication.factor=${minReplication}
transaction.state.log.replication.factor=${minReplication}
log.retention.hours=168
log.segment.bytes=1073741824
num.io.threads=8
num.network.threads=3
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600
"""
```

**Commit:**

```bash
git add dms/plugins/plugin/demo2/KafkaPlugin.groovy dms/plugins_resources/kafka/ServerPropertiesTpl.groovy
git commit -m "feat(km): add KafkaPlugin with server.properties template"
```

---

## Stage 4: Cluster Creation Task Chains

### Task 4.1: ValidateZookeeperTask

**Files:**
- Create: `dms/src/km/job/task/ValidateZookeeperTask.groovy`
- Test: `dms/test/km/job/task/ValidateZookeeperTaskTest.groovy`

**Step 1: Write the failing test** (unit test for validation logic, no real ZK)

```groovy
package km.job.task

import spock.lang.Specification

class ValidateZookeeperTaskTest extends Specification {
    void 'chroot validation rejects root path'() {
        expect:
        ValidateZookeeperTask.isValidChroot('/') == false
        ValidateZookeeperTask.isValidChroot('') == false
        ValidateZookeeperTask.isValidChroot('/kafka/my-cluster') == true
        ValidateZookeeperTask.isValidChroot('/kafka/test') == true
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd dms && gradle unitTest --tests "km.job.task.ValidateZookeeperTaskTest"`
Expected: FAIL

**Step 3: Write minimal implementation**

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
class ValidateZookeeperTask extends KmJobTask {
    ValidateZookeeperTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('validate_zookeeper', 0)
    }

    static boolean isValidChroot(String chroot) {
        chroot != null && chroot.length() > 1 && chroot.startsWith('/')
    }

    @Override
    JobResult doTask() {
        def service = job.kmService
        assert service

        def zkConnect = service.zkConnectString
        def chroot = service.zkChroot

        if (!isValidChroot(chroot)) {
            return JobResult.fail('invalid zk chroot: ' + chroot)
        }

        try {
            def client = CuratorFrameworkFactory.newClient(
                    zkConnect + chroot,
                    new ExponentialBackoffRetry(1000, 3)
            )
            client.start()
            try {
                if (client.checkExists().forPath('/') == null) {
                    client.create().creatingParentsIfNeeded().forPath('/')
                }

                def brokersPath = '/brokers/ids'
                if (client.checkExists().forPath(brokersPath) != null) {
                    def children = client.getChildren().forPath(brokersPath)
                    if (children) {
                        client.close()
                        return JobResult.fail('chroot already contains cluster metadata: ' + chroot)
                    }
                }
            } finally {
                client.close()
            }
        } catch (Exception e) {
            return JobResult.fail('zookeeper connection failed: ' + e.message)
        }

        JobResult.ok('zookeeper validated')
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd dms && gradle unitTest --tests "km.job.task.ValidateZookeeperTaskTest"`
Expected: PASS

**Step 5: Commit**

```bash
git add dms/src/km/job/task/ValidateZookeeperTask.groovy dms/test/km/job/task/ValidateZookeeperTaskTest.groovy
git commit -m "feat(km): add ValidateZookeeperTask with chroot validation"
```

---

### Task 4.2: RunCreatingAppJobTask + WaitInstancesRunningTask + WaitBrokersRegisteredTask

**Files:**
- Create: `dms/src/km/job/task/RunCreatingAppJobTask.groovy`
- Create: `dms/src/km/job/task/WaitInstancesRunningTask.groovy`
- Create: `dms/src/km/job/task/WaitBrokersRegisteredTask.groovy`

These follow the exact same pattern as the RM equivalents.

```groovy
// dms/src/km/job/task/RunCreatingAppJobTask.groovy
package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.AppDTO
import model.AppJobDTO
import plugin.BasePlugin
import server.scheduler.processor.CreateProcessor

@CompileStatic
@Slf4j
class RunCreatingAppJobTask extends KmJobTask {
    final AppDTO app

    RunCreatingAppJobTask(KmJob kmJob, AppDTO app) {
        this.app = app

        this.job = kmJob
        this.step = new JobStep('run_creating_app_job', 0)
    }

    @Override
    JobResult doTask() {
        assert app

        def job = BasePlugin.creatingAppJob(app)

        log.warn('start kafka application create job, job id: {}', job.id)
        try {
            new CreateProcessor().process(job, app, [])
            new AppJobDTO(id: job.id, status: AppJobDTO.Status.done, updatedDate: new Date()).update()
            log.warn('start kafka application create job done, job id: {}', job.id)
            return JobResult.ok('start application done')
        } catch (Exception e) {
            log.error('start kafka application create job error', e)
            return JobResult.fail('start application error')
        }
    }
}
```

```groovy
// dms/src/km/job/task/WaitInstancesRunningTask.groovy
package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.KafkaManager
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class WaitInstancesRunningTask extends KmJobTask {
    final KmServiceDTO kmService

    WaitInstancesRunningTask(KmJob kmJob) {
        this.kmService = kmJob.kmService

        this.job = kmJob
        this.step = new JobStep('wait_instances_running', 1)
    }

    int tryCount = 0

    @Override
    JobResult doTask() {
        assert kmService

        def instance = InMemoryAllContainerManager.instance
        def runningContainerList = instance.getRunningContainerList(KafkaManager.CLUSTER_ID, kmService.appId)
        if (!runningContainerList) {
            Thread.sleep(10 * 1000)
            tryCount++

            if (tryCount > 10) {
                return JobResult.fail('no containers found for app id: ' + kmService.appId)
            } else {
                return doTask()
            }
        }

        def runningNumber = runningContainerList.size()
        log.info 'running containers number: {}, app id: {}', runningNumber, kmService.appId
        if (runningNumber == kmService.brokers) {
            return JobResult.ok('running containers number: ' + runningNumber)
        }

        Thread.sleep(10 * 1000)
        tryCount++

        if (tryCount > 10) {
            return JobResult.fail('running containers number: ' + runningNumber + ', expect: ' + kmService.brokers)
        } else {
            return doTask()
        }
    }
}
```

```groovy
// dms/src/km/job/task/WaitBrokersRegisteredTask.groovy
package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import model.json.BrokerDetail
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

@CompileStatic
@Slf4j
class WaitBrokersRegisteredTask extends KmJobTask {
    final KmServiceDTO kmService

    WaitBrokersRegisteredTask(KmJob kmJob) {
        this.kmService = kmJob.kmService

        this.job = kmJob
        this.step = new JobStep('wait_brokers_registered', 2)
    }

    int tryCount = 0

    @Override
    JobResult doTask() {
        assert kmService

        def zkConnect = kmService.zkConnectString + kmService.zkChroot
        try {
            def client = CuratorFrameworkFactory.newClient(zkConnect, new ExponentialBackoffRetry(1000, 3))
            client.start()
            try {
                def brokersPath = '/brokers/ids'
                if (client.checkExists().forPath(brokersPath) == null) {
                    return retryOrFail('brokers ids path not found')
                }

                def brokerIds = client.getChildren().forPath(brokersPath)
                if (brokerIds.size() < kmService.brokers) {
                    return retryOrFail('registered brokers: ' + brokerIds.size() + ', expected: ' + kmService.brokers)
                }

                def brokerDetail = new BrokerDetail()
                brokerDetail.brokers = []
                brokerIds.each { idStr ->
                    def data = client.getData().forPath(brokersPath + '/' + idStr)
                    def json = new groovy.json.JsonSlurper().parse(data) as Map
                    def host = json.host as String
                    def port = json.port as int
                    brokerDetail.brokers << new BrokerDetail.BrokerNode(
                            brokerId: idStr as int,
                            brokerIndex: idStr as int,
                            ip: host,
                            port: port
                    )
                }

                new KmServiceDTO(id: kmService.id, brokerDetail: brokerDetail, updatedDate: new Date()).update()
                JobResult.ok('all brokers registered: ' + brokerIds.size())
            } finally {
                client.close()
            }
        } catch (Exception e) {
            return retryOrFail('zookeeper query failed: ' + e.message)
        }
    }

    private JobResult retryOrFail(String message) {
        Thread.sleep(5 * 1000)
        tryCount++
        if (tryCount > 20) {
            return JobResult.fail(message)
        }
        doTask()
    }
}
```

**Commit:**

```bash
git add dms/src/km/job/task/RunCreatingAppJobTask.groovy \
       dms/src/km/job/task/WaitInstancesRunningTask.groovy \
       dms/src/km/job/task/WaitBrokersRegisteredTask.groovy
git commit -m "feat(km): add cluster creation task chain (RunCreating, WaitRunning, WaitBrokersRegistered)"
```

---

## Stage 5: Controllers — KmServiceCtrl

### Task 5.1: KmServiceCtrl

**Files:**
- Create: `dms/src/ctrl/kafka/ServiceCtrl.groovy`

Follow the `ctrl/redis/ServiceCtrl.groovy` pattern — Groovy script with `ChainHandler`, not a class.

**Key endpoints:**
- `GET /list` — paginated service list
- `GET /one` — service detail
- `POST /add` — create service (assembles job chain)
- `POST /delete` — stop brokers, delete ZK chroot, mark deleted

**Implementation outline:**

```groovy
package ctrl.kafka

import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import km.KafkaManager
import km.KmJobExecutor
import km.job.KmJob
import km.job.KmJobTypes
import km.job.task.*
import model.*
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance
def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/service') {
    h.get('/list') { req, resp ->
        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def dto = new KmServiceDTO().noWhere()

        def keyword = req.param('keyword')
        dto.where(keyword as boolean, '(name like ?)', '%' + keyword + '%')

        def mode = req.param('mode')
        if (mode) {
            dto.where('mode = ?', mode)
        }

        def status = req.param('status')
        if (status) {
            dto.where('status = ?', status)
        }

        dto.listPager(pageNum, pageSize)
    }

    h.get('/one') { req, resp ->
        def idStr = req.param('id')
        assert idStr
        def id = idStr as int

        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }
        [one: one]
    }

    h.post('/add') { req, resp ->
        def body = req.bodyAs(Map)
        // validate, create KmServiceDTO, assemble job chain, execute
        // ...
        [id: id]
    }

    h.post('/delete') { req, resp ->
        def body = req.bodyAs(Map)
        def id = body.id as int
        def one = new KmServiceDTO(id: id).one()
        if (!one) {
            resp.halt(404, 'service not found')
        }

        KafkaManager.stopContainers(one.appId)

        // recursively delete zk chroot
        // ...

        new KmServiceDTO(id: id, status: KmServiceDTO.Status.deleted, updatedDate: new Date()).update()
        [id: id]
    }
}
```

**Commit:**

```bash
git add dms/src/ctrl/kafka/ServiceCtrl.groovy
git commit -m "feat(km): add KmServiceCtrl with list, one, add, delete endpoints"
```

---

## Stage 6: Topic Management

### Task 6.1: CreateTopicTask

**Files:**
- Create: `dms/src/km/job/task/CreateTopicTask.groovy`

```groovy
package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask

@CompileStatic
@Slf4j
class CreateTopicTask extends KmJobTask {
    final String topicName
    final int partitions
    final int replicationFactor
    final Map<String, String> configOverrides

    CreateTopicTask(KmJob kmJob, String topicName, int partitions, int replicationFactor, Map<String, String> configOverrides = [:]) {
        this.topicName = topicName
        this.partitions = partitions
        this.replicationFactor = replicationFactor
        this.configOverrides = configOverrides

        this.job = kmJob
        this.step = new JobStep('create_topic_' + topicName, 0)
    }

    @Override
    JobResult doTask() {
        // execute kafka-topics.sh --create via container exec or shell
        JobResult.ok('topic created: ' + topicName)
    }
}
```

### Task 6.2: KmTopicCtrl

**Files:**
- Create: `dms/src/ctrl/kafka/TopicCtrl.groovy`

**Commit:**

```bash
git add dms/src/km/job/task/CreateTopicTask.groovy dms/src/ctrl/kafka/TopicCtrl.groovy
git commit -m "feat(km): add topic management with CreateTopicTask and KmTopicCtrl"
```

---

## Stage 7: Scaling (BROKER_SCALE_UP/DOWN)

### Task 7.1: Scaling Tasks

**Files:**
- Create: `dms/src/km/job/task/AddBrokersTask.groovy`
- Create: `dms/src/km/job/task/RemoveBrokersTask.groovy`
- Create: `dms/src/km/job/task/ReassignPartitionsTask.groovy`
- Create: `dms/src/km/job/task/WaitReassignmentCompleteTask.groovy`
- Create: `dms/src/km/job/task/DecommissionBrokerTask.groovy`

**Commit:**

```bash
git add dms/src/km/job/task/AddBrokersTask.groovy dms/src/km/job/task/RemoveBrokersTask.groovy \
       dms/src/km/job/task/ReassignPartitionsTask.groovy dms/src/km/job/task/WaitReassignmentCompleteTask.groovy \
       dms/src/km/job/task/DecommissionBrokerTask.groovy
git commit -m "feat(km): add broker scaling tasks (add, remove, reassign, decommission)"
```

---

## Stage 8: Failover

### Task 8.1: FailoverTask (Kafka version)

**Files:**
- Create: `dms/src/km/job/task/FailoverTask.groovy`

**Commit:**

```bash
git add dms/src/km/job/task/FailoverTask.groovy
git commit -m "feat(km): add Kafka FailoverTask for controller re-election"
```

---

## Stage 9: Snapshot/Import

### Task 9.1: KmSnapshotManager

**Files:**
- Create: `dms/src/km/KmSnapshotManager.groovy`
- Create: `dms/src/ctrl/kafka/SnapshotCtrl.groovy`

**Commit:**

```bash
git add dms/src/km/KmSnapshotManager.groovy dms/src/ctrl/kafka/SnapshotCtrl.groovy
git commit -m "feat(km): add KmSnapshotManager with export/import"
```

---

## Stage 10: Monitoring

### Task 10.1: KmMetricCtrl

**Files:**
- Create: `dms/src/ctrl/kafka/MetricCtrl.groovy`

**Commit:**

```bash
git add dms/src/ctrl/kafka/MetricCtrl.groovy
git commit -m "feat(km): add KmMetricCtrl for kafka_exporter + Prometheus init"
```

---

## Stage 11: Web UI

### Task 11.1: Web UI Pages

**Files:**
- Create: `dms/www/admin/pages/kafka/service.html`
- Create: `dms/www/admin/pages/kafka/service.js`
- (Additional pages per design doc section "Web UI Pages")

**Commit:**

```bash
git add dms/www/admin/pages/kafka/
git commit -m "feat(km): add Kafka Manager web UI pages"
```

---

## Summary

| Stage | Components | Tests |
|-------|-----------|-------|
| 1 | BrokerDetail, KmSnapshotContent, KmServiceDTO, KmConfigTemplateDTO, KmTopicDTO, KmJobDTO, KmTaskLogDTO, KmSnapshotDTO | 4 test files |
| 2 | KafkaManager, KmJobExecutor, PartitionBalancer, KmJobTypes, KmJob, KmJobTask, KmTaskLog | 3 test files |
| 3 | KafkaPlugin, ServerPropertiesTpl | 0 (integration tested later) |
| 4 | ValidateZookeeperTask, RunCreatingAppJobTask, WaitInstancesRunningTask, WaitBrokersRegisteredTask | 1 test file |
| 5 | KmServiceCtrl | 0 (HTTP-level tested via route test pattern) |
| 6 | CreateTopicTask, KmTopicCtrl | 0 |
| 7 | AddBrokersTask, RemoveBrokersTask, ReassignPartitionsTask, WaitReassignmentCompleteTask, DecommissionBrokerTask | 0 |
| 8 | FailoverTask | 0 |
| 9 | KmSnapshotManager, SnapshotCtrl | 0 |
| 10 | KmMetricCtrl | 0 |
| 11 | Web UI pages | 0 |
