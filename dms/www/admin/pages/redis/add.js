var md = angular.module('module_redis/add', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    var params = {};
    params.memMB = 1024;
    params.cpuFixed = 1.0;

    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.editOne = {
        port: 6379,
        mode: 'standalone',
        engineType: 'redis',
        engineVersion: '7.2',
        extendParams: {params: params}
    };

    $http.get('/dms/redis/node/tag/list').success(function (data) {
        $scope.tmp.nodeTagList = data.list;
    });

    $http.get('/dms/redis/sentinel-service/simple-list').success(function (data) {
        $scope.tmp.sentinelServiceList = data.list;
    });

    $http.get('/dms/redis/config-template/simple-list').success(function (data) {
        $scope.tmp.configTemplateList = data.list;
    });

    $scope.back = function () {
        Page.go('/page/redis_service', {});
    };

    $scope.add = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = _.clone($scope.editOne);
        if (one.mode == 'standalone') {
            one.replicas = 1;
        }
        if (one.mode != 'cluster') {
            one.shards = 1;
        }
        $http.post('/dms/redis/service/add', one).success(function (data) {
            if (data.id) {
                uiTips.tips('Add Success');
                Page.go('/page/redis_service', {});
            }
        });
    };
});
