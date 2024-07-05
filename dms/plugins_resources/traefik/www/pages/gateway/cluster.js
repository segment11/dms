var md = angular.module('module_gateway/cluster', ['base']);
md.controller('MainCtrl', function ($scope, $http) {
    $scope.queryLl = function () {
        $http.get('/dms/gw/cluster/list', {params: {}}).success(function (data) {
            $scope.ll = data;
        });
    };

    $scope.queryLl();

    var Page = window.Page;
    $scope.goRouter = function (one) {
        Page.go('/page/gateway_router', {clusterId: one.id, clusterName: one.name});
    };
});
