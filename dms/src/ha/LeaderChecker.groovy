package ha

import groovy.transform.CompileStatic

@CompileStatic
interface LeaderChecker {
    boolean isLeader()

    void continueLeader()
}
