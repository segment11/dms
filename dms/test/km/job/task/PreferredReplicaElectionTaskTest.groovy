package km.job.task

import org.apache.zookeeper.data.Stat
import spock.lang.Specification

class PreferredReplicaElectionTaskTest extends Specification {

    private static Stat statWithCtime(long ctime) {
        Stat stat = new Stat()
        stat.setCtime(ctime)
        stat
    }

    void 'fresh election path is not stale'() {
        given:
        long now = System.currentTimeMillis()

        expect:
        !PreferredReplicaElectionTask.isStaleElection(statWithCtime(now - 1000L), now)
    }

    void 'election path exactly at threshold is stale'() {
        given:
        long now = System.currentTimeMillis()

        expect:
        PreferredReplicaElectionTask.isStaleElection(
                statWithCtime(now - PreferredReplicaElectionTask.STALE_THRESHOLD_MS), now)
    }

    void 'election path older than threshold is stale'() {
        given:
        long now = System.currentTimeMillis()

        expect:
        PreferredReplicaElectionTask.isStaleElection(
                statWithCtime(now - PreferredReplicaElectionTask.STALE_THRESHOLD_MS - 1L), now)
    }

    void 'null stat is not stale so caller keeps the in-progress guard'() {
        expect:
        !PreferredReplicaElectionTask.isStaleElection(null, System.currentTimeMillis())
    }

    void 'stale threshold is 5 minutes'() {
        expect:
        PreferredReplicaElectionTask.STALE_THRESHOLD_MS == 5 * 60 * 1000L
    }

    void 'max attempts is positive and small'() {
        expect:
        PreferredReplicaElectionTask.MAX_ATTEMPTS > 0
        PreferredReplicaElectionTask.MAX_ATTEMPTS <= 5
    }
}
