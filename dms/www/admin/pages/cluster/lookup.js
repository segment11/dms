var md = angular.module('module_cluster/lookup', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    $http.get('/dms/cluster/list/simple').success(function (data) {
        $scope.clusterList = data.list;
        if (data.list.length) {
            $scope.tmp.clusterId = data.list[0].id;
            $scope.queryLl();
        }
    });

    $scope.queryLl = function () {
        if (!$scope.tmp.clusterId) {
            $scope.list = [];
            return;
        }
        $http.get('/dms/dns/lookup', {params: {clusterId: $scope.tmp.clusterId}}).success(function (data) {
            $scope.list = data.list;
        });
    };

    $scope.goAppOne = function (one) {
        Page.go('/page/cluster_container', {
            appId: one.appId, appName: one.appName, appDes: one.appDes,
            clusterId: one.clusterId,
            namespaceId: one.namespaceId
        });
    };
});
