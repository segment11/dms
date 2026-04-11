package ctrl

import com.github.kevinsawicki.http.HttpRequest
import org.segment.web.RouteServer
import org.segment.web.handler.ChainHandler
import spock.lang.Specification

class FirstCtrlRouteTest extends Specification {
    private static final int PORT = 5115

    def setup() {
        RouteServer.instance.stop()
        clearRoutes()
    }

    def cleanup() {
        RouteServer.instance.stop()
        clearRoutes()
    }

    def 'route list returns the routes registered by the first controller script'() {
        given:
        new GroovyShell(this.class.classLoader).evaluate(new File('src/ctrl/First.groovy'))

        when:
        RouteServer.instance.start(PORT, '127.0.0.1')
        Thread.sleep(1000)
        def body = HttpRequest.get("http://127.0.0.1:${PORT}/route/list").body()

        then:
        body.contains('GET:/route/list')
        body.contains('GET:/hz')
        body.contains('GET:/leader/hz')
    }

    private static void clearRoutes() {
        def handler = ChainHandler.instance
        handler.list.clear()
        handler.beforeList.clear()
        handler.afterList.clear()
        handler.afterAfterList.clear()
        handler.context(null)
        handler.exceptionHandler(null)
    }
}
