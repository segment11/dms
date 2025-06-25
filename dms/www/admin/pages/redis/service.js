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

    $scope.delete = function (one, isTerminate) {
        var warnPrefix = isTerminate ? 'Sure Terminate - ' : 'Sure Delete - ';
        uiTips.confirm(warnPrefix + one.name + '?', function () {
            uiTips.loading();
            $http.delete('/dms/redis/service/delete', {
                params: {
                    id: one.id,
                    isTerminate: isTerminate
                }
            }).success(function (data) {
                if (data.flag) {
                    if (isTerminate) {
                        var i = _.indexOf($scope.ll, one);
                        $scope.ll.splice(i, 1);
                    } else {
                        one.status = 'deleted';
                    }
                }
            });
        }, null);
    };

    $scope.goServiceOne = function (one) {
        Page.go('/page/redis_one', {
            id: one.id
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

            $http.get('/dms/redis/metric/init-exporters', {
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

    $scope.initLogCollectors = function () {
        uiTips.prompt('Choose one node ip to create loki application', function (val) {
            if (!val) {
                uiTips.alert('Please input a node ip');
                return;
            }

            $http.get('/dms/redis/metric/init-log-collectors', {
                params: {
                    targetNodeIp: val
                }
            }).success(function (data) {
                if (data.flag) {
                    uiTips.alert('Init redis log collectors success');
                }
            })
        }, '127.0.0.1');
    };

    $scope.initNodeExporters = function () {
        $http.get('/dms/redis/metric/init-node-exporters', {params: {}}).success(function (data) {
            if (data.flag) {
                uiTips.alert('Init node exporters success');
            }
        })
    };
});
