var md = angular.module('module_redis/overview', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {nodeIpPrefix: ''};
    $scope.ctrl = {};

    $scope.refresh = function () {
        uiTips.loading();
        $http.get('/dms/redis/overview').success(function (data) {
            $scope.nodeStatsList = data.nodeStatsList;
        });
    };

    $scope.refresh();

    $scope.nodeIpFilter = function (one) {
        if ($scope.tmp.nodeIpKeyword == '') {
            return true;
        }

        return one.nodeIp.startsWith($scope.tmp.nodeIpPrefix);
    };
});
