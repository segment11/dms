var Portal = {
	_wrapper: null,
	columnArr: ['left', 'center', 'right'],

	disableSelection: true, 

	// get query params
	params: function(url){
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
	},

	// save portlets position info here when change
	locHistoryLl: [],
	locHistoryMaxLen: 5,

	// setInterval id
	locHistoryWatchTimerId: null,
	// save portlets position every 10 seconds
	locHistoryWatchInterval: 1000 * 10,
	locHistoryWatchFn: function(){
		var len = Portal.locHistoryLl.length;
		if(!len){
			console.log('Portlets loc not change!');
			return;
		}

		if(!PortalConf.locSaveUrl){
			console.log('Portlets loc change url not given!');
			return;
		}

		var loc = Portal.locHistoryLl[len - 1];
		var url = PortalConf.locSaveUrl;
		var params = PortalConf.requestParams || {};
		$.extend(params, Portal.params());
		params.json = JSON.stringify(loc);

		$.post(url, params, function(r){
			if(r.flag){
				console.log('Portlets loc saved ok!');
				Portal.locHistoryLl.splice(0, Portal.locHistoryLl.length);
			}else{
				console.log('Portlets loc saved error!');
			}
		});
	},

	// dom operations
	dom: {
		// create portlets wrapper
		createColumn: function(id, classes){
			var _col = $(PortalConf.layoutColumnTpl.format(id));
			if(classes)
				_col.addClass(classes);
			return _col;
		},
		// create a portlet
		createItem: function(id, title, linkLl){
			var _item = $(PortalConf.itemTpl.format(id, title));
			_item.find('.itemHeader').append($(PortalConf.itemActionTpl));
			if(linkLl){
				var _innerContent = this.createItemContent(linkLl);
				if(_innerContent)
					_item.find('.itemContent').append(_innerContent);
			}
			return _item;
		},
		// create portlet content
		createItemContent: function(linkLl){
			if(linkLl && linkLl.length){
				if(PortalConf.itemContentLinkOutputer){
					return PortalConf.itemContentLinkOutputer(linkLl);
				}else{
					return null;
				}
			}
		}
	},

	// dom events bind
	events: {
		bindItemAction: function(){
			$('body').on('click', '.portletMin', function(){
				var _this = $(this);
				var _item = _this.parent().parent().parent();
				_item.find('.itemContent').hide();
				_item.find('.portletMax').show();
				_this.hide();
			});
			$('body').on('click', '.portletMax', function(){
				var _this = $(this);
				var _item = _this.parent().parent().parent();
				_item.find('.itemContent').show();
				_item.find('.portletMin').show();
				_this.hide();
			});
			$('body').on('click', '.portletClose', function(){
				var _this = $(this);
				var _item = _this.parent().parent().parent();
				_item.fadeOut('500', function(){
					var _removedItem = $(this);
					// 得到模块id
					var id = _removedItem.attr('id');

					// callback
					if(PortalConf.itemRemoveCallback){
						PortalConf.itemRemoveCallback(id);
					}

					_removedItem.remove();
				});
			});
			$('body').on('click', '.portletLink', function(e){
				e.preventDefault();

				if(PortalConf.itemContentLinkCallback){
					return PortalConf.itemContentLinkCallback($(this));
				}else{
					console.log($(this).attr('href'));
					return false;
				}
			});
		},

		dump: ''
	},

	init: function(wrapperId, dataLl, loc){
		this._wrapper = $('#' + wrapperId);
		if(!this._wrapper.length)
			return;

		this._wrapper.empty();

		this.initColumn(loc);
		this.events.bindItemAction();
		if(dataLl)
			this.load(dataLl, loc);

		this.initSortable();
		this.startLocWatch();
	},

	// change layout (portlets wrapper)
	reInit: function(loc){
		if(!this._wrapper || !loc)
			return;

		var widthArr = [];
		widthArr.push(loc.left ? loc.left.clz : 'w250');
		widthArr.push(loc.center ? loc.center.clz : 'w250');
		widthArr.push(loc.right ? loc.right.clz : 'w250');

		var clzArr = ['', 'w250', 'w500', 'w750'];
		var layoutArr = _.map(widthArr, function(it){
			return clzArr.indexOf(it);
		});
		var layoutClz = layoutArr.join('_');
		this.refreshLayout(layoutClz);

		var leftIdLl = loc.left ? loc.left.idLl : [];
		var centerIdLl = loc.center ? loc.center.idLl : [];
		var rightIdLl = loc.right ? loc.right.idLl : [];

		var getColWhich = function(id){
			if(centerIdLl.contains(id))
				return 'center';
			else if(rightIdLl.contains(id))
				return 'right';
			else
				return 'left';
		};

		// move items
		var _this = this;
		_.each(this.columnArr, function(it){
			var _target = _this._wrapper.find('#portal_' + it);
			_target.find('.groupItem').each(function(){
				var _item = $(this);
				var id = _item.attr('id');

				var colWhich = getColWhich(id);
				if(it != colWhich){
					_item.remove().appendTo(_this._wrapper.find('#portal_' + colWhich));
				}
			});
		});

		// sort
		var sortColItem = function(_target, idLl){
			if(!idLl || idLl.length == 0)
				return;

			var i = 0;
			for(; i < idLl.length; i++){
				var id = idLl[i];

				var _itemLl = _target.find('.groupItem');
				var _targetItem = _itemLl.filter('[id="' + id + '"]:eq(0)');
				if(_targetItem.length){
					var itemIndex = _itemLl.index(_targetItem);
					if(i != itemIndex){
						// set portlet (_targetItem) first
						if(i == 0){
							_targetItem.remove().prependTo(_target);
						}else{
							// set portlet (_targetItem) i - 1 (last ?)
							_itemLl.eq(i - 1).after(_targetItem.remove());
						}
					}
				}
			}
		};

		sortColItem(_this._wrapper.find('#portal_left'), leftIdLl);
		sortColItem(_this._wrapper.find('#portal_center'), centerIdLl);
		sortColItem(_this._wrapper.find('#portal_right'), rightIdLl);
	},

	initColumn: function(loc){
		var _this = this;
		_.each(this.columnArr, function(it){
			var clz = 'w250';
			if(loc && loc[it]){
				clz = loc[it]['clz'];
			}
			_this.dom.createColumn('portal_' + it, clz).appendTo(_this._wrapper);
		});
	},

	load: function(dataLl, loc){
		if(loc){
			var leftIdLl = (loc.left && loc.left.idLl) ? loc.left.idLl : [];
			var centerIdLl = (loc.center && loc.center.idLl) ? loc.center.idLl : [];
			var rightIdLl = (loc.right && loc.right.idLl) ? loc.right.idLl : [];

			_.each(dataLl, function(it){
				if(!it.loc)
					it.loc = 'left';

				if(leftIdLl.contains(it.id))
					it.loc = 'left';
				else if(centerIdLl.contains(it.id))
					it.loc = 'center';
				else if(rightIdLl.contains(it.id))
					it.loc = 'right';
			});

			// sort porlets by position 'left' < 'center' < 'right'
			_.sortBy(dataLl, function(it){
				var itemLoc = it.loc;

				var sum1 = 100;
				if('center' == itemLoc)
					sum1 = 200;
				else if('right' == itemLoc)
					sum1 = 300;

				if(loc[itemLoc] && loc[itemLoc]['idLl']){
					var idIndex = loc[itemLoc]['idLl'].indexOf(it.id);
					sum1 += idIndex;
				}

				return sum1;
			});
		}

		_.each(dataLl, function(it){
			var item = Portal.dom.createItem(it.id, it.title, it.linkLl);
			Portal.pend(it.loc || 'left', item);
		});
	},

	// refer jquery-ui sortable
	initSortable: function(){
		if(!$.fn.sortable)
			return;

		var wrapper = $('.groupWrapper');
		wrapper.sortable({
			handle: '.itemHeader',
			connectWith: '.groupWrapper',
			opacity: '0.6',
			dropOnEmpty: true,
			scrollSensitivity: 50,
			update: function(){
				if(Portal.locHistoryLl.length > Portal.locHistoryMaxLen)
					Portal.locHistoryLl.splice(0, Portal.locHistoryLl.length);

				Portal.locHistoryLl.push(Portal.getLoc());
			}
		});
		
		if(Portal.disableSelection)
			wrapper.disableSelection();
	},

	startLocWatch: function(){
		if(this.locHistoryWatchTimerId || !this.locHistoryWatchFn)
			return;

		this.locHistoryWatchTimerId = setInterval(this.locHistoryWatchFn,
			this.locHistoryWatchInterval);
	},

	// left / center / right
	pend: function(columnWhich, item){
		var _target = this._wrapper.find('#portal_' + columnWhich);
		_target.append(item);
	},

	refreshLayout: function(layoutClz){
		var _this = this;

		layoutClz = layoutClz || '1_1_1';
		var clz = PortalConf.layoutClz[layoutClz];

		var clzArr = clz.split(' ');
		var removedClz = 'w250 w750 w500 wnone';

		_.eachWithIndex(this.columnArr, function(it, index){
			var _target = _this._wrapper.find('#portal_' + it);
			// 3 column -> 2 column
			if(clzArr[index] == 'wnone'){
				_target.find('.groupItem').remove().appendTo(_this._wrapper.find('#portal_left'));
			}
			_target.removeClass(removedClz).addClass(clzArr[index]);
		});
	},

	// get portlets position info
	getLoc: function(){
		var loc = {};
		var _this = this;
		_.each(this.columnArr, function(it){
			if(!loc[it])
				loc[it] = {};

			var _target = _this._wrapper.find('#portal_' + it);
			var clzArr = _target.attr('class').split(' ');
			var i = 0;
			for(; i < clzArr.length; i++){
				var clzWidth = clzArr[i];
				if(PortalConf.colClzLl.contains(clzWidth)){
					loc[it]['clz'] = clzWidth;
					break;
				}
			}

			if(!loc[it]['idLl'])
				loc[it]['idLl'] = [];
			_target.find('.groupItem').each(function(){
				var id = $(this).attr('id');
				loc[it]['idLl'].push(id);
			});
		});
		return loc;
	},

	dump: ''
};