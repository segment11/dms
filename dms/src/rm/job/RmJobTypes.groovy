package rm.job

import com.segment.common.job.chain.JobType
import groovy.transform.CompileStatic

@CompileStatic
class RmJobTypes {
    static final JobType BASE_CREATE = new JobType('base_create')
    static final JobType SENTINEL_CREATE = new JobType('sentinel_create')
    static final JobType CLUSTER_CREATE = new JobType('cluster_create')

    static final JobType CLUSTER_SCALE = new JobType('cluster_scale')
    static final JobType REPLICAS_SCALE = new JobType('replicas_scale')

    static final JobType FAILOVER = new JobType('failover')

    static final JobType COPY_FROM = new JobType('copy_from')
}
