package rm.job

import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic

@CompileStatic
class RmJobSteps {
    // redis cluster steps
    static final JobStep CLUSTER_WAIT_INSTANCES_RUNNING = new JobStep('wait_instances_running', 0)
    static final JobStep CLUSTER_MEAT_INSTANCES = new JobStep('meat_instances', 1)
    static final JobStep CLUSTER_SET_SLOTS = new JobStep('set_slots', 2)
    static final JobStep CLUSTER_REPLICA_OF = new JobStep('replica_of', 3)
    static final JobStep CLUSTER_WAIT_STATE_OK = new JobStep('wait_state_ok', 4)
}
