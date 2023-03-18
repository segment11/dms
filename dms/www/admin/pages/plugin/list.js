var md = angular.module('module_plugin/list', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.ctrl = {};
    $scope.tmp = {pageNum: 1};
    $scope.editOne = {};

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var old = store.get('plugin_keyword');
    if (old && old.keyword) {
        $scope.tmp.keyword = old.keyword;
    }

    $scope.queryLl = function (pageNum) {
        uiTips.loading();
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword};
        if (p.keyword) {
            store.set('plugin_keyword', p);
        } else {
            store.remove('plugin_keyword');
        }

        $http.get('/dms/plugin/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };
    $scope.queryLl();

    $scope.load = function () {
        uiTips.prompt('Load one plugin, plugin groovy file dir: $project_root/plugins, input full class name:', function (val) {
            if (!val.trim()) {
                uiTips.alert('Need class name!');
                return;
            }
            $http.post('/dms/plugin/load', {className: val}).success(function (data) {
                if (data.flag) {
                    uiTips.tips('Done load plugin - ' + val, null, 'face-smile.png');
                    setTimeout(function () {
                        $scope.queryLl();
                    }, 2000);
                } else {
                    uiTips.tips(data.message);
                }
            });
        }, 'plugin.demo2.ConsulPlugin');
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/plugin/delete', {params: {name: one.name}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                } else {
                    uiTips.tips(data.message);
                }
            });
        }, null);
    };

    $scope.reload = function (one) {
        uiTips.confirm('Sure Reload - ' + one.name + '?', function () {
            uiTips.loading();
            $http.post('/dms/plugin/load', {className: one.className}).success(function (data) {
                uiTips.unloading();
                if (data.flag) {
                    uiTips.tips('Done reload plugin - ' + one.name, null, 'face-smile.png');
                    setTimeout(function () {
                        $scope.queryLl();
                    }, 2000);
                } else {
                    uiTips.tips(data.message);
                }
            });
        }, null);
    };
});
