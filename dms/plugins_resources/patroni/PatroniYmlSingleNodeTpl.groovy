package patroni

import model.server.ContainerMountTplHelper

def appId = super.binding.getProperty('appId') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int
def nodeIp = super.binding.getProperty('nodeIp') as String

def step = 100 * instanceIndex

// patroni port
def port = super.binding.getProperty('port') as int
def pgPort = super.binding.getProperty('pgPort') as int
def pgEncoding = super.binding.getProperty('pgEncoding') as String
def pgPassword = super.binding.getProperty('pgPassword') as String
def customParameters = super.binding.getProperty('customParameters') as String
def dataDir = super.binding.getProperty('dataDir') as String

def etcdAppName = super.binding.getProperty('etcdAppName') as String
ContainerMountTplHelper applications = super.binding.getProperty('applications') as ContainerMountTplHelper
ContainerMountTplHelper.OneApp etcdApp = applications.app(etcdAppName)
def etcdEndpoint = etcdApp.allNodeIpList[0] + ':2379'

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
  listen: 0.0.0.0:${port + step}
  connect_address: ${nodeIp}:${port + step}

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
        port: ${pgPort + step}
        wal_level: logical
        hot_standby: "on"
        wal_keep_segments: 100
        max_wal_senders: 10
        max_replication_slots: 10
        wal_log_hints: "on"
        logging_collector: "on"
        log_destination: "csvlog"
        log_directory: "pg_log"
        log_min_duration_statement: "800"
        log_filename: "pg-%d_%H%M%S.log"
        log_rotation_size: "1024MB"
        log_truncate_on_rotation: "off"
        shared_preload_libraries: "citus,timescaledb"
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
  listen: 0.0.0.0:${pgPort + step}
  connect_address: ${nodeIp}:${pgPort + step}
  data_dir: ${dataDir}/instance_${instanceIndex}
  bin_dir: /usr/libexec/postgresql

  authentication:
    replication:
      username: repl
      password: repl@pass
    superuser:
      username: postgres
      password: '${pgPassword}'
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
