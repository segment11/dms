var md = angular.module('module_kafka/topic-one', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.alterOne = {configItems: {items: []}};
    $scope.configItemList = [];

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    $scope.tmp.serviceId = params.serviceId;
    $scope.tmp.serviceName = params.serviceName;
    $scope.tmp.name = params.name;

    var serviceId = params.serviceId;
    var serviceName = params.serviceName;
    var name = params.name;

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

    var buildConfigItemList = function (one) {
        var ll = [];
        if (!one || !one.configOverrides || !one.configOverrides.params) {
            return ll;
        }
        _.each(one.configOverrides.params, function (val, key) {
            ll.push({key: key, value: val});
        });
        return ll;
    };

    $scope.refresh = function () {
        if (serviceId == null || name == null) {
            // f5 reset browser, go back
            Page.go('/page/kafka_topic', {});
            return;
        }

        $http.get('/dms/kafka/topic/one', {
            params: {serviceId: serviceId, name: name}
        }).success(function (data) {
            $scope.one = data;
            $scope.configItemList = buildConfigItemList(data);
        });
    };

    $scope.refresh();

    $scope.showAlter = function () {
        var one = $scope.one;
        $scope.alterOne = {
            name: one.name,
            partitions: one.partitions,
            currentPartitions: one.partitions,
            configItems: {items: []}
        };
        _.each(buildConfigItemList(one), function (item) {
            $scope.alterOne.configItems.items.push({key: item.key, value: item.value});
        });
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
                $scope.refresh();
            }
        });
    };

    $scope.deleteTopic = function () {
        uiTips.confirm('Sure Delete - ' + name + '?', function () {
            uiTips.loading();
            $http.post('/dms/kafka/topic/delete', {
                serviceId: serviceId, name: name
            }).success(function (data) {
                if (data.id) {
                    uiTips.alert('Delete topic ok, view jobs for detail');
                    $scope.back();
                }
            });
        }, null);
    };

    $scope.reassign = function () {
        uiTips.confirm('Sure Reassign - ' + name + '?', function () {
            $http.post('/dms/kafka/topic/reassign', {
                serviceId: serviceId, topicNames: [name]
            }).success(function (data) {
                if (data.id) {
                    uiTips.alert('Reassign partitions ok, view jobs for detail');
                }
            });
        }, null);
    };

    $scope.back = function () {
        Page.go('/page/kafka_topic', {serviceId: serviceId, serviceName: serviceName});
    };
});
