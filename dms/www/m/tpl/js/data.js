(function(){
	PortalConf.navLl = [
	];

	PortalConf.themes = {
		df : {
			pageTitleColor: '#f60', //logo标题字体颜色
			pageTitleLogo: '/m/tpl/images/logo.png', //logo图片
			screenColor: '#000', //#fff
			color:'#e74c3c', //默认主题色
			hoverColor : '#fff', //白色
			cssPath : '/m/tpl/css/themes/' //主题风格css路径
		},
		blue : {
			pageTitleColor: '#f60',
			pageTitleLogo: '/m/tpl/images/logo.png',
			screenColor: '#000',
			color:'#168dd6',
			hoverColor : '#fff',
			cssPath : '/m/tpl/css/themes/'
		},
		green : {
			pageTitleColor: '#f60',
			pageTitleLogo: '/m/tpl/images/logo.png',
			screenColor: '#000',
			color:'#71b914',
			hoverColor : '#fff',
			cssPath : '/m/tpl/css/themes/'
		},
		gray :{
			pageTitleColor: '#f60',
			pageTitleLogo: '/m/tpl/images/logo.png',
			screenColor: '#000',
			color:'#e3e6e8',
			hoverColor : '#efefef', 
			cssPath : '/m/tpl/css/themes/'
		}
	};

})();

(function(){
	var md = angular.module('base', ['ng.ui']);
	md.factory('reqError', ['$q', 'uiTips', function($q, uiTips) {
    var reqError = {
			responseError: function(response){
				uiTips.unloading();
				return $q.reject(response);
			}
    };
		return reqError;
	}]);
	md.run(['uiValid', function(uiValid){
		uiValid.regPat('email', /^(\w)+(\.\w+)*@(\w)+((\.\w+)+)$/, '请录入有效的邮箱地址');
		uiValid.regPat('tel', /^\d{11}$/, '请录入11位手机号码');
		uiValid.regPat('mobileAndTel', /^(\(\d{3,4}\)|\d{3,4}-|\s)?\d{7,14}(-\d{1,8})?$/, '请录入11位手机号码或者电话号码如020-1234567');
	}]);
	md.config(['$httpProvider', function($httpProvider) {
			$httpProvider.interceptors.push('reqError');
	}]);
	md.value('urls', window.urls);
})();