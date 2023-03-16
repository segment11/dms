package traefik

import model.server.ContainerMountTplHelper

def logLevel = super.binding.getProperty('logLevel') as String
def logDir = super.binding.getProperty('logDir') as String
def serverPort = super.binding.getProperty('serverPort') as int
def dashboardPort = super.binding.getProperty('dashboardPort') as int
def prefix = super.binding.getProperty('prefix') as String

def zkAppName = super.binding.getProperty('zkAppName') as String
def applications = super.binding.getProperty('applications') as ContainerMountTplHelper
def app = applications.app(zkAppName)
def zkConnectString = app.allNodeIpList.collect { it + ':2181' }.join(',')

"""
logLevel="${logLevel}"
defaultEntryPoints=["http"]
[entryPoints]
    [entryPoints.http]
    address=":${serverPort}"
    [entryPoints.bar]
    address=":${dashboardPort}"
[traefikLog]
filePath="${logDir}/log.txt"
[accessLog]
filePath="${logDir}/access.log"
[api]
entryPoint="bar"
dashboard=true
[zookeeper]
endpoint="${zkConnectString}"
watch=true
prefix="${prefix}"
"""
