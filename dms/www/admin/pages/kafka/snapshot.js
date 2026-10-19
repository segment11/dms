var md = angular.module('module_kafka/snapshot', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.editOne = {};

    var Page = window.Page;
    var params = Page.params();
    $scope.params = params;

    var serviceId = params.serviceId;

    $scope.queryLl = function () {
        if (serviceId == null) {
            // f5 reset browser, go back
            Page.go('/page/kafka_service', {});
            return;
        }

        $http.get('/dms/kafka/snapshot/list', {params: {serviceId: serviceId}}).success(function (data) {
            $scope.ll = data;
        });
    };

    $scope.queryLl();

    $scope.exportSnapshot = function () {
        uiTips.confirm('Sure Export Snapshot For Service - ' + serviceId + '?', function () {
            $http.post('/dms/kafka/snapshot/export', {serviceId: serviceId}).success(function (data) {
                uiTips.alert('Export success, snapshot dir: ' + data.snapshotDir);
                $scope.queryLl();
            });
        }, null);
    };

    $scope.showImport = function () {
        $scope.editOne = {};
        $scope.ctrl.isShowImport = true;
    };

    $scope.saveImport = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var body = {
            snapshotPath: $scope.editOne.snapshotPath,
            zkConnectString: $scope.editOne.zkConnectString
        };
        if ($scope.editOne.zkChroot) {
            body.zkChroot = $scope.editOne.zkChroot;
        }

        $http.post('/dms/kafka/snapshot/import', body).success(function (data) {
            $scope.ctrl.isShowImport = false;
            uiTips.alert('Import success, service id: ' + data.serviceId);
            $scope.queryLl();
        });
    };

    $scope.download = function (one) {
        window.open('/dms/kafka/snapshot/download?id=' + one.id, '_blank');
    };

    $scope.back = function () {
        Page.go('/page/kafka_one', {id: serviceId});
    };
});
