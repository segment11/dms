package script.tpl

import common.Conf

def nodeIp = super.binding.getProperty('nodeIp') as String
def instanceIndex = super.binding.getProperty('instanceIndex') as int
def isMasterSlave = '1' == super.binding.getProperty('isMasterSlave')
def dataDir = '/var/lib/mysql'
def logDir = '/var/log/mysql'
// eg. key1=value1,key2=value2
def customParameters = super.binding.getProperty('customParameters') as String
String customSegment = ''
if (customParameters) {
    customSegment = customParameters.split(',').join('\r\n')
}

if (!isMasterSlave) {
    return """
[mysqld]
skip-host-cache
skip-name-resolve
datadir = ${dataDir}
socket = /var/run/mysqld/mysqld.sock
secure-file-priv = /var/lib/mysql-files
user = mysql
symbolic-links = 0
pid-file = /var/run/mysqld/mysqld.pid
log-error = ${logDir}/mysqld.log
innodb-use-native-aio = ${Conf.isWindows() ? 0 : 1}
explicit-defaults-for-timestamp = on
#query
slow-query-log = 1
long-query-time = 2
log-queries-not-using-indexes = 0
slow-query-log-file = ${logDir}/slow.log
${customSegment}
[client]
socket=/var/run/mysqld/mysqld.sock
"""
}

// master slave
if (0 == instanceIndex) {
    return """
[mysqld]
skip-host-cache
skip-name-resolve
datadir = ${dataDir}
socket = /var/run/mysqld/mysqld.sock
secure-file-priv = /var/lib/mysql-files
user = mysql
symbolic-links = 0
pid-file = /var/run/mysqld/mysqld.pid
log-error = ${logDir}/mysqld.log
innodb-use-native-aio = ${Conf.isWindows() ? 0 : 1}
explicit-defaults-for-timestamp = on
#binlog
server-id = 1
report-host = ${nodeIp}
gtid-mode = on
enforce-gtid-consistency = on
log-bin = mysql-bin 
binlog-format = ROW
log-slave-updates = on
skip-slave-start = 1
expire-logs-days = 7
max-binlog-size = 1G
binlog-ignore-db = mysql
binlog-ignore-db = sys
binlog-ignore-db = information_schema
binlog-ignore-db = performance_schema
#query
slow-query-log = 1
long-query-time = 2
log-queries-not-using-indexes = 0
slow-query-log-file = ${logDir}/slow.log
log-slow-slave-statements = 1
${customSegment}
[client]
socket=/var/run/mysqld/mysqld.sock
"""
}

// slave
return """
[mysqld]
skip-host-cache
skip-name-resolve
datadir = ${dataDir}
socket = /var/run/mysqld/mysqld.sock
secure-file-priv = /var/lib/mysql-files
user = mysql
symbolic-links = 0
pid-file = /var/run/mysqld/mysqld.pid
log-error = ${logDir}/mysqld.log
innodb-use-native-aio = ${Conf.isWindows() ? 0 : 1}
explicit-defaults-for-timestamp = on
#binlog
server-id = ${instanceIndex + 1}
report-host = ${nodeIp}
gtid-mode = on
enforce-gtid-consistency = on
log-bin = mysql-slave-bin
binlog-format = ROW
log-slave-updates = on
skip-slave-start = 1
read-only = 1
expire-logs-days = 7
max-binlog-size = 100m
replicate-ignore-db = information_schema
replicate-ignore-db = performance_schema
replicate-ignore-db = mysql
replicate-ignore-db = sys
#query
slow-query-log = 1
long-query-time = 2
log-queries-not-using-indexes = 0
slow-query-log-file = ${logDir}/slow.log
${customSegment}
[client]
socket=/var/run/mysqld/mysqld.sock
"""
