// 配置
(function(global){
	var PortalConf = {};
	PortalConf.contextPath = '/';

	// datasource ajax or js
	PortalConf.dynamicDataSource = 'js';
	PortalConf.dynamicDataSourceUrl = PortalConf.contextPath + 'a/m/get-nav';
	PortalConf.dynamicDataSourceLinkUrl = PortalConf.contextPath + 'a/m/get-link';
//	PortalConf.locSaveUrl = PortalConf.contextPath + 'store/p/save-loc';
	PortalConf.getTemplateByTabIdUrl = PortalConf.contextPath + 'a/m/tab-js';

	PortalConf.requestParams = {};

	// portlets
	var portlet = {};
	portlet.id = 'portletIdTest1';
	portlet.title = 'Portlet1';
	portlet.loc = 'left';
	portlet.linkLl = [{title: 'xxx1', tabId: 'portlet11'},
		{title: 'xxx2', tabId: 'portlet12'},
		{title: 'xxx3', tabId: 'portlet13'}
	];

	var portlet2 = {};
	portlet2.id = 'portletIdTest2';
	portlet2.title = 'Portlet2';
	portlet2.loc = 'center';
	portlet2.linkLl = [{title: '系统功能升级了，介绍下',
		content: '系统升级了，请用新系统，系统升级了，请用新系统，系统升级了，请用新系统，系统升级了，请用新系统，系统升级了，请用新系统，请大家配合，谢谢！', 
		tabId: 'portlet21'}];

	var portlet3 = {};
	portlet2.id = 'portletIdTest3';
	portlet3.title = 'Portlet3';
	portlet3.loc = 'right';
	portlet3.linkLl = [{title: '一个统计', 'imgSrc': '/m/tpl/images/test/test_pie.jpg',
		tabId: 'tabIdTest3'}];

	var portletLl = [];
//	portletLl.push(portlet);
//	portletLl.push(portlet2);
//	portletLl.push(portlet3);

	PortalConf.portletLl = portletLl;

	var linkLl = [];
	var i = 0;
	for(; i < 49; i++){
		var linkItem = {link: '#', title: 'Link - ' + i};
		linkLl.push(linkItem);
	}

	PortalConf.linkLl = linkLl;

	// get nav item by index
	PortalConf.getNavByTabId = function(tabId, targetNavLl){
		if(!tabId)
			return null;

		targetNavLl = targetNavLl || this.navLl;
		if(!targetNavLl)
			return null;

		var i = 0;
		for(; i < targetNavLl.length; i++){
			var item = targetNavLl[i];
			if(tabId == item.tabId)
				return item;

			if(item.subLl){
				var targetSub = this.getNavByTabId(tabId, item.subLl);
				if(targetSub)
					return targetSub;
			}
		}

		return null;
	};

	// *** *** *** *** *** *** *** *** *** *** *** ***
	// *** *** *** *** *** *** *** *** *** *** *** ***
	// default style' image dir
	PortalConf.imgDir = 'images-blue';

	// portlets layout, different width
	PortalConf.colClzLl = ['w250', 'w500', 'w750', 'wnone'];
	PortalConf.layoutClz = {
		'1_3': 'w250 w750 wnone',
		'3_1': 'w750 w250 wnone',
		'1_2_1': 'w250 w500 w250',
		'1_1_2': 'w250 w250 w500',
		'2_1_1': 'w500 w250 w250',
		'1_1_1': 'w250 w250 w250'
	};

	PortalConf.layoutColumnTpl = '<div id="{0}" class="groupWrapper"></div>';
	PortalConf.itemTpl = '<div id="{0}" class="groupItem">' +
			'<div class="itemHeader"><h3>{1}</h3></div>' +
			'<div class="itemContent"></div>' +
		'</div>';

	PortalConf.itemActionTpl = '<div class="action">' +
		'<a href="javascript:void(0);" class="portletMin" title="Collapse" style="display: block; "></a>' +
		'<a href="javascript:void(0);" class="portletMax" title="Extend" style="display: none; "></a>' +
		'<a href="javascript:void(0);" class="portletClose" title="Close"></a>' +
		'</div>';

	// add portlet content generate funcation here
	PortalConf.contentGenerate = {};

	// portlet alink click event bind
	PortalConf.itemContentLinkCallback = function(_link){
		var title = _link.attr('title');
		var tabIdPortal = _link.attr('tabIdPortal');

		if(PortalTab){
			PortalTab.open(tabIdPortal, title);
		}

		return false;
	};

	PortalConf.itemContentLinkGenImg = function(_ul, it){
		var tplSrc = '<li><a class="portletLink" tabIdPortal="{1}" href="#" title="{0}">{0}</a>';
		var tplInnerSrc = '<br /><img src="{0}" class="portletImg" />';
		var tplSuf = '</li>';

		var tpl = tplSrc.format(it.title, it.tabId || Math.guid());
		var tplInner = tplInnerSrc.format(it.imgSrc);

		_ul.append(tpl + tplInner + tplSuf);
	};
	PortalConf.itemContentLinkGenContent = function(_ul, it){
		// paragraph
		var tplSrc = '<li><a class="portletLink" tabIdPortal="{1}" href="#" title="{0}">{0}</a>';
		var tplInner = '<br /><p class="portletContent">';
		var tplSuf = '</p></li>';

		var tpl = tplSrc.format(it.title, it.tabId || Math.guid());

		_ul.append(tpl + tplInner + it.content + tplSuf);
	};
	PortalConf.itemContentLinkGenLink = function(_ul, it){
		// default generate alink
		var tplSrc = '<li><a class="portletLink" tabIdPortal="{1}" title="{0}">{0}</a></li>';
		var tpl = tplSrc.format(it.title, it.tabId || Math.guid());
		_ul.append(tpl);
	};

	// portlet content generate function, return a jQuery object wrapped a ul element
	PortalConf.itemContentLinkOutputer = function(linkLl){
		var _ul = $('<ul></ul>');
		_.each(linkLl, function(it){
			// image
			if(it.imgSrc){
				PortalConf.itemContentLinkGenImg(_ul, it);
			}else if(it.content){
				PortalConf.itemContentLinkGenContent(_ul, it);
			}else if(it.callbackScript && it.callbackScriptFnName){
				// generate with a customized function, use $LAB to request a script file
				// synchronized
				$LAB.script(it.callbackScript).wait(function(){
					var fn = PortalConf.contentGenerate[it.callbackScriptFnName];
					if(fn){
						var tplLi = fn();
						_ul.append(tplLi);

						var fnAfterAppend = PortalConf.contentGenerate[it.callbackScriptFnName + 'AfterAppend'];
						if(fnAfterAppend){
							fnAfterAppend(_ul.find('li:last'));
						}
					}
				});
			}else{
				PortalConf.itemContentLinkGenLink(_ul, it);
			}
		});
		return _ul;
	};

	// called when delete a portlet item
	PortalConf.itemRemoveCallback = '';

	// to be rewrite
	// um登陆页面（session失效后会跳转的页面路径）
	PortalConf.loginUrl = '/m/index.html';
	// 用户登出url，用户点击登出，服务端做session失效的url
	PortalConf.logoutUrl = '/a/m/logout';
	// 获取登陆用户信息的url，以json形式返回，返回内容如：{"loginUser": "张三"}
	PortalConf.getLoginUserUrl = '/a/m/get-login-user';
	// 系统页面的title
	PortalConf.pageTitle = 'Admin Platform';

	PortalConf.noDesktop = false;
	PortalConf.portletColumnSettings = {left: {clz: 'w250'}, center: {clz: 'w500'}, right: {clz: 'w250'}};

	// consts
	PortalConf.DOM_ID_TAB_HEADER = 'tabHeader_';
	PortalConf.DOM_ID_TAB_CONTENT = 'tabContent_wrap_';

	global.PortalConf = PortalConf;

	// *** *** *** *** *** *** *** *** *** *** *** *** ***
	// *** *** *** *** *** *** *** *** *** *** *** *** ***
	global.PortalUtils = {
		getCookie: function(cookieName){
			var cookieStart = document.cookie.indexOf(cookieName);
			var cookieEnd = document.cookie.indexOf(";", cookieStart);
			return cookieStart == -1 ? '' : unescape(document.cookie.substring(cookieStart + cookieName.length + 1, (cookieEnd > cookieStart ? cookieEnd : document.cookie.length)));
		},

		setCookie: function(cookieName, cookieValue, seconds, path, domain, secure) {
			var expires = new Date();
			expires.setTime(expires.getTime() + seconds);
			document.cookie = escape(cookieName) + '=' + escape(cookieValue)
				+ (expires ? '; expires=' + expires.toGMTString() : '')
				+ (path ? '; path=' + path : '/')
				+ (domain ? '; domain=' + domain : '')
				+ (secure ? '; secure' : '');
		},

		// 添加url时间戳后缀，避免IE下iframe缓存
		addSuf: function(url, ext){
			ext = ext || {};

			var suf = '' + new Date().getTime() + '_' + Math.random();
			ext.sysdate = suf;

			var realUrl = url;
			for(key in ext){
				realUrl = this.addUrlParam(realUrl, key, ext[key]);
			}

			return realUrl;
		},

		addUrlParam: function(url, key, val){
			var arr = url.split(/\//);
			var pathLast = arr[arr.length - 1];
			if(pathLast.contains('?')){
				url += '&' + key + '=' + val;
			}else{
				url += '?' + key + '=' + val;
			}

			return url;
		}
	};
	

	// *** *** *** *** *** *** *** *** *** *** *** *** ***
	// *** *** *** *** *** *** *** *** *** *** *** *** ***
	global.PortalTab = {
		tab: null,
		layout: null,

		// show confirm when close
		isShowTip: true,
		tabIdLl: [],

		postDataCached: {},

		tabIdLoaded: [], 

		collapseNav: function(){
			if(!this.layout){
				return;
			}
			this.layout.omBorderLayout('collapseRegion', 'west');
		},

		openNav: function(){
			if(!this.layout){
				return;
			}
			this.layout.omBorderLayout('expandRegion', 'west');
		},

		activate: function(id){
			if(!this.tab){
				return;
			}			

			this.tab.activate(id);
		}, 

		open: function(id, title, option){
			// use om tab
			console.log('Open tab : ' + [id, title].join(','));
			if(!this.tab){
				return;
			}

			option = option || {};

			if(this.tabIdLl.contains(id)){
				this.tab.activate(id);
				if(option.reload){
					// reload compile
				}
			}else{
				var that = this;
				var renderContent = function(){
					$.get(PortalConf.getTemplateByTabIdUrl + '?type=tpl&id=' + id, function(content){
						that.tab.add({
							tabId: id,
							title: title,
							content: content,
							closable: option.closable, 
							img: option.img, 
							notCheckScroller: option.notCheckScroller
						});
						that.tabIdLl.push(id);	

						// angular compile
						var tabContent = $('#' + PortalConf.DOM_ID_TAB_CONTENT + id)[0];
						angular.bootstrap(tabContent, ['module_' + id]);
						
						$.dialog.unloading();
					});
				};

				$.dialog.loading();
				if(this.tabIdLoaded.contains(id)){
					renderContent();
				}else{
					$LAB.script(PortalConf.getTemplateByTabIdUrl + '?type=js&id=' + id).wait(function(){
						that.tabIdLoaded.push(id);
						renderContent();
					});
				}
			}
		},

		removeTabByTabId: function(tabId, donotShowTip){
			if(!this.tab){
				return false;
			}

			var index = this.tabIdLl.indexOf(tabId);
			if(index == -1)
				return;

			if(donotShowTip)
				this.isShowTip = false;

			this.tab.close(tabId);
			this.isShowTip = true;
		},

		removeTabIndex: function(n){
			if(!this.tab){
				return false;
			}

			var tabId = this.tab.getAlter(n);
			var index = this.tabIdLl.indexOf(tabId);
			if(index == -1)
				return false;


			if(!this.isShowTip || (this.isShowTip && confirm('确定要关闭此页面么？'))){
				this.tabIdLl.splice(index, 1);
				return true;
			}else{
				return false;
			}
		},

		removeCurrent: function(donotShowTip){
			console.log('Remove current tab...');
			if(!this.tab){
				return;
			}

			var tabId = this.tab.getActivated();
			if(!tabId)
				return;

			if(donotShowTip)
				this.isShowTip = false;

			this.tab.close(tabId);
			this.isShowTip = true;
		},

		removeAll: function(){
			// close except desktop
			while(this.tabIdLl.length > 0){
				var tabId = this.tabIdLl[0];
				this.removeTabByTabId(tabId, 'notip');
			}
		}, 

		// openByPost
		openByPost: function(id, url, title, option, data){
			if(!option)
				option = {};
			option.reload = true;

			// add pre
			if(url.startsWith('docc')){
				url = PortalConf.contextPath + url;
			}

			this.postDataCached['tab_url_' + id] = url;
			this.postDataCached['tab_data_' + id] = data;

			var blankUrl = PortalConf.contextPath + 'm/tpl/blank.html?tabId=' + id;
			this.open(id, blankUrl, title, option);
		}
	};

	// *** *** *** *** *** *** *** *** *** *** *** *** ***
	// *** *** *** *** *** *** *** *** *** *** *** *** ***
	if(!global.PortalAuth){
		global.PortalAuth = {};
		global.PortalAuth.login = function(data){
			global.top.document.location.href = PortalConf.loginUrl;
		};
	}
})(this);