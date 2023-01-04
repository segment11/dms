var md = angular.module('module_script/pull-log', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.queryLl = function () {
        $http.get('/dms/agent/script/pull/log', {params: {}}).success(function (data) {
            uiTips.unloading();
            $scope.ll = data;
        });
    };
    $scope.queryLl();
});