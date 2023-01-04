// localstorage
(function(global){
	var TIME_SUF = '__SAVED_TIME';

	var Store = function(isSession, refreshInterval){
		this.storage = isSession ? window.sessionStorage : window.localStorage;
		// default 1 hour
		this.refreshInterval = refreshInterval || 1000 * 60 * 60;
		if(!this.storage)
			console.log('Web local storage api not support!');
	};

	var extend = {
		clear: function(){
			if(!this.storage)
				return;

			this.storage.clear();
		},

		get: function(key){
			if(!this.storage)
				return;

			var str = this.storage[key];
			if(!str || 'undefined' == str || 'null' == str)
				return null;

			// check if expired
			var timeSaved = this.storage[key + TIME_SUF];
			if(!timeSaved)
				return null;

			if((new Date().getTime() - parseInt(timeSaved)) > this.refreshInterval){
				this.storage.removeItem(key);
				this.storage.removeItem(key + TIME_SUF);
				return null;
			}

			return JSON.parse(str);
		},

		set: function(key, val){
			if(!this.storage)
				return;

			this.storage[key] = JSON.stringify(val);
			this.storage[key + TIME_SUF] = '' + new Date().getTime();
		},

		remove: function(key){
			if(!this.storage)
				return;

			this.storage.removeItem(key);
			this.storage.removeItem(key + TIME_SUF);
		}
	};

	for(key in extend){
		Store.prototype[key] = extend[key];
	}

	global.Store = Store;

	// *** *** *** *** *** *** *** *** *** *** ***
	// *** *** *** *** *** *** *** *** *** *** ***
	var PortalSidebar = {};
	// generate from local storage
	(function(){
		var localStorageRefreshInterval = 1000 * 60 * 60 * 24 * 30;
		PortalSidebar.storage = new Store(null, localStorageRefreshInterval);
	})();
	PortalSidebar.SIDEBAR_SHORTCUTS_KEY = 'portal_ace_sidebar_shortcuts';
	PortalSidebar.tplShortcutsBtn = '<button class="btn {3}" title="{0}" ' +
		'data-tabid="{1}" ' +
		'><i class="{2}"></i></button>';
	PortalSidebar.tplShortcutsSpan = '<span class="btn {0}"></span>';

	PortalSidebar.resetShortcutsHeight = function(arr){
		if(arr && !arr.length)
			$('#sidebar-shortcuts').hide();
		else
			$('#sidebar-shortcuts').show();
	};

	PortalSidebar.getShortcutsCss = function(){
		var arr = this.storage.get(this.SIDEBAR_SHORTCUTS_KEY) || PortalConf.sidebarShortcutsButs,
			btnArr = ['green','yellow','cyan','purple'];
		if(arr){
			var showArr = [],
				index,
				tplCss = '', i, j;
			if(arr.length >= 4){
				return arr[0].css;
			}
			for(i=0; i<arr.length; i++){
				tplCss = arr[i].css;
				showArr.push(tplCss);

			}
			for(i=0; i<showArr.length; i++){
				index = $.inArray(showArr[i], btnArr);
				if(index>=0){
					btnArr.splice(index,1);
				}
			}
			return btnArr[0];
		}else{
			return btnArr[0];
		}
	}

	PortalSidebar.generateShortcuts = function(){
		var arr = this.storage.get(this.SIDEBAR_SHORTCUTS_KEY) || PortalConf.sidebarShortcutsButs;
		if(arr){
			var tplShortcutsBig = _.map(arr, function(it){
				return PortalSidebar.tplShortcutsBtn.format(it.title, it.tabId, it.img || 'icon-link', it.css);
			}).join('');
			var tplShortcutsMini = _.map(arr, function(it){
				return PortalSidebar.tplShortcutsSpan.format(it.css);
			}).join('');

			$('#sidebar-shortcuts-large').html(tplShortcutsBig);
			$('#sidebar-shortcuts-mini').html(tplShortcutsMini);

		}

		this.resetShortcutsHeight(arr);
	};

	PortalSidebar.addShortcuts = function(item){
		// no use
		return;

		var arr = this.storage.get(this.SIDEBAR_SHORTCUTS_KEY) || PortalConf.sidebarShortcutsButs;
		if(!arr)
			arr = [];

		// check if exists already
		var existsItem = _.findWhere(arr, {tabId: item.tabId});
		if(existsItem){
			$('#sidebar-shortcuts-large').find('[data-tabid="' + item.tabId + '"]').focus();
			return;
		}

		// remove first
		if(arr.length >= 4){
			arr.splice(0, 1);
			PortalSidebar.generateShortcuts();
		}

		item.css = this.getShortcutsCss();

		arr.push(item);
		this.storage.set(this.SIDEBAR_SHORTCUTS_KEY, arr);

		var miniWrapper = $('#sidebar-shortcuts-mini');
		if(miniWrapper.find('span').length < 4){

			miniWrapper.append(PortalSidebar.tplShortcutsSpan.format(item.css));
		}

		var largeWrapper = $('#sidebar-shortcuts-large');
		if(largeWrapper.find('button').length < 4){
			var shortcutsBtn = PortalSidebar.tplShortcutsBtn.format(item.title, item.tabId, item.url, item.img || 'icon-link', item.css);
			$(shortcutsBtn).hide().appendTo(largeWrapper).fadeIn('normal');
		}

		this.resetShortcutsHeight(arr);
	};

	PortalSidebar.removeShortcuts = function(tabId){
		var targetShortcuts = $('#sidebar-shortcuts-large').find('button[data-tabid="' + tabId + '"]');
		if(!targetShortcuts.length)
			return;
		targetShortcuts.fadeOut('normal', function(){
			$(this).remove();
		});
		$('#sidebar-shortcuts-mini').find('span:last').remove();


		var arr = this.storage.get(this.SIDEBAR_SHORTCUTS_KEY) || PortalConf.sidebarShortcutsButs;
		if(!arr)
			return;

		var targetIndex = -1;
		var i = 0;
		for(; i < arr.length; i++){
			if(tabId === arr[i].tabId){
				targetIndex = i;
				break;
			}
		}
		if(targetIndex != -1)
			arr.splice(targetIndex, 1);

		this.storage.set(this.SIDEBAR_SHORTCUTS_KEY, arr);

		this.resetShortcutsHeight(arr);
	};
	global.PortalSidebar = PortalSidebar;
})(this);

(function(global){
	// title
	document.title = PortalConf.pageTitle || '首页';
	var pageLogo = '<img src="/m/tpl/images/logo.png" width="256" height="24">&nbsp;';
	$('#page-title').html(pageLogo + PortalConf.pageTitle);
	$('#page-title').attr('title', document.title);
	$('#page-title').css('color', '#f60');
	// logout url
	$('#link-logout').attr('href', PortalConf.logoutUrl);

	// get login user
	if(PortalConf.getLoginUserUrl){
		// loading
		$.dialog.loading();
		$.ajax({url: PortalConf.getLoginUserUrl, type: 'GET',
			dataType: 'JSON',
			success: function(r){
				$('#link-user').text(r.loginUser);
				$('#login-last-time').text(r.loginLastTime);
        
				if(PortalConf.getLoginUserHandler){
					PortalConf.getLoginUserHandler(r);
				}
				$.dialog.unloading();
			}, error: function(e){
				if(PortalConf.getLoginUserErrorHandler){
					PortalConf.getLoginUserErrorHandler(e);
				}

				$.dialog.unloading();
			}
		});
	}

	// events navbar-right
	$('.tech11-nav').find('.dropdown-toggle').click(function(e){
		e.preventDefault();
		e.stopPropagation();

		$(this).siblings('ul').toggle();
	});

	$(document).click(function(e){
		$('.dropdown-menu').filter(':visible').hide();
	});

	// shortcuts
	/*
	PortalSidebar.generateShortcuts();

	$('#sidebar-shortcuts-large').delegate('button', 'click', function(e){
		var btn = $(e.target);
		if(!btn.is('button'))
			btn = btn.closest('button');

		var title = btn.attr('title');
		var tabId = btn.attr('data-tabid');
		var img = btn.find('i').attr('class');

		PortalTab.open(tabId, title, {img: img});
	});

	// context menu
	var menu = $('#sidebar-context-menu');
	menu.bind('contextmenu', function(){return false;});
	menu.find('a').click(function(e){
		menuBg.hide();
		menu.hide();

		var fn = $(this).attr('data-fn');
		if('remove' == fn && PortalSidebar.clickedRemovedTabId){
			PortalSidebar.removeShortcuts(PortalSidebar.clickedRemovedTabId);
		}else if('add' == fn && PortalSidebar.clickedLink){
			PortalSidebar.addShortcuts(PortalSidebar.clickedLink);
		}

		return false;
	});
	var menuBg = $('#sidebar-context-menu-bg');
	menuBg.bind('contextmenu click', function(){
		menuBg.hide();
		menu.hide();
		return false;
	});

	$('#sidebar').bind('contextmenu', function(e){
		var link = $(e.target);

		var needShow = true;

		// add shortcuts
		if(link.closest('.nav-list').length){
			if(!link.is('a'))
				link = link.closest('a');

			var tabId = link.attr('data-tab-id');
			var navItem = PortalConf.getNavByTabId(tabId);
			if(!navItem){
				needShow = false;
			}else{
				PortalSidebar.clickedRemovedTabId = null;

				PortalSidebar.clickedLink = {tabId: navItem.tabId,
					title: navItem.label,
					url: navItem.link,
					img: navItem.img};
				menu.find('a[data-fn="remove"]').hide();
				menu.find('a[data-fn="add"]').show();
			}
		}else{
			if(!link.is('button'))
				link = link.closest('button');

			PortalSidebar.clickedRemovedTabId = link.attr('data-tabid');

			// remove shortcuts
			PortalSidebar.clickedLink = null;
			menu.find('a[data-fn="add"]').hide();
			menu.find('a[data-fn="remove"]').show();
		}

		if(!needShow)
			return false;

		var left = e.pageX + 5;
		var top = e.pageY;
		if (top + menu.height() >= $(window).height()){
			top -= menu.height();
		}
		if (left + menu.width() >= $(window).width()){
			left -= menu.width();
		}

		menu.css({left: left, top: top}).show();
		menuBg.show();
		return false;
	});
	*/

	// ***********************************************
	var PortalTabInner = function(opts){
		this.currentTabId = null;
		this.tabIdLl = [];

		this.opts = opts || {};

		window.setTimeout(function(){

			window.portalTabInner.checkScroller();

		}, 100);

	};
	PortalTabInner.prototype.getActivated = function(){
		return this.currentTabId;
	};
	PortalTabInner.prototype.activate = function(tabId){
		if(!tabId){
			this.currentTabId = null;
			return;
		}

		$('#crumb-tabs').find('li.active').removeClass('active');
		$('#wrap-tabs').find('div.active').removeClass('active');

		var tabHeader = $('#' + PortalConf.DOM_ID_TAB_HEADER + tabId);
		var tabContent = $('#' + PortalConf.DOM_ID_TAB_CONTENT + tabId);
		tabHeader.addClass('active');
		tabContent.addClass('active');

		this.currentTabId = tabId;

	};
	PortalTabInner.prototype.getAlter = function(n){
		if(typeof n == 'number')
			return this.tabIdLl[n];
		else
			return this.tabIdLl.indexOf(n);
	};
	PortalTabInner.prototype.close = function(tabId){
		var index = this.getAlter(tabId);

		var flag = true;
		if(this.opts.onBeforeClose)
			flag = this.opts.onBeforeClose(index);

		if(!flag)
			return;

		var tabHeader = $('#' + PortalConf.DOM_ID_TAB_HEADER + tabId);
		var tabContent = $('#' + PortalConf.DOM_ID_TAB_CONTENT + tabId);
		// angular scope destroy
		tabContent.remove();
		tabHeader.remove();

		this.tabIdLl.splice(index, 1);

		//active pre or next
		if(this.tabIdLl.length){
			if(tabId == this.getActivated())
				this.activate(this.tabIdLl[index - 1] || this.tabIdLl[index]);
		}else{
			// show desktop
			this.activate('desktop');
		}

		this.checkScroller('close');

		// add font css again ...
	};

	/*
		{
			tabId: tabId,
			title: title,
			content: content,
			closable: option.closable !== false
		}
	*/
	PortalTabInner.prototype.add = function(options){
		var tabId = options.tabId;

		var tabHeaderTpl = '<li title="' + options.title + '" class="tab-header" id="' + PortalConf.DOM_ID_TAB_HEADER + '{0}">';
		if(options.img)
			tabHeaderTpl += '<i class="' + options.img + '"></i>';
		tabHeaderTpl += options.title;
		if(options.closable !== false)
			tabHeaderTpl += '<i class="ui_close"></i>';
		tabHeaderTpl += '</li>';
		tabHeaderTpl = tabHeaderTpl.format(tabId);

		var tabWrapTpl = '<div class="tab-content"' +
			'id="' + PortalConf.DOM_ID_TAB_CONTENT + '{0}">' + options.content + '</div>';
		tabWrapTpl = tabWrapTpl.format(tabId);

		$('#crumb-tabs').append(tabHeaderTpl);
		$('#wrap-tabs').append(tabWrapTpl);

		// 设置tabli的默认宽度
		$('#crumb-tabs>li').each(function(){
			if( $(this).outerWidth(true) < 100 ) {
				$(this).width('90px');
			}
		});

		this.activate(tabId);
		this.tabIdLl.push(tabId);

		if(!options.notCheckScroller)
			this.checkScroller('add');
	};

	// tabs scroller 检测
	PortalTabInner.prototype.checkScroller = function(state){
		var $header = $('#breadcrumbs'),
			$ul = $('#crumb-tabs'),
			totalWidth = 4;

		$ul.children().each(function() {
			//计算一个li占用的总宽度
			var liPaddingL = parseInt( $(this).css('padding-left') ), //左内边距
				liPaddingR = parseInt( $(this).css('padding-right') ); //右内边距
			totalWidth += ( $(this).outerWidth(true) + liPaddingL + liPaddingR );
		});

		if(totalWidth > $ul.parent().outerWidth(true)) {

			if($('.om-tabs-scroll-left').length==0){
				$('<span class="om-tabs-scroll-left icon-caret-left"></span>').insertBefore($ul);
					$('<span class="om-tabs-scroll-right icon-caret-right"></span>').insertAfter($ul);
					this.scroll();
			}

			if(state){
				var	ulLeft  = parseInt($ul.css('left')),
					$li = $ul.children(':last'),
					rBorder = $ul.next().offset().left,
					ulRight = $li.offset().left + $li.outerWidth(true);
				if( ulRight - rBorder >0 ){
					$ul.animate({
							left : ulLeft - (ulRight-rBorder) + 'px'
						});
				}
			}

		}else{
			if($('.om-tabs-scroll-left').length && $('.om-tabs-scroll-right').length){
				$('.om-tabs-scroll-left').remove();
				$('.om-tabs-scroll-right').remove();
				$ul.css('left', 0);
			}
		}
	};

	PortalTabInner.prototype.scroll = function() {

        var $header = $('#breadcrumbs'),
        	$ul = $('#crumb-tabs'),
        	btnWidth = $ul.prev().outerWidth(true),
        	lBorder = btnWidth-1,
        	distance = $ul.children(':last').outerWidth(true);

        $ul.prev().bind('click', function(e){

        	var queuedFn = function(next) {
        		var	ulLeft  = parseInt($ul.css('left'));

        		$ul.stop(true, true);
                $header.clearQueue();

	        	if(ulLeft >= lBorder){
	        		return;
	        	}

	        	if(ulLeft < lBorder && ulLeft >= -distance){
	        		$ul.animate({
			            left : lBorder + 'px'
			        },function(){
			        	next();
			        });
	        	}else{
	        		$ul.animate({
			            left : ulLeft + distance + 'px'
			        },function(){
			        	next();
			        });
	        	}
        	}

        	$header.queue(queuedFn);
	        if( $header.queue().length == 1){
	            $header.dequeue(); //start queue
	        }

	    });

	    $ul.next().bind('click', function(e) {

	    	var queuedFn = function(next) {
        		var	ulLeft  = parseInt($ul.css('left')),
        		$li = $ul.children(':last'),
	    		rBorder = $ul.next().offset().left,
	    		ulRight = $li.offset().left + $li.outerWidth(true);

                $ul.stop(true, true);
                $header.clearQueue();

                if(ulRight <= rBorder){
	        		return;
	        	}

	        	if((ulRight-rBorder) < distance){
	        		$ul.animate({
			            left : ulLeft - (ulRight-rBorder) + 'px'
			        },function(){
			        	next();
			        });
	        	}else{
	        		$ul.animate({
			            left : ulLeft - distance + 'px'
			        },function(){
			        	next();
			        });
	        	}

        	}

        	$header.queue(queuedFn);
	        if( $header.queue().length == 1){
	            $header.dequeue(); //start queue
	        }

	    });
	};

	var PortalLayoutInner = function(){};
	PortalLayoutInner.prototype.omBorderLayout = function(command, options){
		return this[command].call(this, options);
	};
	PortalLayoutInner.prototype.collapseRegion = function(region){
		var icon = $('#sidebar-collapse').find('i');
		if(icon.is('.icon-double-angle-left')){
			$('#sidebar-collapse').trigger('click');
		}
	};
	PortalLayoutInner.prototype.expandRegion = function(region){
		var icon = $('#sidebar-collapse').find('i');
		if(icon.is('.icon-double-angle-right')){
			$('#sidebar-collapse').trigger('click');
		}
	};

	global.PortalStyle = {

		currentStyle: 'df',

		linkId : 'themes',

		changeStyle: function(style){

			if(this.currentStyle == style)
				return;
			this.changeColor(style);
			this.changeCss(style);
			this.currentStyle = style;
			this.storage.set(this.CURRENTSTYLE_KEY, style);
		},

		// style change
		changeColor: function(style){

			var themes = PortalConf.themes[style]; //皮肤主题对象
			$('#page-title').find('img').attr('src', themes.pageTitleLogo);
			$('#page-title').css('color', themes.pageTitleColor);
			$('.navbar-header ul .screen').css('color', themes.screenColor);

			if($('.groupItem').length)
				$('.groupItem').css('border-top-color',  themes.color);

			if(style == 'gray')
				$('#breadcrumbs').append('<div class="bd1"></div>');
		},

		changeCss: function(style){
			var themes = PortalConf.themes[style]; //皮肤主题对象
			$('#'+this.linkId).attr('href', themes.cssPath + style + '/default.css');
		},

		Fkey : function(){
			var el = document.documentElement;
			var rfs = el.requestFullScreen || el.webkitRequestFullScreen || el.mozRequestFullScreen || el.msRequestFullScreen;
			if(rfs){
				rfs.call(el);
			}else{

				try{

					var WsShell = new ActiveXObject('WScript.Shell');
					WsShell.SendKeys('{F11}');

				}catch(e){

					alert('Internet选项 -> 安全 -> 自定义安全级别 将“不允许运行未标记未安全的active控件”禁用改为启用');

				}

			}
		}

	};

	//主题颜色保存一个月
	(function(){
		var localStorageRefreshInterval = 1000 * 60 * 60 * 24 * 30;
		PortalStyle.storage = new Store(null, localStorageRefreshInterval);
	})();

	PortalStyle.CURRENTSTYLE_KEY = 'Portal_currentStyle_key';
	PortalStyle.getHistoryStyle = function(){
		var style = this.storage.get(this.CURRENTSTYLE_KEY) || (PortalConf.defaultThemes ? PortalConf.defaultThemes : 'df');
		this.currentStyle = style;
		this.changeColor(style);
		this.changeCss(style);
		this.setHistoryStyle(style);
	};

	PortalStyle.setHistoryStyle = function(style){
		if(!style)
			return;
		this.storage.set(this.CURRENTSTYLE_KEY, style);
	};

	global.portalTabInner = new PortalTabInner();
	global.portalLayoutInner = new PortalLayoutInner();
})(this);

$(function(){

	// sidebar
	$('#sidebar-collapse').click(function(e){
		e.preventDefault();
		e.stopPropagation();

		var icon = $(this).find('i');
		if(icon.is('.icon-double-angle-left')){
			icon.removeClass('icon-double-angle-left').addClass('icon-double-angle-right');
			$('#sidebar').addClass('menu-min');

			portalTabInner.checkScroller();
		}else{
			icon.removeClass('icon-double-angle-right').addClass('icon-double-angle-left');
			$('#sidebar').removeClass('menu-min');

			portalTabInner.checkScroller();
		}
	});

	//move home tab margin-right -5px
	$('#tabHeader_desktop').css('margin-right', '-5px');

	// sidebar show/hide when in small screen device
	$('#menu-toggler').click(function(e){
		e.preventDefault();
		e.stopPropagation();

		$('#sidebar').toggle();

	});
	//show sidebar when container outerwidth > 991
	$(window).resize(function(){
		if($('#main-container').outerWidth(true) > 991){
			$('#sidebar').show();
		}
	});

	// sidebar link event
	var bindNavEvent = function(){
		// sidebar nav sub list show/hide
		$('.nav-list').delegate('a').click(function(e){
			e.preventDefault();
			e.stopPropagation();
			var link = $(e.target);
			if(!link.is('a'))
				link = link.closest('a');

			if(link.is('.dropdown-toggle')){
				var menuList = link.next('ul');
				if(menuList.is(':hidden')){
					link.children('b.arrow').removeClass('icon-angle-down').addClass('icon-angle-up'); //-
					menuList.stop().slideDown();

					var menuSub1;
					if(!menuList.is('.sub1'))
						menuSub1 = menuList.closest('.sub1');

					if(PortalConf.sidebarSingleCollapse === true){
						$('.dropdown-toggle ~ ul.sub1').not(menuList).not(menuSub1).each(function(){
							$(this).stop().slideUp();
							$(this).prev('.dropdown-toggle').children('b.arrow').removeClass('icon-angle-up').addClass('icon-angle-down');
						});
					}
				}else{
					link.children('b.arrow').removeClass('icon-angle-up').addClass('icon-angle-down'); //-
					menuList.stop().slideUp();
				}
			}else{
				// change active
				$('.nav-list').find('li.active').removeClass('active');
				link.parentsUntil('.nav-list').addClass('active');

				var tabId = link.attr('data-tab-id');
				var navItem = PortalConf.getNavByTabId(tabId);
				if(navItem){
					PortalTab.open(navItem.tabId, navItem.label, navItem);
				}
			}
		});

	};

	var getTabId = function(li){
		var tabHeaderId = li.attr('id');
		if(!tabHeaderId || !tabHeaderId.startsWith(PortalConf.DOM_ID_TAB_HEADER))
			return null;

		return tabHeaderId.substring(PortalConf.DOM_ID_TAB_HEADER.length);
	};

	// tabbar event
	$('body').on('click', '.tab-header', function(e){
		var li = $(this);
		var tabId = getTabId(li);

		if(tabId){
			PortalTab.activate(tabId);
		}
	});

	$('body').on('click', '.ui_close', function(e){
		e.preventDefault();
		e.stopPropagation();

		var li = $(this).closest('li');
		var tabId = getTabId(li);

		if(!tabId){
			li.remove();
		}else{
			PortalTab.removeTabByTabId(tabId);
		}
	});

	//change themes
	$('.navbar-header ul').children(':first').find('span').not('.screen').click(function(){
		var spanClass = $(this).attr('class');
		PortalStyle.changeStyle(spanClass);
	});

	//fullScreen
	$('.navbar-header ul .screen').bind('click', function(){
		PortalStyle.Fkey();
	});

	// navigation, use template to generate html
	var genNav = function(ll){
		if(!ll || !ll.length){
			return '';
		}

		var tpl = Consts.format($('#tplNav').html(), {ll: ll});
		$('#nav-list').html(tpl);
		bindNavEvent();

		// //修复左侧点击sidebar第一个li下沉问题
		// $('.nav-list').find('li:first').css('height','39px');
		$('.nav-list').find('.menu-text:first').css('top','-1px');
		$('.nav-list').find('.menu-text:first').css('height','41px');

	};

	PortalTab.initByData = function(data){
		// navigation
		if(data.navLl){
			genNav(data.navLl);
		}
		// \/ navLl

		if(data.portletLl){
			Portal.init(PortalConf.DOM_ID_TAB_CONTENT + 'desktop', data.portletLl,
				PortalConf.portletColumnSettings);
		}
		// \/ portletLl

		if(data.tabLl){
			var i = 0, len = data.tabLl.length;
			for(; i < len; i++){
				var one = data.tabLl[i];
				if(!one.option)
					one.option = {};

				one.option.img = one.img;
				if(i !== len - 1)
					one.option.notCheckScroller = true;

				PortalTab.open(one.tabId, one.title, one.option, true);
			}
		}
		// \/ tabLl

		PortalStyle.getHistoryStyle();
	};

	var getParams = function(url){
		url = url || document.location.href;
		var data = {};
		var pos = url.indexOf('?');
		if(pos == -1)
				return data;

		var query = url.substring(pos);
		if(query){
			var ll = query.substring(1).split('&');
			var i = 0;
			for(; i < ll.length; i++){
				var arr = ll[i].split('=');
				if(arr.length == 2)
					data[arr[0]] = arr[1];
			}
		}
		return data;
	};

	PortalTab.initByAjax = function(){
		var url = PortalConf.dynamicDataSourceUrl;
		var params = PortalConf.requestParams || {};
		$.extend(params, getParams());

		var loadingMsg = PortalConf.getNavDataLoadingMsg || '加载中...';
		$.dialog.loading(loadingMsg);

		$.ajax({url: url, data: params, type: 'GET',
			dataType: 'JSON',
			success: function(r){
				// 添加用户自定义过滤方法
				if(PortalConf.getNavDataFilter){
					var flag = PortalConf.getNavDataFilter(r);
					if(flag)
						PortalTab.initByData(r);
				}else{
					PortalTab.initByData(r);
				}

				$.dialog.unloading();
			}, error: function(xhr){
				if(PortalConf.getUserNavErrorHandler){
					PortalConf.getUserNavErrorHandler(xhr);
				}

				$.dialog.unloading();
			}
		});
	};

	// *** *** *** ***
	// tabs
	PortalTab.tab = window.portalTabInner;
	PortalTab.layout = window.portalLayoutInner;
	PortalTab.tab.opts.onBeforeClose = function(n){
		return PortalTab.removeTabIndex(n);
	};

	// initialize sidebar tabbar portlets
	if('js' == PortalConf.dynamicDataSource){
		PortalTab.initByData(PortalConf);
	}else{
		PortalTab.initByAjax();
	}
});

(function(global){
	var defaultOptions = {
		scrollStep: 100,
		needScrollHeight: 112,
		itemHeight: 28
	};

	var NavScroller = function(el, height, opts){
		this.el = el;
		this.height = height;
		this.opts = $.extend(defaultOptions, opts);

		this.init();
	};

	var fn = NavScroller.prototype;
	fn.init = function(){
		var wrap = $('<div></div>').height(this.height).css({'position': 'relative', 'overflow': 'hidden'});
		this.el.wrap(wrap);

		var btnTop = $('<span class="nav-list-top"></span>');
		var btnBottom = $('<span class="nav-list-bottom"></span>');
		this.el.after(btnTop).after(btnBottom).height('100%');

		var that = this;
		btnTop.click(function(e){
			that.scroll('up');
		});
		btnBottom.click(function(e){
			var lastLi = $("#nav-list>li:visible:last");
			if(lastLi[0].offsetTop > 50){
				that.scroll('down');
			}
		});

		that.el.find('a').click(function(){
			var maxHeight = $(window).height() - that.opts.needScrollHeight;
			if(that.el.height() > maxHeight){
				btnTop.show();
				btnBottom.show();
				wrap.height(maxHeight);
			}else{
				btnTop.show();
				btnBottom.show();
				wrap.height('auto');
			}
		});

		if(this.height < this.el.height()){
			btnTop.add(btnBottom).show();
		}
	};

	fn.scroll = function(direction){
		var marginTopMove = this.opts.scrollStep;
		if('down' === direction)
			marginTopMove = -marginTopMove;

		var top = +(this.el.css('margin-top').replace('px', ''));
		top += marginTopMove;

		var maxTop = -this.el.height() + this.opts.itemHeight;

		if(top > this.opts.itemHeight){
			top = this.opts.itemHeight;
		} else if(top < maxTop){
			top = maxTop;
		}

		this.el.stop().animate({
			'margin-top': top
		});
	};

	jQuery.fn.extend({
		mousewheel: function(up, down, preventDefault) {
			return this.hover(function() {
				jQuery.event.mousewheel.giveFocus(this, up, down, preventDefault);
			},
			function() {
				jQuery.event.mousewheel.removeFocus(this);
			});
		},
		mousewheeldown: function(fn, preventDefault) {
			return this.mousewheel(function() {},
			fn, preventDefault);
		},
		mousewheelup: function(fn, preventDefault) {
			return this.mousewheel(fn,
			function() {},
			preventDefault);
		},
		unmousewheel: function() {
			return this.each(function() {
				jQuery(this).unmouseover().unmouseout();
				jQuery.event.mousewheel.removeFocus(this);
			});
		},
		unmousewheeldown: jQuery.fn.unmousewheel,
		unmousewheelup: jQuery.fn.unmousewheel
	});

	jQuery.event.mousewheel = {
		giveFocus: function(el, up, down, preventDefault) {
			if (el._handleMousewheel) jQuery(el).unmousewheel();

			if (preventDefault == window.undefined && down && down.constructor != Function) {
				preventDefault = down;
				down = null;
			}

			el._handleMousewheel = function(event) {
				if (!event) event = window.event;
				if (preventDefault) if (event.preventDefault) event.preventDefault();
				else event.returnValue = false;
				var delta = 0;
				if (event.wheelDelta) {
					delta = event.wheelDelta / 120;
					if (window.opera) delta = -delta;
				} else if (event.detail) {
					delta = -event.detail / 3;
				}
				if (up && (delta > 0 || !down)) up.apply(el, [event, delta]);
				else if (down && delta < 0) down.apply(el, [event, delta]);
			};

			if (window.addEventListener) window.addEventListener('DOMMouseScroll', el._handleMousewheel, false);
			window.onmousewheel = document.onmousewheel = el._handleMousewheel;
		},

		removeFocus: function(el) {
			if (!el._handleMousewheel) return;

			if (window.removeEventListener) window.removeEventListener('DOMMouseScroll', el._handleMousewheel, false);
			window.onmousewheel = document.onmousewheel = null;
			el._handleMousewheel = null;
		}
	};

	var navScroller = new NavScroller($('#nav-list'), $(window).height() - 87 - 28);
	var fnScroll = _.debounce(_.bind(navScroller.scroll, navScroller), 100);
	$('#sidebar').mousewheel(function(e, detail){
		$('.nav-list-top,.nav-list-bottom').show();

		var direct = detail > 0 ? 'up' : 'down';
		if('down' == direct){
			var lastLi = $("#nav-list>li:visible:last");
			if(lastLi[0].offsetTop > 50){
				fnScroll(direct);
			}
		}else{
			fnScroll(direct);
		}
	}, false);
})(this);