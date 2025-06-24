var md = angular.module('module_redis/one', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiLog) {
    $scope.tmp = {
        timeRangeList: ['5m', '1h', '3h', '1d', '7d', '30d'],
        timeRange: '5m',
        metricInstanceList: [],
        metricInstance: ''
    };
    $scope.ctrl = {};
    $scope.charts = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var id = params.id;
    if (!id) {
        id = store.get('redis_service_one_id');
        if (!id) {
            Page.go('/page/redis_service', {});
            return;
        }
    } else {
        store.set('redis_service_one_id', id);
    }
    console.log('id - ' + id);

    var isReg = Page.registerIntervalFunc(document.location.hash, 'refresh');
    if (isReg) {
        uiLog.i('begin refresh interval');
    }

    $scope.refresh = function () {
        $http.get('/dms/redis/service/one', {params: {id: id}}).success(function (data) {
            $scope.one = data.one;
            $scope.checkResult = data.checkResult;
            $scope.ext = data.ext;
            $scope.nodes = data.nodes;

            $scope.tmp.metricInstanceList = [];
            if (data.nodes) {
                _.each(data.nodes, function (n) {
                    $scope.tmp.metricInstanceList.push('redis://' + n.ip + ':' + n.port);
                });
                if (!$scope.tmp.metricInstance) {
                    $scope.tmp.metricInstance = $scope.tmp.metricInstanceList[0];
                } else {
                    // not in instance list
                    if (!_.contains($scope.tmp.metricInstanceList, $scope.tmp.metricInstance)) {
                        $scope.tmp.metricInstance = $scope.tmp.metricInstanceList[0];
                    }
                }
            }

            $scope.tmp.refreshTime = new Date();
        });
    };

    $scope.refresh();

    $scope.goServiceJobs = function () {
        var one = $scope.one;
        Page.go('/page/redis_jobs', {
            id: one.id, name: one.name, des: one.des
        });
    };

    $scope.goServiceBackups = function () {
        var one = $scope.one;
        Page.go('/page/redis_backups', {
            id: one.id, name: one.name, des: one.des
        });
    };

    $scope.chooseCopyFrom = function () {
        $http.get('/dms/redis/service/simple-list', {params: {}}).success(function (data) {
            $scope.tmp.copyFromServiceList = _.filter(data.list, function (it) {
                return it.id != id;
            });
            $scope.ctrl.isShowCopyFrom = true;
        });
    };

    $scope.doCopyFrom = function () {
        if (!$scope.tmp.copyFromId) {
            uiTips.alert('Please choose a service to copy from');
            return;
        }

        $http.post('/dms/redis/job/service/copy-from', {
            fromId: $scope.tmp.copyFromId,
            id: id
        }).success(function (data) {
            if (data.flag) {
                $scope.ctrl.isShowCopyFrom = false;
                uiTips.alert('Doing copy from, view jobs for detail');
            }
        });
    };

    $scope.goAppDetail = function (appId, appName, appDes) {
        Page.go('/page/cluster_container', {
            appId: appId, appName: appName, appDes: appDes,
            clusterId: $scope.ext.clusterId,
            namespaceId: $scope.ext.namespaceId,
            targetIndex: 0
        });
    };

    $scope.back = function () {
        Page.go('/page/redis_service', {});
    };

    $scope.viewPass = function (one) {
        $http.get('/dms/redis/service/view-pass', {params: {id: one.id}}).success(function (data) {
            if (data.pass) {
                uiTips.alert(data.pass);
            }
        });
    };

    $scope.updateMaxmemory = function (one) {
        uiTips.prompt('Update redis server maxmemory: ', function (val) {
            if (!val) {
                uiTips.alert('Please input maxmemory (MB)');
                return;
            }

            var isNumber = /\d/.test(val);
            if (!isNumber) {
                uiTips.alert('Please input maxmemory (MB) as a number');
                return;
            }

            var valInt = parseInt(val);
            if (valInt < 256) {
                uiTips.alert('Please input maxmemory (MB) as a number greater than 256, yours: ' + valInt);
                return;
            }
            if (valInt > 1024 * 64) {
                uiTips.alert('Please input maxmemory (MB) as a number less than 1024 * 64, yours: ' + valInt);
                return;
            }
            if (valInt % 256 != 0) {
                uiTips.alert('Please input maxmemory (MB) as a number multiple of 256, yours: ' + valInt);
                return;
            }

            $http.post('/dms/redis/config/update-maxmemory', {
                id: one.id,
                maxmemoryMb: val
            }).success(function (data) {
                if (data.flag) {
                    uiTips.alert('Update maxmemory success');
                    one.maxmemoryMb = valInt;
                }
            });
        }, one.maxmemoryMb);
    };

    $scope.doScale = function (one) {
        uiTips.prompt('Update redis cluster shards to: ', function (val) {
            if (!val) {
                uiTips.alert('Please input to shards');
                return;
            }

            var isNumber = /\d/.test(val);
            if (!isNumber) {
                uiTips.alert('Please input to shards as a number');
                return;
            }

            var valInt = parseInt(val);
            if (valInt < 2) {
                uiTips.alert('Please input to shards as a number >= 2, yours: ' + valInt);
                return;
            }
            if (valInt > 32) {
                uiTips.alert('Please input to shards as a number <= 32, yours: ' + valInt);
                return;
            }
            if (valInt != one.shards * 2 && valInt != one.shards / 2) {
                uiTips.alert('Please input to shards as a number = old shards * 2 or old shards / 2, yours: ' + valInt);
                return;
            }

            var isScaleUp = valInt == one.shards * 2;
            if (isScaleUp) {
                $http.post('/dms/redis/service/cluster-scale-up', {
                    id: one.id,
                    toShards: valInt
                }).success(function (data) {
                    if (data.id) {
                        uiTips.alert('Update shards ok, view jobs for detail');
                        one.shards = valInt;
                    }
                });
            } else {
                $http.post('/dms/redis/service/cluster-scale-down', {
                    id: one.id,
                    toShards: valInt
                }).success(function (data) {
                    if (data.id) {
                        uiTips.alert('Update shards ok, view jobs for detail');
                        one.shards = valInt;
                    }
                });
            }
        }, one.shards);
    };

    $scope.doUpdateReplicas = function (one) {
        uiTips.prompt('Update redis cluster shards to: ', function (val) {
            if (!val) {
                uiTips.alert('Please input to replicas');
                return;
            }

            var isNumber = /\d/.test(val);
            if (!isNumber) {
                uiTips.alert('Please input to replicas as a number');
                return;
            }

            var valInt = parseInt(val);
            if (valInt < 1) {
                uiTips.alert('Please input to replicas as a number >= 1, yours: ' + valInt);
                return;
            }
            if (valInt > 4) {
                uiTips.alert('Please input to replicas as a number <= 4, yours: ' + valInt);
                return;
            }
            $http.post('/dms/redis/service/update-replicas', {
                id: one.id,
                toReplicas: valInt
            }).success(function (data) {
                if (data.id) {
                    uiTips.alert('Update replicas ok, view jobs for detail');
                    one.replicas = valInt;
                }
            });
        }, one.replicas);
    };

    $scope.doFailover = function (id, shardIndex, replicaIndex) {
        uiTips.confirm('Sure Do Failover - Shard ' + shardIndex + ', set primary to replica ' + replicaIndex + '?', function () {
            $http.post('/dms/redis/service/failover', {
                id: id,
                shardIndex: shardIndex,
                replicaIndex: replicaIndex
            }).success(function (data) {
                if (data.id) {
                    uiTips.alert('Failover ok, view jobs for detail');
                }
            });
        });
    };

    $scope.changeTab = function (index, triggerIndex) {
        if (index == 1) {
            $scope.queryMetrics();
        }
        return true;
    };

    var metricsMap = {};

    $scope.queryMetrics = function () {
        $http.get('/dms/redis/metric/query', {
            params: {
                id: id,
                timeRange: $scope.tmp.timeRange,
            }
        }).success(function (data) {
            $scope.tmp.metricsRefreshTime = new Date();
            if (data.map) {
                metricsMap = data.map;
                for (metricName in data.map) {
                    if ($scope.tmp.metricInstance) {
                        $scope.charts[metricName] = _.find(data.map[metricName], function (it) {
                            return it.metricInstance == $scope.tmp.metricInstance;
                        });
                    } else {
                        $scope.charts[metricName] = data.map[metricName][0];
                        $scope.tmp.metricInstance = data.map[metricName][0].metricInstance;
                    }
                }
            }
        });
    };

    $scope.changeInstanceMetrics = function () {
        for (metricName in metricsMap) {
            $scope.charts[metricName] = _.find(metricsMap[metricName], function (it) {
                return it.metricInstance == $scope.tmp.metricInstance;
            });
        }
    };
});
