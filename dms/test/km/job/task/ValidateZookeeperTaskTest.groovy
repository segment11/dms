package km.job.task

import spock.lang.Specification

class ValidateZookeeperTaskTest extends Specification {
    void 'isValidChroot rejects root path'() {
        expect:
        !ValidateZookeeperTask.isValidChroot('/')
        !ValidateZookeeperTask.isValidChroot('')
        !ValidateZookeeperTask.isValidChroot(null)
        ValidateZookeeperTask.isValidChroot('/kafka/my-cluster')
        ValidateZookeeperTask.isValidChroot('/kafka/test')
    }
}
