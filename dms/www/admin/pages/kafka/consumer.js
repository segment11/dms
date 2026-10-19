var md = angular.module('module_kafka/consumer', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var serviceId = params.serviceId;

    $scope.queryLl = function () {
        if (serviceId == null) {
            // f5 reset browser, go back
            Page.go('/page/kafka_service', {});
            return;
        }

        $http.get('/dms/kafka/consumer/list', {params: {serviceId: serviceId}}).success(function (data) {
            $scope.mode = data.mode;
            $scope.ll = data.list;
        });
    };

    $scope.queryLl();

    $scope.showLag = function (groupId) {
        uiTips.loading();
        $http.get('/dms/kafka/consumer/lag', {
            params: {serviceId: serviceId, groupId: groupId}
        }).success(function (data) {
            $scope.lagOne = data;
            $scope.ctrl.isShowLag = true;
        });
    };

    $scope.goOne = function (groupId) {
        Page.go('/page/kafka_consumer-one', {
            serviceId: serviceId,
            groupId: groupId,
            serviceName: params.serviceName
        });
    };
});
