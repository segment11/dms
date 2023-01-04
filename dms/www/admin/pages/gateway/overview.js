var md = angular.module('module_gateway/overview', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    $http.get('/dms/gw/cluster/list/simple').success(function (data) {
        $scope.clusterList = data;
        if (data.length) {
            $scope.tmp.clusterId = data[0].id;
            $scope.onClusterChoose();
        }
    });

    $scope.onClusterChoose = function () {
        if (!$scope.tmp.clusterId) {
            $scope.list = [];
            return;
        }
        $http.get('/dms/gw/cluster/overview', {params: {clusterId: $scope.tmp.clusterId}}).success(function (data) {
            $scope.list = data;
        });
    };
});
