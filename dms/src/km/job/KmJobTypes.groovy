package km.job

import com.segment.common.job.chain.JobType
import groovy.transform.CompileStatic

@CompileStatic
class KmJobTypes {
    static final JobType STANDALONE_CREATE = new JobType('standalone_create')
    static final JobType CLUSTER_CREATE = new JobType('cluster_create')
    static final JobType BROKER_SCALE_UP = new JobType('broker_scale_up')
    static final JobType BROKER_SCALE_DOWN = new JobType('broker_scale_down')
    static final JobType TOPIC_CREATE = new JobType('topic_create')
    static final JobType TOPIC_ALTER = new JobType('topic_alter')
    static final JobType TOPIC_DELETE = new JobType('topic_delete')
    static final JobType REASSIGN_PARTITIONS = new JobType('reassign_partitions')
    static final JobType PREFERRED_REPLICA_ELECTION = new JobType('preferred_replica_election')
    static final JobType FAILOVER = new JobType('failover')
    static final JobType SNAPSHOT = new JobType('snapshot')
    static final JobType IMPORT = new JobType('import')
}
