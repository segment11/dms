var md = angular.module('module_kafka/service', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {pageNum: 1};
    $scope.ctrl = {};

    $scope.queryLl = function (pageNum) {
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword};
        p.mode = $scope.tmp.mode;
        p.status = $scope.tmp.status;

        $http.get('/dms/kafka/service/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };

    $scope.queryLl();

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            uiTips.loading();
            $http.post('/dms/kafka/service/delete', {id: one.id}).success(function (data) {
                if (data.id) {
                    one.status = 'deleted';
                }
            });
        }, null);
    };

    $scope.stop = function (one) {
        uiTips.confirm('Sure Stop - ' + one.name + '?', function () {
            $http.post('/dms/kafka/service/stop', {id: one.id}).success(function (data) {
                if (data.id) {
                    one.status = 'stopped';
                }
            });
        }, null);
    };

    $scope.start = function (one) {
        uiTips.confirm('Sure Start - ' + one.name + '?', function () {
            $http.post('/dms/kafka/service/start', {id: one.id}).success(function (data) {
                if (data.id) {
                    one.status = 'creating';
                }
            });
        }, null);
    };

    $scope.goServiceOne = function (one) {
        Page.go('/page/kafka_one', {
            id: one.id
        });
    };

    $scope.goAddOne = function () {
        Page.go('/page/kafka_add', {});
    };

    $scope.initExporters = function () {
        uiTips.prompt('Choose one node ip to create prometheus application', function (val) {
            if (!val) {
                uiTips.alert('Please input a node ip');
                return;
            }

            $http.get('/dms/kafka/service/simple-list', {params: {}}).success(function (data) {
                if (!data.list || data.list.length == 0) {
                    uiTips.alert('No running kafka service');
                    return;
                }
                var serviceId = data.list[0].id;
                $http.get('/dms/kafka/metric/init-exporters', {
                    params: {
                        targetNodeIp: val,
                        serviceId: serviceId
                    }
                }).success(function (data) {
                    if (data.flag) {
                        uiTips.alert('Init kafka exporters success');
                    }
                });
            });
        }, '127.0.0.1');
    };
});
