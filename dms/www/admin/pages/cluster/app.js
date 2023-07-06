var md = angular.module('module_cluster/app', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid, uiLog) {
    $scope.ctrl = {};
    $scope.tmp = {pageNum: 1};
    $scope.editOne = {conf: {}};
    $scope.confOne = {};

    var LocalStore = window.LocalStore;
    var store = new LocalStore(true);
    var old = store.get('cluster_app_old');
    if (old) {
        $scope.tmp.pageNum = old.pageNum;
        $scope.tmp.keyword = old.keyword;
        $scope.tmp.clusterId = old.clusterId;
        $scope.tmp.namespaceId = old.namespaceId;
    }

    var Page = window.Page;
    var params = Page.params();
    if (params.clusterId) {
        $scope.tmp.clusterId = params.clusterId;
    }
    if (params.namespaceId) {
        $scope.tmp.namespaceId = params.namespaceId;
    }

    $scope.queryLl = function (pageNum) {
        var p = {
            pageNum: pageNum || $scope.tmp.pageNum,
            keyword: $scope.tmp.keyword,
            clusterId: $scope.tmp.clusterId || '',
            namespaceId: $scope.tmp.namespaceId || ''
        };
        store.set('cluster_app_old', p);

        $http.get('/dms/app/list', {params: p}).success(function (data) {
            $scope.ll = data.list;
            $scope.pager = {pageNum: data.pageNum, pageSize: data.pageSize, totalCount: data.totalCount};
            $scope.tmp.pageNum = data.pageNum;

            _.each(data.list, function (it) {
                if (!it.logConf) {
                    it.logConf = {logFileList: []};
                }
            });
        });
    };

    $scope.onClusterChoose = function () {
        var clusterId = $scope.tmp.clusterId || $scope.editOne.clusterId;
        if (!clusterId) {
            $scope.tmp.namespaceList = [];
            $scope.tmp.nodeIpList = [];
            $scope.tmp.nodeTagList = [];
            return;
        }
        $scope.tmp.namespaceList = _.filter($scope.tmp.namespaceAllList, function (one) {
            return one.clusterId == clusterId;
        });
        $scope.tmp.nodeIpList = _.map(_.filter($scope.tmp.nodeIpAllList, function (one) {
            return one.clusterId == clusterId;
        }), function (one) {
            one.ipWithCpuMemUsed = one.ip + ' / cpu used: ' + one.cpuUsedPercent + '%, mem used: ' + one.memUsedPercent + '%';
            return one;
        });
        $scope.tmp.nodeTagList = _.filter($scope.tmp.nodeTagAllList, function (one) {
            return one.clusterId == clusterId;
        });
        $scope.tmp.appOtherList = _.filter($scope.tmp.appOtherAllList, function (one) {
            return one.clusterId == clusterId;
        });
    };

    $scope.changeTab = function (index, triggerIndex) {
        var image;
        var conf = $scope.editOne.conf;
        if (conf && conf.group && conf.image) {
            image = conf.group + '/' + conf.image;
        }
        if (index == 2) {
            // onClusterChoose
            return true;
        } else if (index == 3) {
            if (!image) {
                uiTips.tips('need group/image input');
                return;
            }
            $http.get('/dms/app/option/image/env/list', {params: {image: image}}).success(function (data) {
                $scope.tmp.imageEnvList = data;
            });
            return true;
        } else if (index == 4) {
            var clusterId = $scope.editOne.clusterId;
            if (!clusterId) {
                uiTips.tips('need choose cluster');
                return;
            }
            $http.get('/dms/app/option/image/volume/list', {
                params: {
                    clusterId: clusterId,
                    image: image
                }
            }).success(function (data) {
                $scope.tmp.imageVolumeList = data;
            });
            return true;
        } else if (index == 5) {
            if (!image) {
                uiTips.tips('need group/image input');
                return;
            }
            $http.get('/dms/app/option/image/tpl/list', {params: {image: image}}).success(function (data) {
                $scope.tmp.imageTplList = data;
                Page.fixCenter('dialogApp');
            });
            return true;
        } else if (index == 6) {
            if (!image) {
                uiTips.tips('need group/image input');
                return;
            }
            $http.get('/dms/app/option/image/port/list', {params: {image: image}}).success(function (data) {
                $scope.tmp.imagePortList = data;
            });
            return true;
        } else {
            return true;
        }
    };

    $http.get('/dms/app/option/list', {params: {}}).success(function (data) {
        $scope.tmp.clusterList = data.clusterList;
        $scope.tmp.namespaceAllList = data.namespaceList;
        $scope.tmp.nodeTagAllList = data.nodeTagList;
        $scope.tmp.nodeIpAllList = data.nodeIpList;
        $scope.tmp.registryList = data.registryList;
        $scope.tmp.appOtherAllList = data.appOtherList;
        $scope.tmp.deployFileList = data.deployFileList;
        if (data.clusterList.length) {
            if (!$scope.tmp.clusterId) {
                $scope.tmp.clusterId = params.clusterId || data.clusterList[0].id;
            }
            $scope.onClusterChoose();
            if (!$scope.tmp.namespaceId) {
                $scope.tmp.namespaceId = params.namespaceId ||
                    ($scope.tmp.namespaceList.length ? $scope.tmp.namespaceList[0].id : '');
            }
            $scope.queryLl();
        }
    });

    $scope.edit = function (one) {
        if (!one.conf) {
            one.conf = {};
        }
        if (!one.conf.envList) {
            one.conf.envList = [];
        }
        if (!one.conf.uLimitList) {
            one.conf.uLimitList = [];
        }
        if (!one.conf.portList) {
            one.conf.portList = [];
        }
        if (!one.conf.dirVolumeList) {
            one.conf.dirVolumeList = [];
        }
        if (!one.conf.fileVolumeList) {
            one.conf.fileVolumeList = [];
        }
        if (!one.conf.targetNodeTagList) {
            one.conf.targetNodeTagList = [];
        }
        if (!one.conf.targetNodeIpList) {
            one.conf.targetNodeIpList = [];
        }
        if (!one.conf.excludeNodeTagList) {
            one.conf.excludeNodeTagList = [];
        }
        $scope.editOne = _.clone(one);
        $scope.tmp.isLatest = one.conf.tag == 'latest';
        $scope.tmp.isLibrary = one.conf.group == 'library';
        $scope.tmp.appOtherList = _.filter($scope.tmp.appOtherList, function (it) {
            return it.id != one.id;
        });
        $scope.ctrl.isShowAdd = true;
    };

    $scope.add = function () {
        var one = {clusterId: $scope.tmp.clusterId, namespaceId: $scope.tmp.namespaceId};
        one.conf = {memMB: '1024', cpuShares: '1024', cpuFixed: '0', containerNumber: '1'};
        one.conf.envList = [];
        one.conf.uLimitList = [];
        one.conf.portList = [];
        one.conf.dirVolumeList = [];
        one.conf.fileVolumeList = [];
        one.conf.targetNodeTagList = [];
        one.conf.targetNodeIpList = [];
        one.conf.excludeNodeTagList = [];
        $scope.editOne = one;
        $scope.ctrl.isShowAdd = true;
    };

    $scope.onChooseTpl = function (param) {
        var choosed = _.find($scope.tmp.imageTplList, function (one) {
            return one.id == param.imageTplId;
        });
        if (!choosed) {
            return;
        }
        param.dist = choosed.mountDist;
        param.content = choosed.content;
        param.isParentDirMount = choosed.isParentDirMount;

        var paramList = choosed.params.paramList;
        var savedList = param.paramList;
        if (!savedList && paramList) {
            savedList = _.map(paramList, function (one) {
                return {key: one.name, value: one.defaultValue, type: one.type};
            });
        }
        param.paramList = savedList;
    };

    $scope.onChooseVolume = function (param) {
        var choosed = _.find($scope.tmp.imageVolumeList, function (one) {
            return one.id == param.nodeVolumeId;
        });
        if (!choosed) {
            return;
        }
        param.dir = choosed.dir;
    };

    $scope.save = function () {
        if (!uiValid.checkForm($scope.tmp.addForm) || !$scope.tmp.addForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var one = _.clone($scope.editOne);
        delete one.isConfLiveCheck;
        delete one.isConfMonitor;
        delete one.isConfLog;
        delete one.isConfAb;
        delete one.isConfJob;
        delete one.isConfGateway;
        delete one.isLiveCheckOk;
        delete one.healthCheckResults;
        $http.post('/dms/app/update', one).success(function (data) {
            if (data.jobId) {
                $scope.ctrl.isShowAdd = false;
                Page.go('/page/cluster_container', {
                    appId: data.id, appName: one.name, appDes: one.des,
                    clusterId: one.clusterId,
                    namespaceId: one.namespaceId,
                    targetIndex: 2
                });
            }

            if (data.id) {
                $scope.ctrl.isShowAdd = false;
                $scope.queryLl();
            }
        });
    };

    $scope.delete = function (one) {
        uiTips.confirm('Sure Delete - ' + one.name + '?', function () {
            $http.delete('/dms/app/delete', {params: {id: one.id}}).success(function (data) {
                if (data.flag) {
                    var i = _.indexOf($scope.ll, one);
                    $scope.ll.splice(i, 1);
                }
            });
        }, null);
    };

    $scope.showHealthCheckResult = function (one) {
        // one.healthCheckResults -> [['checker name', true, timeMs]]
        var isOk = _.every(one.healthCheckResults, function (it) {
            return it[1];
        });
        $.dialog({
            title: one.name + ' / Is Ok: ' + isOk,
            content: _.map(one.healthCheckResults.reverse(), function (it) {
                return '<span class="' + (it[1] ? 'bg-success' : 'bg-danger') + '">' + it[0] + ': ' + it[1] + ' / ' +
                    new Date(it[2]).format('yyyy-MM-dd HH:mm:ss') + '</span>';
            }).join('<br />')
        });
    };

    $scope.confShow = function (one, tabIndex) {
        $scope.confOne = _.clone(one);
        $scope.tmp.confTabIndex = tabIndex;
        $scope.tmp.confTabIndexTarget = tabIndex;
        $scope.ctrl.isShowConf = true;

        // scale
        if (6 == tabIndex) {
            $scope.tmp.scaleNumber = one.conf.containerNumber;
        }
    };

    $scope.changeConfTab = function (index, triggerIndex) {
        $scope.tmp.confTabIndexTarget = index;
        if (index == 0 || index == 1 || index == 5) {
            $scope.tmp.confPortList = $scope.confOne.conf.portList;
        }

        if (index == 5) {
            $http.get('/dms/gw/cluster/list/simple').success(function (data) {
                $scope.tmp.gwClusterList = data;
            });
        }
        return true;
    };

    $scope.$watch('confOne.gatewayConf.clusterId', function (gwClusterId) {
        if (!gwClusterId) {
            return;
        }
        $http.get('/dms/gw/frontend/list/simple', {params: {clusterId: gwClusterId}}).success(function (data) {
            $scope.tmp.gwFrontendList = data;
        });
    });

    $scope.setManual = function (one) {
        $http.get('/dms/app/manual', {params: {id: one.id}}).success(function (data) {
            one.status = data.status;
        });
    };

    $scope.saveConf = function () {
        if (!uiValid.checkForm($scope.tmp.confForm) || !$scope.tmp.confForm.$valid) {
            uiTips.tips('Input Invalid');
            return;
        }

        var tabIndex = $scope.tmp.confTabIndexTarget;
        var confOne = $scope.confOne;
        var one = {id: confOne.id};

        // scale
        if (6 == tabIndex) {
            if ($scope.tmp.scaleNumber == confOne.conf.containerNumber) {
                uiTips.alert('instance number not change!');
                return;
            }

            one.scaleNumber = $scope.tmp.scaleNumber;
            $http.post('/dms/app/scale', one).success(function (data) {
                if (data.id) {
                    $scope.ctrl.isShowConf = false;
                    $scope.queryLl();
                }
            });
            return;
        }
        // ***

        if (0 == tabIndex) {
            one.liveCheckConf = confOne.liveCheckConf;
            if (confOne.conf.isRunningUnbox && confOne.liveCheckConf.isShellScript) {
                uiTips.alert('run as process not support shell live check!');
                return;
            }
        } else if (1 == tabIndex) {
            one.monitorConf = confOne.monitorConf;
        } else if (2 == tabIndex) {
            one.logConf = confOne.logConf;
        } else if (3 == tabIndex) {
            one.abConf = confOne.abConf;
        } else if (4 == tabIndex) {
            one.jobConf = confOne.jobConf;
        } else if (5 == tabIndex) {
            one.gatewayConf = confOne.gatewayConf;
        }

        $http.post('/dms/app/conf/update', one).success(function (data) {
            if (data.id) {
                $scope.ctrl.isShowConf = false;
                $scope.queryLl();
            } else {
                if (data.message) {
                    uiTips.alert(data.message);
                }
            }
        });
    };

    $scope.go = function (one) {
        Page.go('/page/cluster_container', {
            appId: one.id, appName: one.name, appDes: one.des,
            clusterId: one.clusterId,
            namespaceId: one.namespaceId
        });
    };
});
