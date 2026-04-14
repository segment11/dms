package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask
import model.KmTopicDTO
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry

@CompileStatic
@Slf4j
class DeleteTopicTask extends KmJobTask {
    final String topicName

    DeleteTopicTask(KmJob kmJob, String topicName) {
        this.topicName = topicName
        this.job = kmJob
        this.step = new JobStep('delete_topic_' + topicName, 0)
    }

    @Override
    JobResult doTask() {
        def kmService = ((KmJob) job).kmService
        assert kmService

        def connectionString = kmService.zkConnectString + kmService.zkChroot
        def client = CuratorFrameworkFactory.newClient(connectionString,
                new ExponentialBackoffRetry(1000, 3))
        try {
            client.start()

            def topicPath = '/brokers/topics/' + topicName
            if (client.checkExists().forPath(topicPath) != null) {
                client.delete().deletingChildrenIfNeeded().forPath(topicPath)
            }

            def configPath = '/config/topics/' + topicName
            if (client.checkExists().forPath(configPath) != null) {
                client.delete().deletingChildrenIfNeeded().forPath(configPath)
            }

            def adminPath = '/admin/delete_topics/' + topicName
            if (client.checkExists().forPath('/admin/delete_topics') == null) {
                client.create().creatingParentsIfNeeded().forPath('/admin/delete_topics')
            }
            if (client.checkExists().forPath(adminPath) == null) {
                client.create().forPath(adminPath)
            }

            def existingTopic = new KmTopicDTO().where('service_id = ? and name = ?', kmService.id, topicName).one()
            if (existingTopic) {
                new KmTopicDTO(id: existingTopic.id, status: KmTopicDTO.Status.deleted, updatedDate: new Date()).update()
            }

            JobResult.ok('topic deleted: ' + topicName)
        } catch (Exception e) {
            log.error('delete topic error', e)
            JobResult.fail('delete topic error: ' + e.message)
        } finally {
            client.close()
        }
    }
}
