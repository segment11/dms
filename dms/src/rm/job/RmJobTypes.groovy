package rm.job

import com.segment.common.job.chain.JobType
import groovy.transform.CompileStatic

@CompileStatic
class RmJobTypes {
    static final JobType CLUSTER_CREATE = new JobType('cluster_create')
}
