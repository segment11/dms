package server.scheduler

import common.Event
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppDTO
import model.AppJobDTO
import model.ClusterDTO
import server.DBLeaderFlagHolder
import server.InMemoryAllContainerManager
import spi.SpiSupport
import transfer.ContainerInfo

@CompileStatic
@Slf4j
class RunAppTask implements Runnable {
    private int appId

    RunAppTask(int appId) {
        this.appId = appId
    }

    @Override
    void run() {
        if (!DBLeaderFlagHolder.instance.isLeader) {
            log.warn 'i am not the leader'
            return
        }

        Event.builder().type(Event.Type.cluster).reason('cron job scheduler task run').
                result(appId).build().log().toDto().add()

        def app = new AppDTO(id: appId).one()
        if (!app.jobConf || !app.jobConf.isOn) {
            log.info 'job is not on - {}', appId
            return
        }

        def lock = SpiSupport.createLock()
        lock.lockKey = '/app/guard/' + app.id
        lock.exe {
            def cluster = new ClusterDTO(id: app.clusterId).one()

            def instance = InMemoryAllContainerManager.instance
            List<ContainerInfo> containerList = instance.getContainerList(app.clusterId, appId)

            def oneAppGuardian = new OneAppGuardian()
            oneAppGuardian.cluster = cluster
            oneAppGuardian.app = app
            oneAppGuardian.containerList = containerList

            boolean isOk = oneAppGuardian.guard()
            if (!isOk) {
                // guard method create job already
                def job = new AppJobDTO(appId: appId).orderBy('created_date desc').one()
                if (job && job.status == AppJobDTO.Status.created.val) {
                    oneAppGuardian.process(job, false)
                }
            }
        }
    }
}
