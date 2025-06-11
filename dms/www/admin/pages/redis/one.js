var md = angular.module('module_redis/one', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.ext = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var id = params.id;
    if (!id) {
        id = store.get('redis_service_one_id');
        if (!id) {
            Page.go('/page/redis_service', {});
            return;
        }
    } else {
        store.set('redis_service_one_id', id);
    }

    $scope.refresh = function () {
        uiTips.loading();
        $http.get('/dms/redis/service/one', {params: {id: id}}).success(function (data) {
            $scope.one = data.one;
            $scope.checkResult = data.checkResult;
            $scope.ext.configTemplateOne = data.configTemplateOne;
            $scope.nodes = data.nodes;
        });
    };

    $scope.refresh();

    $scope.back = function () {
        Page.go('/page/redis_service', {});
    };

    $scope.updateMaxmemory = function (one) {
        uiTips.prompt('Update redis server maxmemory: ', function (val) {
            if (!val) {
                uiTips.alert('Please input maxmemory (MB)');
                return;
            }

            var isNumber = /\d/.test(val);
            if (!isNumber) {
                uiTips.alert('Please input maxmemory (MB) as a number');
                return;
            }

            var valInt = parseInt(val);
            if (valInt < 256) {
                uiTips.alert('Please input maxmemory (MB) as a number greater than 256, yours: ' + valInt);
                return;
            }
            if (valInt > 1024 * 64) {
                uiTips.alert('Please input maxmemory (MB) as a number less than 1024 * 64, yours: ' + valInt);
                return;
            }
            if (valInt % 256 != 0) {
                uiTips.alert('Please input maxmemory (MB) as a number multiple of 256, yours: ' + valInt);
                return;
            }

            $http.post('/dms/redis/config/update-maxmemory', {
                id: one.id,
                maxmemoryMb: val
            }).success(function (data) {
                if (data.flag) {
                    uiTips.alert('Update maxmemory success');
                }
            });
        }, one.maxmemoryMb);
    };
});
