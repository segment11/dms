-- kafka manager tables for existing deployments

create table if not exists km_service
(
    id                         int auto_increment primary key,
    name                       varchar(50),
    des                        varchar(200),
    mode                       varchar(20),
    kafka_version              varchar(20),
    config_template_id         int,
    config_overrides           varchar(2000),
    zk_connect_string          varchar(500),
    zk_chroot                  varchar(200) not null,
    app_id                     int,
    port                       int,
    brokers                    int,
    default_replication_factor int,
    default_partitions         int,
    heap_mb                    int,
    pass                       varchar(200),
    is_sasl_on                 bit,
    is_tls_on                  bit,
    node_tags                  varchar(100),
    node_tags_by_broker_index  varchar(500),
    log_policy                 varchar(200),
    status                     varchar(20),
    extend_params              varchar(2000),
    broker_detail              varchar(4000),
    last_updated_message       varchar(200),
    created_date               timestamp,
    updated_date               timestamp default current_timestamp
);
create unique index if not exists idx_km_service_name on km_service (name);
create unique index if not exists idx_km_service_zk_chroot on km_service (zk_chroot);
create index if not exists idx_km_service_config_template_id on km_service (config_template_id);
create index if not exists idx_km_service_status on km_service (status);

create table if not exists km_config_template
(
    id           int auto_increment primary key,
    name         varchar(50),
    des          varchar(200),
    config_items text,
    updated_date timestamp default current_timestamp
);
create unique index if not exists idx_km_config_template_name on km_config_template (name);

create table if not exists km_topic
(
    id                int auto_increment primary key,
    service_id        int,
    name              varchar(200),
    partitions        int,
    replication_factor int,
    config_overrides  varchar(2000),
    status            varchar(20),
    created_date      timestamp,
    updated_date      timestamp default current_timestamp
);
create index if not exists idx_km_topic_service_id on km_topic (service_id);
create unique index if not exists idx_km_topic_service_id_name on km_topic (service_id, name);

create table if not exists km_job
(
    id           int auto_increment primary key,
    busi_id      int,
    type         varchar(20),
    status       varchar(20),
    result       varchar(500),
    content      text,
    failed_num   int       default 0,
    cost_ms      int       default 0,
    created_date timestamp,
    updated_date timestamp default current_timestamp
);
create index if not exists idx_km_job_busi_id on km_job (busi_id);

create table if not exists km_task_log
(
    id           int auto_increment primary key,
    job_id       int,
    step         varchar(100),
    job_result   text,
    cost_ms      int       default 0,
    created_date timestamp,
    updated_date timestamp default current_timestamp
);
create index if not exists idx_km_task_log_job_id on km_task_log (job_id);

create table if not exists km_snapshot
(
    id           int auto_increment primary key,
    name         varchar(100),
    service_id   int,
    snapshot_dir varchar(500),
    status       varchar(20),
    message      varchar(200),
    cost_ms      int,
    created_date timestamp,
    updated_date timestamp default current_timestamp
);
create index if not exists idx_km_snapshot_service_id on km_snapshot (service_id);
