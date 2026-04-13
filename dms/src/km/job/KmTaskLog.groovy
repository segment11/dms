package km.job

import com.segment.common.job.chain.JobTask
import groovy.transform.CompileStatic
import model.job.KmTaskLogDTO
import org.segment.d.json.DefaultJsonTransformer
import org.segment.d.json.JsonTransformer

@CompileStatic
class KmTaskLog extends JobTask.TaskLog {
    private JsonTransformer json = new DefaultJsonTransformer()

    @Override
    int add() {
        new KmTaskLogDTO(jobId: jobId, step: step, jobResult: json.json(jobResult),
                createdDate: new Date(), updatedDate: new Date()).add()
    }

    @Override
    void update() {
        new KmTaskLogDTO(id: id, jobResult: json.json(jobResult), costMs: costMs, updatedDate: new Date()).update()
    }
}
