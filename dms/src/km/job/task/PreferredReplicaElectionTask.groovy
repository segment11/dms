package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.CuratorPoolHolder
import km.job.KmJobTask

@CompileStatic
@Slf4j
class PreferredReplicaElectionTask extends KmJobTask {

    PreferredReplicaElectionTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('preferred_replica_election', 0)
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {

            def electionPath = '/admin/preferred_replica_election'
            if (client.checkExists().forPath(electionPath) != null) {
                return JobResult.fail('preferred replica election already in progress')
            }

            client.create().creatingParentsIfNeeded().forPath(electionPath, '{}'.getBytes('UTF-8'))

            JobResult.ok('preferred replica election triggered')
        } catch (Exception e) {
            log.error('preferred replica election error', e)
            JobResult.fail('preferred replica election error: ' + e.message)
        }
    }
}
