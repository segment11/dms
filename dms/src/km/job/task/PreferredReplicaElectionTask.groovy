package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.CuratorPoolHolder
import km.job.KmJob
import km.job.KmJobTask
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.data.Stat

@CompileStatic
@Slf4j
class PreferredReplicaElectionTask extends KmJobTask {

    static final String ELECTION_PATH = '/admin/preferred_replica_election'
    static final long STALE_THRESHOLD_MS = 5 * 60 * 1000L
    static final int MAX_ATTEMPTS = 3

    PreferredReplicaElectionTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('preferred_replica_election', 0)
    }

    static boolean isStaleElection(Stat stat, long nowMs) {
        if (stat == null) {
            return false
        }
        nowMs - stat.ctime >= STALE_THRESHOLD_MS
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                Stat stat = client.checkExists().forPath(ELECTION_PATH)
                if (stat != null) {
                    if (!isStaleElection(stat, System.currentTimeMillis())) {
                        return JobResult.fail('preferred replica election already in progress')
                    }
                    log.warn 'deleting stale election path, ctime={}, ageMs={}',
                            stat.ctime, System.currentTimeMillis() - stat.ctime
                    client.delete().guaranteed().deletingChildrenIfNeeded().forPath(ELECTION_PATH)
                }

                try {
                    client.create().creatingParentsIfNeeded().forPath(ELECTION_PATH, '{}'.getBytes('UTF-8'))
                    return JobResult.ok('preferred replica election triggered')
                } catch (KeeperException.NodeExistsException e) {
                    // raced with another writer (or controller re-created it) between delete and create
                    log.warn 'election path exists after create, attempt {}/{}, {}', attempt, MAX_ATTEMPTS, e.message
                }
            }

            JobResult.fail('preferred replica election path conflict after ' + MAX_ATTEMPTS + ' attempts')
        } catch (Exception e) {
            log.error('preferred replica election error', e)
            JobResult.fail('preferred replica election error: ' + e.message)
        }
    }
}
