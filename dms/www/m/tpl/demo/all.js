var md = angular.module('module_demoAll', ['ng.ui', 'ng.ext.uploadify', 
	'ng.ext.filter-grid', 
	'ng.ext.tree']);
md.controller('MyCtrl', function($scope, uiLog, uiTips, uiValid){
	$scope.handleTimeBegin = new Date().add(-6).format('yyyy-MM-dd 23:59:59');
	$scope.handleTimeEnd = new Date().format('yyyy-MM-dd 23:59');

	$scope.showAlert = function(){
		uiTips.alert('xxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
	};

	// dropdown
	var genList = function(num){
		return _.map(_.range(1, num), function(i){
			return {code: i, label: 'label' + i};
		});
	};
	var list = genList(100);
	$scope.list = list;

	$scope.dropdownTest = 10;
//		$scope.dropdownTest2 = 10;

	// dialog
	$scope.ctrl = {};
	$scope.ctrl.isDialogShow = false;
	$scope.ctrl.isDialogShowInner = false;
	var cc = 0;
	$scope.openDialog = function(){
		$scope.detail = {name: ''};

		$scope.dialogTitle = 'TITLE - ' + ++cc;
		$scope.ctrl.isDialogShow = true;
	};

	// tabs
	$scope.tab2val = 'bbb';
	$scope.beforeFn = function(index, triggerIndex){
		if(index == 0){
			uiTips.alert('0 index tab not available after change!');
			return false;
		}

		return true;
	};

	// context menu
	var menuList = [];
	menuList.push({id: 1, label: 'TTT', icon: 'images/plus.png'});
	menuList.push(null);
	menuList.push({id: 3, label: 'YYY', icon: 'images/minus.png'});

	$scope.menuList = menuList;
	$scope.menuClick = function(itemId){
		uiLog.w(itemId);
	};

	// filter grid
	// filter grid options
	var buts = [{label: '修改'}];
	$scope.columns = [{choose: true, multiple: true}, 
		{name: 'name', label: '姓名', isSortable: true, link: true}, 
		{name: 'age', label: '年龄', isSortable: true, bind: true, style: 'width: 50px;', valid: 'r int fn:checkAge'}, 
		{name: 'xxx', label: 'XXX', options: [{value: 1, title: 1}, {value: 2, title: 2}]}, 
			{buts: buts}];

	var genList4filter = function(num){
		return _.map(_.range(1, num), function(i){
			return {name: 'name' + i, age: i};
		});
	};
	var list4filter = genList4filter(20);
	list4filter[0].isChecked = true;
	list4filter[2].isChecked = true;
	list4filter[3].xxx = 2;

	$scope.list4filter = list4filter;

	$scope.gridClick = function(index, columnIndex, butIndex){
		var item = $scope.list4filter[index];
		uiTips.alert(JSON.stringify(item));

		uiLog.w(columnIndex);

		return function(tr){
			tr.find(':checkbox').trigger('click');
		};
	};

	$scope.checkAge = function(val, one){
		var maxAge = parseInt(one.name.substring('name'.length));
		return parseInt(val) > maxAge ? '年龄必须小于name的后缀数字！' : true;
	};

	$scope.next = function(){
		if(!uiValid.checkForm($scope.gridForm0) || 
			!$scope.gridForm0.$valid){
			uiLog.w('check failed!');
			return;
		}
		uiLog.w('check ok!');
	};

	// tree 
	$scope.columns4tree = [
		{name: 'name', label: '姓名', width: '40%'}, 
		{name: 'age', label: '年龄', width: '40%'}];

	var genList4tree = function(num){
		return _.map(_.range(1, num), function(i){
			return {label: 'node' + i, level: 1, name: 'name' + i, age: i};
		});
	};
	var list4tree = genList4tree(10);
	list4tree[0].isChecked = true;

	list4tree[2].isChecked = true;
	list4tree[1].level = 2;
	list4tree[2].level = 2;
	list4tree[3].level = 3;
	list4tree[4].level = 4;

	$scope.list4tree = list4tree;
	$scope.list4tree2 = _.clone(list4tree);

	$scope.treeClick = function(index){
		var item = $scope.list4tree[index];
		uiTips.alert(JSON.stringify(item));
	};

	// pagination
	// 分页信息
	// 总记录数
	var llAll = [];
	var i = 0;
	for(; i < 102; i++){
		llAll.push({name: '' + i});
	}

	$scope.changePage = function(pageNum){
		var pageSize = 10;
		var cc = llAll.length;
		var currentPage = pageNum || 1;

		var beginIndex = (currentPage - 1) * pageSize;
		var endIndex = currentPage * pageSize;

		if(beginIndex > cc - 1)
			beginIndex = 0;
		if(endIndex > cc)
			endIndex = cc;
		
		$scope.currentPageLl = llAll.slice(beginIndex, endIndex);
		$scope.queryPager = {pageNum: currentPage, pageSize: pageSize, totalCount: cc};
	};

	$scope.changePage();
});