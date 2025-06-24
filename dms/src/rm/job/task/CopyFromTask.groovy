package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.ImageTplDTO
import model.NamespaceDTO
import model.json.AppConf
import model.json.FileVolumeMount
import model.json.KVPair
import plugin.BasePlugin
import rm.RedisManager
import rm.RmJobExecutor
import rm.job.RmJob
import rm.job.RmJobTask
import server.InMemoryAllContainerManager

@CompileStatic
@Slf4j
class CopyFromTask extends RmJobTask {
    final String uuid
    final String type
    final String srcAddress
    final String srcPassword
    final String targetType
    final String targetAddress
    final String targetPassword

    CopyFromTask(RmJob rmJob, String uuid, String type, String srcAddress, String srcPassword,
                 String targetType, String targetAddress, String targetPassword) {
        this.uuid = uuid
        this.type = type
        this.srcAddress = srcAddress
        this.srcPassword = srcPassword
        this.targetType = targetType
        this.targetAddress = targetAddress
        this.targetPassword = targetPassword

        this.job = rmJob
        this.step = new JobStep('copy_from_' + uuid, 0)
    }

    @Override
    JobResult doTask() {
        // create application if not exists
        def app = new AppDTO(name: step.name).one()
        if (!app) {
            app = new AppDTO()
            app.clusterId = RedisManager.CLUSTER_ID
            app.namespaceId = NamespaceDTO.createIfNotExist(RedisManager.CLUSTER_ID, 'redis-shake')
            app.name = step.name

            app.conf = createAppConf()

            app.id = app.add()
        } else {
            app.conf = createAppConf()
            new AppDTO(id: app.id, conf: app.conf, updatedDate: new Date()).update()
            log.warn 'Update app conf for app: ' + app.name
        }

        // if is running
        def instance = InMemoryAllContainerManager.instance
        def containerList = instance.getContainerList(app.clusterId, app.id)
        if (containerList) {
            log.warn 'this job is running, check it, app name: {}', app.name
            return JobResult.fail('this job is running, check it, app name: ' + app.name)
        }

        def jobId = RmJobExecutor.instance.runCreatingAppJob(app)
        log.warn 'created job {}', jobId

        // check container running done? todo

        JobResult.ok()
    }

    private AppConf createAppConf() {
        def conf = new AppConf()
        conf.registryId = BasePlugin.getRegistryIdByUrl('https://docker.io')
        conf.group = 'montplex'
        conf.image = 'redis-shake'
        conf.tag = '4.4.0'

        conf.cpuFixed = 1
        conf.memReservationMB = 256

        def tplOne = new ImageTplDTO(imageName: 'montplex/redis-shake', name: 'redis-shake.toml.tpl').one()
        def mountOne = new FileVolumeMount(imageTplId: tplOne.id, content: tplOne.content, dist: '/etc/redis-shake.toml')
        mountOne.isParentDirMount = false

        mountOne.paramList << new KVPair<String>('type', type)
        mountOne.paramList << new KVPair<String>('targetType', targetType)
        mountOne.paramList << new KVPair<String>('targetAddress', targetAddress)
        mountOne.paramList << new KVPair<String>('targetUsername', '')
        mountOne.paramList << new KVPair<String>('targetPassword', targetPassword ?: '')
        mountOne.paramList << new KVPair<String>('srcAddress', srcAddress)
        mountOne.paramList << new KVPair<String>('srcUsername', '')
        mountOne.paramList << new KVPair<String>('srcPassword', srcPassword ?: '')
        conf.fileVolumeList << mountOne
        conf
    }
}
