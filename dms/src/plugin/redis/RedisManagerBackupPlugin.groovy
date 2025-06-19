package plugin.redis

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import model.job.RmBackupLogDTO
import model.job.RmBackupTemplateDTO
import model.server.CreateContainerConf
import plugin.BasePlugin
import rm.BackupManager
import rm.RedisManager
import server.InMemoryAllContainerManager
import server.scheduler.checker.Checker
import server.scheduler.checker.CheckerHolder
import server.scheduler.processor.JobStepKeeper

@CompileStatic
@Slf4j
class RedisManagerBackupPlugin extends BasePlugin {
    @Override
    String name() {
        'redis_manager_backup'
    }

    @Override
    void init() {
        initChecker()
    }

    private void initChecker() {
        CheckerHolder.instance.add new Checker() {

            @Override
            boolean check(CreateContainerConf conf, JobStepKeeper keeper) {
                if (!canUseTo(conf.conf.group, conf.conf.image)) {
                    return true
                }

                if (!conf.app.extendParams || !conf.app.extendParams.get('rmServiceId')) {
                    return true
                }

                def rmServiceId = conf.app.extendParams.get('rmServiceId') as int
                def rmService = new RmServiceDTO(id: rmServiceId).one()
                if (!rmService) {
                    log.warn 'redis service not found, id: {}', rmServiceId
                    return true
                }

                if (!rmService.backupPolicy || !rmService.backupPolicy.isAutomaticBackup) {
                    return true
                }

                int shardIndex
                if (rmService.mode == RmServiceDTO.Mode.cluster) {
                    def shard = rmService.clusterSlotsDetail.shards.find { it.appId == conf.app.id }
                    shardIndex = shard.shardIndex
                } else {
                    shardIndex = 0
                }

                // last backup log
                def backupLogList = new RmBackupLogDTO(
                        serviceId: rmService.id,
                        shardIndex: shardIndex,
                        status: RmBackupLogDTO.Status.done
                ).orderBy('id desc').list()
                def expireDate = new Date() - rmService.backupPolicy.retentionPeriod
                def filterList = backupLogList.findAll {
                    it.createdDate > expireDate
                }

                if (!filterList) {
                    log.warn 'no backup log found, service id: {}, shard index: {}', rmService.id, shardIndex
                    return true
                }

                def backupLog = filterList.first()
                def backupTemplate = new RmBackupTemplateDTO(id: rmService.backupPolicy.backupTemplateId).one()
                def backupDataDir = backupTemplate.backupDataDir ?: RedisManager.backupDataDir()
                def backupFilePath = backupDataDir + '/' + backupLog.dateTimeStr + '/service-' + rmService.id + '/shard-' + shardIndex + '.rdb'

                def instance = InMemoryAllContainerManager.instance
                def containerList = instance.getContainerList(conf.app.clusterId, conf.app.id)
                def x = containerList.find { x -> x.id == conf.containerId }
                if (!x) {
                    // never happen
                    log.warn 'container not found, id: {}, app id: {}', conf.containerId, conf.app.id
                    return false
                }

                def hostDataDir = x.mounts.find { m -> m.destination == '/data/redis' }.source
                def hostRdbFilePath = hostDataDir + '/instance_' + x.instanceIndex() + '/dump.rdb'

                def result = BackupManager.doDownload(backupTemplate, conf.nodeIp, hostRdbFilePath, backupFilePath, backupLog.saveDate.time)
                log.warn 'backup manager download {} to {} result: {}', backupFilePath, hostRdbFilePath, result

                // return true or false?
                return true
            }

            @Override
            Checker.Type type() {
                Checker.Type.beforeStart
            }

            @Override
            String name() {
                'get last backup rdb file'
            }

            @Override
            String imageName() {
                null
            }

            @Override
            boolean canUseTo(String group, String image) {
                RedisManagerBackupPlugin.this.canUseTo(group, image)
            }
        }
    }

    @Override
    boolean canUseTo(String group, String image) {
        if ('library' == group && 'redis' == image) {
            return true
        }
        if ('library' == group && 'valkey' == image) {
            return true
        }
        if ('montplex' == group && 'engula' == image) {
            return true
        }

        false
    }
}
