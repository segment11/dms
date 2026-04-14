package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.CuratorPoolHolder
import km.job.KmJobTask
import model.KmTopicDTO

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
        def client = CuratorPoolHolder.instance.create(connectionString)
        try {

            def topicPath = '/brokers/topics/' + topicName
            if (client.checkExists().forPath(topicPath) != null) {
                client.delete().deletingChildrenIfNeeded().forPath(topicPath)
            }

            def configPath = '/config/topics/' + topicName
            if (client.checkExists().forPath(configPath) != null) {
                client.delete().deletingChildrenIfNeeded().forPath(configPath)
            }

            def existingTopic = new KmTopicDTO().where('service_id = ? and name = ?', kmService.id, topicName).one()
            if (existingTopic) {
                new KmTopicDTO(id: existingTopic.id, status: KmTopicDTO.Status.deleted, updatedDate: new Date()).update()
            }

            JobResult.ok('topic deleted: ' + topicName)
        } catch (Exception e) {
            log.error('delete topic error', e)
            JobResult.fail('delete topic error: ' + e.message)
        }
    }
}
