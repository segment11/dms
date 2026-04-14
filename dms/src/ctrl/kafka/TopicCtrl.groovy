package ctrl.kafka

import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import km.KmJobExecutor
import km.job.KmJob
import km.job.KmJobTypes
import km.job.task.AlterTopicTask
import km.job.task.CreateTopicTask
import km.job.task.DeleteTopicTask
import km.job.task.ReassignPartitionsTask
import km.job.task.WaitReassignmentCompleteTask
import model.KmServiceDTO
import model.KmTopicDTO
import model.json.ExtendParams
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/topic') {
    h.get('/list') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        assert serviceIdStr
        def serviceId = serviceIdStr as int

        def list = new KmTopicDTO(serviceId: serviceId).list()
        list.findAll { it.status != KmTopicDTO.Status.deleted }
    }

    h.get('/one') { req, resp ->
        def serviceIdStr = req.param('serviceId')
        def name = req.param('name')
        assert serviceIdStr && name
        def serviceId = serviceIdStr as int

        def one = new KmTopicDTO(serviceId: serviceId, name: name).one()
        if (!one) {
            resp.halt(404, 'topic not found')
        }
        one
    }

    h.post('/add') { req, resp ->
        def body = req.bodyAs(Map)

        def serviceId = body.serviceId as int
        def name = body.name as String
        def partitions = body.partitions as int
        def replicationFactor = body.replicationFactor as int
        def configOverrides = body.configOverrides as Map

        if (!name) {
            resp.halt(409, 'name is required')
        }
        if (partitions <= 0) {
            resp.halt(409, 'partitions must be positive')
        }
        if (replicationFactor <= 0) {
            resp.halt(409, 'replicationFactor must be positive')
        }

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        def existOne = new KmTopicDTO(serviceId: serviceId, name: name).one()
        if (existOne) {
            resp.halt(409, 'topic already exists')
        }

        def topic = new KmTopicDTO()
        topic.serviceId = serviceId
        topic.name = name
        topic.partitions = partitions
        topic.replicationFactor = replicationFactor
        if (configOverrides) {
            topic.configOverrides = new ExtendParams(configOverrides)
        }
        topic.status = KmTopicDTO.Status.creating
        topic.createdDate = new Date()
        topic.updatedDate = new Date()

        def topicId = topic.add()
        topic.id = topicId

        def kmJob = new KmJob()
        kmJob.kmService = service
        kmJob.type = KmJobTypes.TOPIC_CREATE
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmTopicId', topicId.toString())

        kmJob.taskList << new CreateTopicTask(kmJob, name, partitions, replicationFactor)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: topicId]
    }

    h.post('/alter') { req, resp ->
        def body = req.bodyAs(Map)
        def serviceId = body.serviceId as int
        def name = body.name as String
        def newPartitions = body.partitions ? (body.partitions as int) : 0
        def configOverrides = body.configOverrides as Map

        def one = new KmTopicDTO(serviceId: serviceId, name: name).one()
        if (!one) {
            resp.halt(404, 'topic not found')
        }

        if (one.status != KmTopicDTO.Status.active) {
            resp.halt(409, 'topic must be active')
        }

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (newPartitions > 0 && newPartitions < one.partitions) {
            resp.halt(409, 'cannot decrease partitions')
        }

        if (newPartitions == 0 && !configOverrides) {
            resp.halt(409, 'nothing to alter')
        }

        Map<String, String> overridesMap = [:]
        if (configOverrides) {
            configOverrides.each { k, v -> overridesMap.put(k as String, v as String) }
        }

        def kmJob = new KmJob()
        kmJob.kmService = service
        kmJob.type = KmJobTypes.TOPIC_ALTER
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmTopicId', one.id.toString())

        kmJob.taskList << new AlterTopicTask(kmJob, name, newPartitions, overridesMap)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: one.id]
    }

    h.post('/delete') { req, resp ->
        def body = req.bodyAs(Map)
        def serviceId = body.serviceId as int
        def name = body.name as String

        def one = new KmTopicDTO(serviceId: serviceId, name: name).one()
        if (!one) {
            resp.halt(404, 'topic not found')
        }

        if (one.status != KmTopicDTO.Status.active) {
            resp.halt(409, 'topic must be active')
        }

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        new KmTopicDTO(id: one.id, status: KmTopicDTO.Status.deleting, updatedDate: new Date()).update()

        def kmJob = new KmJob()
        kmJob.kmService = service
        kmJob.type = KmJobTypes.TOPIC_DELETE
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmTopicId', one.id.toString())

        kmJob.taskList << new DeleteTopicTask(kmJob, name)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: one.id]
    }

    h.post('/reassign') { req, resp ->
        def body = req.bodyAs(Map)
        def serviceId = body.serviceId as int

        def service = new KmServiceDTO(id: serviceId).one()
        if (!service) {
            resp.halt(404, 'service not found')
        }

        if (service.status != KmServiceDTO.Status.running) {
            resp.halt(409, 'service must be running')
        }

        def kmJob = new KmJob()
        kmJob.kmService = service
        kmJob.type = KmJobTypes.REASSIGN_PARTITIONS
        kmJob.status = JobStatus.created
        kmJob.params = new JobParams()
        kmJob.params.put('kmServiceId', serviceId.toString())

        kmJob.taskList << new ReassignPartitionsTask(kmJob)
        kmJob.taskList << new WaitReassignmentCompleteTask(kmJob)

        kmJob.createdDate = new Date()
        kmJob.updatedDate = new Date()
        kmJob.save()

        KmJobExecutor.instance.execute {
            kmJob.run()
        }

        [id: serviceId]
    }
}
