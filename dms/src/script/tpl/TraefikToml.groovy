package script.tpl

def logLevel = super.binding.getProperty('logLevel') as String
def zkConnectString = super.binding.getProperty('zkConnectString') as String
def serverPort = super.binding.getProperty('serverPort') as int
def dashboardPort = super.binding.getProperty('dashboardPort') as int
def prefix = super.binding.getProperty('prefix') as String

"""
logLevel="${logLevel}"
defaultEntryPoints=["http"]
[entryPoints]
    [entryPoints.http]
    address=":${serverPort}"
    [entryPoints.bar]
    address=":${dashboardPort}"
[traefikLog]
filePath="/var/log/traefik/log.txt"
[accessLog]
filePath="/var/log/traefik/access.log"
[api]
entryPoint="bar"
dashboard=true
[zookeeper]
endpoint="${zkConnectString}"
watch=true
prefix="${prefix}"
"""
