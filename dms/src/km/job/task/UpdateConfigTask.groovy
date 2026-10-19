package km.job.task

import com.alibaba.fastjson.JSON
import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.CuratorPoolHolder
import km.job.KmJob
import km.job.KmJobTask
import model.KmServiceDTO
import model.json.ExtendParams

@CompileStatic
@Slf4j
class UpdateConfigTask extends KmJobTask {

    UpdateConfigTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('update_config', 0)
    }

    static Map<String, Object> mergeConfig(Map<String, Object> existing, Map<String, Object> overrides) {
        Map<String, Object> merged = existing != null ? new LinkedHashMap<>(existing) : new LinkedHashMap<String, Object>()
        if (overrides != null) {
            merged.putAll(overrides)
        }
        merged
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def configOverridesJson = job.params?.get('configOverrides') as String
        if (!configOverridesJson) {
            return JobResult.fail('configOverrides param missing')
        }
        Map<String, Object> configOverrides = JSON.parseObject(configOverridesJson, Map.class)

        def brokerDetail = kmService.brokerDetail
        if (!brokerDetail?.brokers) {
            return JobResult.fail('no broker detail found')
        }

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)

        // note: multi-broker ZK write is not transactional - a failure mid-way can leave
        // brokers with inconsistent config; the read-merge-write below is idempotent,
        // so the operator can safely retry the same request
        int updated = 0
        for (broker in brokerDetail.brokers) {
            def configPath = '/config/brokers/' + broker.brokerId
            Map<String, Object> existingConfig = [:]
            if (client.checkExists().forPath(configPath) != null) {
                def existingData = new String(client.getData().forPath(configPath), 'UTF-8')
                def existingJson = JSON.parseObject(existingData, Map.class)
                existingConfig = (existingJson['config'] as Map<String, Object>) ?: [:]
            }
            Map<String, Object> merged = mergeConfig(existingConfig, configOverrides)
            def configJson = JSON.toJSONString([version: 1, config: merged])

            if (client.checkExists().forPath(configPath) != null) {
                client.setData().forPath(configPath, configJson.getBytes('UTF-8'))
            } else {
                client.create().creatingParentsIfNeeded().forPath(configPath, configJson.getBytes('UTF-8'))
            }
            updated++
        }

        def one = new KmServiceDTO(id: kmService.id).one()
        if (!one) {
            return JobResult.fail('service not found, id: ' + kmService.id)
        }
        if (!one.configOverrides) {
            one.configOverrides = new ExtendParams()
        }
        configOverrides.each { k, v -> one.configOverrides.put(k as String, v) }
        new KmServiceDTO(id: one.id, configOverrides: one.configOverrides, updatedDate: new Date()).update()

        JobResult.ok('config updated for ' + updated + ' broker(s)')
    }
}
