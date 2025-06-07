var md = angular.module('module_redis/jobs', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {pageNum: 1};
    $scope.ctrl = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var id = params.id;

    $scope.refresh = function () {
        if (id == null) {
            // f5 reset browser, go back
            Page.go('/page/redis_service', {});
            return;
        }

        $http.get('/dms/redis/job/list', {params: {rmServiceId: id}}).success(function (data) {
            $scope.ll = data.list;
            _.each(data.list, function (it) {
                it.content = JSON.parse(it.content)
            });
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };

    $scope.refresh();

    $scope.showDetail = function (one) {
        $.dialog({
            title: 'Steps',
            content: '<pre style="height: 400px;">' + JSON.stringify(one.content, null, 2) + '</pre>'
        });
    };

    $scope.back = function () {
        Page.go('/page/redis_service', {});
    };
});
