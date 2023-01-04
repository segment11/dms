var md = angular.module('module_cluster/container', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid, uiLog) {
    $scope.ctrl = {};
    $scope.tmp = {targetIndex: 0, log: {tail: 100}};

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var old = store.get('cluster_container_old');

    var Page = window.Page;
    var params = Page.params();

    if (params.appId) {
        store.set('cluster_container_old', params)
    } else if (old) {
        params = old;
    }
    $scope.params = params;

    if (params.targetIndex) {
        $scope.tmp.targetIndex = params.targetIndex;
    }

    $scope.queryLl = function () {
        $http.get('/dms/container/list', {params: {appId: params.appId}}).success(function (data) {
            $scope.tmp.isMonitorOn = data.isMonitorOn;

            var ll = _.sortBy(data.list, function (it) {
                return it.instanceIndex;
            });

            _.each($scope.ll, function (it) {
                delete it['$$hashKey'];
            });
            if (!_.isEqual($scope.ll, ll)) {
                $scope.ll = ll;
            } else {
                uiLog.i('skip set ll');
            }
        });
    };

    $scope.queryEventLl = function (pageNum) {
        var p = {
            pageNum: pageNum || 1,
            type: 'app',
            reason: $scope.tmp.reason,
            appId: params.appId,
            clusterId: params.clusterId
        };
        $http.get('/dms/event/list', {params: p}).success(function (data) {
            $scope.eventLl = data.list;
            $scope.eventPager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
        });
    };

    var tabIndexChoosed = 0;
    $scope.changeTab = function (index, triggerIndex) {
        tabIndexChoosed = index;
        if (index == 0) {
            $scope.queryLl();
            $scope.tmp.refreshTime = new Date();
        } else if (index == 1) {
            $http.get('/dms/event/reason/list', {params: {clusterId: params.clusterId, type: 'app'}}).success(function (data) {
                $scope.tmp.reasonList = data;
            });
            $scope.queryEventLl();
        } else if (index == 2) {
            $http.get('/dms/app/job/list', {params: {appId: params.appId}}).success(function (data) {
                $scope.jobList = data;
                $scope.tmp.refreshTime = new Date();
            });
        }

        var Page = window.Page;
        var isReg = Page.registerIntervalFunc(document.location.hash, 'refreshList');
        if (isReg) {
            uiLog.i('begin refreshList interval');
        }
        return true;
    };

    $scope.back = function () {
        Page.go('/page/cluster_app', {
            appId: $scope.appId,
            clusterId: params.clusterId,
            namespaceId: params.namespaceId
        });
    };

    $scope.inspect = function (x) {
        $http.get('/dms/container/manage/inspect', {params: {id: x.id}}).success(function (data) {
            $.dialog({
                title: 'Container Inspect Info',
                content: '<pre style="height: 400px;">' + JSON.stringify(data, null, 2) + '</pre>'
            });
        });
    };

    $scope.opt = function (x, cmd) {
        $.dialog({
            id: 'loading',
            title: false,
            cancel: false,
            fixed: true,
            lock: true,
            resize: false,
            icon: 'loading.gif'
        }).content('loading');
        $http({
            method: 'get',
            url: '/dms/container/manage/' + cmd,
            params: {id: x.id},
            timeout: 30 * 1000
        }).success(function (data) {
            $.dialog({id: 'loading'}).close();
            uiTips.tips(JSON.stringify(data));

            if ('remove' == cmd && data.flag) {
                var i = _.indexOf($scope.ll, x);
                $scope.ll.splice(i, 1);
                return;
            }

            $scope.queryLl();
        }).error(function () {
            $.dialog({id: 'loading'}).close();
        });
    };

    $scope.getJobTypeLabel = function (jobType) {
        var map = {1: 'create', 2: 'remove', 3: 'scroll'};
        return map[jobType];
    };

    $scope.getJobStatusLabel = function (status) {
        var map = {'0': 'created', '1': 'processing', '-1': 'failed', '10': 'done'};
        return map[status];
    };

    $scope.showMessage = function (one) {
        $.dialog({title: 'Job Message', content: '<pre style="height: 400px;">' + one.message + '</pre>'});
    };

    $scope.showBindList = function (x) {
        $http.get('/dms/container/manage/bind/list', {params: {id: x.id}}).success(function (data) {
            $scope.tmp.containerId = x.id;
            $scope.tmp.bindList = data;
            $scope.ctrl.isShowBinds = true;
        });
    };

    $scope.showPortBind = function (x) {
        $http.get('/dms/container/manage/port/bind', {params: {id: x.id}}).success(function (data) {
            $.dialog({
                title: 'Port Binds',
                content: '<pre style="height: 400px;">' + JSON.stringify(data, null, 2) + '</pre>'
            });
        });
    };

    $scope.showBindContent = function (containerDir) {
        var p = {id: $scope.tmp.containerId, containerDir: containerDir};
        $http.get('/dms/container/manage/bind/content', {params: p}).success(function (data) {
            $.dialog({title: 'Bind Detail', content: '<pre style="height: 400px;">' + data + '</pre>'});
        });
    };

    var Page = window.Page;
    var jobListSizeById = {};
    $scope.showJobLogList = function (one) {
        if (one && !_.isEqual($scope.tmp.job, one)) {
            $scope.tmp.job = one;
        }
        $http.get('/dms/app/job/log/list', {params: {jobId: $scope.tmp.job.id}}).success(function (data) {
            var number = data.number;
            var lastNumber = jobListSizeById[$scope.tmp.job.id];
            if (lastNumber && lastNumber == number && $('#jobLogList').findAll('li').length > 0) {
                uiLog.i('skip html set');
                // skip
                return;
            }
            jobListSizeById[$scope.tmp.job.id] = number;

            setTimeout(function () {
                $('#jobLogList').html(data.str);
            }, 500);
            $scope.ctrl.isShowJobLog = true;
            $scope.tmp.refreshTime = new Date();
//            Page.fixCenter('dialogJobLog');
        });
    };

    $scope.deleteJob = function (one) {
        uiTips.confirm('Sure Delete - ' + one.id + '?', function () {
            $http.delete('/dms/app/job/delete/', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.jobList, one);
                    $scope.jobList.splice(i, 1);
                }
            });
        }, null);
    };

    $scope.refreshList = function () {
        if ($scope.ctrl.isShowJobLog) {
            $scope.showJobLogList();
        } else if ($scope.ctrl.isShowContainerStats) {
            $scope.changeContainerStatsTab(statsTabIndexChoosed, -1, true);
        }

        if (tabIndexChoosed == 2) {
            $http.get('/dms/app/job/list', {params: {appId: params.appId}}).success(function (data) {
                _.each($scope.jobList, function (it) {
                    delete it['$$hashKey'];
                });
                if (!_.isEqual($scope.jobList, data)) {
                    $scope.jobList = data;
                } else {
                    uiLog.i('skip set jobList');
                }
            });
            $scope.tmp.refreshTime = new Date();
        } else if (tabIndexChoosed == 0) {
            $scope.queryLl();
            $scope.tmp.refreshTime = new Date();
        }
    };

    $scope.showStats = function (one) {
        $http.get('/dms/node/metric/queue', {
            params: {
                clusterId: $scope.tmp.clusterId,
                nodeIp: one.nodeIp,
                containerId: one.id,
                queueType: 'container',
                type: 'cpu'
            }
        }).success(function (data) {
            $scope.tmp.targetContainer = one;
            $scope.tmp.containerCpuChartData = {data: data.list, xData: data.timelineList};
            $scope.ctrl.isShowContainerStats = true;
            Page.fixCenter('dialogContainerStats');
        });
    };

    var statsTabIndexChoosed;
    $scope.changeContainerStatsTab = function (index, triggerIndex, isRefresh) {
        statsTabIndexChoosed = index;
        if (index == 0) {
            $http.get('/dms/node/metric/queue', {
                params: {
                    clusterId: $scope.tmp.clusterId,
                    nodeIp: $scope.tmp.targetContainer.nodeIp,
                    containerId: $scope.tmp.targetContainer.id,
                    queueType: 'container',
                    type: 'cpu'
                }
            }).success(function (data) {
                $scope.tmp.containerCpuChartData = {data: data.list, xData: data.timelineList};
                Page.fixCenter('dialogContainerStats');
            });
        } else if (index == 1) {
            $http.get('/dms/node/metric/queue', {
                params: {
                    clusterId: $scope.tmp.clusterId,
                    nodeIp: $scope.tmp.targetContainer.nodeIp,
                    containerId: $scope.tmp.targetContainer.id,
                    queueType: 'container',
                    type: 'mem'
                }
            }).success(function (data) {
                $scope.tmp.containerMemChartData = {data: data.list, xData: data.timelineList};
                Page.fixCenter('dialogContainerStats');
            });
        } else if (index == 2) {
            if (!isRefresh) {
                $http.get('/dms/node/metric/gauge/name/list', {
                    params: {
                        clusterId: $scope.tmp.clusterId,
                        nodeIp: $scope.tmp.targetContainer.nodeIp,
                        containerId: $scope.tmp.targetContainer.id
                    }
                }).success(function (data) {
                    $scope.tmp.gaugeNameFilterKeyword = '';
                    $scope.tmp.gaugeName = '';
                    $scope.tmp.gaugeNameList = data;
                    $scope.tmp.gaugeNameAllList = data;
                    $scope.tmp.containerGaugeChartData = {data: [], xData: []};
                    Page.fixCenter('dialogContainerStats');
                });
            } else {
                $scope.getGaugeValueList();
            }
        }
        return true;
    };

    $scope.filterGaugeNameList = function () {
        var keyword = $scope.tmp.gaugeNameFilterKeyword;
        if (!keyword) {
            return;
        }
        if (!$scope.tmp.gaugeNameAllList) {
            return;
        }

        $scope.tmp.gaugeNameList = _.filter($scope.tmp.gaugeNameAllList, function (it) {
            return it.contains(keyword);
        });
    };

    $scope.getGaugeValueList = function () {
        if (!$scope.tmp.gaugeName) {
            $scope.tmp.containerGaugeChartData = {data: [], xData: []};
            return;
        }

        $http.get('/dms/node/metric/queue', {
            params: {
                clusterId: $scope.tmp.clusterId,
                nodeIp: $scope.tmp.targetContainer.nodeIp,
                containerId: $scope.tmp.targetContainer.id,
                queueType: 'app',
                type: 'gauge',
                gaugeName: $scope.tmp.gaugeName
            }
        }).success(function (data) {
            $scope.tmp.containerGaugeChartData = {data: data.list, xData: data.timelineList};
            Page.fixCenter('dialogContainerStats');
        });
    };

    $scope.showLog = function (one) {
        if (!one) {
            one = $scope.tmp.showContainerLogOne;
        } else {
            $scope.tmp.showContainerLogOne = one;
        }
        $scope.ctrl.isShowContainerLog = true;

        var x = $scope.tmp.log.since;
        var since;
        if (x) {
            since = parseInt(Date.parse2(x).getTime() / 1000);
        } else {
            var date = new Date();
            $scope.tmp.log.since = new Date(date.getTime() - 3600 * 1000 * 24).format('yyyy-MM-dd HH:mm:ss');
            since = parseInt(date.getTime() / 1000 - 24 * 3600);
        }
        var p = {params: {id: one.id, tail: $scope.tmp.log.tail, since: since}, responseType: 'text'};
        uiTips.loading();
        $http.get('/dms/container/log', p).success(function (data) {
            $scope.tmp.logMessage = data;
            Page.fixCenter('dialogContainerLog');
        });
    };
});
