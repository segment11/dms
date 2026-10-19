var md = angular.module('module_kafka/config-template', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {pageNum: 1};
    $scope.ctrl = {};
    $scope.editOne = {configItems: {items: []}};

    $scope.queryLl = function (pageNum) {
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword};

        $http.get('/dms/kafka/config-template/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;
        });
    };

    $scope.queryLl();

    $scope.addOne = function () {
        $scope.editOne = {configItems: {items: []}};
        $scope.ctrl.isShowAdd = true;
    };

    $scope.edit = function (one) {
        $scope.editOne = _.clone(one);
        $scope.editOne.configItems = {items: _.clone(one.configItems.items)};
        $scope.ctrl.isShowAdd = true;
    };

    $scope.save = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = _.clone($scope.editOne);
        if (one.id) {
            $http.post('/dms/kafka/config-template/update', one).success(function (data) {
                if (data.flag) {
                    $scope.ctrl.isShowAdd = false;
                    $scope.queryLl();
                }
            });
        } else {
            $http.post('/dms/kafka/config-template/add', one).success(function (data) {
                if (data.id) {
                    $scope.ctrl.isShowAdd = false;
                    $scope.queryLl();
                }
            });
        }
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/kafka/config-template/delete', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };
});
