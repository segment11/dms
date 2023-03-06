var md = angular.module('module_cluster/deploy', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiLog, uiValid) {
    $scope.ctrl = {};
    $scope.tmp = {tabIndex: 0, pageNum: 1};
    $scope.editOne = {};

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var old = store.get('deploy_choose_node_ip');

    var Page = window.Page;
    var params = Page.params();

    if (params.ip) {
        store.set('deploy_choose_node_ip', params)
    } else if (old) {
        params = old;
    }

    $scope.queryLl = function (pageNum) {
        var p = {pageNum: pageNum || $scope.tmp.pageNum, keyword: $scope.tmp.keyword, clusterId: $scope.tmp.clusterId};
        if (!p.clusterId) {
            uiTips.tips('Choose One Cluster First', null, 'alert.gif');
            return;
        }
        $http.get('/dms/deploy/node-file/list', {params: p}).success(function (data) {
            $scope.deployFileList = data.deployFileList;
            if (!data.deployFileList.length) {
                uiTips.tips('No files get! Change another keyword to query!', 3, 'alert.gif');
            }
            if (p.keyword) {
                $('#deploy-tabs').find('a').eq(1).trigger('click');
            }

            var pager = data.pager;
            $scope.nodeList = pager.list;
            $scope.pager = {pageNum: pager.pageNum, pageSize: pager.pageSize, totalCount: pager.totalCount};
            $scope.tmp.pageNum = data.pageNum;

            var one = _.find($scope.nodeList, function (it) {
                return it.ip == params.ip;
            });
            if (one) {
                one.isChecked = true;
            }
        });
    };

    $http.get('/dms/cluster/list/simple').success(function (data) {
        $scope.tmp.clusterList = data.list;
        if (data.list.length) {
            $scope.tmp.clusterId = data.list[0].id;
            $scope.queryLl();
        }
    });

    $scope.doDeploy = function () {
        var chooseNodeList = _.filter($scope.nodeList, function (it) {
            return it.isChecked;
        });
        if (!chooseNodeList.length) {
            uiTips.alert('Choose one node first!');
            return;
        }

        var targetFile = _.find($scope.deployFileList, function (it) {
            return it.id == $scope.tmp.fileId;
        });
        if (!targetFile) {
            uiTips.alert('Choose one file first!');
            return;
        }

        var one = {};
        one.nodeIpList = _.collect(chooseNodeList, function (it) {
            return it.ip;
        });
        one.targetId = targetFile.id;

        uiTips.loading();
        $http.post('/dms/deploy/begin', one).success(function (data) {
            uiTips.unloading();
            if (data.flag) {
                uiTips.tips('Done deploy files!', null, 'face-smile.png');
            } else {
                uiTips.alert(data.message);
            }
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
        one.clusterId = $scope.tmp.clusterId;
        delete one.id;

        uiTips.loading();
        $http.post('/dms/node/key-pair/init', one).success(function (data) {
            uiTips.unloading();
            if (data.flag) {
                $scope.ctrl.isShowAdd = false;
                $scope.queryLl();
                uiTips.tips(data.message);
            } else {
                uiTips.alert(data.message);
            }
        });
    };

    $scope.resetRootPassword = function (one) {
        $.dialog({
            id: 'loading',
            title: false,
            cancel: false,
            fixed: true,
            lock: true,
            resize: false,
            icon: 'loading.gif'
        }).content('loading');
        var body = {
            id: one.id
        };
        $http({
            method: 'post',
            url: '/dms/node/key-pair/reset-root-pass',
            data: body,
            timeout: 60 * 1000
        }).success(function (data) {
            $.dialog({id: 'loading'}).close();
            if (data.steps) {
                $.dialog({
                    title: 'Steps',
                    content: '<pre style="height: 400px;">' + JSON.stringify(data.steps, null, 2) + '</pre>'
                });
            }
            if (data.flag) {
                uiTips.tips('init ok! ' + data.message, null, 'face-smile.png');
            } else {
                uiTips.alert(data.message);
            }
        }).error(function () {
            $.dialog({id: 'loading'}).close();
        });
    };

    $scope.initAgent = function (one) {
        $.dialog({
            id: 'loading',
            title: false,
            cancel: false,
            fixed: true,
            lock: true,
            resize: false,
            icon: 'loading.gif'
        }).content('loading');
        var body = {
            id: one.id
        };
        $http({
            method: 'post',
            url: '/dms/node/agent/init',
            data: body,
            timeout: 60 * 1000
        }).success(function (data) {
            $.dialog({id: 'loading'}).close();
            if (data.steps) {
                $.dialog({
                    title: 'Steps',
                    content: '<pre style="height: 400px;">' + JSON.stringify(data.steps, null, 2) + '</pre>'
                });
            }
            if (data.flag) {
                uiTips.tips('init ok! ' + data.message, null, 'face-smile.png');
            } else {
                uiTips.alert(data.message);
            }
        }).error(function () {
            $.dialog({id: 'loading'}).close();
        });
    };

    $scope.startAgent = function (one) {
        $.dialog({
            id: 'loading',
            title: false,
            cancel: false,
            fixed: true,
            lock: true,
            resize: false,
            icon: 'loading.gif'
        }).content('loading');
        var body = {
            id: one.id
        };
        $http({
            method: 'post',
            url: '/dms/node/agent/start',
            data: body,
            timeout: 60 * 1000
        }).success(function (data) {
            $.dialog({id: 'loading'}).close();
            if (data.steps) {
                $.dialog({
                    title: 'Steps',
                    content: '<pre style="height: 400px;">' + JSON.stringify(data.steps, null, 2) + '</pre>'
                });
            }
            if (data.flag) {
                uiTips.tips('start ok! wait and check heart beat if ok. ' + data.message, null, 'face-smile.png');
            } else {
                uiTips.alert(data.message);
            }
        }).error(function () {
            $.dialog({id: 'loading'}).close();
        });
    };

    $scope.stopAgent = function (one) {
        uiTips.confirm('Sure Stop - ' + one.ip + '?', function () {
            $.dialog({
                id: 'loading',
                title: false,
                cancel: false,
                fixed: true,
                lock: true,
                resize: false,
                icon: 'loading.gif'
            }).content('loading');
            var body = {
                id: one.id
            };
            $http({
                method: 'post',
                url: '/dms/node/agent/stop',
                data: body,
                timeout: 10 * 1000
            }).success(function (data) {
                $.dialog({id: 'loading'}).close();
                if (data.steps) {
                    $.dialog({
                        title: 'Steps',
                        content: '<pre style="height: 400px;">' + JSON.stringify(data.steps, null, 2) + '</pre>'
                    });
                }
                if (data.flag) {
                    uiTips.tips('stop ok! ' + data.message, null, 'face-smile.png');
                } else {
                    uiTips.alert(data.message);
                }
            }).error(function () {
                $.dialog({id: 'loading'}).close();
            });
        });
    };

    $scope.removeNode = function (one) {
        uiTips.confirm('Sure Remove Node - ' + one.ip + '?', function () {
            $http.delete('/dms/node/agent/remove-node', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.nodeList, one);
                    $scope.nodeList.splice(i, 1);
                }
            });
        }, null);
    };

    $scope.editDeployFile = function (one) {
        $scope.editOneDeployFile = _.clone(one);
        $scope.ctrl.isShowAddDeployFile = true;
    };

    $scope.queryDeployFileList = function () {
        var p = {keyword: $scope.tmp.keyword};
        $http.get('/dms/deploy-file/list', {params: p}).success(function (data) {
            $('#deploy-tabs').find('a').eq(1).trigger('click');

            $scope.deployFileList = data.deployFileList;
            if (!data.deployFileList.length) {
                uiTips.tips('No files get! Change another keyword to query!', 3, 'alert.gif');
            }
        });
    };

    $scope.saveDeployFile = function () {
        if (!uiValid.checkForm($scope.tmp.addDeployFileForm) || !$scope.tmp.addDeployFileForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = _.clone($scope.editOneDeployFile);
        uiTips.loading();
        $http.post('/dms/deploy-file/update', one).success(function (data) {
            uiTips.unloading();
            if (data.id) {
                $scope.ctrl.isShowAddDeployFile = false;
                $scope.queryDeployFileList();
            }

            if (data.message) {
                uiTips.alert(data.message);
            }
        });
    };

    $scope.deleteDeployFile = function (one) {
        uiTips.confirm('Sure Delete (not local file) - ' + one.localPath + '?', function () {
            $http.delete('/dms/deploy-file/delete', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.deployFileList, one);
                    $scope.deployFileList.splice(i, 1);
                }
            });
        }, null);
    };

    // todo
    $scope.imageList = [
        {file: 'etcd3.tar.gz', tag: '3.4.22', des: 'segment/etcd:latest'},
        {file: 'filebeat.tar.gz', tag: '6', des: 'segment/filebeat:6'},
        {file: 'ibex.tar.gz', tag: '0.3', des: 'n9e/ibex:0.3'},
        {file: 'kafka.tar.gz', tag: '1.1.1', des: 'segment/kafka:1.1.1'},
        {file: 'kvrocks.tar.gz', tag: '2.2.0', des: 'apache/kvrocks:2.2.0'},
        {file: 'kvrocks_exporter.tar.gz', tag: 'latest', des: 'kvrocks/kvrocks_exporter:latest'},
        {file: 'mysql.tar.gz', tag: '5.7', des: 'library/mysql:5.7'},
        {file: 'mysql_exporter.tar.gz', tag: 'latest', des: 'mysql/mysql_exporter:latest'},
        {file: 'nginx.tar.gz', tag: '1.22.1', des: 'library/nginx:1.22.1'},
        {file: 'nginx_exporter.tar.gz', tag: '0.11', des: 'nginx/nginx-prometheus-exporter:0.11'},
        {file: 'nightingale.tar.gz', tag: 'v5', des: 'n9e/nightingale:5'},
        {file: 'node_exporter.tar.gz', tag: 'latest', des: 'prometheus/node-exporter:latest'},
        {file: 'pg.tar.gz', tag: '14.5', des: 'segment/patroni_pg:14.5'},
        {file: 'pg_exporter.tar.gz', tag: 'latest', des: 'prom/postgres-exporter:latest'},
        {file: 'prometheus.tar.gz', tag: 'v2.25.0', des: 'prom/prometheus:v2.25.0'},
        {file: 'redis.tar.gz', tag: '6.2', des: 'library/redis:6.2'},
        {file: 'zk.tar.gz', tag: '3.6.4', des: 'key232323/zookeeper:3.6.4'}
    ];

    $scope.loadImage = function (one) {
        _.each($scope.imageList, function (it) {
            it.isLoaded = false;
        });

        $scope.tmp.nodeOne = one;
        $scope.viewImages(one, function (data) {
            var list = data.list;
            if (!list) {
                return;
            }

            var tags = [];
            _.each(list, function (it) {
                _.each(it.RepoTags, function (that) {
                    if (that.indexOf('/') == -1) {
                        tags.push('library/' + that);
                    } else {
                        tags.push(that);
                    }
                });
            });
            uiLog.log(tags);

            _.each($scope.imageList, function (it) {
                it.isLoaded = tags.indexOf(it.des) != -1;
            });

            $scope.ctrl.isShowLoadImage = true;
            Page.fixCenter('dialogLoadImage');
        });
    };

    $scope.doLoadImage = function (imageOne) {
        $.dialog({
            id: 'loading',
            title: false,
            cancel: false,
            fixed: true,
            lock: true,
            resize: false,
            icon: 'loading.gif'
        }).content('loading');

        var one = {};
        one.id = $scope.tmp.nodeOne.id;
        one.imageTarGzName = imageOne.file;
        uiTips.loading();
        $http.post('/dms/agent/image/init/load', one).success(function (data) {
            $.dialog({id: 'loading'}).close();
            if (data.steps) {
                $.dialog({
                    title: 'Steps',
                    content: '<pre style="height: 400px;">' + JSON.stringify(data.steps, null, 2) + '</pre>'
                });
            }
            if (data.flag) {
                uiTips.tips('load ok! ' + data.message, null, 'face-smile.png');
                $scope.loadImage($scope.tmp.nodeOne);
            } else {
                uiTips.alert(data.message);
            }
        }).error(function () {
            $.dialog({id: 'loading'}).close();
        });
    };

    $scope.viewImages = function (one, callback) {
        $http.get('/dms/agent/image/init/view', {params: {id: one.id}}).success(function (data) {
            if (callback) {
                callback(data);
            } else {
                $.dialog({
                    title: 'Container Image List - ' + one.ip,
                    content: '<pre style="height: 400px;">' + JSON.stringify(data, null, 2) + '</pre>'
                });
            }
        });
    };
});
