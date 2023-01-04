var md = angular.module('module_cluster/namespace', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.ctrl = {};
    $scope.tmp = {pageNum: 1};
    $scope.editOne = {};

    $scope.queryLl = function (pageNum) {
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword, clusterId: $scope.tmp.clusterId};
        if (!p.clusterId) {
            uiTips.tips('Choose One Cluster First');
            return;
        }
        $http.get('/dms/namespace/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };

    $http.get('/dms/cluster/list/simple').success(function (data) {
        $scope.tmp.clusterList = data.list;
        if (data.list.length) {
            $scope.tmp.clusterId = data.list[0].id;
            $scope.queryLl();
        }
    });

    $scope.edit = function (one) {
        $scope.editOne = _.clone(one);
        $scope.ctrl.isShowAdd = true;
    };

    $scope.save = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = _.clone($scope.editOne);
        $http.post('/dms/namespace/update', one).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                $scope.tmp.clusterId = one.clusterId;
                $scope.queryLl();
            }
        });
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/namespace/delete/', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };
});
