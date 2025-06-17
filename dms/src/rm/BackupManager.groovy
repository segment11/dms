package rm

import com.segment.common.job.IntervalJob
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.NodeKeyPairDTO
import model.RmServiceDTO
import model.job.RmBackupLogDTO
import model.job.RmBackupTemplateDTO
import model.json.BackupPolicy
import server.AgentCaller
import transfer.ContainerInfo

@CompileStatic
@Singleton
@Slf4j
class BackupManager extends IntervalJob {
    @Override
    String name() {
        'redis backup manager'
    }

    @CompileStatic
    record CheckResult(Date saveDate, boolean isNeedDoBackup) {

    }

    private static CheckResult isNeedDoBackup(int serviceId, String name) {
        def one = new RmBackupLogDTO(serviceId: serviceId, name: name).orderBy('id desc').one()
        if (!one) {
            return new CheckResult(null, true)
        }

        if (one.status == RmBackupLogDTO.Status.done) {
            return new CheckResult(one.saveDate, false)
        }
        if (one.status == RmBackupLogDTO.Status.failed) {
            // try again
            return new CheckResult(one.saveDate, true)
        }
        // created
        return new CheckResult(one.saveDate, false)
    }

    Map<Integer, RmBackupTemplateDTO> cachedBackupTemplates = [:]

    private void doBackup(RmServiceDTO one, BackupPolicy backupPolicy, int backupLogId, String dateTimeStr, int shardIndex,
                          ContainerInfo x, CheckResult checkResult) {
        def backupTemplate = cachedBackupTemplates[backupPolicy.backupTemplateId]
        if (!backupTemplate) {
            backupTemplate = new RmBackupTemplateDTO(id: backupPolicy.backupTemplateId).one()
            cachedBackupTemplates[backupPolicy.backupTemplateId] = backupTemplate
        }

        if (!backupTemplate) {
            log.warn 'backup template not found, backup template id: {}'
            new RmBackupLogDTO(id: backupLogId, status: RmBackupLogDTO.Status.failed, costMs: 1, updatedDate: new Date()).update()
            return
        }

        // check target nodes already configured
        if (backupTemplate.targetType == RmBackupTemplateDTO.TargetType.scp) {
            for (targetNodeIp in backupTemplate.targetNodeIps) {
                def kp = new NodeKeyPairDTO(ip: targetNodeIp).queryFields('id').one()
                if (kp == null) {
                    log.warn 'node key pair not found, ip: {}', targetNodeIp
                    new RmBackupLogDTO(id: backupLogId, status: RmBackupLogDTO.Status.failed, costMs: 1, updatedDate: new Date()).update()
                    return
                }
            }
        }

        log.info 'begin do backup for service {}, backup template: {}, backup log id: {}', one.name, backupTemplate.name, backupLogId
        def beginT = System.currentTimeMillis()

        def isNeedNotDoSave = checkResult.saveDate && (new Date().time - checkResult.saveDate.time) < backupPolicy.durationHours * 3600 * 1000
        if (!isNeedNotDoSave) {
            try {
                def r = one.connectAndExe(x) { jedis ->
                    jedis.save()
                }
                def costT = System.currentTimeMillis() - beginT
                log.info 'server instance do save cost: {} ms', costT
                new RmBackupLogDTO(id: backupLogId, saveDate: new Date()).update()
            } catch (Exception e) {
                def costT2 = System.currentTimeMillis() - beginT
                log.error 'server do save error, service name: {}, {}', one.name, x.nodeIp + ':' + one.listenPort(x), e
                new RmBackupLogDTO(id: backupLogId, status: RmBackupLogDTO.Status.failed, costMs: costT2, updatedDate: new Date()).update()
                return
            }
        }

        def backupDataDir = backupTemplate.backupDataDir ?: RedisManager.backupDataDir()
        def backupFilePath = backupDataDir + '/' + dateTimeStr + '/service-' + one.id + '/shard-' + shardIndex + '.rdb'

        def hostDataDir = x.mounts.find { m -> m.destination == '/data/redis' }.source
        def hostRdbFilePath = hostDataDir + '/instance_' + x.instanceIndex() + '/dump.rdb'

        def isUpdateOk = doUpload(backupTemplate, x.nodeIp, hostRdbFilePath, backupFilePath)
        def costMs = System.currentTimeMillis() - beginT
        if (isUpdateOk) {
            new RmBackupLogDTO(id: backupLogId, status: RmBackupLogDTO.Status.done, costMs: costMs, updatedDate: new Date()).update()
        } else {
            new RmBackupLogDTO(id: backupLogId, status: RmBackupLogDTO.Status.failed, costMs: costMs, updatedDate: new Date()).update()
        }
    }

    static boolean doUpload(RmBackupTemplateDTO backupTemplate, String nodeIp, String hostRdbFilePath, String backupFilePath) {
        if (backupTemplate.targetType == RmBackupTemplateDTO.TargetType.scp) {
            assert backupTemplate.targetNodeIps != null
            for (targetNodeIp in backupTemplate.targetNodeIps) {
                def kp = new NodeKeyPairDTO(ip: targetNodeIp).one()
                if (kp == null) {
                    log.warn 'node key pair not found, ip: {}', targetNodeIp
                    return false
                }
                try {
                    AgentCaller.instance.doSshCopy(kp, nodeIp, hostRdbFilePath, backupFilePath)
                } catch (Exception e) {
                    log.error 'scp from {} to {} error, host file: {}, dest file: {}', nodeIp, targetNodeIp, hostRdbFilePath, backupFilePath, e
                    return false
                }
            }
            return true
        } else if (backupTemplate.targetType == RmBackupTemplateDTO.TargetType.nfs) {
            // todo
            return true
        } else if (backupTemplate.targetType == RmBackupTemplateDTO.TargetType.s3) {
            // todo
            return true
        } else {
            log.warn 'unknown backup target type: {}', backupTemplate.targetType
            return false
        }
    }

    @Override
    void doJob() {
        // every 1min
        if (intervalCount % 6 != 0) {
            return
        }

        cachedBackupTemplates.clear()

        def serviceList = new RmServiceDTO(status: RmServiceDTO.Status.running).list()
        def autoBackupServiceList = serviceList.findAll {
            it.backupPolicy && it.backupPolicy.isAutomaticBackup
        }

        if (!autoBackupServiceList) {
            log.info 'there are no services need backup'
            return
        }

        final String namePrefix = 'rm-service-'
        for (one in autoBackupServiceList) {
            def backupPolicy = one.backupPolicy
            // for test
            if (!backupPolicy.backupTemplateId) {
                log.info 'no backup template id for service {}, ignore', one.name
                continue
            }

            def dateTimeStr = backupPolicy.dailyOrHourly == 'daily' ?
                    new Date().format('yyyyMMdd') :
                    new Date().format('yyyyMMddHH')

            def nameThisService = namePrefix + one.id + '-' + dateTimeStr + '-'
            if (one.mode == RmServiceDTO.Mode.standalone) {
                def name = nameThisService + 'standalone'
                def checkResult = isNeedDoBackup(one.id, name)
                if (!checkResult.isNeedDoBackup) {
                    log.debug 'no need to backup, name: {}', name
                    continue
                }

                def runningContainerList = one.runningContainerList()
                if (!runningContainerList) {
                    log.warn 'no running instance for service {}, ignore', one.name
                    continue
                }
                def x = runningContainerList[0]

                new RmBackupLogDTO(name: name, serviceId: one.id).deleteAll()
                def backupLogId = new RmBackupLogDTO(
                        name: name,
                        serviceId: one.id,
                        shardIndex: 0,
                        replicaIndex: 0,
                        status: RmBackupLogDTO.Status.created,
                        backupTemplateId: backupPolicy.backupTemplateId,
                        createdDate: new Date()).add()

                RmJobExecutor.instance.execute {
                    doBackup(one, backupPolicy, backupLogId, dateTimeStr, 0, x, checkResult)
                }
            } else if (one.mode == RmServiceDTO.Mode.sentinel) {
                def name = nameThisService + 'from-slave'
                def checkResult = isNeedDoBackup(one.id, name)
                if (!checkResult.isNeedDoBackup) {
                    log.debug 'no need to backup, name: {}', name
                    continue
                }

                def runningContainerList = one.runningContainerList()
                if (!runningContainerList) {
                    log.warn 'no running instance for service {}, ignore', one.name
                    continue
                }

                def slaveX = runningContainerList.find { x ->
                    'master' != one.connectAndExe(x) { jedis ->
                        jedis.role()[0] as String
                    }
                }
                if (!slaveX) {
                    log.warn 'no slave instance for service {}, ignore', one.name
                    continue
                }

                new RmBackupLogDTO(name: name, serviceId: one.id).deleteAll()
                def backupLogId = new RmBackupLogDTO(
                        name: name,
                        serviceId: one.id,
                        shardIndex: 0,
                        replicaIndex: slaveX.instanceIndex(),
                        status: RmBackupLogDTO.Status.created,
                        backupTemplateId: backupPolicy.backupTemplateId,
                        createdDate: new Date()).add()

                RmJobExecutor.instance.execute {
                    doBackup(one, backupPolicy, backupLogId, dateTimeStr, 0, slaveX, checkResult)
                }
            } else {
                def runningContainerList = one.runningContainerList()
                if (!runningContainerList) {
                    log.warn 'no running instance for service {}, ignore', one.name
                    continue
                }

                for (shard in one.clusterSlotsDetail.shards) {
                    def slaveNode = shard.nodes.find { n -> !n.isPrimary }
                    if (!slaveNode) {
                        log.warn 'no slave node for shard {}, service {}, ignore', shard.shardIndex, one.name
                        continue
                    }

                    def name = nameThisService + 'shard-' + shard.shardIndex
                    def checkResult = isNeedDoBackup(one.id, name)
                    if (!checkResult.isNeedDoBackup) {
                        log.debug 'no need to backup, name: {}', name
                        continue
                    }

                    def slaveX = runningContainerList.find { x ->
                        x.instanceIndex() == slaveNode.replicaIndex
                    }
                    if (!slaveX) {
                        log.warn 'no slave instance for shard {}, service {}, ignore', shard.shardIndex, one.name
                        continue
                    }

                    new RmBackupLogDTO(name: name, serviceId: one.id).deleteAll()
                    def backupLogId = new RmBackupLogDTO(
                            name: name,
                            serviceId: one.id,
                            shardIndex: 0,
                            replicaIndex: slaveX.instanceIndex(),
                            status: RmBackupLogDTO.Status.created,
                            backupTemplateId: backupPolicy.backupTemplateId,
                            createdDate: new Date()).add()

                    RmJobExecutor.instance.execute {
                        doBackup(one, backupPolicy, backupLogId, dateTimeStr, shard.shardIndex, slaveX, checkResult)
                    }
                }
            }
        }
    }
}
