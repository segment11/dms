var md = angular.module('module_redis/one', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiLog) {
    $scope.tmp = {};
    $scope.ctrl = {};

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
});
