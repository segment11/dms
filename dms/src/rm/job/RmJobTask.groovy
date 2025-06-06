package rm.job

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobTask
import groovy.transform.CompileStatic
import model.job.RmTaskLogDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer

@CompileStatic
abstract class RmJobTask extends JobTask implements Serializable {
    private JsonTransformer json = new DefaultJsonTransformer()

    @Override
    TaskLog load(Integer jobId, String stepAsUuid) {
        def one = new RmTaskLogDTO(jobId: jobId, step: stepAsUuid).one()
        if (!one) {
            return null
        }

        def r = new RmTaskLog()
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
        new RmTaskLog()
    }
}
