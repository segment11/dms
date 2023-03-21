package n9e

import model.server.ContainerMountTplHelper

ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp redisApp = applications.app('redis')
ContainerMountTplHelper.OneApp ibexApp = applications.app('ibex')

String host = super.binding.getProperty('host')
String port = super.binding.getProperty('port')
String user = super.binding.getProperty('user')
String password = super.binding.getProperty('password')

String promAddressList = super.binding.getProperty('promAddressList')
def arr = promAddressList.split(',')

List<String> list = []
arr.eachWithIndex { str, i ->
    list << """
[[Clusters]]
# Prometheus cluster name
### only Default ? can trigger notify rule messages
Name = "${i == 0 ? 'Default' : 'Default' + i}"
# Prometheus APIs base url
Prom = "${str}"
# Basic auth username
BasicAuthUser = ""
# Basic auth password
BasicAuthPass = ""
# timeout settings, unit: ms
Timeout = 30000
DialTimeout = 3000
MaxIdleConnsPerHost = 100
""".toString()
}

String clusterInner = list.join("\r\n")

"""
# debug, release
RunMode = "release"

# # custom i18n dict config
# I18N = "./etc_ext/i18n.json"

# # custom i18n request header key
# I18NHeaderKey = "X-Language"

# metrics descriptions
MetricsYamlFile = "./etc_ext/metrics.yaml"

BuiltinAlertsDir = "./etc_ext/alerts"
BuiltinDashboardsDir = "./etc_ext/dashboards"

# config | api
ClustersFrom = "config"

# using when ClustersFrom = "api"
ClustersFromAPIs = []

[[NotifyChannels]]
Label = "邮箱"
# do not change Key
Key = "email"

[[NotifyChannels]]
Label = "钉钉机器人"
# do not change Key
Key = "dingtalk"

[[NotifyChannels]]
Label = "企微机器人"
# do not change Key
Key = "wecom"

[[NotifyChannels]]
Label = "飞书机器人"
# do not change Key
Key = "feishu"

[[NotifyChannels]]
Label = "mm bot"
# do not change Key
Key = "mm"

[[NotifyChannels]]
Label = "telegram机器人"
# do not change Key
Key = "telegram"

[[ContactKeys]]
Label = "Wecom Robot Token"
# do not change Key
Key = "wecom_robot_token"

[[ContactKeys]]
Label = "Dingtalk Robot Token"
# do not change Key
Key = "dingtalk_robot_token"

[[ContactKeys]]
Label = "Feishu Robot Token"
# do not change Key
Key = "feishu_robot_token"

[[ContactKeys]]
Label = "Feishu User ID"
# do not change Key
Key = "feishu_user_id"

[[ContactKeys]]
Label = "MatterMost Webhook URL"
# do not change Key
Key = "mm_webhook_url"

[[ContactKeys]]
Label = "Telegram Robot Token"
# do not change Key
Key = "telegram_robot_token"

[Log]
# log write dir
Dir = "logs"
# log level: DEBUG INFO WARNING ERROR
Level = "DEBUG"
# stdout, stderr, file
Output = "stdout"
# # rotate by time
# KeepHours: 4
# # rotate by size
# RotateNum = 3
# # unit: MB
# RotateSize = 256

[HTTP]
# http listening address
Host = "0.0.0.0"
# http listening port
Port = 18000
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

[JWTAuth]
# signing key
SigningKey = "5b94a0fd640fe2765af826acfe42d151"
# unit: min
AccessExpired = 1500
# unit: min
RefreshExpired = 10080
RedisKeyPrefix = "/jwt/"

[ProxyAuth]
# if proxy auth enabled, jwt auth is disabled
Enable = false
# username key in http proxy header
HeaderUserNameKey = "X-User-Name"
DefaultRoles = ["Standard"]

[BasicAuth]
user001 = "ccc26da7b9aba533cbb263a36c07dcc5"

[AnonymousAccess]
PromQuerier = false
AlertDetail = false

[LDAP]
Enable = false
Host = "ldap.example.org"
Port = 389
BaseDn = "dc=example,dc=org"
# AD: manange@example.org
BindUser = "cn=manager,dc=example,dc=org"
BindPass = "*******"
# openldap format e.g. (&(uid=%s))
# AD format e.g. (&(sAMAccountName=%s))
AuthFilter = "(&(uid=%s))"
CoverAttributes = true
TLS = false
StartTLS = true
# ldap user default roles
DefaultRoles = ["Standard"]

[LDAP.Attributes]
Nickname = "cn"
Phone = "mobile"
Email = "mail"

[OIDC]
Enable = false
RedirectURL = "http://n9e.com/callback"
SsoAddr = "http://sso.example.org"
ClientId = ""
ClientSecret = ""
CoverAttributes = true
DefaultRoles = ["Standard"]

[OIDC.Attributes]
Nickname = "nickname"
Phone = "phone_number"
Email = "email"

[Redis]
# address, ip:port
Address = "${redisApp.allNodeIpList[0]}:6379"
# requirepass
Password = ""
# # db
# DB = 0

[DB]
# postgres: host=%s port=%s user=%s dbname=%s password=%s sslmode=%s
DSN="${user}:${password}@tcp(${host}:${port})/n9e_v5?charset=utf8mb4&parseTime=True&loc=Local&allowNativePasswords=true"
# enable debug mode or not
Debug = true
# mysql postgres
DBType = "mysql"
# unit: s
MaxLifetime = 7200
# max open connections
MaxOpenConns = 150
# max idle connections
MaxIdleConns = 50
# table prefix
TablePrefix = ""
# enable auto migrate or not
# EnableAutoMigrate = false

${clusterInner}

[Ibex]
Address = "http://${ibexApp.allNodeIpList[0]}:10090"
# basic auth
BasicAuthUser = "ibex"
BasicAuthPass = "ibex"
# unit: ms
Timeout = 3000

[TargetMetrics]
TargetUp = '''max(max_over_time(target_up{ident=~"(%s)"}[%dm])) by (ident)'''
LoadPerCore = '''max(max_over_time(system_load_norm_1{ident=~"(%s)"}[%dm])) by (ident)'''
MemUtil = '''100-max(max_over_time(mem_available_percent{ident=~"(%s)"}[%dm])) by (ident)'''
DiskUtil = '''max(max_over_time(disk_used_percent{ident=~"(%s)", path="/"}[%dm])) by (ident)'''
"""