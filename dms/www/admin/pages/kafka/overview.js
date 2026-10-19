var md = angular.module('module_kafka/overview', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    var Page = window.Page;

    $scope.refresh = function () {
        uiTips.loading();
        $http.get('/dms/kafka/service/list', {params: {pageNum: 1, pageSize: 100}}).success(function (data) {
            var list = data.list || [];

            var stat = {
                clusterCount: list.length,
                brokerCount: 0,
                runningCount: 0,
                unhealthyCount: 0
            };

            _.each(list, function (it) {
                stat.brokerCount += it.brokers || 0;
                if (it.status == 'running') {
                    stat.runningCount++;
                }
                if (it.status == 'unhealthy') {
                    stat.unhealthyCount++;
                }
            });

            $scope.stat = stat;
            $scope.ll = list;
        });
    };

    $scope.refresh();

    $scope.goOne = function (one) {
        Page.go('/page/kafka_one', {id: one.id});
    };
});
