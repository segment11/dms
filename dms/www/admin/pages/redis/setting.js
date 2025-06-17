var md = angular.module('module_redis/setting', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    $http.get('/dms/redis/setting', {params: {}}).success(function (data) {
        $scope.tmp.dataDir = data.dataDir;
        $scope.tmp.backupDataDir = data.backupDataDir;
        $scope.tmp.isOnlyOneNodeForTest = data.isOnlyOneNodeForTest;
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
                    $scope.tmp.dataDir = val;
                }
            });
        }, $scope.tmp.dataDir);
    };

    $scope.changeBackupDataDir = function () {
        uiTips.prompt('Update redis backup data directory: ', function (val) {
            if (!val) {
                uiTips.alert('Please input a directory path');
                return;
            }

            $http.post('/dms/redis/setting/backup-data-dir', {
                backupDataDir: val
            }).success(function (data) {
                if (data.flag) {
                    uiTips.alert('Update backup data dir success');
                    $scope.tmp.backupDataDir = val;
                }
            })
        }, $scope.tmp.backupDataDir);
    };

    $scope.changeOneNodeForTestFlag = function () {
        $http.post('/dms/redis/setting/change-one-node-for-test-flag', {}).success(function (data) {
            $scope.tmp.isOnlyOneNodeForTest = data.isOnlyOneNodeForTest;
        });
    };
});
