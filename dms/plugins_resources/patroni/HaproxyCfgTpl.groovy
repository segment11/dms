package patroni

import model.server.ContainerMountTplHelper

def maxConn = super.binding.getProperty('maxConn') as int
def statsPort = super.binding.getProperty('statsPort') as int
def port = super.binding.getProperty('port') as int

def patroniAppName = super.binding.getProperty('patroniAppName') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp patroniApp = applications.app(patroniAppName)

def patroniAppIsSingleNode = super.binding.getProperty('patroniAppIsSingleNode') as String
def isSingleNode = 'true' == patroniAppIsSingleNode
def list = []
patroniApp.containerList.each { x ->
    def instanceIndex = x.instanceIndex()
    def step = 100 * instanceIndex
    list << "server pg${instanceIndex} ${x.nodeIp}:${isSingleNode ? 5432 + step : x.publicPort(5432)} maxconn ${maxConn} ".toString() +
            "check port ${isSingleNode ? 4432 + step : x.publicPort(4432)}".toString()
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