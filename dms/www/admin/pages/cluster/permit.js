var md = angular.module('module_cluster/permit', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.ctrl = {};
    $scope.tmp = {pageNum: 1};
    $scope.editOne = {};

    $scope.$watch('editOne.permitType', function (val) {
        if (!val || val == 'admin' || val == 'imageManager') {
            $scope.editOne.resourceId = 0;
            $scope.tmp.targetList = [{id: 0, name: 'all'}];
            return;
        }
        $http.get('/dms/permit/resource/list', {params: {permitType: val}}).success(function (data) {
            $scope.tmp.targetList = data;
        });
    }, true);

    $scope.queryLl = function (pageNum) {
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword};
        if ($scope.tmp.appId) {
            p.permitType = 'app';
            p.resourceId = $scope.tmp.appId;
        } else if ($scope.tmp.namespaceId) {
            p.permitType = 'namespace';
            p.resourceId = $scope.tmp.namespaceId;
        } else if ($scope.tmp.clusterId) {
            p.permitType = 'cluster';
            p.resourceId = $scope.tmp.clusterId;
        } else {
            p.permitType = $scope.tmp.permitType;
        }
        $http.get('/dms/permit/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };
    $scope.queryLl();

    $http.get('/dms/cluster/list/simple').success(function (data) {
        $scope.tmp.clusterList = data.list;
    });

    $scope.onClusterChoose = function () {
        var clusterId = $scope.tmp.clusterId;
        if (!clusterId) {
            $scope.tmp.namespaceList = [];
            $scope.tmp.namespaceId = null;
            $scope.tmp.appList = [];
            $scope.tmp.appId = null;
            return;
        }

        $http.get('/dms/namespace/list/simple', {params: {clusterId: clusterId}}).success(function (data) {
            $scope.tmp.namespaceList = data;
            $scope.tmp.appList = [];
            $scope.tmp.namespaceId = null;
            $scope.tmp.appId = null;
        });
    };

    $scope.onNamespaceChoose = function () {
        var namespaceId = $scope.tmp.namespaceId;
        if (!namespaceId) {
            $scope.tmp.appList = [];
            return;
        }

        $http.get('/dms/app/list/simple', {params: {namespaceId: namespaceId}}).success(function (data) {
            $scope.tmp.appList = data;
        });
    };

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
        delete one.resourceName;
        delete one.resourceDes;
        $http.post('/dms/permit/update', one).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                $scope.queryLl();
            }
        });
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/permit/delete', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };
});
