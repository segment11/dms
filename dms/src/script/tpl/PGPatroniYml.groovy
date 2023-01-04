package script.tpl

import model.server.ContainerMountTplHelper

def appId = super.binding.getProperty('appId') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int
def nodeIp = super.binding.getProperty('nodeIp') as String

// patroni port
def port = super.binding.getProperty('port') as String
def pgPort = super.binding.getProperty('pgPort') as String
def pgEncoding = super.binding.getProperty('pgEncoding') as String
def customParameters = super.binding.getProperty('customParameters') as String
def ENV_PGDATA = super.binding.getProperty('ENV_PGDATA') as String
def ENV_PG_BIN_DIR = super.binding.getProperty('ENV_PG_BIN_DIR') as String
def ENV_POSTGRES_PASSWORD = super.binding.getProperty('ENV_POSTGRES_PASSWORD') as String

def etcdAppName = super.binding.getProperty('etcdAppName') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp etcdApp = applications.app(etcdAppName)
def etcdEndpoint = etcdApp.allNodeIpList.collect { "${it}:2379" }[0]

def parameters = customParameters.split(',')
def pre = ''.padLeft(8, ' ')
def x = parameters.collect {
    def keyValue = it.toString().trim()
    def arr = keyValue.split('=')
    def val = arr[1].trim()
    pre + (arr[0].trim() + ': ' + (val in ['true', 'false'] ? val : '"' + val + '"'))
}.join('\n')

"""
scope: app_${appId}
namespace: /service/
name: pg${instanceIndex}

restapi:
  listen: 0.0.0.0:${port}
  connect_address: ${nodeIp}:${port}

etcd:
  host: ${etcdEndpoint}

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    master_start_timeout: 300
    synchronous_mode: false
    postgresql:
      use_pg_rewind: true
      use_slots: true
      parameters:
        listen_addresses: "0.0.0.0"
        port: 5432
        wal_level: logical
        hot_standby: "on"
        wal_keep_segments: 100
        max_wal_senders: 10
        max_replication_slots: 10
        wal_log_hints: "on"
${x}

# custom settings: ${customParameters}
###

  initdb:
    - encoding: ${pgEncoding}
    - data-checksums

  pg_hba:
    - host replication repl 0.0.0.0/0 md5
    - host all all 0.0.0.0/0 md5

  users:
    admin:
      password: admin@pass
      options:
        - createrole
        - createdb

postgresql:
  listen: 0.0.0.0:${pgPort}
  connect_address: ${nodeIp}:${pgPort}
  data_dir: ${ENV_PGDATA}
  bin_dir: ${ENV_PG_BIN_DIR}

  authentication:
    replication:
      username: repl
      password: repl@pass
    superuser:
      username: postgres
      password: '${ENV_POSTGRES_PASSWORD}'
    rewind:
      username: rewind_user
      password: rewind_user@pass

  basebackup:
    max-rate: 100M
    checkpoint: fast

tags:
  # first second be leader
  nofailover: ${instanceIndex > 1}
  noloadbalance: false
  clonefrom: false
  nosync: false
"""
