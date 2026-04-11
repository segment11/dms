package ctrl

import com.alibaba.fastjson.JSON
import org.segment.web.handler.ChainHandler
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import utils.RouterInit

class FirstCtrlRouteTest extends Specification {
    def h = ChainHandler.instance

    def setup() {
//        LocalGroovyScriptLoader.loadWhenFirstStart(true)
        h.clear()
    }

    def cleanup() {
        h.clear()
    }

    def 'route list handles an in-process mocked GET request'() {
        given:
        RouterInit.init(First)
        def request = new MockHttpServletRequest('GET', '/dms/route/list')
        def response = new MockHttpServletResponse()

        when:
        def handled = h.handle(request, response)
        def result = JSON.parseObject(response.contentAsString, HashMap)
        List<String> list = result.list as List
        then:
        handled
        response.status == 200
        list.contains('GET:/dms/route/list')
        list.contains('GET:/dms/hz')
        list.contains('GET:/dms/leader/hz')
    }
}
