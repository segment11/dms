package proxy

import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.mitre.dsmiley.httpproxy.ProxyServlet

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

def server = new Server()

def connector = new ServerConnector(server)
connector.host = 'localhost'
connector.port = 3001
server.addConnector(connector)

def handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
handler.addServlet(new ServletHolder(new ProxyServlet() {
    @Override
    protected String getConfigParam(String key) {
        if (ProxyServlet.P_TARGET_URI == key) {
            return 'http://localhost:3000'
        }
        return super.getConfigParam(key)
    }

    @Override
    protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                     HttpRequest proxyRequest) throws IOException {
        proxyRequest.setHeader('X-WEBAUTH-USER', 'viewer')
        return super.doExecute(servletRequest, servletResponse, proxyRequest)
    }
}), '/*')
server.handler = handler

server.start()
server.join()



