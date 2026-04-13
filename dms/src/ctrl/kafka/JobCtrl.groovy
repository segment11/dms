package ctrl.kafka

import model.job.KmJobDTO
import model.job.KmTaskLogDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/kafka/job') {
    h.get('/list') { req, resp ->
        def serviceId = req.param('serviceId') as int
        def pageNum = (req.param('pageNum') ?: '1') as int
        def pageSize = (req.param('pageSize') ?: '10') as int

        def dto = new KmJobDTO(busiId: serviceId)
        dto.listPager(pageNum, pageSize)
    }

    h.get('/task/list') { req, resp ->
        def jobId = req.param('jobId') as int

        def list = new KmTaskLogDTO(jobId: jobId).list()
        [list: list]
    }
}
