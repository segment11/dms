create table cluster
(
    id              int auto_increment primary key,
    name            varchar(50),
    des             varchar(200),
    secret          varchar(32),
    is_in_guard     bit,
    global_env_conf varchar(500),
    updated_date    timestamp default current_timestamp
);
create unique index idx_cluster_name on cluster (name);

create table namespace
(
    id           int auto_increment primary key,
    cluster_id   int,
    name         varchar(50),
    des          varchar(200),
    updated_date timestamp default current_timestamp
);
create index idx_namespace_cluster_id on namespace (cluster_id);
create unique index idx_namespace_name on namespace (name);

create table node
(
    id            int auto_increment primary key,
    cluster_id    int,
    ip            varchar(20),
    tags          varchar(100),
    agent_version varchar(10),
    updated_date  timestamp default current_timestamp
);
create index idx_node_cluster_id on node (cluster_id);
create unique index idx_node_ip on node (ip);

create table node_volume
(
    id           int auto_increment primary key,
    cluster_id   int,
    image_name   varchar(200),
    name         varchar(50),
    des          varchar(200),
    dir          varchar(200),
    updated_date timestamp default current_timestamp
);
create index idx_node_volume_cluster_id on node_volume (cluster_id);

create table image_port
(
    id         int auto_increment primary key,
    image_name varchar(200),
    name       varchar(50),
    des        varchar(200),
    port       int
);
create index idx_image_port_image_name on image_port (image_name);

create table image_env
(
    id          int auto_increment primary key,
    image_name  varchar(200),
    name        varchar(50),
    des         varchar(200),
    env         varchar(50),
    default_val varchar(100)
);
create index idx_image_env_image_name on image_env (image_name);

create table image_tpl
(
    id                  int auto_increment primary key,
    image_name          varchar(200),
    name                varchar(50),
    des                 varchar(200),
    tpl_type            varchar(20),
    mount_dist          varchar(200),
    is_parent_dir_mount bit,
    content             text,
    params              text,
    updated_date        timestamp default current_timestamp
);
create index idx_image_tpl_image_name on image_tpl (image_name);

create table image_registry
(
    id             int auto_increment primary key,
    name           varchar(50),
    url            varchar(200),
    login_user     varchar(50),
    login_password varchar(50),
    updated_date   timestamp default current_timestamp
);

create table app
(
    id              int auto_increment primary key,
    cluster_id      int,
    namespace_id    int,
    name            varchar(50),
    des             varchar(200),
    conf            text,
    gateway_conf    varchar(2000),
    monitor_conf    varchar(2000),
    log_conf        varchar(2000),
    ab_conf         varchar(2000),
    job_conf        varchar(2000),
    live_check_conf varchar(2000),
    status          int,
    extend_params   varchar(2000),
    updated_date    timestamp default current_timestamp
);
create index idx_app_cluster_id on app (cluster_id);
create index idx_app_namespace_id on app (namespace_id);
create unique index idx_app_name on app (name);

create table app_job
(
    id           int auto_increment primary key,
    app_id       int,
    status       int,
    fail_num     int,
    job_type     int,
    message      text,
    params       varchar(1000),
    created_date timestamp,
    updated_date timestamp default current_timestamp
);
create index idx_app_job_app_id_created_date on app_job (app_id, created_date);

create table app_job_log
(
    id             int auto_increment primary key,
    job_id         int,
    instance_index int,
    is_ok          bit,
    title          varchar(100),
    message        text,
    cost_ms        int,
    created_date   timestamp
);
create index idx_app_job_log_job_id on app_job_log (job_id);
create index idx_app_job_log_created_date on app_job_log (created_date);

create table user_permit
(
    id           int auto_increment primary key,
    user_name    varchar(50),
    created_user varchar(50),
    permit_type  varchar(50),
    resource_id  int,
    updated_date timestamp default current_timestamp
);
create index idx_user_permit_user_name on user_permit (user_name);
create index idx_user_permit_permit_type_resource_id on user_permit (permit_type, resource_id);

create table event
(
    id           int auto_increment primary key,
    type         varchar(20),
    reason       varchar(100),
    result       varchar(100),
    message      text,
    created_date timestamp
);
create index idx_event_type_reason on event (type, reason);

create table agent_script
(
    id           int auto_increment primary key,
    name         varchar(50),
    des          varchar(200),
    content      text,
    updated_date timestamp default current_timestamp
);

create table agent_script_pull_log
(
    id           int auto_increment primary key,
    agent_host   varchar(50),
    content      varchar(4000),
    created_date timestamp
);

create table gw_cluster
(
    id             int auto_increment primary key,
    name           varchar(50),
    des            varchar(200),
    app_id         int,
    server_url     varchar(200),
    server_port    int,
    dashboard_port int,
    created_date   timestamp,
    updated_date   timestamp default current_timestamp
);
create index idx_gw_cluster_app_id on gw_cluster (app_id);

create table gw_router
(
    id           int auto_increment primary key,
    cluster_id   int,
    name         varchar(50),
    des          varchar(200),
    rule         varchar(200),
    service      varchar(4000),
    middlewares  varchar(1000),
    tls          varchar(200),
    failover     varchar(200),
    entry_points varchar(200),
    priority     int,
    created_date timestamp,
    updated_date timestamp default current_timestamp
);
create index idx_gw_router_cluster_id on gw_router (cluster_id);

create table node_key_pair
(
    id           int auto_increment primary key,
    cluster_id   int,
    ip           varchar(20),
    ssh_port     int,
    user_name    varchar(20),
    pass         varchar(20),
    root_pass    varchar(20),
    key_name     varchar(100),
    key_type     varchar(10),
    key_private  varchar(5000),
    key_public   varchar(5000),
    updated_date timestamp default current_timestamp
);
create index idx_node_key_pair_cluster_id on node_key_pair (cluster_id);
create unique index idx_node_key_pair_ip on node_key_pair (ip);

create table deploy_file
(
    id           int auto_increment primary key,
    local_path   varchar(200),
    dest_path    varchar(200),
    file_len     bigint,
    is_overwrite bit,
    init_cmd     varchar(200),
    updated_date timestamp default current_timestamp
);
create unique index idx_deploy_file_dest_path on deploy_file (dest_path);

create table dyn_config
(
    id           int auto_increment primary key,
    name         varchar(50),
    vv           varchar(500),
    updated_date timestamp default current_timestamp
);
create unique index idx_dyn_config_name on dyn_config (name);

-- for redis manager module
create table rm_service
(
    id                   int auto_increment primary key,
    name                 varchar(50),
    des                  varchar(200),
    mode                 varchar(20),   -- standalone/sentinel/cluster
    engine_type          varchar(20),   -- redis/valkey/engula/kvrocks/velo
    engine_version       varchar(20),   -- 5.0/6.2/7.2/8.1
    config_template_id   int,
    pass                 varchar(200),
    port                 int,
    shards               int,
    replicas             int,
    backup_policy        varchar(500),
    log_policy           varchar(200),
    is_tls_on            bit,
    app_ids              varchar(200),  -- one replica one application
    status               varchar(20),
    cluster_slots_detail varchar(4000), -- for cluster mode
    created_data         timestamp,
    updated_date         timestamp default current_timestamp
);
create unique index idx_rm_service_name on rm_service (name);

create table rm_service_config_template
(
    id           int auto_increment primary key,
    name         varchar(50),
    des          varchar(200),
    config_items text,
    created_data timestamp,
    updated_date timestamp default current_timestamp
);
create unique index idx_rm_service_config_template_name on rm_service_config_template (name);

-- for redis sentinel mode
create table rm_sentinel_service
(
    id           int auto_increment primary key,
    name         varchar(50),
    replicas     int, -- 3 or 5
    config_items varchar(500),
    app_id       int,
    created_data timestamp,
    updated_date timestamp default current_timestamp
);

-- for redis manager module
create table rm_job
(
    id           int auto_increment primary key,
    name         varchar(50),
    des          varchar(200),
    service_id   int,
    created_data timestamp,
    updated_date timestamp default current_timestamp
);
create unique index idx_rm_job_service_id on rm_job (service_id);