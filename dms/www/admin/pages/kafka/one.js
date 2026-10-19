var md = angular.module('module_kafka/one', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiLog, uiValid) {    $scope.tmp = {};
    $scope.ctrl = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var id = params.id;
    if (!id) {
        id = store.get('kafka_service_one_id');
        if (!id) {
            Page.go('/page/kafka_service', {});
            return;
        }
    } else {
        store.set('kafka_service_one_id', id);
    }
    console.log('id - ' + id);

    var isReg = Page.registerIntervalFunc(document.location.hash, 'refresh');
    if (isReg) {
        uiLog.i('begin refresh interval');
    }

    $scope.refresh = function () {
        $http.get('/dms/kafka/service/one', {params: {id: id}}).success(function (data) {
            $scope.one = data.one;
            $scope.ext = data.ext;
            $scope.nodes = data.nodes;
            $scope.tmp.refreshTime = new Date();
        });
    };

    $scope.refresh();

    $scope.goTopics = function () {
        var one = $scope.one;
        Page.go('/page/kafka_topic', {serviceId: one.id, serviceName: one.name});
    };

    $scope.goConsumers = function () {
        var one = $scope.one;
        Page.go('/page/kafka_consumer', {serviceId: one.id, serviceName: one.name});
    };

    $scope.goJobs = function () {
        var one = $scope.one;
        Page.go('/page/kafka_jobs', {id: one.id, name: one.name});
    };

    $scope.goSnapshots = function () {
        var one = $scope.one;
        Page.go('/page/kafka_snapshot', {serviceId: one.id, serviceName: one.name});
    };

    $scope.doScaleUp = function () {
        var one = $scope.one;
        uiTips.prompt('Add how many brokers to ' + one.name + '?', function (val) {
            if (!val) {
                uiTips.alert('Please input a number');
                return;
            }
            if (!/^\d+$/.test(val)) {
                uiTips.alert('Please input a positive integer');
                return;
            }
            $http.post('/dms/kafka/service/scale-up', {id: one.id, brokerCount: parseInt(val)}).success(function (data) {
                if (data.id) {
                    uiTips.alert('Scale up ok, view jobs for detail');
                    $scope.refresh();
                }
            });
        }, '1');
    };

    $scope.doScaleDown = function () {
        var one = $scope.one;
        uiTips.prompt('Remove how many brokers from ' + one.name + '?', function (val) {
            if (!val) {
                uiTips.alert('Please input a number');
                return;
            }
            if (!/^\d+$/.test(val)) {
                uiTips.alert('Please input a positive integer');
                return;
            }
            $http.post('/dms/kafka/service/scale-down', {id: one.id, brokerCount: parseInt(val)}).success(function (data) {
                if (data.id) {
                    uiTips.alert('Scale down ok, view jobs for detail');
                    $scope.refresh();
                }
            });
        }, '1');
    };

    $scope.doFailover = function () {
        var one = $scope.one;
        uiTips.confirm('Sure do failover for ' + one.name + '?', function () {
            $http.post('/dms/kafka/service/failover', {id: one.id}).success(function (data) {
                if (data.id) {
                    uiTips.alert('Failover ok, view jobs for detail');
                }
            });
        }, null);
    };

    $scope.doPreferredReplicaElection = function () {
        var one = $scope.one;
        uiTips.confirm('Sure trigger preferred replica election for ' + one.name + '?', function () {
            $http.post('/dms/kafka/service/preferred-replica-election', {id: one.id}).success(function (data) {
                if (data.id) {
                    uiTips.alert('Preferred replica election triggered, view jobs for detail');
                }
            });
        }, null);
    };

    $scope.openUpdateConfig = function () {
        $scope.tmp.configItems = [];
        $scope.ctrl.isShowUpdateConfig = true;
    };

    $scope.doUpdateConfig = function () {
        if (!uiValid.checkForm($scope.tmp.updateConfigForm) || !$scope.tmp.updateConfigForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = $scope.one;
        var configOverrides = {};
        var itemNumber = 0;
        for (var i = 0; i < $scope.tmp.configItems.length; i++) {
            var item = $scope.tmp.configItems[i];
            if (item.key) {
                configOverrides[item.key] = item.value;
                itemNumber++;
            }
        }
        if (itemNumber == 0) {
            uiTips.alert('Please input at least one config item');
            return;
        }
        $http.post('/dms/kafka/service/update-config', {id: one.id, configOverrides: configOverrides}).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowUpdateConfig = false;
                uiTips.alert('Update config ok, view jobs for detail');
            }
        });
    };

    $scope.stop = function () {
        var one = $scope.one;
        uiTips.confirm('Sure stop ' + one.name + '?', function () {
            $http.post('/dms/kafka/service/stop', {id: one.id}).success(function (data) {
                if (data.id) {
                    uiTips.alert('Stop ok');
                    $scope.refresh();
                }
            });
        }, null);
    };

    $scope.start = function () {
        var one = $scope.one;
        uiTips.confirm('Sure start ' + one.name + '?', function () {
            $http.post('/dms/kafka/service/start', {id: one.id}).success(function (data) {
                if (data.id) {
                    uiTips.alert('Start ok');
                    $scope.refresh();
                }
            });
        }, null);
    };

    $scope.deleteOne = function () {
        var one = $scope.one;
        uiTips.confirm('Sure delete ' + one.name + '?', function () {
            $http.post('/dms/kafka/service/delete', {id: one.id}).success(function (data) {
                if (data.id) {
                    Page.go('/page/kafka_service', {});
                }
            });
        }, null);
    };

    $scope.back = function () {
        Page.go('/page/kafka_service', {});
    };
});
