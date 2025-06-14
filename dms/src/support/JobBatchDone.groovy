package support

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AppJobDTO

@CompileStatic
@Slf4j
class JobBatchDone {
    static void doneJobWhenFirstStart() {
        def undoneAppJobList = new AppJobDTO().queryFields('id,status').
                where('status != ?', AppJobDTO.Status.done).list()
        if (undoneAppJobList) {
            for (job in undoneAppJobList) {
                def oldStatus = job.status
                job.status = AppJobDTO.Status.done
                job.update()
                log.warn 'job id {} done - before {}', job.id, oldStatus
            }
        }
    }
}
