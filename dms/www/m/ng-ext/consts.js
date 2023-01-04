(function () {
    if (!Date.prototype.format) {
        Date.prototype.format = function (pat) {
            var year = this.getFullYear();
            var month = this.getMonth() + 1;
            var day = this.getDate();
            var hour = this.getHours();
            var minute = this.getMinutes();
            var second = this.getSeconds();
            month = month > 9 ? month : '0' + month;
            day = day > 9 ? day : '0' + day;
            hour = hour > 9 ? hour : '0' + hour;
            minute = minute > 9 ? minute : '0' + minute;
            second = second > 9 ? second : '0' + second;
            if (!pat) {
                pat = 'yyyy-MM-dd HH:mm:ss';
            }
            pat = pat.replace(/yyyy/g, year);
            pat = pat.replace(/MM/g, month);
            pat = pat.replace(/dd/g, day);
            pat = pat.replace(/HH/gi, hour);
            pat = pat.replace(/mm/g, minute);
            pat = pat.replace(/ss/g, second);
            return pat;
        };
    }

    if (!Date.prototype.formatTime) {
        Date.prototype.formatTime = function (pat) {
            pat = pat || '00:00:00';
            var arr = pat.split(':');
            var hh = arr[0];
            var mm = arr.length > 1 ? arr[1] : 0;
            var ss = arr.length > 2 ? arr[2] : 0;

            if ('HH' != hh) {
                this.setHours(hh);
            }
            if ('mm' != mm) {
                this.setMinutes(mm);
            }
            if ('ss' != ss) {
                this.setSeconds(ss);
            }
            return this;
        };
    }

    if (!Date.prototype.diff) {
        // diff by day
        Date.prototype.diff = function (date) {
            return Math.ceil((this - date) / (1000 * 60 * 60 * 24));
        };
    }

    if (!Date.prototype.add) {
        // add days
        Date.prototype.add = function (days) {
            return new Date(this.getTime() + days * (1000 * 60 * 60 * 24));
        };
    }

    if (!Date.prototype.addMonth) {
        // add months
        Date.prototype.addMonth = function (months) {
            months = months || 1;
            var tmpDate = this.getDate();
            this.setMonth(this.getMonth() + months);
            // 2-28 -> 2-29
            if (tmpDate != this.getDate()) {
                this.setDate(0);
            }
            return this;
        };
    }

    if (!Date.prototype.addYear) {
        // add years
        Date.prototype.addYear = function (years) {
            years = years || 1;
            var tmpDate = this.getDate();
            this.setYear(this.getFullYear() + years);
            if (tmpDate != this.getDate()) {
                this.setDate(0);
            }
            return this;
        };
    }

    if (!Date.parse2) {
        // string 'yyyy-MM-dd HH:mm:ss' -> date
        Date.parse2 = function (str, sepDate, sepTime) {
            if (!str)
                return null;

            // typeof str === number
            if (typeof str === 'number')
                return new Date(str);

            sepDate = sepDate || '-';
            sepTime = sepTime || ':';

            var arr = str.split(' ');
            var ymd = arr[0];
            var arrYmd = ymd.split(sepDate);

            if (arrYmd.length != 3) {
                return null;
            }

            var dat = new Date(2000, 0, 1);
            dat.setFullYear(parseInt(arrYmd[0], 10));
            dat.setMonth(parseInt(arrYmd[1], 10) - 1);
            dat.setDate(parseInt(arrYmd[2], 10));

            if (arr.length > 1) {
                var hms = arr[1];
                var arrHms = hms.split(sepTime);

                dat.setHours(parseInt(arrHms[0], 10));
                if (arrHms.length > 1)
                    dat.setMinutes(parseInt(arrHms[1], 10));
                if (arrHms.length > 2)
                    dat.setSeconds(parseInt(arrHms[2], 10));
            } else {
                dat.setHours(0);
                dat.setMinutes(0);
                dat.setSeconds(0);
            }

            return dat;
        };
    }

    if (!Date.isDateValid) {
        Date.isDateValid = function (str, sep) {
            sep = sep || '-';
            var arr = str.split(sep);
            // yyyy-MM-dd
            if (arr.length != 3)
                return false;

            if (arr[0].length > 4 || arr[1].length > 2 || arr[2].length > 2)
                return false;

            var year = parseInt(arr[0], 10);
            var month = parseInt(arr[1], 10);
            var day = parseInt(arr[2], 10);

            if (isNaN(year) || isNaN(month) || isNaN(day))
                return false;

            var maxYear = 9999;
            var minYear = 0;
            if (year < minYear || year > maxYear)
                return false;

            if (month < 1 || month > 12)
                return false;

            var dayInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];

            var maxDayInMonth = dayInMonth[month - 1];

            var isLeapYear = 0 == +year % 4 && ((+year % 100 != 0) || (+year % 400 == 0));
            if (2 == month && isLeapYear)
                maxDayInMonth = 29;

            if (day < 1 || day > maxDayInMonth)
                return false;

            return true;
        };
    }

    if (!Date.isTimeValid) {
        Date.isTimeValid = function (str, sep) {
            sep = sep || ':';

            var arr = str.split(sep);
            // HH:mm:ss
            if (arr.length > 3)
                return false;

            if (arr[0].length > 2)
                return false;
            if (arr.length > 1 && arr[1].length > 2)
                return false;
            if (arr.length > 2 && arr[2].length > 2)
                return false;

            if (str.endsWith(sep))
                return false;


            var hour = parseInt(arr[0], 10);
            var minute = arr.length > 1 ? parseInt(arr[1], 10) : 0;
            var second = arr.length > 2 ? parseInt(arr[2], 10) : 0;

            if (isNaN(hour) || isNaN(minute) || isNaN(second))
                return false;

            if (hour < 0 || hour > 23)
                return false;
            if (minute < 0 || minute > 59)
                return false;
            if (second < 0 || second > 59)
                return false;

            return true;
        };
    }

    if (!Date.isDateTimeValid) {
        Date.isDateTimeValid = function (str, sepDate, sepTime) {
            var arr = str.split(' ');
            var isDateValid = Date.isDateValid(arr[0], sepDate);
            if (!isDateValid)
                return false;

            if (arr.length > 1) {
                return Date.isTimeValid(arr[1], sepTime);
            } else {
                return true;
            }
        };
    }

    if (!Math.guid) {
        // guid
        Math.guid = function () {
            var guid = '';
            var i = 1;
            for (; i <= 32; i++) {
                var n = Math.floor(Math.random() * 16.0).toString(16);
                guid += n;
                if (i == 8 || i == 12 || i == 16 || i == 20) {
                    guid += '-';
                }
            }
            return guid;
        };
    }

    if (!String.prototype.trim) {
        String.prototype.trim = function () {
            return this.replace(/(^\s*)|(\s*$)/g, '');
        };
    }

    if (!String.prototype.format) {
        String.prototype.format = function () {
            var args = Array.prototype.slice.call(arguments);
            return this.replace(/\{(\d+)\}/g,
                function (m, i) {
                    return args[i];
                });
        };
    }

    if (!String.prototype.contains) {
        String.prototype.contains = function (sub) {
            return this.indexOf(sub) !== -1;
        };
    }

    if (!String.prototype.charlen) {
        String.prototype.charlen = function () {
            var arr = this.match(/[^\x00-\xff]/ig);
            return this.length + (arr === null ? 0 : arr.length);
        };
    }

    if (!String.prototype.startsWith) {
        String.prototype.startsWith = function (str) {
            return this.indexOf(str) === 0;
        };
    }

    if (!String.prototype.endsWith) {
        String.prototype.endsWith = function (str) {
            var index = this.lastIndexOf(str);
            return index !== -1 && index + str.length === this.length;
        };
    }

    if (!Array.prototype.contains) {
        Array.prototype.contains = function (el) {
            var i;
            for (i = 0; i < this.length; i++) {
                if (this[i] === el) {
                    return true;
                }
            }
            return false;
        };
    }

    if (!Array.prototype.indexOf) {
        Array.prototype.indexOf = function (val, field) {
            var i = 0;
            for (; i < this.length; i++) {
                if (this[i] && (field ? this[i][field] === val : this[i] === val)) {
                    return i;
                }
            }
            return -1;
        };
    }

    if (!Array.prototype.lastIndexOf) {
        Array.prototype.lastIndexOf = function (val, field) {
            var max = -1;
            var i = 0;
            for (; i < this.length; i++) {
                if (this[i] && (field ? this[i][field] == val : this[i] == val)) {
                    max = i;
                }
            }
            return max;
        };
    }

    if (!Array.prototype.merge) {
        Array.prototype.merge = function (arr) {
            if (arr) {
                var i;
                for (i = 0; i < arr.length; i++) {
                    this.push(arr[i]);
                }
            }
            return this;
        };
    }

    if (!Array.prototype.remove) {
        Array.prototype.remove = function (one) {
            var index = -1;
            for (var i = 0; i < this.length; i++) {
                if (this[i] == one) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                this.splice(index, 1);
            }
        };
    }
})();

// global framework configuration
(function (global) {
    var appnameLocal = global.appname == null ? '' : global.appname;

    var conf = {
        version: '2.0',

        // framework log level
        logLevel: 'INFO',

        appname: appnameLocal,
        context: appnameLocal + '/ng-ext/',

        getAppPath: function (suf) {
            var mid = '/';
            if (suf && suf.indexOf('/') === 0)
                mid = '';
            return this.appname + mid + suf;
        },

        dump: ''
    };
    global.Consts = conf;
})(this);

// template generate
(function (global) {
    var easyTemplate = function (s, d) {
        if (!s) {
            return '';
        }
        if (s !== easyTemplate.template) {
            easyTemplate.template = s;
            easyTemplate.aStatement = easyTemplate
                .parsing(easyTemplate.separate(s));
        }
        var aST = easyTemplate.aStatement;
        var process = function (d2) {
            if (d2) {
                d = d2;
            }
            return arguments.callee;
        };
        process.toString = function () {
            return (new Function(aST[0], aST[1]))(d);
        };
        return process;
    };
    easyTemplate.separate = function (s) {
        var r = /\\'/g;
        var sRet = s.replace(
            /(<(\/?)#(.*?(?:\(.*?\))*)>)|(')|([\r\n\t])|(\$\{([^\}]*?)\})/g,
            function (a, b, c, d, e, f, g, h) {
                if (b) {
                    return '{|}' + (c ? '-' : '+') + d + '{|}';
                }
                if (e) {
                    return '\\\'';
                }
                if (f) {
                    return '';
                }
                if (g) {
                    return '\'+(' + h.replace(r, '\'') + ' || \'\')+\'';
                }
            });
        return sRet;
    };
    easyTemplate.parsing = function (s) {
        var mName, vName, sTmp, aTmp, sFL, sEl, aList, aStm = ['var aRet = [];'];
        aList = s.split(/\{\|\}/);
        var r = /\s/;
        while (aList.length) {
            sTmp = aList.shift();
            if (!sTmp) {
                continue;
            }
            sFL = sTmp.charAt(0);
            if (sFL !== '+' && sFL !== '-') {
                sTmp = '\'' + sTmp + '\'';
                aStm.push('aRet.push(' + sTmp + ');');
                continue;
            }
            aTmp = sTmp.split(r);
            switch (aTmp[0]) {
                case '+macro':
                    mName = aTmp[1];
                    vName = aTmp[2];
                    //			aStm.push('aRet.push("<!--' + mName + ' start--\>");');
                    break;
                case '-macro':
                    //			aStm.push('aRet.push("<!--' + mName + ' end--\>");');
                    break;
                case '+if':
                    aTmp.splice(0, 1);
                    aStm.push('if' + aTmp.join(' ') + '{');
                    break;
                case '+elseif':
                    aTmp.splice(0, 1);
                    aStm.push('}else if' + aTmp.join(' ') + '{');
                    break;
                case '-if':
                    aStm.push('}');
                    break;
                case '+else':
                    aStm.push('}else{');
                    break;
                case '+list':
                    aStm.push('if(' + aTmp[1] + '.constructor === Array){with({i:0,l:'
                        + aTmp[1] + '.length,' + aTmp[3] + '_index:0,' + aTmp[3]
                        + ':null}){for(i=l;i--;){' + aTmp[3] + '_index=(l-i-1);'
                        + aTmp[3] + '=' + aTmp[1] + '[' + aTmp[3] + '_index];'
                        + 'if(!' + aTmp[3] + '){break;}');
                    break;
                case '-list':
                    aStm.push('}}}');
                    break;
                default:
                    break;
            }
        }
        aStm.push('return aRet.join("");');
        if (!vName) {
            aStm.unshift('var data = arguments[0];');
        }
        return [vName, aStm.join('')];
    };

    global.Consts.format = function (txt, data) {
        return easyTemplate(txt, data).toString();
    };
})(this);
