var md = angular.module('module_redis/backup-template', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.editOne = {provider: 'idc', targetType: 'scp', targetBucket: {}};

    $scope.queryLl = function (pageNum) {
        $http.get('/dms/redis/backup-template/list', {params: {}}).success(function (data) {
            $scope.ll = data.list;
        });
    };

    $scope.queryLl();

    $http.get('/dms/redis/backup-template/backup-target-node/list', {params: {}}).success(function (data) {
        $scope.tmp.backupTargetNodeList = data.list;
        if (data.list.length == 0) {
            uiTips.tips('Please add backup target nodes first, menu: Cluster -> Node Init');
        }
    });

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
        if (one.targetType == 'scp') {
            if (!one.targetNodeIps || one.targetNodeIps.length == 0) {
                uiTips.tips('Need choose target node ips');
                return;
            }
        }
        $http.post('/dms/redis/backup-template/update', one).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                $scope.queryLl();
            }
        });
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/redis/backup-template/delete', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };
});
