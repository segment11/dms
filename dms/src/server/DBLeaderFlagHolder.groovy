package server

import com.segment.common.Utils
import com.segment.common.job.IntervalJob
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.DynConfigDTO

@CompileStatic
@Singleton
@Slf4j
class DBLeaderFlagHolder extends IntervalJob {
    boolean isLeader

    int ttl

    @Override
    String name() {
        'db leader flag holder'
    }

    @Override
    void doJob() {
        def isLeaderLastTime = isLeader
        isLeader = DynConfigDTO.acquireLock(Utils.localIp(), ttl == 0 ? (int) interval * 3 : ttl)
        if (isLeader != isLeaderLastTime) {
            log.info 'change leader, ip: {}, I am the leader: {}', Utils.localIp(), isLeader
        }
    }
}
