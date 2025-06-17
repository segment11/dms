var md = angular.module('module_redis/backups', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {pageNum: 1};
    $scope.ctrl = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var id = params.id;

    $scope.queryLl = function (pageNum) {
        if (id == null) {
            // f5 reset browser, go back
            Page.go('/page/redis_service', {});
            return;
        }

        var p = {pageNum: pageNum || $scope.tmp.pageNum, serviceId: id};
        $http.get('/dms/redis/backup-log/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };

    $scope.queryLl();

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/redis/backup-log/delete', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };

    $scope.back = function () {
        Page.go('/page/redis_one', {id: id});
    };
});
