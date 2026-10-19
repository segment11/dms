var md = angular.module('module_kafka/jobs', ['base']);
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
            Page.go('/page/kafka_service', {});
            return;
        }

        $http.get('/dms/kafka/job/list', {
            params: {serviceId: id, pageNum: pageNum || $scope.tmp.pageNum}
        }).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };

    $scope.queryLl();

    $scope.showTaskLog = function (one) {
        uiTips.loading();
        $http.get('/dms/kafka/job/task/list', {params: {jobId: one.id}}).success(function (data) {
            $scope.taskLogList = data.list;
            _.each(data.list, function (it) {
                it.jobResult = it.jobResult ? JSON.parse(it.jobResult) : {};
            });

            $scope.ctrl.isShowTaskLog = true;
        });
    };

    $scope.back = function () {
        Page.go('/page/kafka_one', {id: id});
    };
});
