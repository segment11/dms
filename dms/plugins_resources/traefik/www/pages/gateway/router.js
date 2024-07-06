var md = angular.module('module_gateway/router', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.ctrl = {};
    $scope.tmp = {};
    $scope.editOne = {globalEnvConf: {}};

    var Page = window.Page;
    var params = Page.params();

    $scope.clusterName = params.clusterName;

    var queryLl = function () {
        $http.get('/dms/gw/router/list', {params: {clusterId: params.clusterId}}).success(function (data) {
            $scope.ll = data;
        });
    };
    queryLl();

    $http.get('/dms/gw/cluster/list/simple').success(function (data) {
        $scope.tmp.clusterList = data.list;
    });

    $http.get('/dms/gw/middleware/type/list').success(function (data) {
        $scope.tmp.middlewareTypeList = data.list;
    });

    $scope.back = function () {
        Page.go('/page/gateway_cluster');
    };

    $scope.addOne = function () {
        $scope.editOne = {
            middlewares: {list: []},
            service: {
                name: 'my-service',
                loadBalancer: {serverUrlList: []},
                weighted: {services: [], stickyCookie: {}},
                mirroring: {healthCheck: {}, mirrors: []}
            }
        };
        $scope.ctrl.isShowAdd = true;
    };

    $scope.edit = function (one) {
        if (!one.middlewares) {
            one.middlewares = {list: []};
        }
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
        $http.post('/dms/gw/router/update', one).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                queryLl();
            }
        });
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/gw/router/delete/', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };
});
