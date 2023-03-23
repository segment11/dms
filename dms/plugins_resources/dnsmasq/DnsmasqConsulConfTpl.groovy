package dnsmasq

import model.server.ContainerMountTplHelper

def port = super.binding.getProperty('port') as int
def defaultServer = super.binding.getProperty('defaultServer') as String

def consulAppName = super.binding.getProperty('consulAppName') as String
def applications = super.binding.getProperty('applications') as ContainerMountTplHelper

List<String> r = []
r << "port=${port}".toString()

def app = applications.app(consulAppName)
if (app) {
    app.allNodeIpList.each {
        r << "server=/consul/${it}#8600".toString()
    }
}

if (defaultServer) {
    defaultServer.split(',').each {
        r << "server=${it}".toString()
    }
}

r.join('\r\n')