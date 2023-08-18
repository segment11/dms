package redis

def nodeIp = super.binding.getProperty('nodeIp') as String
def port = super.binding.getProperty('port') as int
def dataDir = super.binding.getProperty('dataDir') as String
def password = super.binding.getProperty('password') as String

// eg. key1 value1,key2 value2
def customParameters = super.binding.getProperty('customParameters') as String
String customSegment = ''
if (customParameters) {
    customSegment = customParameters.split(',').join('\r\n')
}

"""
bind ${nodeIp} -::1
port ${port}
logfile ${dataDir}/redis.log
dir ${dataDir}
${password ? 'requirepass ' + password : ''}
${password ? 'masterauth ' + password : ''}

rdbchecksum yes
daemonize no
io-threads-do-reads no
lua-replicate-commands yes
always-show-logo no
protected-mode yes
rdbcompression yes
rdb-del-sync-files no
activerehashing yes
stop-writes-on-bgsave-error yes
set-proc-title yes
dynamic-hz yes
lazyfree-lazy-eviction no
lazyfree-lazy-expire no
lazyfree-lazy-server-del no
lazyfree-lazy-user-del no
lazyfree-lazy-user-flush no
repl-disable-tcp-nodelay no
repl-diskless-sync no
gopher-enabled no
aof-rewrite-incremental-fsync yes
no-appendfsync-on-rewrite no
cluster-require-full-coverage yes
rdb-save-incremental-fsync yes
aof-load-truncated yes
aof-use-rdb-preamble yes
cluster-replica-no-failover no
cluster-slave-no-failover no
replica-lazy-flush no
slave-lazy-flush no
replica-serve-stale-data yes
slave-serve-stale-data yes
replica-read-only yes
slave-read-only yes
replica-ignore-maxmemory yes
slave-ignore-maxmemory yes
jemalloc-bg-thread yes
activedefrag no
syslog-enabled no
appendonly no
cluster-allow-reads-when-down no
crash-log-enabled yes
crash-memcheck-enabled yes
use-exit-on-panic no
disable-thp yes
cluster-allow-replica-migration yes
replica-announced yes
pidfile /var/run/redis_${port}.pid
syslog-ident redis
dbfilename dump.rdb
appendfilename appendonly.aof
supervised no
syslog-facility local0
repl-diskless-load disabled
loglevel notice
maxmemory-policy noeviction
appendfsync everysec
oom-score-adj no
acl-pubsub-default allchannels
sanitize-dump-payload no
databases 16
io-threads 1
auto-aof-rewrite-percentage 100
cluster-replica-validity-factor 10
cluster-slave-validity-factor 10
list-max-ziplist-size -2
tcp-keepalive 300
cluster-migration-barrier 1
active-defrag-cycle-min 1
active-defrag-cycle-max 25
active-defrag-threshold-lower 10
active-defrag-threshold-upper 100
lfu-log-factor 10
lfu-decay-time 1
replica-priority 100
slave-priority 100
repl-diskless-sync-delay 5
maxmemory-samples 5
maxmemory-eviction-tenacity 10
timeout 0
replica-announce-port 0
slave-announce-port 0
tcp-backlog 511
cluster-announce-bus-port 0
cluster-announce-port 0
cluster-announce-tls-port 0
repl-timeout 60
repl-ping-replica-period 10
repl-ping-slave-period 10
list-compress-depth 0
rdb-key-save-delay 0
key-load-delay 0
active-expire-effort 1
hz 10
min-replicas-to-write 0
min-slaves-to-write 0
min-replicas-max-lag 10
min-slaves-max-lag 10
maxclients 10000
active-defrag-max-scan-fields 1000
slowlog-max-len 128
acllog-max-len 128
lua-time-limit 5000
cluster-node-timeout 5000
slowlog-log-slower-than 10000
latency-monitor-threshold 0
proto-max-bulk-len 536870912
stream-node-max-entries 100
repl-backlog-size 1048576
maxmemory 0
hash-max-ziplist-entries 512
set-max-intset-entries 512
zset-max-ziplist-entries 128
active-defrag-ignore-bytes 104857600
hash-max-ziplist-value 64
stream-node-max-bytes 4096
zset-max-ziplist-value 64
hll-sparse-max-bytes 3000
tracking-table-max-keys 1000000
client-query-buffer-limit 1073741824
repl-backlog-ttl 3600
auto-aof-rewrite-min-size 67108864
save 3600 1 300 100 60 10000
unixsocketperm 0
oom-score-adj-values 0 200 800

${customSegment}
"""