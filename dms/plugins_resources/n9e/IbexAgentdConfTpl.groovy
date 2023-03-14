package n9e

import model.server.ContainerMountTplHelper

ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp ibexApp = applications.app('ibex')

"""
# debug, release
RunMode = "release"

# task meta storage dir
MetaDir = "./meta"

[HTTP]
Enable = true
# http listening address
Host = "0.0.0.0"
# http listening port
Port = 2090
# https cert file path
CertFile = ""
# https key file path
KeyFile = ""
# whether print access log
PrintAccessLog = true
# whether enable pprof
PProf = false
# http graceful shutdown timeout, unit: s
ShutdownTimeout = 30
# max content length: 64M
MaxContentLength = 67108864
# http server read timeout, unit: s
ReadTimeout = 20
# http server write timeout, unit: s
WriteTimeout = 40
# http server idle timeout, unit: s
IdleTimeout = 120

[Heartbeat]
# unit: ms
Interval = 1000
# rpc servers
Servers = ["${ibexApp.allNodeIpList[0]}:20090"]
# \$ip or \$hostname or specified string
Host = "categraf01"
"""