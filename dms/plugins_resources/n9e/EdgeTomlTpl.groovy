package n9e

String centerAddr = super.binding.getProperty('centerAddr')
String centerAuthUser = super.binding.getProperty('centerAuthUser')
String centerAuthPass = super.binding.getProperty('centerAuthPass')

String prometheusAddr = super.binding.getProperty('prometheusAddr')

"""
[Global]
RunMode = "release"

[CenterApi]
Addrs = ["${centerAddr}"]
BasicAuthUser = "${centerAuthUser}"
BasicAuthPass = "${centerAuthPass}"
# unit: ms
Timeout = 9000

[Log]
# log write dir
Dir = "logs"
# log level: DEBUG INFO WARNING ERROR
Level = "DEBUG"
# stdout, stderr, file
Output = "stdout"
# # rotate by time
# KeepHours = 4
# # rotate by size
# RotateNum = 3
# # unit: MB
# RotateSize = 256

[HTTP]
# http listening address
Host = "0.0.0.0"
# http listening port
Port = 19000
# https cert file path
CertFile = ""
# https key file path
KeyFile = ""
# whether print access log
PrintAccessLog = false
# whether enable pprof
PProf = false
# expose prometheus /metrics?
ExposeMetrics = true
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

[HTTP.APIForAgent]
Enable = true 
# [HTTP.APIForAgent.BasicAuth]
# user001 = "ccc26da7b9aba533cbb263a36c07dcc5"

[HTTP.APIForService]
Enable = true 
[HTTP.APIForService.BasicAuth]
user001 = "ccc26da7b9aba533cbb263a36c07dcc5"

[Alert]
[Alert.Heartbeat]
# auto detect if blank
IP = ""
# unit ms
Interval = 1000
EngineName = "edge"

# [Alert.Alerting]
# NotifyConcurrency = 10

[Pushgw]
# use target labels in database instead of in series
LabelRewrite = true
# # default busigroup key name
# BusiGroupLabelKey = "busigroup"
# ForceUseServerTS = false

# [Pushgw.DebugSample]
# ident = "xx"
# __name__ = "xx"

# [Pushgw.WriterOpt]
# QueueMaxSize = 1000000
# QueuePopSize = 1000

[[Pushgw.Writers]] 
# Url = "http://127.0.0.1:8480/insert/0/prometheus/api/v1/write"
Url = "${prometheusAddr}/api/v1/write"
# Basic auth username
BasicAuthUser = ""
# Basic auth password
BasicAuthPass = ""
# timeout settings, unit: ms
Headers = ["X-From", "n9e"]
Timeout = 10000
DialTimeout = 3000
TLSHandshakeTimeout = 30000
ExpectContinueTimeout = 1000
IdleConnTimeout = 90000
# time duration, unit: ms
KeepAlive = 30000
MaxConnsPerHost = 0
MaxIdleConns = 100
MaxIdleConnsPerHost = 100
"""