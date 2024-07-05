package traefik

import com.segment.common.Utils
import common.Const

def logLevel = super.binding.getProperty('logLevel') as String
def logDir = super.binding.getProperty('logDir') as String
def serverPort = super.binding.getProperty('serverPort') as int
//def dashboardPort = super.binding.getProperty('dashboardPort') as int

"""
log:
  level: ${logLevel}
  filePath: ${logDir}/log.txt
accessLog:
  filePath: ${logDir}/access.log
api:
  insecure: true
  dashboard: true
entryPoints:
  web:
    address: :${serverPort}

providers:
  http:
    endpoint: "http://${Utils.localIp()}:${Const.SERVER_HTTP_LISTEN_PORT}" 
"""
