package ctrl.redis

import model.job.RmJobDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.group('/redis/job') {
    h.get('/list') { req, resp ->
        def rmServiceIdStr = req.param('rmServiceId')
        assert rmServiceIdStr
        def rmServiceId = rmServiceIdStr as int

        def p = req.param('pageNum')
        int pageNum = p ? p as int : 1
        final int pageSize = 10

        def dto = new RmJobDTO(busiId: rmServiceId)
        def pager = dto.listPager(pageNum, pageSize)

        pager
    }
}