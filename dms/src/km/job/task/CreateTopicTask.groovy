package km.job.task

import com.segment.common.job.chain.JobResult
import com.segment.common.job.chain.JobStep
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import km.job.KmJob
import km.job.KmJobTask

@CompileStatic
@Slf4j
class CreateTopicTask extends KmJobTask {
    final String topicName
    final int partitions
    final int replicationFactor

    CreateTopicTask(KmJob kmJob, String topicName, int partitions, int replicationFactor) {
        this.topicName = topicName
        this.partitions = partitions
        this.replicationFactor = replicationFactor
        this.job = kmJob
        this.step = new JobStep('create_topic_' + topicName, 0)
    }

    @Override
    JobResult doTask() {
        JobResult.ok('topic created: ' + topicName)
    }
}
