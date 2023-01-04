package ctrl

import auth.User
import model.AppJobDTO
import model.AppJobLogDTO
import org.segment.web.handler.ChainHandler
import org.segment.web.handler.JsonWriter

def h = ChainHandler.instance

h.group('/app/job') {
    h.get('/list') { req, resp ->
        def appId = req.param('appId')
        assert appId
        User u = req.session('user') as User
        if (!u.isAccessApp(appId as int)) {
            resp.halt(403, 'not this app manager')
        }

        new AppJobDTO(appId: appId as int).orderBy('created_date desc').loadList(100)
    }.get('/log/list') { req, resp ->
        def jobId = req.param('jobId')
        assert jobId
        def job = new AppJobDTO(id: jobId as int).one()
        assert job

        User u = req.session('user') as User
        if (!u.isAccessApp(job.appId)) {
            resp.halt(403, 'not this app manager')
        }

        def list = new AppJobLogDTO(jobId: jobId as int).orderBy('instance_index desc').
                loadList(100)
        def group = list.groupBy { it.instanceIndex }

        List<String> strList = []
        group.each { instanceIndex, subList ->
            def isAllDone = subList.every { it.isOk }
            // sort
            def sortedList = subList.sort { a, b ->
                a.id <=> b.id
            }

            String str = """
 <li style="list-style: none;">
                    <span style="font-weight: bold; font-size: 14px; color: blue;">Instance: ${instanceIndex}</span>
                    <span style="font-weight: bold; font-size: 14px; color: blue;" class="${isAllDone ? 'bg-success' : 'bg-danger'}">All Done ? ${isAllDone}</span>
                    <table class="table table-bordered table-striped">
                        <tr>
                            <th width="30%">title</th>
                            <th width="10%">is ok</th>
                            <th width="40%">message</th>
                            <th width="20%">created date</th>
                        </tr>
                        ${
                sortedList.collect {
                    """
                       <tr>
                            <td>${it.title}</td>
                            <td><p ng-class="${it.isOk ? 'bg-success' : 'bg-danger'}">${it.isOk}</p></td>
                            <td><p style="max-width: 700px; background-color: #d9edf7; text-align: left;">${it.message}
                                <p></td>
                            <td>${it.createdDate.format('yyyy-MM-dd HH:mm:ss')}</td>
                        </tr>
"""
                }.join('\r\n')
            }
 
                    </table>
                </li>
"""
            strList << str
        }

        // angular refresh sucks, too many change
        def data = [:]
        data.str = strList.join('\r\n')
        data.number = list.size()
        data
    }.delete('/delete') { req, resp ->
        def id = req.param('id')
        assert id

        AppJobDTO one = new AppJobDTO(id: id as int).queryFields('id,appId').one()
        if (!one) {
            return [flag: true]
        }

        User u = req.session('user') as User
        if (!u.isAccessApp(one.appId)) {
            resp.halt(500, 'not this application manager')
        }

        one.delete()
        [flag: true]
    }
}

h.group('/api/job') {
    h.post('/step/add') { req, resp ->
        Map body = req.bodyAs()
        int jobId = body.remove('jobId') as int
        int instanceIndex = body.remove('instanceIndex') as int
        String title = body.remove('title') as String

        String message = JsonWriter.instance.json(body)

        new AppJobLogDTO(jobId: jobId, instanceIndex: instanceIndex, title: title,
                message: message, isOk: true, createdDate: new Date()).add()

        [flag: true]
    }
}