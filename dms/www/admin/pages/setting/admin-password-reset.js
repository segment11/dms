var md = angular.module('module_setting/admin-password-reset', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.editOne = {};

    $scope.update = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = _.clone($scope.editOne);
        one.user = 'admin';
        one.isDoUpdate = '1';
        $http.post('/dms/setting/admin-password-reset', one).success(function (data) {
            if (data.error) {
                uiTips.tips(data.error);
            } else {
                uiTips.tips(data.message);
            }
        });
    };
});
