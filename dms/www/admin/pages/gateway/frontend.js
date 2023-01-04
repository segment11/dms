var md = angular.module('module_gateway/frontend', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.ctrl = {};
    $scope.tmp = {};
    $scope.editOne = {globalEnvConf: {}};

    var Page = window.Page;
    var params = Page.params();

    $scope.clusterName = params.clusterName;

    var queryLl = function () {
        $http.get('/dms/gw/frontend/list', {params: {clusterId: params.clusterId}}).success(function (data) {
            $scope.ll = data;
        });
    };
    queryLl();

    $http.get('/dms/gw/cluster/list/simple').success(function (data) {
        $scope.tmp.clusterList = data;
    });

    $scope.addOne = function () {
        $scope.editOne = {conf: {ruleConfList: []}, auth: {basicList: []}, backend: {serverList: []}};
        $scope.ctrl.isShowAdd = true;
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
        uiTips.loading();
        $http.post('/dms/gw/frontend/update', one).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                queryLl();
            }
        });
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/gw/frontend/delete/', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };
});
