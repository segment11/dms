package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

@CompileStatic
@Slf4j
class ValidateZookeeperTask extends KmJobTask {
    ValidateZookeeperTask(KmJob kmJob) {
        this.job = kmJob
        this.step = new JobStep('validate_zookeeper', 0)
    }

    static boolean isValidChroot(String chroot) {
        if (chroot == null) return false
        if (chroot.length() <= 1) return false
        if (!chroot.startsWith('/')) return false
        true
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        if (!isValidChroot(kmService.zkChroot)) {
            return JobResult.fail('invalid zk chroot: ' + kmService.zkChroot)
        }

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString,
                new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            if (client.checkExists().forPath('/') == null) {
                client.create().creatingParentsIfNeeded().forPath('/')
            }

            def brokersPath = '/brokers/ids'
            if (client.checkExists().forPath(brokersPath) != null) {
                def children = client.getChildren().forPath(brokersPath)
                if (children) {
                    return JobResult.fail('cluster already exists, brokers found: ' + children.size())
                }
            }

            return JobResult.ok('zookeeper validated')
        } catch (Exception e) {
            log.error('validate zookeeper error', e)
            return JobResult.fail('validate zookeeper error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
