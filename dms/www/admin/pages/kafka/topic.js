var md = angular.module('module_kafka/topic', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.editOne = {configItems: {items: []}};
    $scope.alterOne = {configItems: {items: []}};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    $scope.tmp.serviceId = params.serviceId;
    $scope.tmp.serviceName = params.serviceName;

    var serviceId = params.serviceId;
    var serviceName = params.serviceName;

    var buildOverrides = function (items) {
        var map = {};
        _.each(items || [], function (it) {
            if (!it.key) {
                return;
            }
            map[it.key] = it.value == null ? '' : it.value;
        });
        return map;
    };

    var isEmptyMap = function (map) {
        for (var key in map) {
            if (map.hasOwnProperty(key)) {
                return false;
            }
        }
        return true;
    };

    $scope.configCount = function (one) {
        if (!one || !one.configOverrides || !one.configOverrides.params) {
            return 0;
        }
        var count = 0;
        for (var key in one.configOverrides.params) {
            if (one.configOverrides.params.hasOwnProperty(key)) {
                count++;
            }
        }
        return count;
    };

    $scope.queryLl = function () {
        if (serviceId == null) {
            // f5 reset browser, go back
            Page.go('/page/kafka_service', {});
            return;
        }

        $http.get('/dms/kafka/topic/list', {params: {serviceId: serviceId}}).success(function (data) {
            $scope.ll = data;
        });
    };

    $scope.queryLl();

    $scope.goTopicOne = function (one) {
        Page.go('/page/kafka_topic-one', {
            serviceId: serviceId, name: one.name, serviceName: serviceName
        });
    };

    $scope.goAddOne = function () {
        $scope.editOne = {name: '', partitions: '', replicationFactor: '', configItems: {items: []}};
        $scope.ctrl.isShowAdd = true;
    };

    $scope.saveAdd = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var body = {
            serviceId: serviceId,
            name: $scope.editOne.name,
            partitions: parseInt($scope.editOne.partitions, 10),
            replicationFactor: parseInt($scope.editOne.replicationFactor, 10)
        };

        var overrides = buildOverrides($scope.editOne.configItems.items);
        if (isEmptyMap(overrides)) {
            body.configOverrides = overrides;
        }

        $http.post('/dms/kafka/topic/add', body).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                uiTips.alert('Add topic ok, view jobs for detail');
                $scope.queryLl();
            }
        });
    };

    $scope.showAlter = function (one) {
        $scope.alterOne = {
            name: one.name,
            partitions: one.partitions,
            currentPartitions: one.partitions,
            configItems: {items: []}
        };
        if (one.configOverrides && one.configOverrides.params) {
            _.each(one.configOverrides.params, function (val, key) {
                $scope.alterOne.configItems.items.push({key: key, value: val});
            });
        }
        $scope.ctrl.isShowAlter = true;
    };

    $scope.saveAlter = function () {
        if (!uiValid.checkForm($scope.tmp.alterForm) || !$scope.tmp.alterForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = $scope.alterOne;
        var newPartitions = one.partitions ? parseInt(one.partitions, 10) : 0;
        var overrides = buildOverrides(one.configItems.items);
        if (newPartitions > 0 && newPartitions < one.currentPartitions) {
            uiTips.alert('Cannot decrease partitions, current: ' + one.currentPartitions);
            return;
        }
        if (newPartitions <= one.currentPartitions && isEmptyMap(overrides)) {
            uiTips.alert('Nothing to alter');
            return;
        }

        var body = {serviceId: serviceId, name: one.name};
        if (newPartitions > one.currentPartitions) {
            body.partitions = newPartitions;
        }
        if (isEmptyMap(overrides)) {
            body.configOverrides = overrides;
        }

        $http.post('/dms/kafka/topic/alter', body).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAlter = false;
                uiTips.alert('Alter topic ok, view jobs for detail');
                $scope.queryLl();
            }
        });
    };

    $scope.deleteTopic = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            uiTips.loading();
            $http.post('/dms/kafka/topic/delete', {
                serviceId: serviceId, name: one.name
            }).success(function (data) {
                if (data.id) {
                    uiTips.alert('Delete topic ok, view jobs for detail');
                    $scope.queryLl();
                }
            });
        }, null);
    };
});
