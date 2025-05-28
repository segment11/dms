var md = angular.module('module_redis/overview', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    $http.get('/dms/redis/overview').success(function (data) {
        // todo
        console.log(data);
    });
});
