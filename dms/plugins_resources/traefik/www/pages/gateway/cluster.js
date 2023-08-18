var md = angular.module('module_gateway/cluster', ['base']);
md.controller('MainCtrl', function ($scope, $http) {
    $scope.queryLl = function () {
        $http.get('/dms/gw/cluster/list', {params: {}}).success(function (data) {
            $scope.ll = data;
        });
    };

    $scope.queryLl();

    var Page = window.Page;
    $scope.goFrontend = function (one) {
        Page.go('/page/gateway_frontend', {clusterId: one.id, clusterName: one.name});
    };
});
