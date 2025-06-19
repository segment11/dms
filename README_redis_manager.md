# dms redis manager

This is a module + some plugins in dms, it's a manager system for redis.

# Features

- redis config management
- redis cluster creation
- redis sentinel creation
- redis primary-replicas creation
- scale out redis cluster
- redis sentinel auto-failover
- add and remove replicas
- auto backup and recover by scp or s3 in cloud
- node exporter, redis exporter and prometheus, one button to collect metrics
- vector log collect and loki, one button to collect logs
- some basic metrics charts
- Node resource overview

# Some screenshots

One redis service overview:
![one redis service overview](./pic/redis_manager/redis_manager_one-detail.png)

Service list:
![service list](./pic/redis_manager/redis_manager_service-list.png)

Metric Charts:
![metric charts](./pic/redis_manager/redis_manager_metric-charts.png)

Job task log:
![job task log](./pic/redis_manager/redis_manager_job-task-log.png)

Config template:
![config template](./pic/redis_manager/redis_manager_config-template.png)

Backup template:
![backup template](./pic/redis_manager/redis_manager_backup-template.png)