package rm.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmServiceDTO
import model.json.PrimaryReplicasDetail
import rm.job.RmJob
import rm.job.RmJobTask

@CompileStatic
@Slf4j
class WaitPrimaryReplicasStateTask extends RmJobTask {
    final RmServiceDTO rmService

    WaitPrimaryReplicasStateTask(RmJob rmJob) {
        this.rmService = rmJob.rmService

        this.job = rmJob
        this.step = new JobStep('wait_primary_replicas_state', 0)
    }

    int tryCount = 0

    @Override
    JobResult doTask() {
        assert rmService

        def jobResult = rmService.checkNodes()
        if (!jobResult.isOk) {
            Thread.sleep(1000 * 5)
            tryCount++

            if (tryCount > 10) {
                return jobResult
            } else {
                return doTask()
            }
        } else {
            // update after nodes updated
            def runningContainerList = rmService.runningContainerList()
            runningContainerList.each { x ->
                def node = new PrimaryReplicasDetail.Node()
                node.ip = x.nodeIp
                node.port = rmService.listenPort(x)
                node.replicaIndex = x.instanceIndex()
                // when first created, the first replica is primary
                node.isPrimary = node.replicaIndex == 0

                def find = rmService.primaryReplicasDetail.nodes.find { n -> n.replicaIndex == node.replicaIndex }
                if (find != null) {
                    log.warn "node ${node.replicaIndex} already exists, {}:{}", find.ip, find.port
                    rmService.primaryReplicasDetail.nodes.remove(find)
                }

                rmService.primaryReplicasDetail.nodes << node
            }

            new RmServiceDTO(id: rmService.id, primaryReplicasDetail: rmService.primaryReplicasDetail, updatedDate: new Date()).update()
            log.warn 'update primary replicas detail ok'
        }

        jobResult
    }
}
