var md = angular.module('module_redis/one', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var id = params.id;

    $scope.refresh = function () {
        $http.get('/dms/redis/service/one', {params: {id: id}}).success(function (data) {
            $scope.one = data;
        });
    };

    $scope.back = function () {
        Page.go('/page/redis_service', {});
    };
});
