(function (Consts) {
    // jquery 'find' perform bad in IE8, use querySelectorAll instead
    if ($.browser.msie && $.browser.version <= 8) {
        $.fn.findAll = function (selector) {
            var list = [];
            this.each(function () {
                var queryList = this.querySelectorAll(selector);
                var i = 0, len = queryList.length;
                for (; i < len; i++) {
                    list.push(queryList[i]);
                }
            });
            return $(list);
        };
    } else {
        $.fn.findAll = $.fn.find;
    }

    $('<div id="loading-block" style="display: none;"></div><div id="loading" style="display: none;"></div>').appendTo($(document.body));

    // change plugin settings
    if ($.dialog) {
        $.dialog.setting.path = Consts.context + 'images/lhgdialog/';
        $.dialog.setting.zIndex = 900;
        $.dialog.setting.padding = '2px';

        $.dialog.loading = function (msg) {
            $('#loading').text(msg);
            $('#loading, #loading-block').show();
        };
        $.dialog.unloading = function () {
            $('#loading, #loading-block').fadeOut();
        };
    }
})(Consts);

// file ng.config.js
(function (angular, Consts) {
    var conf = {};

    conf.validClass = 'ng-valid';
    conf.invalidClass = 'ng-invalid';
    conf.dirtyClass = 'ng-dirty';
    conf.pristineClass = 'ng-pristine';

    conf.tipsXoffset = -12;
    conf.tipsYoffset = 8;

    // jquery datepicker
    conf.date = {
        duration: 'fast',
        dateFormat: 'yy-mm-dd',
        changeMonth: true,
        changeYear: true,
        showMonthAfterYear: false,
        yearSuffix: ''
    };

    // pagination
    conf.defaultPagiBtnClass = 'btn';
    conf.defaultPagiCurrentBtnClass = 'btn btn-primary';

    // dropdown
    conf.dropdownOptions = {
        valueField: 'code',
        labelField: 'label',
        widthDiff: 16,
        widthMultipleInput: 150,

        zIndex: 3000
    };

    conf.dialog = {
        lock: true,
        fixed: true,
        drag: true,
        resize: false,
        max: false,
        min: false
    };

    // copy properties from Consts so that you only need to change one file
    conf.context = Consts.context;
    conf.logLevel = Consts.logLevel;

    // user can overwrite
    for (var keyConsts in Consts) {
        if (keyConsts.startsWith('conf_')) {
            conf[keyConsts.substr('conf_'.length)] = Consts[keyConsts];
        }
    }

    var moduleName = 'ng.config';
    var md = angular.module(moduleName, []);
    md.value('conf', conf);
})(angular, Consts);

// file ng.service.js
(function (angular) {
    var moduleName = 'ng.service';
    var md = angular.module(moduleName, ['ng.config']);

    md.factory('safeApply', function ($rootScope) {
        return function (scope, fn) {
            var phase = scope.$root.$$phase;
            if (phase == '$apply' || phase == '$digest') {
                if (fn && (typeof (fn) === 'function')) {
                    fn();
                }
            } else {
                scope.$apply(fn);
            }
        }
    });

    // log
    md.factory('uiLog', ['conf', '$window', function (conf, win) {
        var levels = ['DEBUG', 'INFO', 'WARN', 'ERROR'];
        return {
            // format to string
            getMsg: function (msg, level) {
                if (!angular.isString(msg))
                    msg = JSON.stringify(msg);

                level = level || 'INFO';

                var dat = new Date().format();
                return '[' + dat + '][' + level + ']' + msg;
            },

            isLevelEnabled: function (level) {
                return levels.indexOf(conf.logLevel) <= levels.indexOf(level);
            },

            log: function (msg, level) {
                if (win.console && win.console.log && this.isLevelEnabled(level)) {
                    win.console.log(this.getMsg(msg, level));
                }
            },

            d: function (msg) {
                this.log(msg, 'DEBUG');
            },

            i: function (msg) {
                this.log(msg, 'INFO');
            },

            w: function (msg) {
                this.log(msg, 'WARN');
            },

            e: function (msg) {
                this.log(msg, 'ERROR');
            }
        };
    }]);

    // tips
    md.factory('uiTips', ['conf', 'uiLog', function (conf, log) {
        return {
            filterClass: function (elm, invalid) {
                if (invalid) {
                    elm.removeClass(conf.validClass).removeClass(conf.pristineClass).addClass(conf.invalidClass).addClass(conf.dirtyClass);
                } else {
                    elm.removeClass(conf.invalidClass).addClass(conf.validClass);
                }
            },

            on: function (el, msg, notHoverShow) {
                log.d('tips on...');

                // check if already executed uiTips.on
                var lastTip = el.data('last-tip');
                if (lastTip && lastTip === msg) {
                    return;
                }
                el.data('last-tip', msg);

                // dropdown rebuild dom
                var dropdownContainer = el.closest('.pui-dropdown');
                if (dropdownContainer.length) {
                    this.filterClass(el, true);
                    el = dropdownContainer;
                }

                var dropdownMultipleContainer = el.prev('.pui-autocomplete-multiple');
                if (dropdownMultipleContainer.length) {
                    this.filterClass(el, true);
                    el = dropdownMultipleContainer;
                }

                this.filterClass(el, true);

                var id = el.attr('ui-valid-id');
                if (!id) {
                    id = Math.guid();
                    el.attr('ui-valid-id', id);
                }

                if (id.contains('.')) {
                    id = id.replace(/\./g, '_');
                }

                if (notHoverShow) {
                    if ('poshytip' == conf.tipsStyle) {
                        el.poshytip('destroy');
                    } else {
                        $("#vtip_" + id).remove();
                        el.unbind('mouseenter mouseleave');
                    }
                    return;
                }

                // already exists, change css style
                var _tip = $("#vtip_" + id);
                if (_tip.length) {
                    _tip.html('<img class="vtip_arrow " src="' + conf.context + 'images/vtip_arrow.png" />' + msg)
                        .css({"display": "none"});
                } else {
                    // generate new and append
                    var html = '<p id="vtip_' + id + '" class="vtip"><img class="vtip_arrow" src="' + conf.context + 'images/vtip_arrow.png" />' + msg + '</p>';
                    $(html).css({"display": "none"}).appendTo($('body'));
                }

                el.unbind('mouseenter mouseleave').bind('mouseenter', _.throttle(function (e) {
                    var _tip = $("#vtip_" + id);
                    _tip.css({left: (e.pageX + conf.tipsXoffset) + 'px', top: (e.pageY + conf.tipsYoffset) + 'px'});
                    if (_tip.is(':hidden'))
                        _tip.show();
                }, 100)).bind('mouseleave', function () {
                    $("#vtip_" + id).hide();
                });
            },

            off: function (el) {
                el.data('last-tip', '');

                // dropdown rebuild dom
                var dropdownContainer = el.closest('.pui-dropdown');
                if (dropdownContainer.length) {
                    this.filterClass(el, true);
                    el = dropdownContainer;
                }

                var dropdownMultipleContainer = el.prev('.pui-autocomplete-multiple');
                if (dropdownMultipleContainer.length) {
                    this.filterClass(el, true);
                    el = dropdownMultipleContainer;
                }

                this.filterClass(el);

                var id = el.attr('ui-valid-id');
                if (!id) {
                    log.w('No ui-valid-id when call tips off!');
                    return;
                }
                if (id.contains('.')) {
                    id = id.replace(/\./g, '_');
                }

                $("#vtip_" + id).remove();
                el.unbind('mouseenter mouseleave');
            },

            // remove all tips div in a speicfic jQuery context
            // TIPS other tips style TODO
            offInContext: function (_context) {
                if (!_context || !_context.length) {
                    return;
                }

                _context.findAll('[ui-valid]').each(function () {
                    var validId = $(this).attr('ui-valid-id');
                    if (validId) {
                        $('#vtip_' + validId).hide();
                    }
                });
            },

            // *** loading block
            unloading: function () {
                $.dialog.unloading();
            },

            loading: function (msg) {
                $.dialog.loading(msg);
            },

            loadingFn: function (fn, msg, sync) {
                this.loading(msg);

                if (sync) {
                    setTimeout(fn, 50);
                } else {
                    fn();
                }
            },

            alert: function (msg, fn) {
                if (!$.dialog)
                    return;

                $.dialog.alert(msg, fn);
            },

            confirm: function (msg, fn, fn2) {
                if (!$.dialog)
                    return;

                $.dialog.confirm(msg, fn, fn2);
            },

            prompt: function (msg, fn, value) {
                if (!$.dialog)
                    return;

                $.dialog.prompt(msg, fn, value);
            },

            tips: function (msg, delay, img, fn) {
                if (!$.dialog)
                    return;

                $.dialog.tips(msg, delay, img, fn);
            }
        };
    }]);

    // valid
    md.factory('uiValid', ['conf', 'uiLog', 'uiTips', '$parse', function (conf, log, tips, $parse) {
        return {
            checkForm: function ($form) {
                return this.checkFormWithVal($form, false);
            },

            // call this method before submit your form or do a ajax request
            // because angular directive do not trigger auto
            checkFormWithVal: function ($form, returnRequiredModel, $index) {
                var formName = $form.$name;
                var _context = $('form[name="{0}"],[ng-form="{0}"],[data-ng-form="{0}"]'.format(formName));
                // ng-repeat create forms with same name, use one
                if ($index != null)
                    _context = _context.eq($index);

                var _elLl = _context.findAll('[ui-valid]');
                if (!_elLl.length)
                    return true;

                // angular unshift value="?" option
                var isSelectNull = function (one, val) {
                    return '?' == val && one.is('select');
                };

                var _this = this;
                var flags = [];
                // no break -> show all tips of form inputs that require value
                _elLl.each(function () {
                    var _el = $(this);

                    var val = _el.val();
                    if (angular.isString(val))
                        val = val.trim();

                    if (val && !isSelectNull(_el, val))
                        return;

                    var rules = _el.attr('ui-valid');
                    if (!rules)
                        return;

                    var arr = rules.split(' ');
                    // 'r' means required
                    if (arr.contains('r')) {
                        var modelName = _el.attr('ng-model') || _el.attr('data-ng-model');
                        flags.push(modelName);
                        tips.on(_el, _el.attr('ui-valid-tips') || _this.getMsg('r'));
                    }
                });
                return returnRequiredModel ? flags : !flags.length;
            },

            // check $form is ok exclude some model/rules
            // fuck! I am confused, these codes are 10 years ago
            validForm: function ($form, skippedList, ruleList, index) {
                // 用于保存去掉ruleList后各个model对应的违背规则列表，用于调节样式
                // 行转列，之前是规则对应false or NgModelController列表
                // 转换后，变成modelName对应的规则列表
                var modelRuleItems = {};

                // 当$form是$dirty false时候，用dom校验必填项
                var requiredFlags = this.checkFormWithVal($form, true, index);
                var requiredFlagsTarget = requiredFlags;

                // exclude required rule for skippedList(exclude model)
                if (ruleList.contains('r')) {
                    requiredFlagsTarget = _.difference(requiredFlags, skippedList);

                    _.each(_.intersection(skippedList, requiredFlags), function (modelName) {
                        if (!modelRuleItems[modelName])
                            modelRuleItems[modelName] = [];

                        modelRuleItems[modelName].push('r');
                    });
                }
                // 排除之后还有违背必填规则的模型，required就未被校验通过
                var isRequiredFlag = !requiredFlagsTarget.length;


                // 看下是不是因为modelList非必填的导致的$valid是false
                var isValidForm = $form.$valid;
                if (!isValidForm) {
                    var errorToken = [];

                    var errors = $form.$error;
                    for (var key in errors) {
                        if (!errors.hasOwnProperty(key) || !key.contains('__')) {
                            continue;
                        }

                        var arr = key.split(/__/);
                        var modelName = arr[0];
                        var rule = arr[1];

                        // === false就是校验通过
                        if (errors[key] === false) {
                            continue;
                        }

                        if (!modelRuleItems[modelName]) {
                            modelRuleItems[modelName] = [];
                        }
                        modelRuleItems[modelName].push(rule);

                        var needSkip = ruleList.contains(rule) && skippedList.contains(modelName);
                        if (needSkip) {
                            continue;
                        }

                        // 如果有非skippedList的model产生的error，就验证不通过
                        errorToken.push(key);
                    }

                    isValidForm = errorToken.length === 0;
                }

                this.filterTipsOff(modelRuleItems, skippedList, ruleList, index);

                return isRequiredFlag && isValidForm;
            },

            // remove tips
            filterTipsOff: function (modelRuleItems, skippedList, ruleList, index) {
                for (var modelName in modelRuleItems) {
                    if (!skippedList.contains(modelName)) {
                        continue;
                    }

                    // 违反规则去掉skip的rule list
                    var rules = modelRuleItems[modelName];
                    var rulesSliced = _.difference(rules, ruleList);

                    if (!rulesSliced.length) {
                        var targetEl = $('[ng-model="{0}"],[data-ng-model="{0}"]'.format(modelName));
                        if (index != null)
                            targetEl = targetEl.eq(index);

                        tips.off(targetEl);
                    }
                }
            },

            // check if val fit these valid rules
            check: function (val, rules, $scope, defaultTips, extendParam) {
                // no rules
                if (!rules)
                    return {flag: true};

                var arr = rules.split(' ');
                // 'r' means required
                // multiple select blank array == '' -> true, use string to compare
                var isBlank = val === null || val === undefined || val === '' || ('' + val === '');
                if (!arr.contains('r') && isBlank)
                    return {flag: true};

                if (!angular.isString(val))
                    val = '' + val;

                var i = 0, len = arr.length;
                for (; i < len; i++) {
                    var rule = arr[i];
                    if (!rule)
                        continue;

                    var flag = true;
                    if ('r' == rule) {
                        // multiple select blank array == '' -> true
                        // so return false
                        flag = !isBlank;
                    } else if (rule.contains(':')) {
                        // rules that is complex
                        flag = this.checkRule(val, rule.split(/:/), $scope, extendParam);
                    } else {
                        var pat = this.pats[rule];
                        if (pat instanceof RegExp) {
                            if (angular.isString(val)) {
                                flag = this.mat(val, pat);
                            }
                        } else if (angular.isFunction(pat)) {
                            flag = pat(val);
                        } else {
                            // only support regexp and function
                            flag = false;
                        }
                    }

                    // flag is a string value means valid failed, just show
                    if (angular.isString(flag)) {
                        return {flag: false, msg: flag, rule: rule};
                    }

                    if (flag === false) {
                        var msg = this.getMsg(rule, defaultTips) || this.getMsg('tips.valid');
                        return {flag: false, msg: msg, rule: rule};
                    }
                }

                return {flag: true};
            },

            // eg. "fn:checkTarget" -> customized valid function
            // eg. "num:range:target_id:+100" -> return true when val - model val(target_id) < 100
            // eg. "date:range:target_id:+2" -> return true when val - model val(target_id) < 2
            // eg. "date:rangeout:target_id:+2" -> return true when val - model val(target_id) > 2
            // eg. "minlen:char:3"
            // eg. "maxval:float:3.23"
            checkRule: function (val, ruleArr, $scope, extendParam) {
                var len = ruleArr.length;
                var pre = ruleArr[0];

                // customized valid function defined in controller $scope
                var getter, targetVal, rangeVal;
                if ('fn' == pre) {
                    var fnName = ruleArr[1];
                    getter = $parse(fnName);
                    var fn = getter($scope);
                    if (!fn) {
                        return true;
                    }

                    // function context is current scope
                    return fn.call($scope, val, extendParam);
                } else if ('num' == pre) {
                    if (len != 4) {
                        log.i('Invalid rules : ' + ruleArr);
                        return false;
                    }

                    // val targetVal is string, usually generated by user's input
                    getter = $parse(ruleArr[2]);
                    targetVal = getter($scope);
                    if (!targetVal)
                        return false;

                    var currentVal = parseFloat(val);
                    var targetNumVal = parseFloat(targetVal);

                    rangeVal = parseInt(ruleArr[3], 10);
                    if (ruleArr[1] == 'range' && currentVal > targetNumVal + rangeVal)
                        return false;
                    if (ruleArr[1] == 'rangeout' && currentVal < targetNumVal + rangeVal)
                        return false;

                    return true;
                } else if ('date' == pre) {
                    if (len != 4) {
                        log.i('Invalid rules : ' + ruleArr);
                        return false;
                    }

                    // val targetVal is better as a Date object, but it's much more complex
                    // here targetVal is a string
                    getter = $parse(ruleArr[2]);
                    targetVal = getter($scope);
                    if (!targetVal)
                        return false;

                    rangeVal = parseInt(ruleArr[3], 10);
                    if (ruleArr[1] == 'range' && Date.parse2(val) > Date.parse2(targetVal).add(rangeVal))
                        return false;
                    if (ruleArr[1] == 'rangeout' && Date.parse2(val) < Date.parse2(targetVal).add(rangeVal))
                        return false;

                    return true;
                } else if ('minlen' == pre || 'maxlen' == pre) {
                    if (len != 3) {
                        log.i('Invalid rules : ' + ruleArr);
                        return false;
                    }

                    var lenVal = parseInt(ruleArr[2], 10);
                    if (ruleArr[0] == 'minlen' &&
                        (('byte' == ruleArr[1] && val.length < lenVal) ||
                            ('char' == ruleArr[1] && val.charlen() < lenVal)))
                        return false;
                    if (ruleArr[0] == 'maxlen' &&
                        (('byte' == ruleArr[1] && val.length > lenVal) ||
                            ('char' == ruleArr[1] && val.charlen() > lenVal)))
                        return false;
                    return true;
                } else if ('minval' == pre || 'maxval' == pre) {
                    if (len != 3) {
                        log.i('Invalid rules : ' + ruleArr);
                        return false;
                    }

                    targetVal = 'float' == ruleArr[1] ? parseFloat(ruleArr[2]) : parseInt(ruleArr[2], 10);
                    var currentVal = 'float' == ruleArr[1] ? parseFloat(val) : parseInt(val, 10);
                    if (pre == 'minval' && currentVal < targetVal)
                        return false;
                    if (pre == 'maxval' && currentVal > targetVal)
                        return false;
                    return true;
                } else if ('ac' == pre) {
                    // autocomplete valid check
                    if (len != 2 && len != 3) {
                        log.i('Invalid rules : ' + ruleArr);
                        return false;
                    }
                    getter = $parse(ruleArr[1]);
                    targetVal = getter($scope);

                    // tips: label-value (format)
                    var splitChar = len == 3 ? ruleArr[2] : '-';
                    return targetVal && val.split(splitChar)[0] == targetVal;
                } else {
                    return true;
                }
            },

            mat: function (val, pat) {
                if (!pat)
                    return true;

                return pat.test(val);
            },

            getMsg: function (rule, tips) {
                // if there are tips (ui-valid-tips) when using this directive, return giving tips
                // if ui-valid-tips="label:Your model label", prepend 'Your model label' to tips and return
                tips = tips || '';
                if (tips && !tips.contains(':')) {
                    return tips;
                }

                var msg = this.msgs[rule];
                if (rule.contains(':')) {
                    var ruleFirst = rule.split(':')[0];
                    if (['ac', 'maxval', 'minval', 'maxlen', 'minlen'].contains(ruleFirst)) {
                        msg = this.msgs[ruleFirst];
                    }
                }

                if (msg) {
                    var params0 = tips.contains(':') ? tips.split(/:/)[1] : '';
                    var params1 = '';
                    if (rule.startsWith('min') || rule.startsWith('max')) {
                        var ruleArr = rule.split(/:/);
                        // eg. rule = "maxval:float:3.23" -> show tips with 3.23
                        params1 = ruleArr[ruleArr.length - 1];
                    }

                    return msg.format(params0, params1);
                } else {
                    log.w('No tips for : ' + rule);
                    return tips;
                }
            },

            // add your valid function using this
            /*
			eg.
			var myModule = angular.module('myModule', ['ng.service']);
			myModule.run(['uiValid', function(uiValid){
				uiValid.regPat('rule.test', /^\d{2,3}$/, 'number >= 10 <= 999');
			}]);
			*/
            regPat: function (code, pat, msg) {
                if (this.pats[code])
                    return;

                this.pats[code] = pat;
                this.msgs[code] = msg;
            },

            // default rule / tips items
            msgs: {
                'r': '{0} required',
                'date': '{0} eg.yyyy-MM-dd',
                'time': '{0} eg.hh:mm',
                'datetime': '{0} eg.yyyy-MM-dd hh:mm:ss',

                'int': '{0} integer required',
                'posint': '{0} positive integer required',
                'float': '{0} number required',

                'minlen': '{0} charlen < {1}',
                'maxlen': '{0} charlen > {1}',
                'maxval': '{0} value > {1}',
                'minval': '{0} value < {1}',
                'tips.valid': 'invalid value'
            },

            // default rule -> regex/function items
            pats: {
                'date': function (val) {
                    return Date.isDateValid(val);
                },
                'time': function (val) {
                    return Date.isTimeValid(val);
                },
                'datetime': function (val) {
                    return Date.isDateTimeValid(val);
                },

                'int': /^[\-\+]?([0-9]+)$/,
                'posint': /^\d+$/,
                'float': /^[\-\+]?([0-9]+\.?([0-9]+)?)$/,
                'float1': /^[\-\+]?([0-9]+(\.[0-9]{1})?)$/,
                'float2': /^[\-\+]?([0-9]+(\.[0-9]{2})?)$/
            }
        };
    }]);
})(angular);

// file ng.ui.js
(function (ag) {
    var moduleName = 'ng.ui';
    var md = ag.module(moduleName, ['ng.config', 'ng.service']);

    // layout tips : most of the time you do not need this directive
    // because it uses some class defined to add width to td/th
    // *** *** *** *** *** *** *** *** *** ***
    // *** *** *** *** *** *** *** *** *** ***
    md.directive('uiLayoutCol', ['uiLog', function (log) {
        'use strict';
        return {
            restrict: 'A',
            link: function (scope, el, attrs, ctrl) {
                if ('TR' != el[0].nodeName) {
                    log.w('Init uiLayoutCol failed : not a TR element!');
                    return;
                }

                log.i('Relayout...');

                var _tds = el.children('td');
                if (_tds.length == 2) {
                    _tds.filter(':first').addClass('l');
                    _tds.filter(':last').addClass('r');
                } else if (_tds.length == 4) {
                    _tds.filter(':even').addClass('l2');
                    _tds.filter(':odd').addClass('r2');
                } else if (_tds.length == 6) {
                    _tds.eq(0).addClass('l3');
                    _tds.eq(1).addClass('r3');
                    _tds.eq(2).addClass('l3');
                    _tds.eq(3).addClass('r3');
                    _tds.eq(4).addClass('l3');
                    _tds.eq(5).addClass('r3last');
                }

                // siblings tr set td text-align to right if exists label
                _tds = el.siblings('tr').children('td');
                _tds.filter(function () {
                    return $(this).findAll('label').length > 0 && !$(this).hasClass('al');
                }).addClass('ar');
                _tds.filter(function () {
                    return $(this).findAll('label').length == 0;
                }).addClass('al p_left5');
            }
        };
    }]);

    // datepicker
    // *** *** *** *** *** *** *** *** *** ***
    // *** *** *** *** *** *** *** *** *** ***
    md.directive('uiDate', ['conf', 'uiLog', function (conf, log) {
        var options = {};
        if (ag.isObject(conf.date)) {
            ag.extend(options, conf.date);
        }
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, el, attrs, ctrl) {
                var getOptions = function () {
                    return ag.extend(ag.copy(options), scope.$eval(attrs.uiDate));
                };

                var init = function () {
                    var opts = getOptions();
                    log.i('Init datepicker : ' + attrs.ngModel);
                    log.i(opts);

                    opts.onSelect = function (value, picker) {
                        scope.$apply(function () {
                            ctrl.$setViewValue(el.val());
                        });
                    };

                    el.datepicker('destroy');
                    el.addClass('date');
                    if (opts.timeFormat) {
                        el.datetimepicker(opts);
                    } else {
                        el.datepicker(opts);
                    }
                };

                // change format auto
                // add strikethrough or add 0
                var format = function () {
                    var val = el.val();
                    if (!val)
                        return;

                    var arr = val.split(' ');
                    var ymd = arr[0];

                    var ymdNew = ymd;

                    // add 0 to month/day
                    if (ymd.contains('-')) {
                        var subArr = ymd.split('-');
                        if (subArr.length != 3)
                            return;

                        ymdNew = subArr[0] +
                            '-' +
                            (subArr[1].length == 1 ? '0' + subArr[1] : subArr[1]) +
                            '-' +
                            (subArr[2].length == 1 ? '0' + subArr[2] : subArr[2]);
                        if (arr.length > 1)
                            ymdNew += ' ' + arr[1];
                    } else {
                        if (!ymd.match(/^\d{8}$/))
                            return;

                        ymdNew = ymd.substr(0, 4) + '-' + ymd.substr(4, 2) + '-' + ymd.substr(6, 2);
                        if (arr.length > 1)
                            ymdNew += ' ' + arr[1];
                    }

                    if (ymdNew != val) {
                        el.val(ymdNew);
                        scope.$apply(function () {
                            ctrl.$setViewValue(ymdNew);
                        });
                    }
                };

                el.blur(format);
                // return
                el.keyup(function (e) {
                    if (e.keyCode == 13)
                        format();
                });

                // Watch for changes to the directives options
                // under one condition it's datepicker, another it's datetimepicker
                scope.$watch(getOptions, init, true);
            }
        };
    }]);

    md.service('uiDropdownHelper', function () {
        // panel begin zindex
        this.zindex = 1000;
        this.defaultPanelScrollHeight = 150;
        this.highlightClass = 'ui-state-highlight';

        this.setPanelPosition = function (panel, el, opts) {
            var offset = el.offset();
            panel.css({
                zindex: this.zindex++,
                width: el.width(),
                top: offset.top + el.height(),
                left: offset.left
            });

            var height = (opts.height || this.defaultPanelScrollHeight) + 'px';
            panel.findAll('.pui-dropdown-items-wrapper').css({height: height});
        };

        this.bindHoverEvent = function (el, selector) {
            var selector = 'li';
            el.delegate(selector, 'mouseenter', function (e) {
                var hovered = $(e.target);
                if (!hovered.is(selector))
                    hovered = hovered.closest(selector);

                el.findAll(selector + '.ui-state-hover').removeClass('ui-state-hover');

                if (!hovered.is('.ui-state-active') && !hovered.is('.ui-state-disabled'))
                    hovered.addClass('ui-state-hover');
            }).on('mouseleave', function () {
                el.findAll(selector).removeClass('ui-state-hover');
            });
        };

        this.bindDelegateDocumentEvent = function (attrId) {
            $(document).click(function (e) {
                var target = $(e.target);

                $('.pui-dropdown-panel').each(function () {
                    var panel = $(this);
                    var panelDropdownId = panel.attr(attrId);

                    if (target.is('.pui-dropdown-filter') || target.is('.ui-dropdown-multiple-input')) {
                        // hide others panel
                        var targetDropdownId = target.attr(attrId);
                        if (targetDropdownId !== panelDropdownId)
                            panel.triggerHandler('uiDropdownHide');
                    } else {
                        panel.triggerHandler('uiDropdownHide');
                    }
                });

                // multiple choosed label delete span
                // is a span <ul><li><span></span></li></ul>
                if (target.is('[data-pui-ac-close-span]')) {
                    var acId = target.attr('data-pui-ac-close-span');
                    $('.pui-dropdown-panel').each(function () {
                        var panel = $(this);
                        var panelAcId = panel.attr(attrId);
                        if (acId === panelAcId) {
                            var targetVal = target.parent().attr('data-raw-value');
                            panel.triggerHandler('puiAcDelVal', [targetVal]);
                        }
                    });

                    target.parent().remove();
                }
            });
        };

        this.wrapMultipleLi = function (targetVal, targetLabel, acId) {
            return '<li data-raw-value="' + targetVal +
                '" class="pui-autocomplete-token ui-state-active ui-corner-all ui-helper-hidden" ' +
                'style="display: list-item;"><span data-pui-ac-close-span="' + acId +
                '" class="pui-autocomplete-token-icon ui-icon ui-icon-close"></span>' +
                '<span class="pui-autocomplete-token-label" title="' + targetLabel + '">' + targetLabel + '</span></li>';
        };
    });

    md.directive('uiDropdownPanel', ['$http', 'conf', 'uiLog', 'uiDropdownHelper', function ($http, conf, log, uiDropdownHelper) {
        var tplLi = '<li data-id="{0}" class="pui-dropdown-item pui-dropdown-list-item ui-corner-all">{1}</li>';
        return {
            scope: {
                list: '=',
                valueField: '@',
                labelField: '@',
                blankLabel: '@',
                queryUrl: '@',
                modelVal: '=',
                labelWithVal: '@'
            },

            template: '' +
                ' <div class="pui-dropdown-filter-container">' +
                ' <input type="text" ng-model="querySearch" class="pui-dropdown-filter pui-inputtext ui-widget ui-state-default ui-corner-all" />' +
                ' <span class="ui-icon ui-icon-search"></span>' +
                ' </div>' +
                ' <div class="pui-dropdown-items-wrapper">' +
                '	<ul class="pui-dropdown-items pui-dropdown-list ui-widget-content ui-widget ui-corner-all ui-helper-reset">' +
                '	</ul>' +
                ' </div>',

            link: function (scope, el, attrs) {
                var isLabelWithVal = 'true' == scope.labelWithVal;
                var filterList = function (list, targetVal, cb, match) {
                    if (!list)
                        return cb([]);

                    if (match && !targetVal)
                        return cb(list);

                    var filteredList = _.filter(list, function (it) {
                        var val = '' + it[scope.valueField];
                        var label = '' + it[scope.labelField];

                        if (match && !(val.contains(targetVal) ||
                            val.toLowerCase().contains(targetVal.toLowerCase()) ||
                            label.contains(targetVal) ||
                            label.toLowerCase().contains(targetVal.toLowerCase())
                        )) {
                            return false;
                        }

                        var isChoosedAlready = angular.isArray(scope.modelVal) ?
                            scope.modelVal.contains(val) : scope.modelVal == val;
                        return !isChoosedAlready;
                    });
                    cb(filteredList);
                };

                // use dom
                el.on('uiDropdownUnique', function (e, modelVal) {
                    e.preventDefault();
                    e.stopPropagation();

                    renderList({list: scope.list, queryUrl: scope.queryUrl, querySearch: scope.querySearch});
                });

                var renderList = function (obj, match, triggerReady) {
                    var list = obj.list;
                    var querySearch = obj.querySearch;
                    var queryUrl = obj.queryUrl;

                    var tpl = scope.blankLabel ? tplLi.format('', scope.blankLabel) : '';
                    var cb = function (ll) {
                        var i = 0, len = ll.length, one;
                        for (; i < len; i++) {
                            one = ll[i];
                            tpl += tplLi.format(one[scope.valueField],
                                isLabelWithVal ? one[scope.valueField] + '-' + one[scope.labelField] : one[scope.labelField]);
                        }

                        el.findAll('ul').html(tpl);

                        if (triggerReady)
                            el.triggerHandler('uiDropdownListReady');
                    };

                    if (queryUrl) {
                        if (!querySearch) {
                            cb([]);
                        } else {
                            $http.get(queryUrl + '?q=' + encodeURI(querySearch)).success(function (data) {
                                cb(data);
                            });
                        }
                    } else {
                        filterList(list, querySearch, cb, match);
                    }
                };

                scope.$watch(function () {
                    return {list: scope.list, queryUrl: scope.queryUrl, querySearch: scope.querySearch};
                }, function (obj) {
                    renderList(obj, true, true);
                }, true);

                var focusByCalIndex = function (calFn) {
                    var liList = el.findAll('li');
                    var liCurrent = liList.filter('.ui-state-hover');
                    liCurrent.removeClass('ui-state-hover');

                    var index = liList.index(liCurrent);

                    var targetIndex = calFn(index, liList.length);
                    liList.eq(targetIndex).addClass('ui-state-hover');
                };

                var focusPrev = function () {
                    focusByCalIndex(function (index, len) {
                        return index <= 0 ? len - 1 : index - 1;
                    });
                };
                var focusNext = function () {
                    focusByCalIndex(function (index, len) {
                        return index < len - 1 ? index + 1 : 0;
                    });
                };
                var chooseCurrent = function () {
                    var li = el.findAll('li.ui-state-hover');
                    // no hover, choose first by default
                    if (!li.length)
                        li = el.findAll('li').eq(0);
                    if (!li.length)
                        return;

                    var targetId = li.attr('data-id');
                    var targetLabel = li.text();
                    el.triggerHandler('uiDropdownChoose', [targetId, targetLabel]);
                };
                var hidePanel = function () {
                    el.triggerHandler('uiDropdownHide');
                };

                el.findAll('.pui-dropdown-filter').keyup(function (e) {
                    e.stopPropagation();

                    var keyCode = e.keyCode;
                    switch (keyCode) {
                        // up
                        case 38:
                            focusPrev();
                            break;
                        // down
                        case 40:
                            focusNext();
                            break;
                        // return
                        case 13:
                            chooseCurrent();
                            break;
                        // esc
                        case 27:
                            hidePanel();
                            break;
                        default:
                            break;
                    }
                });
            }
        };
    }]);

    // dropdown
    md.directive('uiDropdown', ['$parse', '$compile', 'conf', 'uiLog', 'uiDropdownHelper', function ($parse, $compile, conf, log, uiDropdownHelper) {
        var countNum = 0;
        var attrId = 'data-dropdown-id';

        var dropdownPaneTpl = '<div ui-dropdown-panel="" label-with-val="{6}" model-val="{5}" query-url="{4}" blank-label="{3}" label-field="{2}" value-field="{1}" list="{0}" ' +
            'class="pui-dropdown-panel ui-widget-content ui-corner-all ui-helper-hidden pui-shadow">' +
            '</div>';

        uiDropdownHelper.bindDelegateDocumentEvent(attrId);

        return {
            restrict: 'A',
            require: 'ngModel',
            transclude: true,

            priority: 1000,

            compile: function (el, attrs, transcludeFn) {
                return function (scope, el, attrs, ctrl) {
                    var opts = scope.$eval(attrs.uiDropdown) || {};
                    opts = angular.extend(angular.copy(conf.dropdownOptions), opts);
                    var listModel = opts.list;
                    if (!listModel && !opts.queryUrl) {
                        log.w('No listModel or queryUrl given!');
                        return;
                    }

                    var getList = function () {
                        return $parse(listModel)(scope);
                    };

                    var isMultiple = !!opts.multiple;
                    var isEditable = !!opts.editable;

                    var cc = countNum++;

                    if (isEditable) {
                        attrs.$observe('uiEditable', function (val) {
                            if (val === undefined)
                                return;

                            if ('true' === val) {
                                el.parent().addClass('ui-helper-editable');
                            } else {
                                renderLabel(el.val());
                                el.parent().removeClass('ui-helper-editable');
                            }
                        });
                    }

                    attrs.$observe('disabled', function (val) {
                        if (val === undefined)
                            return;

                        // disabled -> true
                        if (val) {
                            hidePanel();

                            if (isMultiple) {
                                var input = el.prev('.pui-autocomplete-multiple').findAll('.ui-dropdown-multiple-input');
                                input.attr('disabled', true);
                                var ul = input.parent().parent();
                                ul.findAll('.ui-icon-close').hide();
                                ul.findAll('.pui-autocomplete-token-label').addClass('ui-state-disabled');
                            } else {
                                el.parent().parent().addClass('ui-state-disabled');
                            }
                        } else {
                            if (isMultiple) {
                                var input = el.prev('.pui-autocomplete-multiple').findAll('.ui-dropdown-multiple-input');
                                input.removeAttr('disabled');
                                var ul = input.parent().parent();
                                ul.findAll('.ui-icon-close').show();
                                ul.findAll('.pui-autocomplete-token-label').removeClass('ui-state-disabled');
                            } else {
                                el.parent().parent().removeClass('ui-state-disabled');
                            }
                        }
                    });

                    var tplPanel = dropdownPaneTpl.format(listModel, opts.valueField, opts.labelField,
                        opts.blankLabel || '', opts.queryUrl || '', attrs.ngModel, opts.labelWithVal);
                    var panel = $compile(tplPanel)(scope);
                    panel.attr(attrId, cc).css('z-index', opts.zIndex);
                    panel.findAll('.pui-dropdown-filter').attr(attrId, cc);
                    panel.appendTo($(document.body));

                    // use enter/tab trigger
                    panel.on('uiDropdownChoose', function (e, targetVal, targetLabel) {
                        e.preventDefault();
                        e.stopPropagation();

                        chooseCurrent(targetVal, targetLabel);
                    });

                    // when set list after model set
                    panel.on('uiDropdownListReady', function (e) {
                        e.preventDefault();
                        e.stopPropagation();

                        ctrl.$render();
                    });


                    panel.on('uiDropdownHide', function (e) {
                        e.preventDefault();
                        e.stopPropagation();

                        if (isActive)
                            hidePanel();
                    });

                    if (isMultiple) {
                        panel.on('puiAcDelVal', function (e, targetVal) {
                            var input = el.prev('.pui-autocomplete-multiple').findAll('.ui-dropdown-multiple-input');

                            // remove one from array
                            var targetValList = ctrl.$modelValue || [];
                            var index = targetValList.indexOf(targetVal);
                            if (index >= 0) {
                                targetValList.splice(index, 1);
                                el.val(targetValList.toString());

                                scope.$apply(function () {
                                    ctrl.$setViewValue(targetValList);
                                    if (attrs.uiChange) {
                                        scope.$eval(attrs.uiChange);
                                    }
                                });
                            }
                        });
                    }

                    uiDropdownHelper.bindHoverEvent(panel, 'li');

                    panel.delegate('li', 'click', function (e) {
                        e.preventDefault();
                        e.stopPropagation();

                        var li = $(e.target);
                        var targetVal = li.attr('data-id');
                        var targetLabel = li.text();
                        chooseCurrent(targetVal, targetLabel);
                    });

                    if (isMultiple) {
                        transcludeFn(scope, function (clone) {
                            el.hide();

                            // help input
                            var input = $('<input type="text" class="pui-textfield ui-dropdown-multiple-input" />')
                                .attr(attrId, cc).width(opts.widthMultipleInput + 'px');
                            el.before(input);

                            input.wrap('<li class="pui-autocomplete-input-token"></li>');
                            input.parent().wrap('<ul class="pui-autocomplete-multiple ui-widget pui-inputtext ui-state-default ui-corner-all"></ul>');
                            if (opts.widthWrapper) {
                                input.parent().parent().width(opts.widthWrapper + 'px');
                            }
                        });
                    } else if (isEditable) {
                        transcludeFn(scope, function (clone) {
                            el.wrap('<div class="ui-helper-hidden-accessible ui-helper-editable"></div>');
                            var elParent = el.parent();

                            opts.width = el.width();
                            elParent.wrap('<div class="pui-dropdown ui-widget ui-state-default ui-corner-all ui-helper-clearfix" style="width: ' + opts.width + 'px;"></div>');
                            elParent.after('<div class="pui-dropdown-trigger ui-state-default ui-corner-right"><span class="ui-icon ui-icon-triangle-1-s"></span></div>');
                            elParent.after('<label class="pui-dropdown-label pui-inputtext ui-corner-all" style="width: ' + (opts.width - opts.widthDiff) + 'px;">' + (opts.blankLabel || '--/--') + '</label>');

                            el.css({
                                border: 'none',
                                'background-color': '#fff',
                                height: (conf.inputHeight || 27) + 'px'
                            });
                        });
                    } else {
                        transcludeFn(scope, function (clone) {
                            el.wrap('<div class="ui-helper-hidden-accessible"></div>');
                            var elParent = el.parent();

                            opts.width = el.width();
                            elParent.wrap('<div class="pui-dropdown ui-widget ui-state-default ui-corner-all ui-helper-clearfix" style="width: ' + opts.width + 'px;"></div>');
                            elParent.after('<div class="pui-dropdown-trigger ui-state-default ui-corner-right"><span class="ui-icon ui-icon-triangle-1-s"></span></div>');
                            elParent.after('<label class="pui-dropdown-label pui-inputtext ui-corner-all" style="width: ' + (opts.width - opts.widthDiff) + 'px;">' + (opts.blankLabel || '--/--') + '</label>');
                        });
                    }

                    var renderLabelMultiple = function (valList, labelList, clear) {
                        var input = el.prev('.pui-autocomplete-multiple').findAll('.ui-dropdown-multiple-input');
                        var li = input.parent();
                        if (clear) {
                            li.siblings().remove();
                        }

                        valList = valList || [];
                        el.val(valList.toString());
                        if (!valList.length)
                            return;

                        labelList = labelList || [];

                        _.each(valList, function (targetVal, i) {
                            var targetLabel = labelList[i];

                            if (!targetLabel) {
                                var item = _.find(getList(), function (it) {
                                    return it[opts.valueField] == targetVal;
                                });
                                targetLabel = item ? item[opts.labelField] : targetVal;

                                if (opts.labelWithVal && item) {
                                    targetLabel = item[opts.valueField] + '-' + targetLabel;
                                }
                            }

                            li.before(uiDropdownHelper.wrapMultipleLi(targetVal, targetLabel, cc));
                        });

                        // if disabled
                        if (attrs.disabled) {
                            var ul = li.parent();
                            ul.findAll('.ui-icon-close').hide();
                            ul.findAll('.pui-autocomplete-token-label').addClass('ui-state-disabled');
                        }
                    };

                    var renderLabel = function (modelVal, labelVal) {
                        el.val(modelVal || '');

                        var label = opts.blankLabel || '';
                        if (labelVal) {
                            label = labelVal;
                        } else if (modelVal) {
                            if (isEditable) {
                                label = modelVal;
                            } else {
                                // not ===
                                var item = _.find(getList(), function (it) {
                                    return it[opts.valueField] == modelVal;
                                });
                                label = item ? item[opts.labelField] : modelVal;

                                if (opts.labelWithVal && item) {
                                    label = item[opts.valueField] + '-' + label;
                                }
                            }
                        }
                        el.parent().parent().findAll('.pui-dropdown-label').attr('title', label).text(label);
                    };

                    var chooseCurrent = function (targetVal, targetLabel) {
                        hidePanel();
                        // reset input for next time choose from panel
                        panel.findAll('.pui-dropdown-filter').val('');

                        if (isMultiple) {
                            var targetValList = ctrl.$modelValue || [];
                            // if not blank
                            if (targetVal && !targetValList.contains(targetVal)) {
                                renderLabelMultiple([targetVal], [targetLabel]);
                                targetValList.push(targetVal);
                                scope.$apply(function () {
                                    ctrl.$setViewValue(targetValList);
                                    if (attrs.uiChange) {
                                        scope.$eval(attrs.uiChange);
                                    }
                                });
                            }
                        } else {
                            renderLabel(targetVal, targetLabel);
                            scope.$apply(function () {
                                ctrl.$setViewValue(targetVal);
                                if (attrs.uiChange) {
                                    scope.$eval(attrs.uiChange);
                                }
                            });
                        }
                    };

                    ctrl.$render = function () {
                        isMultiple ? renderLabelMultiple(ctrl.$modelValue, null, true) : renderLabel(ctrl.$modelValue);
                    };

                    var isActive = false;
                    var hidePanel = function () {
                        panel.hide();
                        isActive = false;
                    };

                    var showPanel = function () {
                        var relativeEl = isMultiple ? el.prev('.pui-autocomplete-multiple').findAll('.ui-dropdown-multiple-input') :
                            el.parent().parent();
                        uiDropdownHelper.setPanelPosition(panel, relativeEl, opts);

                        panel.show();
                        panel.findAll('.pui-dropdown-filter').focus();
                        panel.triggerHandler('uiDropdownUnique', [ctrl.$modelValue]);
                        isActive = true;
                    };

                    if (isMultiple) {
                        el.prev('.pui-autocomplete-multiple').findAll('.ui-dropdown-multiple-input').on('focus', function (e) {
                            e.stopPropagation();
                            showPanel();
                        });
                    } else {
                        el.parent().parent().click(function (e) {
                            e.stopPropagation();
                            if (attrs.disabled)
                                return;

                            // editable
                            if (isEditable) {
                                var target = $(e.target);
                                if (!target.is('input')) {
                                    isActive ? hidePanel() : showPanel();
                                }
                            } else {
                                isActive ? hidePanel() : showPanel();
                            }
                        });
                    }
                }; // end return link
            }
        };
    }]);

    // scope: true better
    md.directive('uiDialog', ['$parse', '$compile', 'conf', 'uiLog', 'safeApply', function ($parse, $compile, conf, log, safeApply) {
        var cc = 0;

        var fixCenter = function (dialog, fixDelay) {
            // setTimeout -> locate center after $digest -> dom rebuild
            // do not use $timeout as need not $digest again
            setTimeout(function () {
                var wrap = dialog.DOM.wrap[0];
                var left = ($(window).width() - wrap.offsetWidth) / 2;
                var top = ($(window).height() - wrap.offsetHeight) / 2 - 20;
                dialog.position(left, top);
            }, fixDelay || 200);
        };
        return {
            restrict: 'A',
            link: function (scope, el, attrs) {
                var opts = scope.$eval(attrs.uiDialog) || {};
                log.i('Compile dialog ui : ');
                log.i(opts);

                if (!opts.showModel) {
                    log.w('No show model given!');
                    return;
                }

                // one page has more than one dialogs with same dialog id
                opts.dialogId = (opts.dialogId || '') + '_' + (++cc);
                opts.closeForce = '0' == opts.closeForce ? false : true;

                var subScope;

                // lhgdialog properties
                var props = {};
                if (ag.isObject(conf.dialog)) {
                    ag.extend(props, conf.dialog);
                }

                props.id = opts.dialogId;
                props.title = opts.titleModel ? ('{{' + opts.titleModel + '}}') : opts.title;
                props.content = el.html();
                props.init = function () {
                    var targetScope = scope;
                    if (opts.closeForce)
                        subScope = targetScope = scope.$new();

                    // in watch
                    $compile(this.DOM.wrap.findAll('.ui_dialog'))(targetScope);
                    if (opts.fixPosition) {
                        var that = this;
                        fixCenter(that, opts.fixDelay);
                    }
                };

                // a flag that make sure lhgdialog close only once
                // because model true -> false trigger close again
                var isInClose = false;
                props.close = function () {
                    isInClose = true;

                    // use close in dialog toolbar will execute twice
                    // use button in dialog user defined will execute once which trigger by watch list
                    var getter = $parse(opts.showModel);
                    var isShow = getter(scope);
                    if (isShow) {
                        var setter = getter.assign;
                        // trigger watch again
                        safeApply(scope, function () {
                            setter(scope, false);
                            if (opts.closeSettings) {
                                var key = opts.closeSettings.key;
                                var val = opts.closeSettings.value;
                                $parse(key).assign(scope, val);
                            }
                            if (opts.closeFn) {
                                var fnTarget = $parse(opts.closeFn)(scope);
                                if (ag.isFunction(fnTarget)) {
                                    fnTarget();
                                }
                            }
                        });
                    }
                    ;

                    isInClose = false;

                    if (opts.closeForce && subScope) {
                        subScope.$destroy();
                        subScope = null;
                    }

                    $('.vtip').remove();

                    // not really close
                    return opts.closeForce ? true : false;
                };

                // @depricated, use ext instead
                _.each(['lock', 'drag', 'fixed', 'resize'], function (it) {
                    if (angular.isDefined(opts[it]))
                        props[it] = opts[it];
                });
                _.each(['width', 'height', 'left', 'top'], function (it) {
                    if (opts[it])
                        props[it] = opts[it];
                });

                scope.$watch(opts.showModel, function (val) {
                    // show
                    if (val) {
                        var target = $.dialog.list[opts.dialogId];
                        if (target) {
                            if (target.config.lock) {
                                target.lock();
                            } else {
                                target.zindex();
                            }
                            if (opts.fixPosition) {
                                fixCenter(target);
                            }
                            target.show();
                        } else {
                            $.dialog(angular.copy(props));
                        }
                    } else {
                        // hide
                        var target = $.dialog.list[opts.dialogId];
                        if (target) {
                            if (opts.closeForce) {
                                if (!isInClose)
                                    target.close();
                            } else {
                                target.hide();
                            }
                        }
                    }
                }); // end $watch showModel
            } // end link
        };
    }]);

    // use template better, the angular way
    // jquery dom way -> support compile template lazy
    md.directive('uiTabs', ['$compile', '$parse', 'safeApply', 'uiLog', 'uiTips',
        function ($compile, $parse, safeApply, log, tips) {
            return {
                restrict: 'A',
                link: function (scope, el, attrs) {
                    var opts = scope.$eval(attrs.uiTabs) || {};

                    var navs = el.findAll('li');
                    var contents = el.siblings('.tabs');

                    if (!navs.length || !contents.length || navs.length != contents.length) {
                        log.i('Compile ui-tabs failed : tabs length not match!');
                        return;
                    }

                    navs.findAll('a').click(function (e) {
                        e.preventDefault();
                        e.stopPropagation();

                        var navLinkLl = navs.findAll('a');
                        var index = navLinkLl.index(this);

                        var triggerIndex = navs.index(navs.filter('.active'));

                        var flag = true;
                        if (opts.beforeFn) {
                            var fnTarget = $parse(opts.beforeFn)(scope);
                            if (fnTarget) {
                                if (opts.digest) {
                                    safeApply(scope, function () {
                                        flag = fnTarget(index, triggerIndex);
                                    });
                                } else {
                                    flag = fnTarget(index, triggerIndex);
                                }
                            }
                        }
                        if (!flag)
                            return;

                        navLinkLl.not(':eq(' + index + ')').parent().removeClass('active');
                        navLinkLl.eq(index).parent().addClass('active');

                        // tips off
                        var lastVisitedContent = contents.not(':eq(' + index + ')').filter('.active');
                        tips.offInContext(lastVisitedContent);

                        contents.not(':eq(' + index + ')').removeClass('active');

                        var targetPane = contents.eq(index);
                        targetPane.addClass('active');

                        // if link delay
                        var isLinkDelay = targetPane.attr('is-link');
                        if (isLinkDelay) {
                            (function () {
                                var tplEl = targetPane.findAll('script').eq(0);
                                if (!tplEl.length)
                                    return;

                                var inner = targetPane.findAll('.tpl');
                                // compile only once
                                if (isLinkDelay !== 'repeat' && inner.length)
                                    return;

                                // empty div first
                                inner.remove();

                                // compile and link
                                var compiledEl = $compile(tplEl.html())(scope);
                                compiledEl.addClass('tpl').appendTo(targetPane);
                            })();
                        }
                        safeApply(scope, function () {
                            scope.$broadcast('TabFocus', index);
                        });

                        return false;
                    });

                    // trigger first
                    navs.findAll('a').eq(opts.targetIndex || 0).trigger('click');
                }
            };
        }]);

    // key enter
    md.directive('uiEnter', function () {
        return {
            restrict: 'A',
            link: function (scope, el, attrs) {
                el.keyup(function (e) {
                    // return
                    if (13 != e.keyCode)
                        return;

                    scope.$apply(function () {
                        scope.$eval(attrs.uiEnter);
                    });
                });
            }
        };
    });

    // validation -> do not watch $validity/$required (binding ng-show etc.), use tips instead
    // *** *** *** *** *** *** *** *** *** ***
    // *** *** *** *** *** *** *** *** *** ***
    md.directive('uiValid', ['$parse', 'conf', 'uiLog', 'uiValid', 'uiTips', function ($parse, conf, log, valid, tips) {
        var uiValidAttrIdName = 'ui-valid-id';
        var uiValidRefer = {};
        return {
            restrict: 'A',
            require: 'ngModel',
            link: function (scope, el, attrs, ctrl) {
                // add guid to this element
                var validId = el.attr(uiValidAttrIdName);
                if (!validId) {
                    validId = Math.guid();
                    el.attr(uiValidAttrIdName, validId);
                }

                var getRules = function () {
                    return attrs.uiValid;
                };

                // require not show tips
                var notHoverShow = 'true' == attrs.uiValidNotHover;

                var lastOldRules;
                var validFn = function (value, oldRules) {
                    var sp = '__';

                    var rules = getRules();
                    var r = valid.check(value, rules, scope, attrs.uiValidTips, {thisModel: attrs.ngModel});

                    if (lastOldRules && !oldRules)
                        oldRules = lastOldRules;

                    if (r.flag && oldRules) {
                        rules = rules ? rules + ' ' + oldRules : oldRules;
                    }

                    if (rules) {
                        // set form $error
                        var arrInner = _.unique(rules.split(' '));
                        var i = 0;
                        for (; i < arrInner.length; i++) {
                            var oneRule = arrInner[i];
                            if (!oneRule.trim())
                                continue;
                            ctrl.$setValidity(attrs.ngModel + sp + oneRule, r.flag ? true : oneRule != r.rule);
                        }
                    }

                    if (!r.flag) {
                        tips.on(el, r.msg, notHoverShow && 'r' == r.rule);
                    } else {
                        tips.off(el);
                    }
                    return r.flag;
                };

                var init = function () {
                    var rules = getRules();
                    log.i('Init valid : ' + attrs.ngModel);
                    log.i(rules);

                    if (!rules)
                        return;

                    // clear ctrl.$parsers, use uiTips.on/off instead $watch form's $error, as ng-show effect layout
                    // do not use angluar valid function (in $parse array)
                    // tips: do not use email/url directives provided by angular
                    ctrl.$parsers.splice(0, ctrl.$parsers.length);
                    ctrl.$formatters.splice(0, ctrl.$formatters.length);

                    ctrl.$parsers.unshift(function (value) {
                        return validFn(value) ? value : undefined;
                    });

                    // set model value directly need not validate again unless ctrl.$invalid === true
                    ctrl.$formatters.unshift(function (value) {
                        if (value !== undefined &&
                            (ctrl.$invalid || el.hasClass(conf.invalidClass))) {
                            validFn(value);
                        }
                        return value;
                    });
                };

                // validation relative to other model
                // if rules is dynamical, make sure that rules first set has target model declaration
                // because bellow block only run once
                var rules = getRules();
                if (rules) {
                    var arr = rules.split(' ');
                    var watchedLl = [];

                    // it sucks...
                    var i = 0;
                    for (; i < arr.length; i++) {
                        if (!arr[i].contains(':'))
                            continue;

                        var ruleArr = arr[i].split(':');
                        if (!['num', 'date', 'watch'].contains(ruleArr[0]))
                            continue;

                        // eg. num:range:targetModelName/date:range:targetModelName/watch:targetModelName1,targetModelName2
                        var modelName = ruleArr['watch' == ruleArr[0] ? 1 : 2];
                        var modelArr = modelName.split(/,/);
                        var j = 0;
                        for (; j < modelArr.length; j++) {
                            var targetModelName = modelArr[j];
                            // already watched
                            if (watchedLl.contains(targetModelName))
                                continue;

                            log.i('Add watch for valid check : ' + targetModelName);
                            scope.$watch(targetModelName, function () {
                                /*
								if you do not want to valid if it's not dirty, add function bellow:
								valid.filterWatchValid = function(ctrl){
									return ctrl.$dirty;
								};
								*/
                                if ((valid.filterWatchValid && valid.filterWatchValid(ctrl, attrs)) ||
                                    !valid.filterWatchValid) {
                                    // valid again
                                    ctrl.$setViewValue(ctrl.$viewValue);
                                }
                            }, true);
                            watchedLl.push(targetModelName);
                            uiValidRefer[attrs.ngModel] = targetModelName + '|' + ruleArr[0];
                        }// \for inner
                    }// \for outer
                }

                // Watch for changes to the directives options
                // if validation rules change, initialize again
                scope.$watch(getRules, function (newRules, oldRules) {
                    init();

                    oldRules = oldRules || '';
                    if (lastOldRules)
                        oldRules += ' ' + lastOldRules;

                    lastOldRules = oldRules;

                    // not bind yet (validate failed or first initialization) include ngModelController initialize value : NaN
                    if (ctrl.$modelValue === undefined ||
                        ctrl.$modelValue === null ||
                        ctrl.$modelValue !== ctrl.$modelValue) {
                        // bind failed
                        // check tips has showed
                        var needValid = false;

                        if (el.hasClass(conf.invalidClass)) {
                            needValid = true;
                        }

                        if (!needValid) {
                            // NaN need not valid
                            // null need valid (ctrl.$invalid || ctrl.$viewValue === null)
                            var isValNaN = ctrl.$viewValue !== ctrl.$viewValue;
                            if (ctrl.$invalid ||
                                (ctrl.$viewValue !== undefined && !isValNaN)) {
                                needValid = true;
                            }
                        }

                        if (needValid) {
                            ctrl.$setViewValue(ctrl.$viewValue);
                        }
                    } else {
                        if (!ctrl.$dirty && attrs.dirtyCheck) {
                            log.i('Skip valid if need not check when undirty...');
                        } else {
                            validFn(ctrl.$modelValue, oldRules);
                        }
                    }
                }, true);
            }
        };
    }]);

    // pagination model helper
    md.factory('uiPager', ['conf', function (conf) {
        return {
            gen: function (pager, opts) {
                var pagi = {};
                if (!pager) {
                    pagi.totalPageLl = [];
                    pagi.totalPage = 0;
                    pagi.totalCount = 0;
                    pagi.pageNum = 0;
                    pagi.pageSize = 0;

                    pagi.style = {};
                    pagi.style.btnClass = conf.defaultPagiBtnClass;

                    // first previous next last / buttons disabled
                    pagi.ctrl = {};
                    pagi.ctrl.isChoosePageDisabled = true;
                    pagi.ctrl.isFirstPageDisabled = true;
                    pagi.ctrl.isPrevPageDisabled = true;
                    pagi.ctrl.isNextPageDisabled = true;
                    pagi.ctrl.isLastPageDisabled = true;

                    return pagi;
                }

                // current page
                pagi.pageNum = pager.pageNum || 0;
                // number per page
                pagi.pageSize = pager.pageSize || 10;
                pagi.totalCount = pager.totalCount || 0;
                // no records
                if (!pagi.totalCount)
                    pagi.pageNum = 0;

                pagi.totalPage = this.getTotalPage(pagi.totalCount, pagi.pageSize);
                pagi.totalPageLl = this.getTotalPageLl(pagi.totalPage, pagi.pageNum, opts);

                pagi.targetPageChoosed = _.find(pagi.totalPageLl, function (it) {
                    return it.pageNum == pagi.pageNum;
                });

                pagi.style = {};
                pagi.style.btnClass = conf.defaultPagiBtnClass;

                // first previous next last
                pagi.ctrl = {};
                pagi.ctrl.isFirstPageDisabled = pagi.totalCount == 0 || pagi.pageNum == 1;
                pagi.ctrl.isPrevPageDisabled = pagi.pageNum <= 1;
                pagi.ctrl.isNextPageDisabled = pagi.pageNum == pagi.totalPage;
                pagi.ctrl.isLastPageDisabled = pagi.totalPage <= 1 || pagi.pageNum == pagi.totalPage;
                pagi.ctrl.isChoosePageDisabled = pagi.totalPage <= 1;

                return pagi;
            },

            refresh: function (pagi) {
                pagi.totalPage = this.getTotalPage(pagi.totalCount, pagi.pageSize);
                pagi.totalPageLl = this.getTotalPageLl(pagi.totalPage, pagi.pageNum);
            },

            getTotalPageLl: function (totalPage, pageNum, opts) {
                return _.map(_.range(1, totalPage + 1), function (it) {
                    var pagiBtnClass = conf.defaultPagiBtnClass;
                    if (opts && opts.defaultBtnClass)
                        pagiBtnClass = opts.defaultBtnClass;

                    // model with button style
                    // use ng-repeat to render different buttons
                    var one = {};
                    one.btnClass = pagiBtnClass;
                    one.pageNum = it;

                    if (pageNum == it) {
                        one.btnClass = conf.defaultPagiCurrentBtnClass;
                        if (opts && opts.currentBtnClass)
                            one.btnClass = opts.currentBtnClass;
                    }

                    return one;
                });
            },

            getTotalPage: function (totalCount, pageSize) {
                var r = totalCount % pageSize;
                var r2 = totalCount / pageSize;
                var result = r == 0 ? r2 : r2 + 1;
                return Math.floor(result);
            }
        };
    }]);

    // pagination view
    md.directive('uiPagi', ['uiPager', function (pager) {
        return {
            restrict: 'A',

            template: '<div style="display: inline-block; margin-right: 4px; font-size: 13px;">' +
                '	<span style="color: blue; margin-right: 6px;">Total Page: ' +
                '		<strong class="colorred">{{pagi.totalPage}}</strong>' +
                '	</span>' +
                '	<span style="color: blue; margin-right: 6px;">This Page: ' +
                '		<strong class="colorred">{{pagi.pageNum}}</strong>' +
                '	</span>' +
                '	<span style="color: blue; margin-right: 6px;">Total Count: ' +
                '		<strong class="colorred">{{pagi.totalCount}}</strong>' +
                '	</span>' +
                '	<span style="color: blue; margin-right: 6px;">Go to Page: </span>' +
                '		<span><select ' +
                '			style="width: 60px;" ' +
                '			ng-disabled="pagi.ctrl.isChoosePageDisabled" ' +
                '			ng-change="onChangePage({cp: pagi.targetPageChoosed.pageNum, event: $event})" ' +
                '			ng-model="pagi.targetPageChoosed" ' +
                '			ng-options="one.pageNum for one in pagi.totalPageLl"></select>' +
                '	</span>' +
                '</div>' +
                '<div style="display: inline-block;">' +
                '	<div class="btn-group">' +
                '		<button class="{{pagi.style.btnClass}}" ng-click="onChangePage({cp: 1, event: $event})" ng-disabled="pagi.ctrl.isFirstPageDisabled">First Page</button>' +
                '		<button class="{{pagi.style.btnClass}}" ng-click="onChangePage({cp: pagi.pageNum - 1, event: $event})" ng-disabled="pagi.ctrl.isPrevPageDisabled">Pre Page</button>' +
                '		<button class="{{pagi.style.btnClass}}" ng-click="onChangePage({cp: pagi.pageNum + 1, event: $event})" ng-disabled="pagi.ctrl.isNextPageDisabled">Next Page</button>' +
                '		<button class="{{pagi.style.btnClass}}" ng-click="onChangePage({cp: pagi.totalPage, event: $event})" ng-disabled="pagi.ctrl.isLastPageDisabled">Last Page</button>' +
                '	</div>' +
                '</div>',

            replace: false,

            scope: {
                pager: '=',

                onChangePage: '&'
            },

            link: function (scope, el, attrs) {
                var opts = scope.$eval(attrs.uiPagi) || {};
                scope.$watch('pager', function (it) {
                    scope.pagi = pager.gen(it, opts);
                }, true);
            }
        };
    }]);

    // sort
    md.directive('uiSort', ['uiLog', '$parse', function (log, $parse) {
        'use strict';
        return {
            restrict: 'A',
            link: function (scope, el, attrs, ctrl) {
                var nodeName = el[0].nodeName;
                if ('TD' != nodeName && 'TH' != nodeName) {
                    log.w('Sort bind failed : not a TD/TH element!');
                    return;
                }

                var opts = scope.$eval(attrs.uiSort) || {};
                log.i('Init sort : ');
                log.i(opts);

                if (!opts.targetModel && !opts.fn) {
                    log.w('Init sort fail : targetModel or fn required!');
                    return;
                }
                el.append('<i></i>');
                el.addClass('ng-ui-sort-all');

                var sortModel = function (isUp) {
                    if (opts.fn) {
                        var fn = $parse(opts.fn)(scope);
                        if (fn) {
                            scope.$apply(function () {
                                fn(isUp);
                            });
                        }

                        return;
                    }

                    var targetModel = opts.targetModel;
                    var getter = $parse(targetModel);
                    var model = getter(scope);
                    if (!model || !ag.isArray(model)) {
                        log.w('Event trigger sort fail : targetModel required and must be a list!');
                        return;
                    }

                    var fnCompareCallback;
                    if (opts.fnCompare) {
                        var getterCompare = $parse(opts.fnCompare);
                        fnCompareCallback = getterCompare(scope);
                    }

                    var sortedModel;
                    // use string localeCompare
                    if (opts.sortLocale) {
                        var fnSortLocale = function (a, b) {
                            if (!opts.field)
                                return 0;
                            if (!a)
                                return -1;
                            if (!b)
                                return 1;

                            var val1 = a[opts.field];
                            var val2 = b[opts.field];

                            if (!ag.isString(val1))
                                val1 = '' + val1;
                            if (!ag.isString(val2))
                                val2 = '' + val2;

                            return val1.localeCompare(val2);
                        };
                        model.sort(function (a, b) {
                            return isUp ? fnSortLocale(a, b) : fnSortLocale(b, a);
                        });
                        sortedModel = model;
                    } else {
                        sortedModel = _.sortBy(model, function (it, index) {
                            if (fnCompareCallback) {
                                return fnCompareCallback(it, index, isUp);
                            } else {
                                if (!opts.field) {
                                    return 0;
                                } else {
                                    var val = it[opts.field];
                                    if (!val) {
                                        return 0;
                                    } else if (ag.isDate(val)) {
                                        return isUp ? val.getTime() : (0 - val.getTime());
                                    } else if (ag.isNumber(val)) {
                                        return isUp ? val : (0 - val);
                                    } else if (ag.isString(val)) {
                                        try {
                                            var intVal = parseFloat(val);
                                            return isUp ? intVal : (0 - intVal);
                                        } catch (e) {
                                            log.e(e);
                                            return 0;
                                        }
                                    } else {
                                        return 0;
                                    }
                                }
                            }
                        });
                    }
                    scope.$apply(function () {
                        var setter = getter.assign;
                        setter(scope, sortedModel);
                    });
                };

                var resetSortedClass = function (element, suf1, suf2, addedSuf3) {
                    var pre = 'ng-ui-sort-';
                    element.removeClass(pre + suf1).removeClass(pre + suf2).addClass(pre + addedSuf3);
                };

                var eventTriggerType = opts.eventTriggerType || 'click';
                el.unbind(eventTriggerType).bind(eventTriggerType, function (e) {
                    e.preventDefault();

                    var isUp = !$(this).hasClass('ng-ui-sort-down');
                    sortModel(isUp);

                    resetSortedClass(el, 'all', isUp ? 'up' : 'down', isUp ? 'down' : 'up');

                    // reset others' style
                    var others = el.siblings('td,th').filter('.ng-ui-sort-down,.ng-ui-sort-up');
                    resetSortedClass(others, 'up', 'down', 'all');

                    return false;
                });
            }
        };
    }]);
})(angular);
(function (global) {
    var moduleName = 'ng.ext.uploadify';
    var md = angular.module(moduleName, []);

    md.directive('extUploadify', ['$parse', function ($parse) {
        return {
            restrict: 'A',

            // begin link ***
            link: function (scope, el, attrs) {
                var opts = scope.$eval(attrs.extUploadify) || {};
                console.log('Init uploadify : ');
                console.log(JSON.stringify(opts));

                if (!opts.uploader) {
                    console.log('Parameter required : uploader!');
                    return;
                }
                // get absolute url path
                opts.uploader = Consts.getAppPath(opts.uploader);

                // default parameters
                var props = {
                    auto: true, multi: false, width: 80, height: 25,
                    buttonText: 'Choose file', fileObjName: 'file', debug: false, preventCaching: false
                };
                props.swf = Consts.getAppPath('ng-ext/uploadify/uploadify.swf');

                // 重新设置formData
                props.onUploadStart = function (file) {
                    if (opts.paramsModel) {
                        var formData = $parse(opts.paramsModel)(scope);
                        el.uploadify('settings', 'formData', formData);
                    }
                };

                props.onUploadError = function (file, errorCode, errorMsg, errorString) {
                    console.log('Upload error : ' + errorCode);
                    console.log(errorMsg);
                    console.log(errorString);
                };

                props.onUploadSuccess = function (file, data, response) {
                    console.log(file);
                    console.log(data);
                    if (opts.fnSuccess) {
                        var jsonObj = '';
                        if (data) {
                            try {
                                jsonObj = JSON.parse(data);
                            } catch (e) {
                                console.error(e);
                            }
                        }

                        var getter = $parse(opts.fnSuccess);
                        var fnTarget = getter(scope);
                        if (fnTarget) {
                            scope.$apply(function () {
                                fnTarget(file, jsonObj, response);
                            });
                        }
                    }
                };

                angular.extend(props, opts);

                console.log(JSON.stringify(props));
                el.uploadify(props);
            } // end link
        };
    }]);
})(this);
