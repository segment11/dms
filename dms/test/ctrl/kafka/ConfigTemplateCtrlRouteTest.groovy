package ctrl.kafka

import com.alibaba.fastjson.JSON
import org.segment.d.Ds
import org.segment.web.handler.ChainHandler
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import support.DmsTestDbSupport
import utils.RouterInit

class ConfigTemplateCtrlRouteTest extends Specification {
    def h = ChainHandler.instance

    private Ds ds

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        def d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, '''
create table km_config_template
(
    id           int auto_increment primary key,
    name         varchar(50),
    des          varchar(200),
    config_items text,
    updated_date timestamp default current_timestamp
);
create table km_service
(
    id                 int auto_increment primary key,
    name               varchar(50),
    config_template_id int,
    status             varchar(20)
)
''')
        h.clear()
        RouterInit.init(ConfigTemplateCtrl)
    }

    def cleanup() {
        h.clear()
        ds?.closeConnect()
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    private MockHttpServletResponse httpPost(String path, String jsonBody) {
        def request = new MockHttpServletRequest('POST', '/dms/kafka/config-template' + path)
        request.setContent(jsonBody.getBytes('UTF-8'))
        request.setContentType('application/json')
        def response = new MockHttpServletResponse()
        h.handle(request, response)
        response
    }

    private MockHttpServletRequest requestWithQuery(String method, String fullPath) {
        def queryIdx = fullPath.indexOf('?')
        def path = queryIdx < 0 ? fullPath : fullPath.substring(0, queryIdx)
        def request = new MockHttpServletRequest(method, '/dms/kafka/config-template' + path)
        if (queryIdx >= 0) {
            request.setQueryString(fullPath.substring(queryIdx + 1))
            fullPath.substring(queryIdx + 1).split('&').each { pair ->
                def kv = pair.split('=', 2)
                request.addParameter(kv[0], kv.length > 1 ? kv[1] : '')
            }
        }
        request
    }

    private MockHttpServletResponse httpGet(String path) {
        def request = requestWithQuery('GET', path)
        def response = new MockHttpServletResponse()
        h.handle(request, response)
        response
    }

    private MockHttpServletResponse httpDelete(String path) {
        def request = requestWithQuery('DELETE', path)
        def response = new MockHttpServletResponse()
        h.handle(request, response)
        response
    }

    void 'add then list shows the template in pager'() {
        when:
        def addResp = httpPost('/add', '''{"name":"tpl-72h","des":"retention template",
            "configItems":{"items":[{"key":"log.retention.hours","value":"72"}]}}''')

        then:
        addResp.status == 200
        def added = JSON.parseObject(addResp.contentAsString, Map)
        added.id > 0

        when:
        def listResp = httpGet('/list?pageNum=1&pageSize=10')

        then:
        listResp.status == 200
        def pager = JSON.parseObject(listResp.contentAsString, Map)
        pager.totalCount == 1
        def first = (pager.list as List)[0] as Map
        first.name == 'tpl-72h'
        ((first.configItems as Map).items as List).size() == 1
    }

    void 'add with duplicate name halts 409'() {
        given:
        httpPost('/add', '{"name":"dup","des":"one"}')

        when:
        def resp = httpPost('/add', '{"name":"dup","des":"two"}')

        then:
        resp.status == 409
    }

    void 'update changes stored config items'() {
        given:
        def addResp = httpPost('/add', '''{"name":"tpl-a","configItems":{"items":[{"key":"a","value":"1"}]}}''')
        def id = JSON.parseObject(addResp.contentAsString, Map).id

        when:
        def updateResp = httpPost('/update', """{"id":${id},"name":"tpl-a","configItems":{"items":[{"key":"a","value":"2"}]}}""")

        then:
        updateResp.status == 200
        def flag = JSON.parseObject(updateResp.contentAsString, Map)
        flag.flag == true

        when:
        def listResp = httpGet('/list?pageNum=1&pageSize=10')

        then:
        def pager = JSON.parseObject(listResp.contentAsString, Map)
        def first = (pager.list as List)[0] as Map
        ((first.configItems as Map).items as List)[0].value == '2'
    }

    void 'delete removes template and halts 404 when missing'() {
        given:
        def addResp = httpPost('/add', '{"name":"tpl-del"}')
        def id = JSON.parseObject(addResp.contentAsString, Map).id

        when:
        def delResp = httpDelete('/delete?id=' + id)

        then:
        delResp.status == 200
        JSON.parseObject(delResp.contentAsString, Map).flag == true

        when:
        def missingResp = httpDelete('/delete?id=' + id)

        then:
        missingResp.status == 404
    }
}
