var md = angular.module('module_kafka/add', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};
    $scope.editOne = {
        mode: 'standalone',
        kafkaVersion: '2.8.2',
        port: 9092,
        brokers: 1,
        defaultPartitions: 8,
        defaultReplicationFactor: 1,
        heapMb: 1024
    };
    $scope.templateList = [];

    $http.get('/dms/kafka/config-template/list', {params: {pageNum: 1, pageSize: 100}}).success(function (data) {
        $scope.templateList = data.list;
    });

    $scope.onModeChange = function () {
        if ($scope.editOne.mode == 'cluster') {
            if (!$scope.editOne.brokers || $scope.editOne.brokers < 3) {
                $scope.editOne.brokers = 3;
            }
        } else {
            $scope.editOne.brokers = 1;
        }
    };

    $scope.save = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        if ($scope.editOne.mode == 'cluster' && $scope.editOne.brokers < 3) {
            uiTips.alert('cluster mode needs at least 3 brokers');
            return;
        }

        var body = {
            name: $scope.editOne.name,
            des: $scope.editOne.des,
            mode: $scope.editOne.mode,
            kafkaVersion: $scope.editOne.kafkaVersion,
            zkConnectString: $scope.editOne.zkConnectString,
            port: parseInt($scope.editOne.port, 10),
            brokers: parseInt($scope.editOne.brokers, 10),
            defaultPartitions: parseInt($scope.editOne.defaultPartitions, 10),
            defaultReplicationFactor: parseInt($scope.editOne.defaultReplicationFactor, 10),
            heapMb: parseInt($scope.editOne.heapMb, 10)
        };

        if ($scope.editOne.configTemplateId) {
            body.configTemplateId = $scope.editOne.configTemplateId;
        }

        if ($scope.tmp.nodeTags) {
            var tagList = $scope.tmp.nodeTags.split(',');
            var nodeTags = [];
            for (var i = 0; i < tagList.length; i++) {
                var tag = tagList[i].replace(/^\s+|\s+$/g, '');
                if (tag) {
                    nodeTags.push(tag);
                }
            }
            if (nodeTags.length > 0) {
                body.nodeTags = nodeTags;
            }
        }

        $http.post('/dms/kafka/service/add', body).success(function (data) {
            if (data.id) {
                uiTips.tips('Add Success');
                Page.go('/page/kafka_one', {id: data.id});
            }
        });
    };

    $scope.back = function () {
        Page.go('/page/kafka_service', {});
    };
});
