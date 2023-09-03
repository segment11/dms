var md = angular.module('module_cluster/overview', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {isGuardianRunning: false};
    $scope.ctrl = {};
    var Page = window.Page;

    $http.get('/dms/cluster/list/simple').success(function (data) {
        $scope.clusterList = data.list;
        if (data.list.length) {
            $scope.tmp.clusterId = data.list[0].id;
            $scope.onClusterChoose();
        }

        $scope.tmp.isGuardianRunning = data.isGuardianRunning;
    });

    var groupByToList = function (groupByObj) {
        var r = [];
        for (k in groupByObj) {
            var list = groupByObj[k];
            var first = list[0];
            r.push({
                key: k, appName: first.appName, appDes: first.appDes, namespaceId: first.namespaceId, list: list
            });
        }
        return r;
    };

    $scope.changeNodeListInPage = function (pageNum) {
        if (!$scope.tmp.nodeAllList) {
            return;
        }

        var nodeList = $scope.tmp.nodeAllList;
        var pageSize = 5;
        var cc = nodeList.length;
        var currentPage = pageNum || 1;

        var beginIndex = (currentPage - 1) * pageSize;
        var endIndex = currentPage * pageSize;

        if (beginIndex > cc - 1)
            beginIndex = 0;
        if (endIndex > cc)
            endIndex = cc;

        $scope.nodeList = nodeList.slice(beginIndex, endIndex);
        $scope.nodePager = {pageNum: currentPage, pageSize: pageSize, totalCount: cc};
    };

    $scope.toggleDMSGuardian = function () {
        uiTips.confirm('Sure toggle?', function () {
            $http.get('/dms/guard/toggle').success(function (data) {
                $scope.tmp.isGuardianRunning = data.isGuardianRunning;
            });
        });
    };

    $scope.toggleClusterIsInGuard = function () {
        uiTips.confirm('Sure toggle?', function () {
            $http.get('/dms/cluster/guard/toggle', {params: {id: $scope.tmp.clusterId}}).success(function (data) {
                $scope.tmp.clusterChoose.isInGuard = data.isInGuard;
            });
        });
    };

    $scope.onClusterChoose = function () {
        $scope.tmp.clusterChoose = _.find($scope.clusterList, function (it) {
            return it.id == $scope.tmp.clusterId;
        });

        $http.get('/dms/container/manage/list', {params: {clusterId: $scope.tmp.clusterId}}).success(function (data) {
            $scope.groupByApp = groupByToList(data.groupByApp);
            $scope.groupByNodeIp = groupByToList(data.groupByNodeIp);

            var cpusetCpusMapByNodeIp = data.cpusetCpusMapByNodeIp;
            var cpuUsedPercentMapByNodeIp = data.cpuUsedPercentMapByNodeIp;

            var memByNodeIp = data.memByNodeIp;
            var memRssUsedMapByNodeIp = data.memRssUsedMapByNodeIp;

            _.each($scope.groupByNodeIp, function (it) {
                var nodeIp = it.key;
                var cpusetCpus = cpusetCpusMapByNodeIp[nodeIp];
                var cpuUsedPercentCpus = cpuUsedPercentMapByNodeIp[nodeIp];
                var clist = [];
                for (_vcore in cpusetCpus) {
                    var requiredList = cpusetCpus[_vcore];
                    var sum = _.reduce(requiredList, function (memo, it) {
                        return memo + it;
                    }, 0);
                    clist.push({
                        vcore: _vcore,
                        required: Math.round(sum * 10000) / 10000,
                        usedPercent: cpuUsedPercentCpus[_vcore]
                    });
                }
                it.clist = clist;

                // long
                var mem = memByNodeIp[nodeIp];
                // map<string,long>
                var memRssUsed = memRssUsedMapByNodeIp[nodeIp];
                var mlist = [];
                var memLeft = mem;
                for (appName in memRssUsed) {
                    var memRss = memRssUsed[appName];
                    var memUsed = Math.round(memRss / mem * 10000) / 10000;
                    mlist.push({appName: appName, memUsed: memUsed, memRss: memRss});
                    memLeft = memLeft - memUsed;
                }

                it.mlist = _.sortBy(mlist, function (it) {
                    return -it.memRss;
                });
                it.memLeft = Math.round(memLeft);
                it.memLeftUsed = Math.round(memLeft / mem * 10000) / 10000;
            });

            var appCheckOkList = data.appCheckOkList;

            $scope.appChartData = [{
                name: 'Check OK', value: _.filter(appCheckOkList, function (it) {
                    return it.isOk;
                }).length
            }, {
                name: 'Check Fail', value: _.filter(appCheckOkList, function (it) {
                    return !it.isOk;
                }).length
            }];
        });

        $http.get('/dms/node/list', {params: {clusterId: $scope.tmp.clusterId}}).success(function (data) {
            // for test sort and node pagination
            // if (data.length == 1) {
            //     var first = data[0];
            //     var second = _.clone(first);
            //     second.cpuUsedPercent = second.cpuUsedPercent - 1;
            //     second.memoryUsedPercent = second.memoryUsedPercent - 1;
            //     second.nodeIp = '0.0.0.0';

            //     var third = _.clone(first);
            //     third.cpuUsedPercent = second.cpuUsedPercent - 2;
            //     third.memoryUsedPercent = second.memoryUsedPercent - 2;
            //     third.nodeIp = '0.0.0.1';

            //     $scope.tmp.nodeAllList = [first, second, third];
            // } else {
            $scope.tmp.nodeAllList = data;
            // }
            $scope.changeNodeListInPage();

            $scope.nodeChartData = [{
                name: 'Agent OK', value: _.filter($scope.nodeList, function (it) {
                    return it.isOk;
                }).length
            }, {
                name: 'Agent Fail', value: _.filter($scope.nodeList, function (it) {
                    return !it.isOk;
                }).length
            }];

            $scope.nodeMemChartData = [{
                name: 'Free', value: Math.round(_.reduce($scope.nodeList, function (num, it) {
                    return it.memoryFreeMB + num;
                }, 0), 2)
            }, {
                name: 'Used', value: Math.round(_.reduce($scope.nodeList, function (num, it) {
                    return it.memoryUsedMB + num;
                }, 0), 2)
            }];

            $scope.nodeCpuChartData = [{
                name: 'Idle', value: Math.round(_.reduce($scope.nodeList, function (num, it) {
                    return it.cpuIdle * 100 + num;
                }, 0), 2)
            }, {
                name: 'Sys', value: Math.round(_.reduce($scope.nodeList, function (num, it) {
                    return it.cpuSys * 100 + num;
                }, 0), 2)
            }, {
                name: 'User', value: Math.round(_.reduce($scope.nodeList, function (num, it) {
                    return it.cpuUser * 100 + num;
                }, 0), 2)
            }];
        });
    };

    $scope.goAppOne = function (one) {
        Page.go('/page/cluster_container', {
            appId: one.key, appName: one.appName, appDes: one.appDes,
            clusterId: one.clusterId,
            namespaceId: one.namespaceId
        });
    };

    $scope.updateTags = function (id, ip, tags) {
        uiTips.prompt('Update tags for - ' + ip, function (val) {
            $http.get('/dms/node/tag/update', {params: {id: id, tags: val || ''}}).success(function (data) {
                $scope.onClusterChoose();
            });
        }, tags || '');
    };

    // chart test
    // $scope.appChartData = [{ name: 'app1', value: 40 }, { name: 'app2', value: 80 }];

    var isCpuUp = false;
    $scope.sortCpu = function () {
        $scope.nodeList = _.sortBy($scope.nodeList, function (it) {
            return isCpuUp ? -it.cpuUsedPercent : it.cpuUsedPercent;
        });
        if (!isCpuUp) {
            $scope.tmp.isSortUpCpu = true;
            $scope.tmp.isSortDownCpu = false;
            $scope.tmp.isSortUpMem = false;
            $scope.tmp.isSortDownMem = false;
            isCpuUp = true;
        } else {
            $scope.tmp.isSortUpCpu = false;
            $scope.tmp.isSortDownCpu = true;
            $scope.tmp.isSortUpMem = false;
            $scope.tmp.isSortDownMem = false;
            isCpuUp = false;
        }
    };

    var isMemUp = false;
    $scope.sortMem = function () {
        $scope.nodeList = _.sortBy($scope.nodeList, function (it) {
            return isMemUp ? -it.memoryUsedPercent : it.memoryUsedPercent;
        });
        if (!isMemUp) {
            $scope.tmp.isSortUpCpu = false;
            $scope.tmp.isSortDownCpu = false;
            $scope.tmp.isSortUpMem = true;
            $scope.tmp.isSortDownMem = false;
            isMemUp = true;
        } else {
            $scope.tmp.isSortUpCpu = false;
            $scope.tmp.isSortDownCpu = false;
            $scope.tmp.isSortUpMem = false;
            $scope.tmp.isSortDownMem = true;
            isMemUp = false;
        }
    };

    $scope.initSshConnect = function (one) {
        uiTips.prompt('Init SSH RSA key pair - ' + one.ip + ', input root password:', function (val) {
            if (!val.trim()) {
                uiTips.alert('Need password!');
                return;
            }
            $http.post('/dms/node/key-pair/init', {id: one.id, pass: val}).success(function (data) {
                if (data.flag) {
                    one.haveKeyPair = true;
                    uiTips.tips(data.message || 'Done init SSH RSA key pair - ' + one.ip);
                }
            });
        }, '');
    };

    $scope.goToDeployPage = function (one) {
        Page.go('/page/cluster_deploy', {ip: one.nodeIp});
    };

    var currentNodeIp;
    $scope.queryEventLl = function (pageNum) {
        var p = {
            pageNum: pageNum || 1,
            pageSize: 5,
            reason: $scope.tmp.reason
        };
        if (isQueryEventForCluster) {
            p.type = 'cluster';
        } else {
            p.type = 'node';
            p.nodeIp = currentNodeIp;
        }
        p.clusterId = $scope.tmp.clusterId;
        $http.get('/dms/event/list', {params: p}).success(function (data) {
            $scope.eventLl = data.list;
            $scope.eventPager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            Page.fixCenter('dialogNodeEvent');
        });
    };

    $scope.queryEventReasonLl = function (one) {
        currentNodeIp = one.nodeIp;
        $scope.tmp.eventTarget = 'Node Ip - ' + currentNodeIp;
        isQueryEventForCluster = false;
        $http.get('/dms/event/reason/list', {
            params: {
                type: 'node',
                nodeIp: currentNodeIp,
                clusterId: $scope.tmp.clusterId
            }
        }).success(function (data) {
            $scope.tmp.reasonList = data;
            $scope.queryEventLl();
        });
    };

    var isQueryEventForCluster = false;
    $scope.queryEventReasonLlForCluster = function () {
        isQueryEventForCluster = true;
        $scope.tmp.eventTarget = 'Cluster Id - ' + $scope.tmp.clusterId;
        $http.get('/dms/event/reason/list', {
            params: {
                type: 'cluster',
                clusterId: $scope.tmp.clusterId
            }
        }).success(function (data) {
            $scope.tmp.reasonList = data;
            $scope.queryEventLl();
        });
    };

    $scope.showStats = function (one) {
        $scope.tmp.targetNode = one;
        $scope.ctrl.isShowNodeStats = true;
    };

    var Page = window.Page;
    $scope.changeNodeStatsTab = function (index, triggerIndex) {
        if (index == 1) {
            $http.get('/dms/node/metric/queue', {
                params: {
                    clusterId: $scope.tmp.clusterId,
                    nodeIp: $scope.tmp.targetNode.nodeIp,
                    type: 'cpu'
                }
            }).success(function (data) {
                $scope.tmp.nodeCpuChartData = {data: data.list, xData: data.timelineList};
                Page.fixCenter('dialogNodeStats');
            });
        } else if (index == 2) {
            $http.get('/dms/node/metric/queue', {
                params: {
                    clusterId: $scope.tmp.clusterId,
                    nodeIp: $scope.tmp.targetNode.nodeIp,
                    type: 'mem'
                }
            }).success(function (data) {
                $scope.tmp.nodeMemChartData = {data: data.list, xData: data.timelineList};
                Page.fixCenter('dialogNodeStats');
            });

        } else {
            Page.fixCenter('dialogNodeStats');
        }
        return true;
    };
});
