var md = angular.module('module_redis/service', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {pageNum: 1};
    $scope.ctrl = {};

    $scope.queryLl = function (pageNum) {
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword};
        p.mode = $scope.tmp.mode;

        $http.get('/dms/redis/service/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };

    $scope.queryLl();

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/redis/service/delete', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };

    $scope.goServiceOne = function (one) {
        Page.go('/page/redis_one', {
            id: one.id, name: one.name, des: one.des
        });
    };

    $scope.goAddOne = function () {
        Page.go('/page/redis_add', {});
    };

    $scope.initExporters = function () {
        uiTips.prompt('Choose one node ip to create prometheus application', function (val) {
            if (!val) {
                uiTips.alert('Please input a node ip');
                return;
            }

            $http.get('/dms/redis/service/init-exporters', {
                params: {
                    targetNodeIp: val
                }
            }).success(function (data) {
                if (data.flag) {
                    uiTips.alert('Init redis exporters success');
                }
            });
        }, '127.0.0.1');
    };
});
