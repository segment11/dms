package rm.job

import com.segment.common.job.chain.Job
import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStatus
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.RmSentinelServiceDTO
import model.RmServiceDTO
import model.job.RmJobDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer
import spi.SpiSupport

@CompileStatic
@Slf4j
class RmJob extends Job implements Serializable {
    private JsonTransformer json = new DefaultJsonTransformer()

    RmServiceDTO rmService

    RmSentinelServiceDTO rmSentinelService

    @Override
    int appId() {
        rmService?.id ?: rmSentinelService.id
    }

    @Override
    void save() {
        def dto = new RmJobDTO()
        dto.busiId = rmService?.id ?: rmSentinelService.id
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
        new RmJobDTO(id: id, failedNum: failedNum, updatedDate: new Date()).update()
    }

    @Override
    void lockExecute(Closure<Void> cl) {
        def id = rmService?.id ?: rmSentinelService.id
        def name = rmService?.name ?: rmSentinelService.name

        def key = "redis_service_job_${id}".toString()

        def lock = SpiSupport.createLock()
        lock.lockKey = key
        boolean isDone = lock.exe {
            cl.call()
        }
        if (!isDone) {
            log.info 'get redis job lock fail - {}', name
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
        new RmJobDTO(id: id, status: status.name(), result: json.json(result), costMs: costMs, updatedDate: new Date()).update()
    }
}
