package ctrl.kafka

import com.segment.common.job.chain.JobParams
import com.segment.common.job.chain.JobStatus
import km.KmJobExecutor
import km.job.KmJob
import km.job.KmJobTypes
import km.job.task.CreateTopicTask
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

        def one = new KmTopicDTO(serviceId: serviceId, name: name).one()
        if (!one) {
            resp.halt(404, 'topic not found')
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

        new KmTopicDTO(id: one.id, status: KmTopicDTO.Status.deleted, updatedDate: new Date()).update()

        [id: one.id]
    }

    h.post('/reassign') { req, resp ->
        def body = req.bodyAs(Map)
        def serviceId = body.serviceId as int
        def name = body.name as String

        def one = new KmTopicDTO(serviceId: serviceId, name: name).one()
        if (!one) {
            resp.halt(404, 'topic not found')
        }

        [id: one.id]
    }
}
