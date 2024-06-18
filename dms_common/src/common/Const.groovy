package common

import groovy.transform.CompileStatic

@CompileStatic
class Const {
    static final int AGENT_HTTP_LISTEN_PORT = 6010
    static final int SERVER_HTTP_LISTEN_PORT = 5010
    static final int METRICS_AGENT_HTTP_LISTEN_PORT = 6011
    static final int METRICS_HTTP_LISTEN_PORT = 5011

    static final String AUTH_TOKEN_HEADER = 'X-Auth-Token'
    static final String CLUSTER_ID_HEADER = 'X-Cluster-Id'

    static final String SCRIPT_NAME_HEADER = 'X-Script-Name'
    static final String PROXY_TARGET_SERVER_ADDR_HEADER = 'X-target-server-addr'
    static final String PROXY_READ_TIMEOUT_HEADER = 'X-read-timeout'

    static final String SERVER_LEADER_LOCK_KEY = 'dms_server_leader'
}
