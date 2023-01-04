package script.tpl

String nodeIp = super.binding.getProperty('nodeIp')
String user = super.binding.getProperty('user')
String password = super.binding.getProperty('password')

"""
# debug, release
RunMode = "release"

[Log]
# log write dir
Dir = "logs-server"
# log level: DEBUG INFO WARNING ERROR
Level = "INFO"
# stdout, stderr, file
Output = "stdout"
# # rotate by time
# KeepHours: 4
# # rotate by size
# RotateNum = 3
# # unit: MB
# RotateSize = 256

[HTTP]
Enable = true
# http listening address
Host = "0.0.0.0"
# http listening port
Port = 10090
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

[BasicAuth]
# using when call apis
ibex = "ibex"

[RPC]
Listen = "0.0.0.0:20090"

[Heartbeat]
# auto detect if blank
IP = ""
# unit: ms
Interval = 1000

[Output]
# database | remote
ComeFrom = "database"
AgtdPort = 2090

[Gorm]
# enable debug mode or not
Debug = false
# mysql postgres
DBType = "mysql"
# unit: s
MaxLifetime = 7200
# max open connections
MaxOpenConns = 50
# max idle connections
MaxIdleConns = 20
# table prefix
TablePrefix = ""

[MySQL]
# mysql address host:port
Address = "${nodeIp}:3306"
# mysql username
User = "${user}"
# mysql password
Password = "${password}"
# database name
DBName = "ibex"
# connection params
Parameters = "charset=utf8mb4&parseTime=True&loc=Local&allowNativePasswords=true"

[Postgres]
# pg address host:port
Address = "${nodeIp}:5432"
# pg user
User = "${user}"
# pg password
Password = "${password}"
# database name
DBName = "ibex"
# ssl mode
SSLMode = "disable"
"""
