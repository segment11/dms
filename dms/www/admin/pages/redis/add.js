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
        shards: 1,
        replicas: 1,
        extendParams: {params: params},
        backupPolicy: {startTime: '00:00', retentionPeriod: 7, durationHours: 3, dailyOrHourly: 'daily'}
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

    $http.get('/dms/redis/backup-template/simple-list').success(function (data) {
        $scope.tmp.backupTemplateList = data.list;
    });

    $scope.back = function () {
        Page.go('/page/redis_service', {});
    };

    $scope.$watch('editOne.mode', function (val) {
        if (val == 'cluster') {
            if (!$scope.editOne.shards || $scope.editOne.shards == 1) {
                $scope.editOne.shards = 2;
            }
            if (!$scope.editOne.replicas) {
                $scope.editOne.replicas = 2;
            }
        } else if (val == 'standalone') {
            $scope.editOne.shards = 1;
            $scope.editOne.replicas = 1;
        } else {
            $scope.editOne.shards = 1;
            if (!$scope.editOne.replicas) {
                $scope.editOne.replicas = 2;
            }
        }
    }, true);

    $scope.add = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }
        if (!uiValid.checkForm($scope.tmp.addForm2) || !$scope.tmp.addForm2.$valid) {
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
