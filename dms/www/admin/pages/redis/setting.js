var md = angular.module('module_redis/setting', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    $http.get('/dms/redis/setting', {params: {}}).success(function (data) {
        $scope.tmp.dataDir = data.dataDir;
    });

    $scope.changeDataDir = function () {
        uiTips.prompt('Update redis data mount node directory: ', function (val) {
            if (!val) {
                uiTips.alert('Please input a directory path');
                return;
            }

            $http.post('/dms/redis/setting/data-dir', {
                dataDir: val
            }).success(function (data) {
                if (data.flag) {
                    uiTips.alert('Update data dir success');
                }
            });
        }, $scope.tmp.dataDir);
    };
});
