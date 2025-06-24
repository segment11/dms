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
import server.InMemoryAllContainerManager
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

        final int timeoutSeconds = 10 * 60
        def isTimeout = (new Date().time - one.createdDate.time) > timeoutSeconds * 1000
        return new CheckResult(one.saveDate, isTimeout)
    }

    Map<Integer, RmBackupTemplateDTO> cachedBackupTemplates = [:]

    private RmBackupTemplateDTO getBackupTemplate(int backupTemplateId) {
        def backupTemplate = cachedBackupTemplates[backupTemplateId]
        if (!backupTemplate) {
            backupTemplate = new RmBackupTemplateDTO(id: backupTemplateId).one()
            cachedBackupTemplates[backupTemplateId] = backupTemplate
        }
        return backupTemplate
    }

    String getBackupFilePath(BackupPolicy backupPolicy, RmBackupLogDTO one) {
        def backupTemplate = getBackupTemplate(backupPolicy.backupTemplateId)
        def backupDataDir = backupTemplate.backupDataDir ?: RedisManager.backupDataDir()
        def backupFilePath = backupDataDir + '/' + one.dateTimeStr + '/service-' + one.id + '/shard-' + one.shardIndex + '.rdb'
        backupFilePath
    }

    static String getHostRdbFilePath(ContainerInfo x) {
        def hostDataDir = x.mounts.find { m -> m.destination == '/data/redis' }.source
        def hostRdbFilePath = hostDataDir + '/instance_' + x.instanceIndex() + '/dump.rdb'
        hostRdbFilePath
    }

    private void doBackup(RmServiceDTO one, BackupPolicy backupPolicy, RmBackupLogDTO backupLogOne,
                          ContainerInfo x, CheckResult checkResult) {
        def backupTemplate = getBackupTemplate(backupPolicy.backupTemplateId)
        if (!backupTemplate) {
            log.warn 'backup template not found, backup template id: {}'
            new RmBackupLogDTO(id: backupLogOne.id, status: RmBackupLogDTO.Status.failed, costMs: 1, updatedDate: new Date()).update()
            return
        }

        // check target nodes already configured
        if (backupTemplate.targetType == RmBackupTemplateDTO.TargetType.scp) {
            for (targetNodeIp in backupTemplate.targetNodeIps) {
                def kp = new NodeKeyPairDTO(ip: targetNodeIp).queryFields('id').one()
                if (kp == null) {
                    log.warn 'node key pair not found, ip: {}', targetNodeIp
                    new RmBackupLogDTO(id: backupLogOne.id, status: RmBackupLogDTO.Status.failed, costMs: 1, updatedDate: new Date()).update()
                    return
                }
            }
        }

        log.info 'begin do backup for service {}, backup template: {}, backup log id: {}', one.name, backupTemplate.name, backupLogOne.id
        def beginT = System.currentTimeMillis()

        def isNeedNotDoSave = checkResult.saveDate && (new Date().time - checkResult.saveDate.time) < backupPolicy.durationHours * 3600 * 1000
        if (!isNeedNotDoSave) {
            try {
                def r = one.connectAndExe(x) { jedis ->
                    jedis.save()
                }
                def costT = System.currentTimeMillis() - beginT
                log.info 'server instance do save cost: {} ms', costT
                new RmBackupLogDTO(id: backupLogOne.id, saveDate: new Date()).update()
            } catch (Exception e) {
                def costT2 = System.currentTimeMillis() - beginT
                log.error 'server do save error, service name: {}, {}', one.name, x.nodeIp + ':' + one.listenPort(x), e
                new RmBackupLogDTO(id: backupLogOne.id, status: RmBackupLogDTO.Status.failed, costMs: costT2, updatedDate: new Date()).update()
                return
            }
        }

        def backupFilePath = getBackupFilePath(backupPolicy, backupLogOne)
        def hostRdbFilePath = getHostRdbFilePath(x)

        def isUpdateOk = doUpload(backupTemplate, x.nodeIp, hostRdbFilePath, backupFilePath)
        def costMs = System.currentTimeMillis() - beginT
        if (isUpdateOk) {
            new RmBackupLogDTO(id: backupLogOne.id, status: RmBackupLogDTO.Status.done, costMs: costMs, updatedDate: new Date()).update()
        } else {
            new RmBackupLogDTO(id: backupLogOne.id, status: RmBackupLogDTO.Status.failed, costMs: costMs, updatedDate: new Date()).update()
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
                    log.error 'scp from {} to {} error, from file: {}, to file: {}', nodeIp, targetNodeIp, hostRdbFilePath, backupFilePath, e
                    // any one failed, return false
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

    // for restore
    static boolean doDownload(RmBackupTemplateDTO backupTemplate, String nodeIp, String hostRdbFilePath, String backupFilePath, long backupFileSavedMillis) {
        if (backupTemplate.targetType == RmBackupTemplateDTO.TargetType.scp) {
            assert backupTemplate.targetNodeIps != null
            for (targetNodeIp in backupTemplate.targetNodeIps) {
                def kp = new NodeKeyPairDTO(ip: targetNodeIp).one()
                if (kp == null) {
                    log.warn 'node key pair not found, ip: {}', targetNodeIp
                    continue
                }
                try {
                    AgentCaller.instance.doSshCopyFrom(kp, nodeIp, hostRdbFilePath, backupFilePath, backupFileSavedMillis)
                    // any one success, return true
                    return true
                } catch (Exception e) {
                    log.error 'scp from {} to {} error, from file: {}, to file: {}', targetNodeIp, nodeIp, backupFilePath, hostRdbFilePath, e
                }
            }
            return false
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

    static boolean doDelete(RmBackupTemplateDTO backupTemplate, String backupFilePath) {
        if (backupTemplate.targetType == RmBackupTemplateDTO.TargetType.scp) {
            assert backupTemplate.targetNodeIps != null
            def instance = InMemoryAllContainerManager.instance
            def hbOkNodeInfList = instance.getHbOkNodeInfoList(RedisManager.CLUSTER_ID)
            if (!hbOkNodeInfList) {
                log.warn 'no hb ok node info list'
                return false
            }
            def nodeIp = hbOkNodeInfList[0].nodeIp

            String command = "rm " + backupFilePath
            for (targetNodeIp in backupTemplate.targetNodeIps) {
                def kp = new NodeKeyPairDTO(ip: targetNodeIp).one()
                if (kp == null) {
                    log.warn 'node key pair not found, ip: {}', targetNodeIp
                    continue
                }
                try {
                    AgentCaller.instance.doSshExec(kp, nodeIp, command)
                } catch (Exception e) {
                    log.error 'ssh delete file, target node ip: {}, file: {}', targetNodeIp, backupFilePath, e
                    // ignore
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

    @CompileStatic
    record BackupUuid(String name, String dateTimeStr) {
    }

    static BackupUuid generateUuid(RmServiceDTO one, int shardIndex, Date givenDate, BackupPolicy backupPolicy) {
        final String namePrefix = 'rm-service-'

        def d = givenDate ?: new Date()
        def dateTimeStr = backupPolicy.dailyOrHourly == 'daily' ?
                d.format('yyyyMMdd') :
                d.format('yyyyMMddHH')
        def namePrefixService = namePrefix + one.id + '-' + dateTimeStr + '-'

        if (one.mode == RmServiceDTO.Mode.standalone) {
            return new BackupUuid(namePrefixService + 'standalone', dateTimeStr)
        } else if (one.mode == RmServiceDTO.Mode.sentinel) {
            return new BackupUuid(namePrefixService + 'from-slave', dateTimeStr)
        } else {
            return new BackupUuid(namePrefixService + 'shard-' + shardIndex, dateTimeStr)
        }
    }

    void removeOldBackupLogs(int serviceId, BackupPolicy backupPolicy) {
        String expiredDateTimeStr
        if (backupPolicy.dailyOrHourly == 'daily') {
            def expireDate = new Date() - backupPolicy.retentionPeriod
            expiredDateTimeStr = expireDate.format('yyyyMMdd')
        } else {
            def expireDate = new Date(new Date().time - backupPolicy.retentionPeriod * 3600 * 1000)
            expiredDateTimeStr = expireDate.format('yyyyMMddHH')
        }

        def backupTemplate = getBackupTemplate(backupPolicy.backupTemplateId)

        def expiredBackupLogList = new RmBackupLogDTO()
                .where('service_id = ?', serviceId)
                .where('date_time_str < ?', expiredDateTimeStr)
                .list()
        for (expiredBackupLog in expiredBackupLogList) {
            def backupFilePath = getBackupFilePath(backupPolicy, expiredBackupLog)
            doDelete(backupTemplate, backupFilePath)
            new RmBackupLogDTO(id: expiredBackupLog.id).delete()
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

        for (one in autoBackupServiceList) {
            def backupPolicy = one.backupPolicy
            // for test
            if (!backupPolicy.backupTemplateId) {
                log.info 'no backup template id for service {}, ignore', one.name
                continue
            }

            if (backupPolicy.isBackupWindowSpecify) {
                assert backupPolicy.startTime && backupPolicy.durationHours
                // check time window
                def nowStr = new Date().format('HH:mm')
                if (nowStr < backupPolicy.startTime || nowStr > backupPolicy.endTime()) {
                    log.debug 'not in backup window, ignore'
                    continue
                }
            }

            // check if there are old backup logs need remove, every 30 minutes
            if (intervalCount % (6 * 30) == 0) {
                removeOldBackupLogs(one.id, backupPolicy)
            }

            if (one.mode == RmServiceDTO.Mode.standalone) {
                def uuid = generateUuid(one, 0, null, backupPolicy)
                def checkResult = isNeedDoBackup(one.id, uuid.name)
                if (!checkResult.isNeedDoBackup) {
                    log.debug 'no need to backup, name: {}', uuid.name
                    continue
                }

                def runningContainerList = one.runningContainerList()
                if (!runningContainerList) {
                    log.warn 'no running instance for service {}, ignore', one.name
                    continue
                }
                def x = runningContainerList[0]

                new RmBackupLogDTO(name: uuid.name, serviceId: one.id).deleteAll()
                def backupLogOne = new RmBackupLogDTO(
                        name: uuid.name,
                        dateTimeStr: uuid.dateTimeStr,
                        serviceId: one.id,
                        shardIndex: 0,
                        replicaIndex: 0,
                        status: RmBackupLogDTO.Status.created,
                        backupTemplateId: backupPolicy.backupTemplateId,
                        createdDate: new Date())
                def backupLogId = backupLogOne.add()
                backupLogOne.id = backupLogId

                RmJobExecutor.instance.execute {
                    doBackup(one, backupPolicy, backupLogOne, x, checkResult)
                }
            } else if (one.mode == RmServiceDTO.Mode.sentinel) {
                def uuid = generateUuid(one, 0, null, backupPolicy)
                def checkResult = isNeedDoBackup(one.id, uuid.name)
                if (!checkResult.isNeedDoBackup) {
                    log.debug 'no need to backup, name: {}', uuid.name
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

                new RmBackupLogDTO(name: uuid.name, serviceId: one.id).deleteAll()
                def backupLogOne = new RmBackupLogDTO(
                        name: uuid.name,
                        dateTimeStr: uuid.dateTimeStr,
                        serviceId: one.id,
                        shardIndex: 0,
                        replicaIndex: slaveX.instanceIndex(),
                        status: RmBackupLogDTO.Status.created,
                        backupTemplateId: backupPolicy.backupTemplateId,
                        createdDate: new Date())
                def backupLogId = backupLogOne.add()
                backupLogOne.id = backupLogId

                RmJobExecutor.instance.execute {
                    doBackup(one, backupPolicy, backupLogOne, slaveX, checkResult)
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

                    def uuid = generateUuid(one, shard.shardIndex, null, backupPolicy)
                    def checkResult = isNeedDoBackup(one.id, uuid.name)
                    if (!checkResult.isNeedDoBackup) {
                        log.debug 'no need to backup, name: {}', uuid.name
                        continue
                    }

                    def slaveX = runningContainerList.find { x ->
                        x.instanceIndex() == slaveNode.replicaIndex
                    }
                    if (!slaveX) {
                        log.warn 'no slave instance for shard {}, service {}, ignore', shard.shardIndex, one.name
                        continue
                    }

                    new RmBackupLogDTO(name: uuid.name, serviceId: one.id).deleteAll()
                    def backupLogOne = new RmBackupLogDTO(
                            name: uuid.name,
                            dateTimeStr: uuid.dateTimeStr,
                            serviceId: one.id,
                            shardIndex: shard.shardIndex,
                            replicaIndex: slaveX.instanceIndex(),
                            status: RmBackupLogDTO.Status.created,
                            backupTemplateId: backupPolicy.backupTemplateId,
                            createdDate: new Date())
                    def backupLogId = backupLogOne.add()
                    backupLogOne.id = backupLogId

                    RmJobExecutor.instance.execute {
                        doBackup(one, backupPolicy, backupLogOne, slaveX, checkResult)
                    }
                }
            }
        }
    }
}
