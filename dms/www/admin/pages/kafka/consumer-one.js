var md = angular.module('module_kafka/consumer-one', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var serviceId = params.serviceId;
    var groupId = params.groupId;

    $scope.queryOne = function () {
        if (serviceId == null || groupId == null) {
            // f5 reset browser, go back
            Page.go('/page/kafka_consumer', {});
            return;
        }

        $http.get('/dms/kafka/consumer/one', {
            params: {serviceId: serviceId, groupId: groupId}
        }).success(function (data) {
            $scope.one = data;
        });

        $http.get('/dms/kafka/consumer/lag', {
            params: {serviceId: serviceId, groupId: groupId}
        }).success(function (data) {
            $scope.lagOne = data;
        });
    };

    $scope.queryOne();

    $scope.back = function () {
        Page.go('/page/kafka_consumer', {serviceId: serviceId, serviceName: params.serviceName});
    };
});
