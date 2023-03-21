package patroni

import model.server.ContainerMountTplHelper

def maxConn = super.binding.getProperty('maxConn') as int
def statsPort = super.binding.getProperty('statsPort') as int
def port = super.binding.getProperty('port') as int

def patroniAppIsSingleNode = super.binding.getProperty('patroniAppIsSingleNode') as String
def isSingleNode = 'true' == patroniAppIsSingleNode

def patroniAppNames = super.binding.getProperty('patroniAppNames') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper

def list = []

patroniAppNames.split(',').each { patroniAppName ->
    ContainerMountTplHelper.OneApp patroniApp = applications.app(patroniAppName)
    def ymlOne = patroniApp.app.conf.fileVolumeList.find { it.dist == '/etc/patroni.yml' }
    def patroniPort = ymlOne.paramList.find { it.key == 'port' }.value as int
    def pgPort = ymlOne.paramList.find { it.key == 'pgPort' }.value as int
    def appId = patroniApp.app.id
    patroniApp.containerList.each { x ->
        def instanceIndex = x.instanceIndex()
        def step = 100 * instanceIndex
        def pgPublicPort = x.publicPort(pgPort)
        def patroniPublicPort = x.publicPort(patroniPort)
        list << "server pg${appId}_${instanceIndex} ${x.nodeIp}:${isSingleNode ? pgPublicPort + step : pgPublicPort} maxconn ${maxConn} ".toString() +
                "check port ${isSingleNode ? patroniPublicPort + step : patroniPublicPort}".toString()
    }
}

def pre = ''.padLeft(4, ' ')
def x = list.collect {
    pre + it
}.join('\n')

"""
global
    maxconn ${maxConn}

defaults
    log global
    mode tcp
    retries 2
    timeout client 30m
    timeout connect 4s
    timeout server 30m
    timeout check 5s

listen stats
    mode http
    bind *:${statsPort}
    stats enable
    stats uri /

listen batman
    bind *:${port}
    option httpchk
    http-check expect status 200
    default-server inter 3s fall 3 rise 2 on-marked-down shutdown-sessions
${x}
"""