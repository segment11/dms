package loki

def nodeIp = super.binding.getProperty('nodeIp') as String

def dataDir = super.binding.getProperty('dataDir') as String
def queryCacheMB = super.binding.getProperty('queryCacheMB') as Integer
def alertManagerNodeIp = super.binding.getProperty('alertManagerNodeIp') as String

"""
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096
  log_level: debug
  grpc_server_max_concurrent_streams: 1000

common:
  instance_addr: ${nodeIp}
  path_prefix: ${dataDir}
  storage:
    filesystem:
      chunks_directory: ${dataDir}/chunks
      rules_directory: ${dataDir}/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

query_range:
  results_cache:
    cache:
      embedded_cache:
        enabled: true
        max_size_mb: ${queryCacheMB}

limits_config:
  metric_aggregation_enabled: true

schema_config:
  configs:
    - from: 2024-10-10
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

pattern_ingester:
  enabled: true
  metric_aggregation:
    loki_address: localhost:3100

ruler:
  alertmanager_url: http://${alertManagerNodeIp}:9093

frontend:
  encoding: protobuf
"""