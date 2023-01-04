var md = angular.module('module_image/env', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.ctrl = {};
    $scope.tmp = {pageNum: 1};
    $scope.editOne = {};

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var old = store.get('image_env_keyword');
    if (old && old.keyword) {
        $scope.tmp.keyword = old.keyword;
    }

    $scope.queryLl = function (pageNum) {
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword};
        if (p.keyword) {
            store.set('image_env_keyword', p);
        } else {
            store.remove('image_env_keyword');
        }

        $http.get('/dms/image/config/env/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };
    $scope.queryLl();

    $scope.edit = function (one) {
        $scope.editOne = _.clone(one);
        $scope.ctrl.isShowAdd = true;
    };

    $scope.copy = function (one) {
        var copy = _.clone(one);
        delete copy.id;
        copy.name += ' copy';
        $scope.editOne = copy;
        $scope.ctrl.isShowAdd = true;
    };

    $scope.save = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = _.clone($scope.editOne);
        $http.post('/dms/image/config/env/update', one).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                $scope.queryLl();
            }
        });
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/image/config/env/delete/', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };
});
