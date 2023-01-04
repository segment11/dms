package script.tpl

import common.Conf

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
innodb_use_native_aio = ${Conf.isWindows() ? 0 : 1}
explicit_defaults_for_timestamp = on
#query
slow_query_log = 1
long_query_time = 2
log_queries_not_using_indexes = 0
slow_query_log_file = ${logDir}/slow.log
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
innodb_use_native_aio = ${Conf.isWindows() ? 0 : 1}
explicit_defaults_for_timestamp = on
#binlog
server-id = 1
log_bin = mysql-bin 
binlog-format = ROW
log-slave-updates = on
skip_slave_start = 1
expire_logs_days = 7
max_binlog_size = 1G
binlog-ignore-db = mysql
binlog-ignore-db = sys
binlog-ignore-db = information_schema
binlog-ignore-db = performance_schema
#query
slow_query_log = 1
long_query_time = 2
log_queries_not_using_indexes = 0
slow_query_log_file = ${logDir}/slow.log
log_slow_slave_statements = 1
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
innodb_use_native_aio = ${Conf.isWindows() ? 0 : 1}
explicit_defaults_for_timestamp = on
#binlog
server-id = ${instanceIndex + 1}
relay_log = slave-relay-bin
log_slave_updates = 0
read_only = 1
expire_logs_days = 7
max_binlog_size = 100m
replicate_ignore_db = information_schema
replicate_ignore_db = performance_schema
replicate_ignore_db = mysql
replicate_ignore_db = sys
#query
slow_query_log = 1
long_query_time = 2
log_queries_not_using_indexes = 0
slow_query_log_file = ${logDir}/slow.log
${customSegment}
[client]
socket=/var/run/mysqld/mysqld.sock
"""
