package rm.job

import com.segment.common.job.chain.JobTask
import groovy.transform.CompileStatic
import model.job.RmTaskLogDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer

@CompileStatic
class RmTaskLog extends JobTask.TaskLog {
    private JsonTransformer json = new DefaultJsonTransformer()

    @Override
    int add() {
        new RmTaskLogDTO(jobId: jobId, step: step, jobResult: json.json(jobResult),
                createdDate: new Date(), updatedDate: new Date()).add()
    }

    @Override
    void update() {
        new RmTaskLogDTO(id: id, jobResult: json.json(jobResult), costMs: costMs, updatedDate: new Date()).update()
    }
}
