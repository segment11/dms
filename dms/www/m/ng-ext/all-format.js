!function () {
    var n = this, t = n._, r = {}, e = Array.prototype, u = Object.prototype, i = Function.prototype, a = e.push,
        o = e.slice, c = e.concat, l = u.toString, f = u.hasOwnProperty, s = e.forEach, p = e.map, v = e.reduce,
        h = e.reduceRight, d = e.filter, g = e.every, m = e.some, y = e.indexOf, b = e.lastIndexOf, x = Array.isArray,
        _ = Object.keys, w = i.bind, j = function (n) {
            return n instanceof j ? n : this instanceof j ? (this._wrapped = n, void 0) : new j(n)
        };
    "undefined" != typeof exports ? ("undefined" != typeof module && module.exports && (exports = module.exports = j), exports._ = j) : n._ = j, j.VERSION = "1.5.1";
    var A = j.each = j.forEach = function (n, t, e) {
        if (null != n) if (s && n.forEach === s) n.forEach(t, e); else if (n.length === +n.length) {
            for (var u = 0, i = n.length; i > u; u++) if (t.call(e, n[u], u, n) === r) return
        } else for (var a in n) if (j.has(n, a) && t.call(e, n[a], a, n) === r) return
    };
    j.map = j.collect = function (n, t, r) {
        var e = [];
        return null == n ? e : p && n.map === p ? n.map(t, r) : (A(n, function (n, u, i) {
            e.push(t.call(r, n, u, i))
        }), e)
    };
    var E = "Reduce of empty array with no initial value";
    j.reduce = j.foldl = j.inject = function (n, t, r, e) {
        var u = arguments.length > 2;
        if (null == n && (n = []), v && n.reduce === v) return e && (t = j.bind(t, e)), u ? n.reduce(t, r) : n.reduce(t);
        if (A(n, function (n, i, a) {
            u ? r = t.call(e, r, n, i, a) : (r = n, u = !0)
        }), !u) throw new TypeError(E);
        return r
    }, j.reduceRight = j.foldr = function (n, t, r, e) {
        var u = arguments.length > 2;
        if (null == n && (n = []), h && n.reduceRight === h) return e && (t = j.bind(t, e)), u ? n.reduceRight(t, r) : n.reduceRight(t);
        var i = n.length;
        if (i !== +i) {
            var a = j.keys(n);
            i = a.length
        }
        if (A(n, function (o, c, l) {
            c = a ? a[--i] : --i, u ? r = t.call(e, r, n[c], c, l) : (r = n[c], u = !0)
        }), !u) throw new TypeError(E);
        return r
    }, j.find = j.detect = function (n, t, r) {
        var e;
        return O(n, function (n, u, i) {
            return t.call(r, n, u, i) ? (e = n, !0) : void 0
        }), e
    }, j.filter = j.select = function (n, t, r) {
        var e = [];
        return null == n ? e : d && n.filter === d ? n.filter(t, r) : (A(n, function (n, u, i) {
            t.call(r, n, u, i) && e.push(n)
        }), e)
    }, j.reject = function (n, t, r) {
        return j.filter(n, function (n, e, u) {
            return !t.call(r, n, e, u)
        }, r)
    }, j.every = j.all = function (n, t, e) {
        t || (t = j.identity);
        var u = !0;
        return null == n ? u : g && n.every === g ? n.every(t, e) : (A(n, function (n, i, a) {
            return (u = u && t.call(e, n, i, a)) ? void 0 : r
        }), !!u)
    };
    var O = j.some = j.any = function (n, t, e) {
        t || (t = j.identity);
        var u = !1;
        return null == n ? u : m && n.some === m ? n.some(t, e) : (A(n, function (n, i, a) {
            return u || (u = t.call(e, n, i, a)) ? r : void 0
        }), !!u)
    };
    j.contains = j.include = function (n, t) {
        return null == n ? !1 : y && n.indexOf === y ? n.indexOf(t) != -1 : O(n, function (n) {
            return n === t
        })
    }, j.invoke = function (n, t) {
        var r = o.call(arguments, 2), e = j.isFunction(t);
        return j.map(n, function (n) {
            return (e ? t : n[t]).apply(n, r)
        })
    }, j.pluck = function (n, t) {
        return j.map(n, function (n) {
            return n[t]
        })
    }, j.where = function (n, t, r) {
        return j.isEmpty(t) ? r ? void 0 : [] : j[r ? "find" : "filter"](n, function (n) {
            for (var r in t) if (t[r] !== n[r]) return !1;
            return !0
        })
    }, j.findWhere = function (n, t) {
        return j.where(n, t, !0)
    }, j.max = function (n, t, r) {
        if (!t && j.isArray(n) && n[0] === +n[0] && n.length < 65535) return Math.max.apply(Math, n);
        if (!t && j.isEmpty(n)) return -1 / 0;
        var e = {computed: -1 / 0, value: -1 / 0};
        return A(n, function (n, u, i) {
            var a = t ? t.call(r, n, u, i) : n;
            a > e.computed && (e = {value: n, computed: a})
        }), e.value
    }, j.min = function (n, t, r) {
        if (!t && j.isArray(n) && n[0] === +n[0] && n.length < 65535) return Math.min.apply(Math, n);
        if (!t && j.isEmpty(n)) return 1 / 0;
        var e = {computed: 1 / 0, value: 1 / 0};
        return A(n, function (n, u, i) {
            var a = t ? t.call(r, n, u, i) : n;
            a < e.computed && (e = {value: n, computed: a})
        }), e.value
    }, j.shuffle = function (n) {
        var t, r = 0, e = [];
        return A(n, function (n) {
            t = j.random(r++), e[r - 1] = e[t], e[t] = n
        }), e
    };
    var F = function (n) {
        return j.isFunction(n) ? n : function (t) {
            return t[n]
        }
    };
    j.sortBy = function (n, t, r) {
        var e = F(t);
        return j.pluck(j.map(n, function (n, t, u) {
            return {value: n, index: t, criteria: e.call(r, n, t, u)}
        }).sort(function (n, t) {
            var r = n.criteria, e = t.criteria;
            if (r !== e) {
                if (r > e || r === void 0) return 1;
                if (e > r || e === void 0) return -1
            }
            return n.index < t.index ? -1 : 1
        }), "value")
    };
    var k = function (n, t, r, e) {
        var u = {}, i = F(null == t ? j.identity : t);
        return A(n, function (t, a) {
            var o = i.call(r, t, a, n);
            e(u, o, t)
        }), u
    };
    j.groupBy = function (n, t, r) {
        return k(n, t, r, function (n, t, r) {
            (j.has(n, t) ? n[t] : n[t] = []).push(r)
        })
    }, j.countBy = function (n, t, r) {
        return k(n, t, r, function (n, t) {
            j.has(n, t) || (n[t] = 0), n[t]++
        })
    }, j.sortedIndex = function (n, t, r, e) {
        r = null == r ? j.identity : F(r);
        for (var u = r.call(e, t), i = 0, a = n.length; a > i;) {
            var o = i + a >>> 1;
            r.call(e, n[o]) < u ? i = o + 1 : a = o
        }
        return i
    }, j.toArray = function (n) {
        return n ? j.isArray(n) ? o.call(n) : n.length === +n.length ? j.map(n, j.identity) : j.values(n) : []
    }, j.size = function (n) {
        return null == n ? 0 : n.length === +n.length ? n.length : j.keys(n).length
    }, j.first = j.head = j.take = function (n, t, r) {
        return null == n ? void 0 : null == t || r ? n[0] : o.call(n, 0, t)
    }, j.initial = function (n, t, r) {
        return o.call(n, 0, n.length - (null == t || r ? 1 : t))
    }, j.last = function (n, t, r) {
        return null == n ? void 0 : null == t || r ? n[n.length - 1] : o.call(n, Math.max(n.length - t, 0))
    }, j.rest = j.tail = j.drop = function (n, t, r) {
        return o.call(n, null == t || r ? 1 : t)
    }, j.compact = function (n) {
        return j.filter(n, j.identity)
    };
    var R = function (n, t, r) {
        return t && j.every(n, j.isArray) ? c.apply(r, n) : (A(n, function (n) {
            j.isArray(n) || j.isArguments(n) ? t ? a.apply(r, n) : R(n, t, r) : r.push(n)
        }), r)
    };
    j.flatten = function (n, t) {
        return R(n, t, [])
    }, j.without = function (n) {
        return j.difference(n, o.call(arguments, 1))
    }, j.uniq = j.unique = function (n, t, r, e) {
        j.isFunction(t) && (e = r, r = t, t = !1);
        var u = r ? j.map(n, r, e) : n, i = [], a = [];
        return A(u, function (r, e) {
            (t ? e && a[a.length - 1] === r : j.contains(a, r)) || (a.push(r), i.push(n[e]))
        }), i
    }, j.union = function () {
        return j.uniq(j.flatten(arguments, !0))
    }, j.intersection = function (n) {
        var t = o.call(arguments, 1);
        return j.filter(j.uniq(n), function (n) {
            return j.every(t, function (t) {
                return j.indexOf(t, n) >= 0
            })
        })
    }, j.difference = function (n) {
        var t = c.apply(e, o.call(arguments, 1));
        return j.filter(n, function (n) {
            return !j.contains(t, n)
        })
    }, j.zip = function () {
        for (var n = j.max(j.pluck(arguments, "length").concat(0)), t = new Array(n), r = 0; n > r; r++) t[r] = j.pluck(arguments, "" + r);
        return t
    }, j.object = function (n, t) {
        if (null == n) return {};
        for (var r = {}, e = 0, u = n.length; u > e; e++) t ? r[n[e]] = t[e] : r[n[e][0]] = n[e][1];
        return r
    }, j.indexOf = function (n, t, r) {
        if (null == n) return -1;
        var e = 0, u = n.length;
        if (r) {
            if ("number" != typeof r) return e = j.sortedIndex(n, t), n[e] === t ? e : -1;
            e = 0 > r ? Math.max(0, u + r) : r
        }
        if (y && n.indexOf === y) return n.indexOf(t, r);
        for (; u > e; e++) if (n[e] === t) return e;
        return -1
    }, j.lastIndexOf = function (n, t, r) {
        if (null == n) return -1;
        var e = null != r;
        if (b && n.lastIndexOf === b) return e ? n.lastIndexOf(t, r) : n.lastIndexOf(t);
        for (var u = e ? r : n.length; u--;) if (n[u] === t) return u;
        return -1
    }, j.range = function (n, t, r) {
        arguments.length <= 1 && (t = n || 0, n = 0), r = arguments[2] || 1;
        for (var e = Math.max(Math.ceil((t - n) / r), 0), u = 0, i = new Array(e); e > u;) i[u++] = n, n += r;
        return i
    };
    var M = function () {
    };
    j.bind = function (n, t) {
        var r, e;
        if (w && n.bind === w) return w.apply(n, o.call(arguments, 1));
        if (!j.isFunction(n)) throw new TypeError;
        return r = o.call(arguments, 2), e = function () {
            if (!(this instanceof e)) return n.apply(t, r.concat(o.call(arguments)));
            M.prototype = n.prototype;
            var u = new M;
            M.prototype = null;
            var i = n.apply(u, r.concat(o.call(arguments)));
            return Object(i) === i ? i : u
        }
    }, j.partial = function (n) {
        var t = o.call(arguments, 1);
        return function () {
            return n.apply(this, t.concat(o.call(arguments)))
        }
    }, j.bindAll = function (n) {
        var t = o.call(arguments, 1);
        if (0 === t.length) throw new Error("bindAll must be passed function names");
        return A(t, function (t) {
            n[t] = j.bind(n[t], n)
        }), n
    }, j.memoize = function (n, t) {
        var r = {};
        return t || (t = j.identity), function () {
            var e = t.apply(this, arguments);
            return j.has(r, e) ? r[e] : r[e] = n.apply(this, arguments)
        }
    }, j.delay = function (n, t) {
        var r = o.call(arguments, 2);
        return setTimeout(function () {
            return n.apply(null, r)
        }, t)
    }, j.defer = function (n) {
        return j.delay.apply(j, [n, 1].concat(o.call(arguments, 1)))
    }, j.throttle = function (n, t, r) {
        var e, u, i, a = null, o = 0;
        r || (r = {});
        var c = function () {
            o = r.leading === !1 ? 0 : new Date, a = null, i = n.apply(e, u)
        };
        return function () {
            var l = new Date;
            o || r.leading !== !1 || (o = l);
            var f = t - (l - o);
            return e = this, u = arguments, 0 >= f ? (clearTimeout(a), a = null, o = l, i = n.apply(e, u)) : a || r.trailing === !1 || (a = setTimeout(c, f)), i
        }
    }, j.debounce = function (n, t, r) {
        var e, u = null;
        return function () {
            var i = this, a = arguments, o = function () {
                u = null, r || (e = n.apply(i, a))
            }, c = r && !u;
            return clearTimeout(u), u = setTimeout(o, t), c && (e = n.apply(i, a)), e
        }
    }, j.once = function (n) {
        var t, r = !1;
        return function () {
            return r ? t : (r = !0, t = n.apply(this, arguments), n = null, t)
        }
    }, j.wrap = function (n, t) {
        return function () {
            var r = [n];
            return a.apply(r, arguments), t.apply(this, r)
        }
    }, j.compose = function () {
        var n = arguments;
        return function () {
            for (var t = arguments, r = n.length - 1; r >= 0; r--) t = [n[r].apply(this, t)];
            return t[0]
        }
    }, j.after = function (n, t) {
        return function () {
            return --n < 1 ? t.apply(this, arguments) : void 0
        }
    }, j.keys = _ || function (n) {
        if (n !== Object(n)) throw new TypeError("Invalid object");
        var t = [];
        for (var r in n) j.has(n, r) && t.push(r);
        return t
    }, j.values = function (n) {
        var t = [];
        for (var r in n) j.has(n, r) && t.push(n[r]);
        return t
    }, j.pairs = function (n) {
        var t = [];
        for (var r in n) j.has(n, r) && t.push([r, n[r]]);
        return t
    }, j.invert = function (n) {
        var t = {};
        for (var r in n) j.has(n, r) && (t[n[r]] = r);
        return t
    }, j.functions = j.methods = function (n) {
        var t = [];
        for (var r in n) j.isFunction(n[r]) && t.push(r);
        return t.sort()
    }, j.extend = function (n) {
        return A(o.call(arguments, 1), function (t) {
            if (t) for (var r in t) n[r] = t[r]
        }), n
    }, j.pick = function (n) {
        var t = {}, r = c.apply(e, o.call(arguments, 1));
        return A(r, function (r) {
            r in n && (t[r] = n[r])
        }), t
    }, j.omit = function (n) {
        var t = {}, r = c.apply(e, o.call(arguments, 1));
        for (var u in n) j.contains(r, u) || (t[u] = n[u]);
        return t
    }, j.defaults = function (n) {
        return A(o.call(arguments, 1), function (t) {
            if (t) for (var r in t) n[r] === void 0 && (n[r] = t[r])
        }), n
    }, j.clone = function (n) {
        return j.isObject(n) ? j.isArray(n) ? n.slice() : j.extend({}, n) : n
    }, j.tap = function (n, t) {
        return t(n), n
    };
    var S = function (n, t, r, e) {
        if (n === t) return 0 !== n || 1 / n == 1 / t;
        if (null == n || null == t) return n === t;
        n instanceof j && (n = n._wrapped), t instanceof j && (t = t._wrapped);
        var u = l.call(n);
        if (u != l.call(t)) return !1;
        switch (u) {
            case"[object String]":
                return n == String(t);
            case"[object Number]":
                return n != +n ? t != +t : 0 == n ? 1 / n == 1 / t : n == +t;
            case"[object Date]":
            case"[object Boolean]":
                return +n == +t;
            case"[object RegExp]":
                return n.source == t.source && n.global == t.global && n.multiline == t.multiline && n.ignoreCase == t.ignoreCase
        }
        if ("object" != typeof n || "object" != typeof t) return !1;
        for (var i = r.length; i--;) if (r[i] == n) return e[i] == t;
        var a = n.constructor, o = t.constructor;
        if (a !== o && !(j.isFunction(a) && a instanceof a && j.isFunction(o) && o instanceof o)) return !1;
        r.push(n), e.push(t);
        var c = 0, f = !0;
        if ("[object Array]" == u) {
            if (c = n.length, f = c == t.length) for (; c-- && (f = S(n[c], t[c], r, e));) ;
        } else {
            for (var s in n) if (j.has(n, s) && (c++, !(f = j.has(t, s) && S(n[s], t[s], r, e)))) break;
            if (f) {
                for (s in t) if (j.has(t, s) && !c--) break;
                f = !c
            }
        }
        return r.pop(), e.pop(), f
    };
    j.isEqual = function (n, t) {
        return S(n, t, [], [])
    }, j.isEmpty = function (n) {
        if (null == n) return !0;
        if (j.isArray(n) || j.isString(n)) return 0 === n.length;
        for (var t in n) if (j.has(n, t)) return !1;
        return !0
    }, j.isElement = function (n) {
        return !(!n || 1 !== n.nodeType)
    }, j.isArray = x || function (n) {
        return "[object Array]" == l.call(n)
    }, j.isObject = function (n) {
        return n === Object(n)
    }, A(["Arguments", "Function", "String", "Number", "Date", "RegExp"], function (n) {
        j["is" + n] = function (t) {
            return l.call(t) == "[object " + n + "]"
        }
    }), j.isArguments(arguments) || (j.isArguments = function (n) {
        return !(!n || !j.has(n, "callee"))
    }), "function" != typeof /./ && (j.isFunction = function (n) {
        return "function" == typeof n
    }), j.isFinite = function (n) {
        return isFinite(n) && !isNaN(parseFloat(n))
    }, j.isNaN = function (n) {
        return j.isNumber(n) && n != +n
    }, j.isBoolean = function (n) {
        return n === !0 || n === !1 || "[object Boolean]" == l.call(n)
    }, j.isNull = function (n) {
        return null === n
    }, j.isUndefined = function (n) {
        return n === void 0
    }, j.has = function (n, t) {
        return f.call(n, t)
    }, j.noConflict = function () {
        return n._ = t, this
    }, j.identity = function (n) {
        return n
    }, j.times = function (n, t, r) {
        for (var e = Array(Math.max(0, n)), u = 0; n > u; u++) e[u] = t.call(r, u);
        return e
    }, j.random = function (n, t) {
        return null == t && (t = n, n = 0), n + Math.floor(Math.random() * (t - n + 1))
    };
    var I = {escape: {"&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#x27;", "/": "&#x2F;"}};
    I.unescape = j.invert(I.escape);
    var T = {
        escape: new RegExp("[" + j.keys(I.escape).join("") + "]", "g"),
        unescape: new RegExp("(" + j.keys(I.unescape).join("|") + ")", "g")
    };
    j.each(["escape", "unescape"], function (n) {
        j[n] = function (t) {
            return null == t ? "" : ("" + t).replace(T[n], function (t) {
                return I[n][t]
            })
        }
    }), j.result = function (n, t) {
        if (null == n) return void 0;
        var r = n[t];
        return j.isFunction(r) ? r.call(n) : r
    }, j.mixin = function (n) {
        A(j.functions(n), function (t) {
            var r = j[t] = n[t];
            j.prototype[t] = function () {
                var n = [this._wrapped];
                return a.apply(n, arguments), z.call(this, r.apply(j, n))
            }
        })
    };
    var N = 0;
    j.uniqueId = function (n) {
        var t = ++N + "";
        return n ? n + t : t
    }, j.templateSettings = {evaluate: /<%([\s\S]+?)%>/g, interpolate: /<%=([\s\S]+?)%>/g, escape: /<%-([\s\S]+?)%>/g};
    var q = /(.)^/, B = {"'": "'", "\\": "\\", "\r": "r", "\n": "n", "	": "t", "\u2028": "u2028", "\u2029": "u2029"},
        D = /\\|'|\r|\n|\t|\u2028|\u2029/g;
    j.template = function (n, t, r) {
        var e;
        r = j.defaults({}, r, j.templateSettings);
        var u = new RegExp([(r.escape || q).source, (r.interpolate || q).source, (r.evaluate || q).source].join("|") + "|$", "g"),
            i = 0, a = "__p+='";
        n.replace(u, function (t, r, e, u, o) {
            return a += n.slice(i, o).replace(D, function (n) {
                return "\\" + B[n]
            }), r && (a += "'+\n((__t=(" + r + "))==null?'':_.escape(__t))+\n'"), e && (a += "'+\n((__t=(" + e + "))==null?'':__t)+\n'"), u && (a += "';\n" + u + "\n__p+='"), i = o + t.length, t
        }), a += "';\n", r.variable || (a = "with(obj||{}){\n" + a + "}\n"), a = "var __t,__p='',__j=Array.prototype.join," + "print=function(){__p+=__j.call(arguments,'');};\n" + a + "return __p;\n";
        try {
            e = new Function(r.variable || "obj", "_", a)
        } catch (o) {
            throw o.source = a, o
        }
        if (t) return e(t, j);
        var c = function (n) {
            return e.call(this, n, j)
        };
        return c.source = "function(" + (r.variable || "obj") + "){\n" + a + "}", c
    }, j.chain = function (n) {
        return j(n).chain()
    };
    var z = function (n) {
        return this._chain ? j(n).chain() : n
    };
    j.mixin(j), A(["pop", "push", "reverse", "shift", "sort", "splice", "unshift"], function (n) {
        var t = e[n];
        j.prototype[n] = function () {
            var r = this._wrapped;
            return t.apply(r, arguments), "shift" != n && "splice" != n || 0 !== r.length || delete r[0], z.call(this, r)
        }
    }), A(["concat", "join", "slice"], function (n) {
        var t = e[n];
        j.prototype[n] = function () {
            return z.call(this, t.apply(this._wrapped, arguments))
        }
    }), j.extend(j.prototype, {
        chain: function () {
            return this._chain = !0, this
        }, value: function () {
            return this._wrapped
        }
    })
}.call(this);
/*! jQuery v1.8.3 jquery.com | jquery.org/license */
(function (e, t) {
    function _(e) {
        var t = M[e] = {};
        return v.each(e.split(y), function (e, n) {
            t[n] = !0
        }), t
    }

    function H(e, n, r) {
        if (r === t && e.nodeType === 1) {
            var i = "data-" + n.replace(P, "-$1").toLowerCase();
            r = e.getAttribute(i);
            if (typeof r == "string") {
                try {
                    r = r === "true" ? !0 : r === "false" ? !1 : r === "null" ? null : +r + "" === r ? +r : D.test(r) ? v.parseJSON(r) : r
                } catch (s) {
                }
                v.data(e, n, r)
            } else r = t
        }
        return r
    }

    function B(e) {
        var t;
        for (t in e) {
            if (t === "data" && v.isEmptyObject(e[t])) continue;
            if (t !== "toJSON") return !1
        }
        return !0
    }

    function et() {
        return !1
    }

    function tt() {
        return !0
    }

    function ut(e) {
        return !e || !e.parentNode || e.parentNode.nodeType === 11
    }

    function at(e, t) {
        do e = e[t]; while (e && e.nodeType !== 1);
        return e
    }

    function ft(e, t, n) {
        t = t || 0;
        if (v.isFunction(t)) return v.grep(e, function (e, r) {
            var i = !!t.call(e, r, e);
            return i === n
        });
        if (t.nodeType) return v.grep(e, function (e, r) {
            return e === t === n
        });
        if (typeof t == "string") {
            var r = v.grep(e, function (e) {
                return e.nodeType === 1
            });
            if (it.test(t)) return v.filter(t, r, !n);
            t = v.filter(t, r)
        }
        return v.grep(e, function (e, r) {
            return v.inArray(e, t) >= 0 === n
        })
    }

    function lt(e) {
        var t = ct.split("|"), n = e.createDocumentFragment();
        if (n.createElement) while (t.length) n.createElement(t.pop());
        return n
    }

    function Lt(e, t) {
        return e.getElementsByTagName(t)[0] || e.appendChild(e.ownerDocument.createElement(t))
    }

    function At(e, t) {
        if (t.nodeType !== 1 || !v.hasData(e)) return;
        var n, r, i, s = v._data(e), o = v._data(t, s), u = s.events;
        if (u) {
            delete o.handle, o.events = {};
            for (n in u) for (r = 0, i = u[n].length; r < i; r++) v.event.add(t, n, u[n][r])
        }
        o.data && (o.data = v.extend({}, o.data))
    }

    function Ot(e, t) {
        var n;
        if (t.nodeType !== 1) return;
        t.clearAttributes && t.clearAttributes(), t.mergeAttributes && t.mergeAttributes(e), n = t.nodeName.toLowerCase(), n === "object" ? (t.parentNode && (t.outerHTML = e.outerHTML), v.support.html5Clone && e.innerHTML && !v.trim(t.innerHTML) && (t.innerHTML = e.innerHTML)) : n === "input" && Et.test(e.type) ? (t.defaultChecked = t.checked = e.checked, t.value !== e.value && (t.value = e.value)) : n === "option" ? t.selected = e.defaultSelected : n === "input" || n === "textarea" ? t.defaultValue = e.defaultValue : n === "script" && t.text !== e.text && (t.text = e.text), t.removeAttribute(v.expando)
    }

    function Mt(e) {
        return typeof e.getElementsByTagName != "undefined" ? e.getElementsByTagName("*") : typeof e.querySelectorAll != "undefined" ? e.querySelectorAll("*") : []
    }

    function _t(e) {
        Et.test(e.type) && (e.defaultChecked = e.checked)
    }

    function Qt(e, t) {
        if (t in e) return t;
        var n = t.charAt(0).toUpperCase() + t.slice(1), r = t, i = Jt.length;
        while (i--) {
            t = Jt[i] + n;
            if (t in e) return t
        }
        return r
    }

    function Gt(e, t) {
        return e = t || e, v.css(e, "display") === "none" || !v.contains(e.ownerDocument, e)
    }

    function Yt(e, t) {
        var n, r, i = [], s = 0, o = e.length;
        for (; s < o; s++) {
            n = e[s];
            if (!n.style) continue;
            i[s] = v._data(n, "olddisplay"), t ? (!i[s] && n.style.display === "none" && (n.style.display = ""), n.style.display === "" && Gt(n) && (i[s] = v._data(n, "olddisplay", nn(n.nodeName)))) : (r = Dt(n, "display"), !i[s] && r !== "none" && v._data(n, "olddisplay", r))
        }
        for (s = 0; s < o; s++) {
            n = e[s];
            if (!n.style) continue;
            if (!t || n.style.display === "none" || n.style.display === "") n.style.display = t ? i[s] || "" : "none"
        }
        return e
    }

    function Zt(e, t, n) {
        var r = Rt.exec(t);
        return r ? Math.max(0, r[1] - (n || 0)) + (r[2] || "px") : t
    }

    function en(e, t, n, r) {
        var i = n === (r ? "border" : "content") ? 4 : t === "width" ? 1 : 0, s = 0;
        for (; i < 4; i += 2) n === "margin" && (s += v.css(e, n + $t[i], !0)), r ? (n === "content" && (s -= parseFloat(Dt(e, "padding" + $t[i])) || 0), n !== "margin" && (s -= parseFloat(Dt(e, "border" + $t[i] + "Width")) || 0)) : (s += parseFloat(Dt(e, "padding" + $t[i])) || 0, n !== "padding" && (s += parseFloat(Dt(e, "border" + $t[i] + "Width")) || 0));
        return s
    }

    function tn(e, t, n) {
        var r = t === "width" ? e.offsetWidth : e.offsetHeight, i = !0,
            s = v.support.boxSizing && v.css(e, "boxSizing") === "border-box";
        if (r <= 0 || r == null) {
            r = Dt(e, t);
            if (r < 0 || r == null) r = e.style[t];
            if (Ut.test(r)) return r;
            i = s && (v.support.boxSizingReliable || r === e.style[t]), r = parseFloat(r) || 0
        }
        return r + en(e, t, n || (s ? "border" : "content"), i) + "px"
    }

    function nn(e) {
        if (Wt[e]) return Wt[e];
        var t = v("<" + e + ">").appendTo(i.body), n = t.css("display");
        t.remove();
        if (n === "none" || n === "") {
            Pt = i.body.appendChild(Pt || v.extend(i.createElement("iframe"), {frameBorder: 0, width: 0, height: 0}));
            if (!Ht || !Pt.createElement) Ht = (Pt.contentWindow || Pt.contentDocument).document, Ht.write("<!doctype html><html><body>"), Ht.close();
            t = Ht.body.appendChild(Ht.createElement(e)), n = Dt(t, "display"), i.body.removeChild(Pt)
        }
        return Wt[e] = n, n
    }

    function fn(e, t, n, r) {
        var i;
        if (v.isArray(t)) v.each(t, function (t, i) {
            n || sn.test(e) ? r(e, i) : fn(e + "[" + (typeof i == "object" ? t : "") + "]", i, n, r)
        }); else if (!n && v.type(t) === "object") for (i in t) fn(e + "[" + i + "]", t[i], n, r); else r(e, t)
    }

    function Cn(e) {
        return function (t, n) {
            typeof t != "string" && (n = t, t = "*");
            var r, i, s, o = t.toLowerCase().split(y), u = 0, a = o.length;
            if (v.isFunction(n)) for (; u < a; u++) r = o[u], s = /^\+/.test(r), s && (r = r.substr(1) || "*"), i = e[r] = e[r] || [], i[s ? "unshift" : "push"](n)
        }
    }

    function kn(e, n, r, i, s, o) {
        s = s || n.dataTypes[0], o = o || {}, o[s] = !0;
        var u, a = e[s], f = 0, l = a ? a.length : 0, c = e === Sn;
        for (; f < l && (c || !u); f++) u = a[f](n, r, i), typeof u == "string" && (!c || o[u] ? u = t : (n.dataTypes.unshift(u), u = kn(e, n, r, i, u, o)));
        return (c || !u) && !o["*"] && (u = kn(e, n, r, i, "*", o)), u
    }

    function Ln(e, n) {
        var r, i, s = v.ajaxSettings.flatOptions || {};
        for (r in n) n[r] !== t && ((s[r] ? e : i || (i = {}))[r] = n[r]);
        i && v.extend(!0, e, i)
    }

    function An(e, n, r) {
        var i, s, o, u, a = e.contents, f = e.dataTypes, l = e.responseFields;
        for (s in l) s in r && (n[l[s]] = r[s]);
        while (f[0] === "*") f.shift(), i === t && (i = e.mimeType || n.getResponseHeader("content-type"));
        if (i) for (s in a) if (a[s] && a[s].test(i)) {
            f.unshift(s);
            break
        }
        if (f[0] in r) o = f[0]; else {
            for (s in r) {
                if (!f[0] || e.converters[s + " " + f[0]]) {
                    o = s;
                    break
                }
                u || (u = s)
            }
            o = o || u
        }
        if (o) return o !== f[0] && f.unshift(o), r[o]
    }

    function On(e, t) {
        var n, r, i, s, o = e.dataTypes.slice(), u = o[0], a = {}, f = 0;
        e.dataFilter && (t = e.dataFilter(t, e.dataType));
        if (o[1]) for (n in e.converters) a[n.toLowerCase()] = e.converters[n];
        for (; i = o[++f];) if (i !== "*") {
            if (u !== "*" && u !== i) {
                n = a[u + " " + i] || a["* " + i];
                if (!n) for (r in a) {
                    s = r.split(" ");
                    if (s[1] === i) {
                        n = a[u + " " + s[0]] || a["* " + s[0]];
                        if (n) {
                            n === !0 ? n = a[r] : a[r] !== !0 && (i = s[0], o.splice(f--, 0, i));
                            break
                        }
                    }
                }
                if (n !== !0) if (n && e["throws"]) t = n(t); else try {
                    t = n(t)
                } catch (l) {
                    return {state: "parsererror", error: n ? l : "No conversion from " + u + " to " + i}
                }
            }
            u = i
        }
        return {state: "success", data: t}
    }

    function Fn() {
        try {
            return new e.XMLHttpRequest
        } catch (t) {
        }
    }

    function In() {
        try {
            return new e.ActiveXObject("Microsoft.XMLHTTP")
        } catch (t) {
        }
    }

    function $n() {
        return setTimeout(function () {
            qn = t
        }, 0), qn = v.now()
    }

    function Jn(e, t) {
        v.each(t, function (t, n) {
            var r = (Vn[t] || []).concat(Vn["*"]), i = 0, s = r.length;
            for (; i < s; i++) if (r[i].call(e, t, n)) return
        })
    }

    function Kn(e, t, n) {
        var r, i = 0, s = 0, o = Xn.length, u = v.Deferred().always(function () {
            delete a.elem
        }), a = function () {
            var t = qn || $n(), n = Math.max(0, f.startTime + f.duration - t), r = n / f.duration || 0, i = 1 - r,
                s = 0, o = f.tweens.length;
            for (; s < o; s++) f.tweens[s].run(i);
            return u.notifyWith(e, [f, i, n]), i < 1 && o ? n : (u.resolveWith(e, [f]), !1)
        }, f = u.promise({
            elem: e,
            props: v.extend({}, t),
            opts: v.extend(!0, {specialEasing: {}}, n),
            originalProperties: t,
            originalOptions: n,
            startTime: qn || $n(),
            duration: n.duration,
            tweens: [],
            createTween: function (t, n, r) {
                var i = v.Tween(e, f.opts, t, n, f.opts.specialEasing[t] || f.opts.easing);
                return f.tweens.push(i), i
            },
            stop: function (t) {
                var n = 0, r = t ? f.tweens.length : 0;
                for (; n < r; n++) f.tweens[n].run(1);
                return t ? u.resolveWith(e, [f, t]) : u.rejectWith(e, [f, t]), this
            }
        }), l = f.props;
        Qn(l, f.opts.specialEasing);
        for (; i < o; i++) {
            r = Xn[i].call(f, e, l, f.opts);
            if (r) return r
        }
        return Jn(f, l), v.isFunction(f.opts.start) && f.opts.start.call(e, f), v.fx.timer(v.extend(a, {
            anim: f,
            queue: f.opts.queue,
            elem: e
        })), f.progress(f.opts.progress).done(f.opts.done, f.opts.complete).fail(f.opts.fail).always(f.opts.always)
    }

    function Qn(e, t) {
        var n, r, i, s, o;
        for (n in e) {
            r = v.camelCase(n), i = t[r], s = e[n], v.isArray(s) && (i = s[1], s = e[n] = s[0]), n !== r && (e[r] = s, delete e[n]), o = v.cssHooks[r];
            if (o && "expand" in o) {
                s = o.expand(s), delete e[r];
                for (n in s) n in e || (e[n] = s[n], t[n] = i)
            } else t[r] = i
        }
    }

    function Gn(e, t, n) {
        var r, i, s, o, u, a, f, l, c, h = this, p = e.style, d = {}, m = [], g = e.nodeType && Gt(e);
        n.queue || (l = v._queueHooks(e, "fx"), l.unqueued == null && (l.unqueued = 0, c = l.empty.fire, l.empty.fire = function () {
            l.unqueued || c()
        }), l.unqueued++, h.always(function () {
            h.always(function () {
                l.unqueued--, v.queue(e, "fx").length || l.empty.fire()
            })
        })), e.nodeType === 1 && ("height" in t || "width" in t) && (n.overflow = [p.overflow, p.overflowX, p.overflowY], v.css(e, "display") === "inline" && v.css(e, "float") === "none" && (!v.support.inlineBlockNeedsLayout || nn(e.nodeName) === "inline" ? p.display = "inline-block" : p.zoom = 1)), n.overflow && (p.overflow = "hidden", v.support.shrinkWrapBlocks || h.done(function () {
            p.overflow = n.overflow[0], p.overflowX = n.overflow[1], p.overflowY = n.overflow[2]
        }));
        for (r in t) {
            s = t[r];
            if (Un.exec(s)) {
                delete t[r], a = a || s === "toggle";
                if (s === (g ? "hide" : "show")) continue;
                m.push(r)
            }
        }
        o = m.length;
        if (o) {
            u = v._data(e, "fxshow") || v._data(e, "fxshow", {}), "hidden" in u && (g = u.hidden), a && (u.hidden = !g), g ? v(e).show() : h.done(function () {
                v(e).hide()
            }), h.done(function () {
                var t;
                v.removeData(e, "fxshow", !0);
                for (t in d) v.style(e, t, d[t])
            });
            for (r = 0; r < o; r++) i = m[r], f = h.createTween(i, g ? u[i] : 0), d[i] = u[i] || v.style(e, i), i in u || (u[i] = f.start, g && (f.end = f.start, f.start = i === "width" || i === "height" ? 1 : 0))
        }
    }

    function Yn(e, t, n, r, i) {
        return new Yn.prototype.init(e, t, n, r, i)
    }

    function Zn(e, t) {
        var n, r = {height: e}, i = 0;
        t = t ? 1 : 0;
        for (; i < 4; i += 2 - t) n = $t[i], r["margin" + n] = r["padding" + n] = e;
        return t && (r.opacity = r.width = e), r
    }

    function tr(e) {
        return v.isWindow(e) ? e : e.nodeType === 9 ? e.defaultView || e.parentWindow : !1
    }

    var n, r, i = e.document, s = e.location, o = e.navigator, u = e.jQuery, a = e.$, f = Array.prototype.push,
        l = Array.prototype.slice, c = Array.prototype.indexOf, h = Object.prototype.toString,
        p = Object.prototype.hasOwnProperty, d = String.prototype.trim, v = function (e, t) {
            return new v.fn.init(e, t, n)
        }, m = /[\-+]?(?:\d*\.|)\d+(?:[eE][\-+]?\d+|)/.source, g = /\S/, y = /\s+/,
        b = /^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, w = /^(?:[^#<]*(<[\w\W]+>)[^>]*$|#([\w\-]*)$)/,
        E = /^<(\w+)\s*\/?>(?:<\/\1>|)$/, S = /^[\],:{}\s]*$/, x = /(?:^|:|,)(?:\s*\[)+/g,
        T = /\\(?:["\\\/bfnrt]|u[\da-fA-F]{4})/g,
        N = /"[^"\\\r\n]*"|true|false|null|-?(?:\d\d*\.|)\d+(?:[eE][\-+]?\d+|)/g, C = /^-ms-/, k = /-([\da-z])/gi,
        L = function (e, t) {
            return (t + "").toUpperCase()
        }, A = function () {
            i.addEventListener ? (i.removeEventListener("DOMContentLoaded", A, !1), v.ready()) : i.readyState === "complete" && (i.detachEvent("onreadystatechange", A), v.ready())
        }, O = {};
    v.fn = v.prototype = {
        constructor: v, init: function (e, n, r) {
            var s, o, u, a;
            if (!e) return this;
            if (e.nodeType) return this.context = this[0] = e, this.length = 1, this;
            if (typeof e == "string") {
                e.charAt(0) === "<" && e.charAt(e.length - 1) === ">" && e.length >= 3 ? s = [null, e, null] : s = w.exec(e);
                if (s && (s[1] || !n)) {
                    if (s[1]) return n = n instanceof v ? n[0] : n, a = n && n.nodeType ? n.ownerDocument || n : i, e = v.parseHTML(s[1], a, !0), E.test(s[1]) && v.isPlainObject(n) && this.attr.call(e, n, !0), v.merge(this, e);
                    o = i.getElementById(s[2]);
                    if (o && o.parentNode) {
                        if (o.id !== s[2]) return r.find(e);
                        this.length = 1, this[0] = o
                    }
                    return this.context = i, this.selector = e, this
                }
                return !n || n.jquery ? (n || r).find(e) : this.constructor(n).find(e)
            }
            return v.isFunction(e) ? r.ready(e) : (e.selector !== t && (this.selector = e.selector, this.context = e.context), v.makeArray(e, this))
        }, selector: "", jquery: "1.8.3", length: 0, size: function () {
            return this.length
        }, toArray: function () {
            return l.call(this)
        }, get: function (e) {
            return e == null ? this.toArray() : e < 0 ? this[this.length + e] : this[e]
        }, pushStack: function (e, t, n) {
            var r = v.merge(this.constructor(), e);
            return r.prevObject = this, r.context = this.context, t === "find" ? r.selector = this.selector + (this.selector ? " " : "") + n : t && (r.selector = this.selector + "." + t + "(" + n + ")"), r
        }, each: function (e, t) {
            return v.each(this, e, t)
        }, ready: function (e) {
            return v.ready.promise().done(e), this
        }, eq: function (e) {
            return e = +e, e === -1 ? this.slice(e) : this.slice(e, e + 1)
        }, first: function () {
            return this.eq(0)
        }, last: function () {
            return this.eq(-1)
        }, slice: function () {
            return this.pushStack(l.apply(this, arguments), "slice", l.call(arguments).join(","))
        }, map: function (e) {
            return this.pushStack(v.map(this, function (t, n) {
                return e.call(t, n, t)
            }))
        }, end: function () {
            return this.prevObject || this.constructor(null)
        }, push: f, sort: [].sort, splice: [].splice
    }, v.fn.init.prototype = v.fn, v.extend = v.fn.extend = function () {
        var e, n, r, i, s, o, u = arguments[0] || {}, a = 1, f = arguments.length, l = !1;
        typeof u == "boolean" && (l = u, u = arguments[1] || {}, a = 2), typeof u != "object" && !v.isFunction(u) && (u = {}), f === a && (u = this, --a);
        for (; a < f; a++) if ((e = arguments[a]) != null) for (n in e) {
            r = u[n], i = e[n];
            if (u === i) continue;
            l && i && (v.isPlainObject(i) || (s = v.isArray(i))) ? (s ? (s = !1, o = r && v.isArray(r) ? r : []) : o = r && v.isPlainObject(r) ? r : {}, u[n] = v.extend(l, o, i)) : i !== t && (u[n] = i)
        }
        return u
    }, v.extend({
        noConflict: function (t) {
            return e.$ === v && (e.$ = a), t && e.jQuery === v && (e.jQuery = u), v
        }, isReady: !1, readyWait: 1, holdReady: function (e) {
            e ? v.readyWait++ : v.ready(!0)
        }, ready: function (e) {
            if (e === !0 ? --v.readyWait : v.isReady) return;
            if (!i.body) return setTimeout(v.ready, 1);
            v.isReady = !0;
            if (e !== !0 && --v.readyWait > 0) return;
            r.resolveWith(i, [v]), v.fn.trigger && v(i).trigger("ready").off("ready")
        }, isFunction: function (e) {
            return v.type(e) === "function"
        }, isArray: Array.isArray || function (e) {
            return v.type(e) === "array"
        }, isWindow: function (e) {
            return e != null && e == e.window
        }, isNumeric: function (e) {
            return !isNaN(parseFloat(e)) && isFinite(e)
        }, type: function (e) {
            return e == null ? String(e) : O[h.call(e)] || "object"
        }, isPlainObject: function (e) {
            if (!e || v.type(e) !== "object" || e.nodeType || v.isWindow(e)) return !1;
            try {
                if (e.constructor && !p.call(e, "constructor") && !p.call(e.constructor.prototype, "isPrototypeOf")) return !1
            } catch (n) {
                return !1
            }
            var r;
            for (r in e) ;
            return r === t || p.call(e, r)
        }, isEmptyObject: function (e) {
            var t;
            for (t in e) return !1;
            return !0
        }, error: function (e) {
            throw new Error(e)
        }, parseHTML: function (e, t, n) {
            var r;
            return !e || typeof e != "string" ? null : (typeof t == "boolean" && (n = t, t = 0), t = t || i, (r = E.exec(e)) ? [t.createElement(r[1])] : (r = v.buildFragment([e], t, n ? null : []), v.merge([], (r.cacheable ? v.clone(r.fragment) : r.fragment).childNodes)))
        }, parseJSON: function (t) {
            if (!t || typeof t != "string") return null;
            t = v.trim(t);
            if (e.JSON && e.JSON.parse) return e.JSON.parse(t);
            if (S.test(t.replace(T, "@").replace(N, "]").replace(x, ""))) return (new Function("return " + t))();
            v.error("Invalid JSON: " + t)
        }, parseXML: function (n) {
            var r, i;
            if (!n || typeof n != "string") return null;
            try {
                e.DOMParser ? (i = new DOMParser, r = i.parseFromString(n, "text/xml")) : (r = new ActiveXObject("Microsoft.XMLDOM"), r.async = "false", r.loadXML(n))
            } catch (s) {
                r = t
            }
            return (!r || !r.documentElement || r.getElementsByTagName("parsererror").length) && v.error("Invalid XML: " + n), r
        }, noop: function () {
        }, globalEval: function (t) {
            t && g.test(t) && (e.execScript || function (t) {
                e.eval.call(e, t)
            })(t)
        }, camelCase: function (e) {
            return e.replace(C, "ms-").replace(k, L)
        }, nodeName: function (e, t) {
            return e.nodeName && e.nodeName.toLowerCase() === t.toLowerCase()
        }, each: function (e, n, r) {
            var i, s = 0, o = e.length, u = o === t || v.isFunction(e);
            if (r) {
                if (u) {
                    for (i in e) if (n.apply(e[i], r) === !1) break
                } else for (; s < o;) if (n.apply(e[s++], r) === !1) break
            } else if (u) {
                for (i in e) if (n.call(e[i], i, e[i]) === !1) break
            } else for (; s < o;) if (n.call(e[s], s, e[s++]) === !1) break;
            return e
        }, trim: d && !d.call("\ufeff\u00a0") ? function (e) {
            return e == null ? "" : d.call(e)
        } : function (e) {
            return e == null ? "" : (e + "").replace(b, "")
        }, makeArray: function (e, t) {
            var n, r = t || [];
            return e != null && (n = v.type(e), e.length == null || n === "string" || n === "function" || n === "regexp" || v.isWindow(e) ? f.call(r, e) : v.merge(r, e)), r
        }, inArray: function (e, t, n) {
            var r;
            if (t) {
                if (c) return c.call(t, e, n);
                r = t.length, n = n ? n < 0 ? Math.max(0, r + n) : n : 0;
                for (; n < r; n++) if (n in t && t[n] === e) return n
            }
            return -1
        }, merge: function (e, n) {
            var r = n.length, i = e.length, s = 0;
            if (typeof r == "number") for (; s < r; s++) e[i++] = n[s]; else while (n[s] !== t) e[i++] = n[s++];
            return e.length = i, e
        }, grep: function (e, t, n) {
            var r, i = [], s = 0, o = e.length;
            n = !!n;
            for (; s < o; s++) r = !!t(e[s], s), n !== r && i.push(e[s]);
            return i
        }, map: function (e, n, r) {
            var i, s, o = [], u = 0, a = e.length,
                f = e instanceof v || a !== t && typeof a == "number" && (a > 0 && e[0] && e[a - 1] || a === 0 || v.isArray(e));
            if (f) for (; u < a; u++) i = n(e[u], u, r), i != null && (o[o.length] = i); else for (s in e) i = n(e[s], s, r), i != null && (o[o.length] = i);
            return o.concat.apply([], o)
        }, guid: 1, proxy: function (e, n) {
            var r, i, s;
            return typeof n == "string" && (r = e[n], n = e, e = r), v.isFunction(e) ? (i = l.call(arguments, 2), s = function () {
                return e.apply(n, i.concat(l.call(arguments)))
            }, s.guid = e.guid = e.guid || v.guid++, s) : t
        }, access: function (e, n, r, i, s, o, u) {
            var a, f = r == null, l = 0, c = e.length;
            if (r && typeof r == "object") {
                for (l in r) v.access(e, n, l, r[l], 1, o, i);
                s = 1
            } else if (i !== t) {
                a = u === t && v.isFunction(i), f && (a ? (a = n, n = function (e, t, n) {
                    return a.call(v(e), n)
                }) : (n.call(e, i), n = null));
                if (n) for (; l < c; l++) n(e[l], r, a ? i.call(e[l], l, n(e[l], r)) : i, u);
                s = 1
            }
            return s ? e : f ? n.call(e) : c ? n(e[0], r) : o
        }, now: function () {
            return (new Date).getTime()
        }
    }), v.ready.promise = function (t) {
        if (!r) {
            r = v.Deferred();
            if (i.readyState === "complete") setTimeout(v.ready, 1); else if (i.addEventListener) i.addEventListener("DOMContentLoaded", A, !1), e.addEventListener("load", v.ready, !1); else {
                i.attachEvent("onreadystatechange", A), e.attachEvent("onload", v.ready);
                var n = !1;
                try {
                    n = e.frameElement == null && i.documentElement
                } catch (s) {
                }
                n && n.doScroll && function o() {
                    if (!v.isReady) {
                        try {
                            n.doScroll("left")
                        } catch (e) {
                            return setTimeout(o, 50)
                        }
                        v.ready()
                    }
                }()
            }
        }
        return r.promise(t)
    }, v.each("Boolean Number String Function Array Date RegExp Object".split(" "), function (e, t) {
        O["[object " + t + "]"] = t.toLowerCase()
    }), n = v(i);
    var M = {};
    v.Callbacks = function (e) {
        e = typeof e == "string" ? M[e] || _(e) : v.extend({}, e);
        var n, r, i, s, o, u, a = [], f = !e.once && [], l = function (t) {
            n = e.memory && t, r = !0, u = s || 0, s = 0, o = a.length, i = !0;
            for (; a && u < o; u++) if (a[u].apply(t[0], t[1]) === !1 && e.stopOnFalse) {
                n = !1;
                break
            }
            i = !1, a && (f ? f.length && l(f.shift()) : n ? a = [] : c.disable())
        }, c = {
            add: function () {
                if (a) {
                    var t = a.length;
                    (function r(t) {
                        v.each(t, function (t, n) {
                            var i = v.type(n);
                            i === "function" ? (!e.unique || !c.has(n)) && a.push(n) : n && n.length && i !== "string" && r(n)
                        })
                    })(arguments), i ? o = a.length : n && (s = t, l(n))
                }
                return this
            }, remove: function () {
                return a && v.each(arguments, function (e, t) {
                    var n;
                    while ((n = v.inArray(t, a, n)) > -1) a.splice(n, 1), i && (n <= o && o--, n <= u && u--)
                }), this
            }, has: function (e) {
                return v.inArray(e, a) > -1
            }, empty: function () {
                return a = [], this
            }, disable: function () {
                return a = f = n = t, this
            }, disabled: function () {
                return !a
            }, lock: function () {
                return f = t, n || c.disable(), this
            }, locked: function () {
                return !f
            }, fireWith: function (e, t) {
                return t = t || [], t = [e, t.slice ? t.slice() : t], a && (!r || f) && (i ? f.push(t) : l(t)), this
            }, fire: function () {
                return c.fireWith(this, arguments), this
            }, fired: function () {
                return !!r
            }
        };
        return c
    }, v.extend({
        Deferred: function (e) {
            var t = [["resolve", "done", v.Callbacks("once memory"), "resolved"], ["reject", "fail", v.Callbacks("once memory"), "rejected"], ["notify", "progress", v.Callbacks("memory")]],
                n = "pending", r = {
                    state: function () {
                        return n
                    }, always: function () {
                        return i.done(arguments).fail(arguments), this
                    }, then: function () {
                        var e = arguments;
                        return v.Deferred(function (n) {
                            v.each(t, function (t, r) {
                                var s = r[0], o = e[t];
                                i[r[1]](v.isFunction(o) ? function () {
                                    var e = o.apply(this, arguments);
                                    e && v.isFunction(e.promise) ? e.promise().done(n.resolve).fail(n.reject).progress(n.notify) : n[s + "With"](this === i ? n : this, [e])
                                } : n[s])
                            }), e = null
                        }).promise()
                    }, promise: function (e) {
                        return e != null ? v.extend(e, r) : r
                    }
                }, i = {};
            return r.pipe = r.then, v.each(t, function (e, s) {
                var o = s[2], u = s[3];
                r[s[1]] = o.add, u && o.add(function () {
                    n = u
                }, t[e ^ 1][2].disable, t[2][2].lock), i[s[0]] = o.fire, i[s[0] + "With"] = o.fireWith
            }), r.promise(i), e && e.call(i, i), i
        }, when: function (e) {
            var t = 0, n = l.call(arguments), r = n.length, i = r !== 1 || e && v.isFunction(e.promise) ? r : 0,
                s = i === 1 ? e : v.Deferred(), o = function (e, t, n) {
                    return function (r) {
                        t[e] = this, n[e] = arguments.length > 1 ? l.call(arguments) : r, n === u ? s.notifyWith(t, n) : --i || s.resolveWith(t, n)
                    }
                }, u, a, f;
            if (r > 1) {
                u = new Array(r), a = new Array(r), f = new Array(r);
                for (; t < r; t++) n[t] && v.isFunction(n[t].promise) ? n[t].promise().done(o(t, f, n)).fail(s.reject).progress(o(t, a, u)) : --i
            }
            return i || s.resolveWith(f, n), s.promise()
        }
    }), v.support = function () {
        var t, n, r, s, o, u, a, f, l, c, h, p = i.createElement("div");
        p.setAttribute("className", "t"), p.innerHTML = "  <link/><table></table><a href='/a'>a</a><input type='checkbox'/>", n = p.getElementsByTagName("*"), r = p.getElementsByTagName("a")[0];
        if (!n || !r || !n.length) return {};
        s = i.createElement("select"), o = s.appendChild(i.createElement("option")), u = p.getElementsByTagName("input")[0], r.style.cssText = "top:1px;float:left;opacity:.5", t = {
            leadingWhitespace: p.firstChild.nodeType === 3,
            tbody: !p.getElementsByTagName("tbody").length,
            htmlSerialize: !!p.getElementsByTagName("link").length,
            style: /top/.test(r.getAttribute("style")),
            hrefNormalized: r.getAttribute("href") === "/a",
            opacity: /^0.5/.test(r.style.opacity),
            cssFloat: !!r.style.cssFloat,
            checkOn: u.value === "on",
            optSelected: o.selected,
            getSetAttribute: p.className !== "t",
            enctype: !!i.createElement("form").enctype,
            html5Clone: i.createElement("nav").cloneNode(!0).outerHTML !== "<:nav></:nav>",
            boxModel: i.compatMode === "CSS1Compat",
            submitBubbles: !0,
            changeBubbles: !0,
            focusinBubbles: !1,
            deleteExpando: !0,
            noCloneEvent: !0,
            inlineBlockNeedsLayout: !1,
            shrinkWrapBlocks: !1,
            reliableMarginRight: !0,
            boxSizingReliable: !0,
            pixelPosition: !1
        }, u.checked = !0, t.noCloneChecked = u.cloneNode(!0).checked, s.disabled = !0, t.optDisabled = !o.disabled;
        try {
            delete p.test
        } catch (d) {
            t.deleteExpando = !1
        }
        !p.addEventListener && p.attachEvent && p.fireEvent && (p.attachEvent("onclick", h = function () {
            t.noCloneEvent = !1
        }), p.cloneNode(!0).fireEvent("onclick"), p.detachEvent("onclick", h)), u = i.createElement("input"), u.value = "t", u.setAttribute("type", "radio"), t.radioValue = u.value === "t", u.setAttribute("checked", "checked"), u.setAttribute("name", "t"), p.appendChild(u), a = i.createDocumentFragment(), a.appendChild(p.lastChild), t.checkClone = a.cloneNode(!0).cloneNode(!0).lastChild.checked, t.appendChecked = u.checked, a.removeChild(u), a.appendChild(p);
        if (p.attachEvent) for (l in {
            submit: !0,
            change: !0,
            focusin: !0
        }) f = "on" + l, c = f in p, c || (p.setAttribute(f, "return;"), c = typeof p[f] == "function"), t[l + "Bubbles"] = c;
        return v(function () {
            var n, r, s, o, u = "padding:0;margin:0;border:0;display:block;overflow:hidden;",
                a = i.getElementsByTagName("body")[0];
            if (!a) return;
            n = i.createElement("div"), n.style.cssText = "visibility:hidden;border:0;width:0;height:0;position:static;top:0;margin-top:1px", a.insertBefore(n, a.firstChild), r = i.createElement("div"), n.appendChild(r), r.innerHTML = "<table><tr><td></td><td>t</td></tr></table>", s = r.getElementsByTagName("td"), s[0].style.cssText = "padding:0;margin:0;border:0;display:none", c = s[0].offsetHeight === 0, s[0].style.display = "", s[1].style.display = "none", t.reliableHiddenOffsets = c && s[0].offsetHeight === 0, r.innerHTML = "", r.style.cssText = "box-sizing:border-box;-moz-box-sizing:border-box;-webkit-box-sizing:border-box;padding:1px;border:1px;display:block;width:4px;margin-top:1%;position:absolute;top:1%;", t.boxSizing = r.offsetWidth === 4, t.doesNotIncludeMarginInBodyOffset = a.offsetTop !== 1, e.getComputedStyle && (t.pixelPosition = (e.getComputedStyle(r, null) || {}).top !== "1%", t.boxSizingReliable = (e.getComputedStyle(r, null) || {width: "4px"}).width === "4px", o = i.createElement("div"), o.style.cssText = r.style.cssText = u, o.style.marginRight = o.style.width = "0", r.style.width = "1px", r.appendChild(o), t.reliableMarginRight = !parseFloat((e.getComputedStyle(o, null) || {}).marginRight)), typeof r.style.zoom != "undefined" && (r.innerHTML = "", r.style.cssText = u + "width:1px;padding:1px;display:inline;zoom:1", t.inlineBlockNeedsLayout = r.offsetWidth === 3, r.style.display = "block", r.style.overflow = "visible", r.innerHTML = "<div></div>", r.firstChild.style.width = "5px", t.shrinkWrapBlocks = r.offsetWidth !== 3, n.style.zoom = 1), a.removeChild(n), n = r = s = o = null
        }), a.removeChild(p), n = r = s = o = u = a = p = null, t
    }();
    var D = /(?:\{[\s\S]*\}|\[[\s\S]*\])$/, P = /([A-Z])/g;
    v.extend({
        cache: {},
        deletedIds: [],
        uuid: 0,
        expando: "jQuery" + (v.fn.jquery + Math.random()).replace(/\D/g, ""),
        noData: {embed: !0, object: "clsid:D27CDB6E-AE6D-11cf-96B8-444553540000", applet: !0},
        hasData: function (e) {
            return e = e.nodeType ? v.cache[e[v.expando]] : e[v.expando], !!e && !B(e)
        },
        data: function (e, n, r, i) {
            if (!v.acceptData(e)) return;
            var s, o, u = v.expando, a = typeof n == "string", f = e.nodeType, l = f ? v.cache : e,
                c = f ? e[u] : e[u] && u;
            if ((!c || !l[c] || !i && !l[c].data) && a && r === t) return;
            c || (f ? e[u] = c = v.deletedIds.pop() || v.guid++ : c = u), l[c] || (l[c] = {}, f || (l[c].toJSON = v.noop));
            if (typeof n == "object" || typeof n == "function") i ? l[c] = v.extend(l[c], n) : l[c].data = v.extend(l[c].data, n);
            return s = l[c], i || (s.data || (s.data = {}), s = s.data), r !== t && (s[v.camelCase(n)] = r), a ? (o = s[n], o == null && (o = s[v.camelCase(n)])) : o = s, o
        },
        removeData: function (e, t, n) {
            if (!v.acceptData(e)) return;
            var r, i, s, o = e.nodeType, u = o ? v.cache : e, a = o ? e[v.expando] : v.expando;
            if (!u[a]) return;
            if (t) {
                r = n ? u[a] : u[a].data;
                if (r) {
                    v.isArray(t) || (t in r ? t = [t] : (t = v.camelCase(t), t in r ? t = [t] : t = t.split(" ")));
                    for (i = 0, s = t.length; i < s; i++) delete r[t[i]];
                    if (!(n ? B : v.isEmptyObject)(r)) return
                }
            }
            if (!n) {
                delete u[a].data;
                if (!B(u[a])) return
            }
            o ? v.cleanData([e], !0) : v.support.deleteExpando || u != u.window ? delete u[a] : u[a] = null
        },
        _data: function (e, t, n) {
            return v.data(e, t, n, !0)
        },
        acceptData: function (e) {
            var t = e.nodeName && v.noData[e.nodeName.toLowerCase()];
            return !t || t !== !0 && e.getAttribute("classid") === t
        }
    }), v.fn.extend({
        data: function (e, n) {
            var r, i, s, o, u, a = this[0], f = 0, l = null;
            if (e === t) {
                if (this.length) {
                    l = v.data(a);
                    if (a.nodeType === 1 && !v._data(a, "parsedAttrs")) {
                        s = a.attributes;
                        for (u = s.length; f < u; f++) o = s[f].name, o.indexOf("data-") || (o = v.camelCase(o.substring(5)), H(a, o, l[o]));
                        v._data(a, "parsedAttrs", !0)
                    }
                }
                return l
            }
            return typeof e == "object" ? this.each(function () {
                v.data(this, e)
            }) : (r = e.split(".", 2), r[1] = r[1] ? "." + r[1] : "", i = r[1] + "!", v.access(this, function (n) {
                if (n === t) return l = this.triggerHandler("getData" + i, [r[0]]), l === t && a && (l = v.data(a, e), l = H(a, e, l)), l === t && r[1] ? this.data(r[0]) : l;
                r[1] = n, this.each(function () {
                    var t = v(this);
                    t.triggerHandler("setData" + i, r), v.data(this, e, n), t.triggerHandler("changeData" + i, r)
                })
            }, null, n, arguments.length > 1, null, !1))
        }, removeData: function (e) {
            return this.each(function () {
                v.removeData(this, e)
            })
        }
    }), v.extend({
        queue: function (e, t, n) {
            var r;
            if (e) return t = (t || "fx") + "queue", r = v._data(e, t), n && (!r || v.isArray(n) ? r = v._data(e, t, v.makeArray(n)) : r.push(n)), r || []
        }, dequeue: function (e, t) {
            t = t || "fx";
            var n = v.queue(e, t), r = n.length, i = n.shift(), s = v._queueHooks(e, t), o = function () {
                v.dequeue(e, t)
            };
            i === "inprogress" && (i = n.shift(), r--), i && (t === "fx" && n.unshift("inprogress"), delete s.stop, i.call(e, o, s)), !r && s && s.empty.fire()
        }, _queueHooks: function (e, t) {
            var n = t + "queueHooks";
            return v._data(e, n) || v._data(e, n, {
                empty: v.Callbacks("once memory").add(function () {
                    v.removeData(e, t + "queue", !0), v.removeData(e, n, !0)
                })
            })
        }
    }), v.fn.extend({
        queue: function (e, n) {
            var r = 2;
            return typeof e != "string" && (n = e, e = "fx", r--), arguments.length < r ? v.queue(this[0], e) : n === t ? this : this.each(function () {
                var t = v.queue(this, e, n);
                v._queueHooks(this, e), e === "fx" && t[0] !== "inprogress" && v.dequeue(this, e)
            })
        }, dequeue: function (e) {
            return this.each(function () {
                v.dequeue(this, e)
            })
        }, delay: function (e, t) {
            return e = v.fx ? v.fx.speeds[e] || e : e, t = t || "fx", this.queue(t, function (t, n) {
                var r = setTimeout(t, e);
                n.stop = function () {
                    clearTimeout(r)
                }
            })
        }, clearQueue: function (e) {
            return this.queue(e || "fx", [])
        }, promise: function (e, n) {
            var r, i = 1, s = v.Deferred(), o = this, u = this.length, a = function () {
                --i || s.resolveWith(o, [o])
            };
            typeof e != "string" && (n = e, e = t), e = e || "fx";
            while (u--) r = v._data(o[u], e + "queueHooks"), r && r.empty && (i++, r.empty.add(a));
            return a(), s.promise(n)
        }
    });
    var j, F, I, q = /[\t\r\n]/g, R = /\r/g, U = /^(?:button|input)$/i,
        z = /^(?:button|input|object|select|textarea)$/i, W = /^a(?:rea|)$/i,
        X = /^(?:autofocus|autoplay|async|checked|controls|defer|disabled|hidden|loop|multiple|open|readonly|required|scoped|selected)$/i,
        V = v.support.getSetAttribute;
    v.fn.extend({
        attr: function (e, t) {
            return v.access(this, v.attr, e, t, arguments.length > 1)
        }, removeAttr: function (e) {
            return this.each(function () {
                v.removeAttr(this, e)
            })
        }, prop: function (e, t) {
            return v.access(this, v.prop, e, t, arguments.length > 1)
        }, removeProp: function (e) {
            return e = v.propFix[e] || e, this.each(function () {
                try {
                    this[e] = t, delete this[e]
                } catch (n) {
                }
            })
        }, addClass: function (e) {
            var t, n, r, i, s, o, u;
            if (v.isFunction(e)) return this.each(function (t) {
                v(this).addClass(e.call(this, t, this.className))
            });
            if (e && typeof e == "string") {
                t = e.split(y);
                for (n = 0, r = this.length; n < r; n++) {
                    i = this[n];
                    if (i.nodeType === 1) if (!i.className && t.length === 1) i.className = e; else {
                        s = " " + i.className + " ";
                        for (o = 0, u = t.length; o < u; o++) s.indexOf(" " + t[o] + " ") < 0 && (s += t[o] + " ");
                        i.className = v.trim(s)
                    }
                }
            }
            return this
        }, removeClass: function (e) {
            var n, r, i, s, o, u, a;
            if (v.isFunction(e)) return this.each(function (t) {
                v(this).removeClass(e.call(this, t, this.className))
            });
            if (e && typeof e == "string" || e === t) {
                n = (e || "").split(y);
                for (u = 0, a = this.length; u < a; u++) {
                    i = this[u];
                    if (i.nodeType === 1 && i.className) {
                        r = (" " + i.className + " ").replace(q, " ");
                        for (s = 0, o = n.length; s < o; s++) while (r.indexOf(" " + n[s] + " ") >= 0) r = r.replace(" " + n[s] + " ", " ");
                        i.className = e ? v.trim(r) : ""
                    }
                }
            }
            return this
        }, toggleClass: function (e, t) {
            var n = typeof e, r = typeof t == "boolean";
            return v.isFunction(e) ? this.each(function (n) {
                v(this).toggleClass(e.call(this, n, this.className, t), t)
            }) : this.each(function () {
                if (n === "string") {
                    var i, s = 0, o = v(this), u = t, a = e.split(y);
                    while (i = a[s++]) u = r ? u : !o.hasClass(i), o[u ? "addClass" : "removeClass"](i)
                } else if (n === "undefined" || n === "boolean") this.className && v._data(this, "__className__", this.className), this.className = this.className || e === !1 ? "" : v._data(this, "__className__") || ""
            })
        }, hasClass: function (e) {
            var t = " " + e + " ", n = 0, r = this.length;
            for (; n < r; n++) if (this[n].nodeType === 1 && (" " + this[n].className + " ").replace(q, " ").indexOf(t) >= 0) return !0;
            return !1
        }, val: function (e) {
            var n, r, i, s = this[0];
            if (!arguments.length) {
                if (s) return n = v.valHooks[s.type] || v.valHooks[s.nodeName.toLowerCase()], n && "get" in n && (r = n.get(s, "value")) !== t ? r : (r = s.value, typeof r == "string" ? r.replace(R, "") : r == null ? "" : r);
                return
            }
            return i = v.isFunction(e), this.each(function (r) {
                var s, o = v(this);
                if (this.nodeType !== 1) return;
                i ? s = e.call(this, r, o.val()) : s = e, s == null ? s = "" : typeof s == "number" ? s += "" : v.isArray(s) && (s = v.map(s, function (e) {
                    return e == null ? "" : e + ""
                })), n = v.valHooks[this.type] || v.valHooks[this.nodeName.toLowerCase()];
                if (!n || !("set" in n) || n.set(this, s, "value") === t) this.value = s
            })
        }
    }), v.extend({
        valHooks: {
            option: {
                get: function (e) {
                    var t = e.attributes.value;
                    return !t || t.specified ? e.value : e.text
                }
            }, select: {
                get: function (e) {
                    var t, n, r = e.options, i = e.selectedIndex, s = e.type === "select-one" || i < 0,
                        o = s ? null : [], u = s ? i + 1 : r.length, a = i < 0 ? u : s ? i : 0;
                    for (; a < u; a++) {
                        n = r[a];
                        if ((n.selected || a === i) && (v.support.optDisabled ? !n.disabled : n.getAttribute("disabled") === null) && (!n.parentNode.disabled || !v.nodeName(n.parentNode, "optgroup"))) {
                            t = v(n).val();
                            if (s) return t;
                            o.push(t)
                        }
                    }
                    return o
                }, set: function (e, t) {
                    var n = v.makeArray(t);
                    return v(e).find("option").each(function () {
                        this.selected = v.inArray(v(this).val(), n) >= 0
                    }), n.length || (e.selectedIndex = -1), n
                }
            }
        },
        attrFn: {},
        attr: function (e, n, r, i) {
            var s, o, u, a = e.nodeType;
            if (!e || a === 3 || a === 8 || a === 2) return;
            if (i && v.isFunction(v.fn[n])) return v(e)[n](r);
            if (typeof e.getAttribute == "undefined") return v.prop(e, n, r);
            u = a !== 1 || !v.isXMLDoc(e), u && (n = n.toLowerCase(), o = v.attrHooks[n] || (X.test(n) ? F : j));
            if (r !== t) {
                if (r === null) {
                    v.removeAttr(e, n);
                    return
                }
                return o && "set" in o && u && (s = o.set(e, r, n)) !== t ? s : (e.setAttribute(n, r + ""), r)
            }
            return o && "get" in o && u && (s = o.get(e, n)) !== null ? s : (s = e.getAttribute(n), s === null ? t : s)
        },
        removeAttr: function (e, t) {
            var n, r, i, s, o = 0;
            if (t && e.nodeType === 1) {
                r = t.split(y);
                for (; o < r.length; o++) i = r[o], i && (n = v.propFix[i] || i, s = X.test(i), s || v.attr(e, i, ""), e.removeAttribute(V ? i : n), s && n in e && (e[n] = !1))
            }
        },
        attrHooks: {
            type: {
                set: function (e, t) {
                    if (U.test(e.nodeName) && e.parentNode) v.error("type property can't be changed"); else if (!v.support.radioValue && t === "radio" && v.nodeName(e, "input")) {
                        var n = e.value;
                        return e.setAttribute("type", t), n && (e.value = n), t
                    }
                }
            }, value: {
                get: function (e, t) {
                    return j && v.nodeName(e, "button") ? j.get(e, t) : t in e ? e.value : null
                }, set: function (e, t, n) {
                    if (j && v.nodeName(e, "button")) return j.set(e, t, n);
                    e.value = t
                }
            }
        },
        propFix: {
            tabindex: "tabIndex",
            readonly: "readOnly",
            "for": "htmlFor",
            "class": "className",
            maxlength: "maxLength",
            cellspacing: "cellSpacing",
            cellpadding: "cellPadding",
            rowspan: "rowSpan",
            colspan: "colSpan",
            usemap: "useMap",
            frameborder: "frameBorder",
            contenteditable: "contentEditable"
        },
        prop: function (e, n, r) {
            var i, s, o, u = e.nodeType;
            if (!e || u === 3 || u === 8 || u === 2) return;
            return o = u !== 1 || !v.isXMLDoc(e), o && (n = v.propFix[n] || n, s = v.propHooks[n]), r !== t ? s && "set" in s && (i = s.set(e, r, n)) !== t ? i : e[n] = r : s && "get" in s && (i = s.get(e, n)) !== null ? i : e[n]
        },
        propHooks: {
            tabIndex: {
                get: function (e) {
                    var n = e.getAttributeNode("tabindex");
                    return n && n.specified ? parseInt(n.value, 10) : z.test(e.nodeName) || W.test(e.nodeName) && e.href ? 0 : t
                }
            }
        }
    }), F = {
        get: function (e, n) {
            var r, i = v.prop(e, n);
            return i === !0 || typeof i != "boolean" && (r = e.getAttributeNode(n)) && r.nodeValue !== !1 ? n.toLowerCase() : t
        }, set: function (e, t, n) {
            var r;
            return t === !1 ? v.removeAttr(e, n) : (r = v.propFix[n] || n, r in e && (e[r] = !0), e.setAttribute(n, n.toLowerCase())), n
        }
    }, V || (I = {name: !0, id: !0, coords: !0}, j = v.valHooks.button = {
        get: function (e, n) {
            var r;
            return r = e.getAttributeNode(n), r && (I[n] ? r.value !== "" : r.specified) ? r.value : t
        }, set: function (e, t, n) {
            var r = e.getAttributeNode(n);
            return r || (r = i.createAttribute(n), e.setAttributeNode(r)), r.value = t + ""
        }
    }, v.each(["width", "height"], function (e, t) {
        v.attrHooks[t] = v.extend(v.attrHooks[t], {
            set: function (e, n) {
                if (n === "") return e.setAttribute(t, "auto"), n
            }
        })
    }), v.attrHooks.contenteditable = {
        get: j.get, set: function (e, t, n) {
            t === "" && (t = "false"), j.set(e, t, n)
        }
    }), v.support.hrefNormalized || v.each(["href", "src", "width", "height"], function (e, n) {
        v.attrHooks[n] = v.extend(v.attrHooks[n], {
            get: function (e) {
                var r = e.getAttribute(n, 2);
                return r === null ? t : r
            }
        })
    }), v.support.style || (v.attrHooks.style = {
        get: function (e) {
            return e.style.cssText.toLowerCase() || t
        }, set: function (e, t) {
            return e.style.cssText = t + ""
        }
    }), v.support.optSelected || (v.propHooks.selected = v.extend(v.propHooks.selected, {
        get: function (e) {
            var t = e.parentNode;
            return t && (t.selectedIndex, t.parentNode && t.parentNode.selectedIndex), null
        }
    })), v.support.enctype || (v.propFix.enctype = "encoding"), v.support.checkOn || v.each(["radio", "checkbox"], function () {
        v.valHooks[this] = {
            get: function (e) {
                return e.getAttribute("value") === null ? "on" : e.value
            }
        }
    }), v.each(["radio", "checkbox"], function () {
        v.valHooks[this] = v.extend(v.valHooks[this], {
            set: function (e, t) {
                if (v.isArray(t)) return e.checked = v.inArray(v(e).val(), t) >= 0
            }
        })
    });
    var $ = /^(?:textarea|input|select)$/i, J = /^([^\.]*|)(?:\.(.+)|)$/, K = /(?:^|\s)hover(\.\S+|)\b/, Q = /^key/,
        G = /^(?:mouse|contextmenu)|click/, Y = /^(?:focusinfocus|focusoutblur)$/, Z = function (e) {
            return v.event.special.hover ? e : e.replace(K, "mouseenter$1 mouseleave$1")
        };
    v.event = {
        add: function (e, n, r, i, s) {
            var o, u, a, f, l, c, h, p, d, m, g;
            if (e.nodeType === 3 || e.nodeType === 8 || !n || !r || !(o = v._data(e))) return;
            r.handler && (d = r, r = d.handler, s = d.selector), r.guid || (r.guid = v.guid++), a = o.events, a || (o.events = a = {}), u = o.handle, u || (o.handle = u = function (e) {
                return typeof v == "undefined" || !!e && v.event.triggered === e.type ? t : v.event.dispatch.apply(u.elem, arguments)
            }, u.elem = e), n = v.trim(Z(n)).split(" ");
            for (f = 0; f < n.length; f++) {
                l = J.exec(n[f]) || [], c = l[1], h = (l[2] || "").split(".").sort(), g = v.event.special[c] || {}, c = (s ? g.delegateType : g.bindType) || c, g = v.event.special[c] || {}, p = v.extend({
                    type: c,
                    origType: l[1],
                    data: i,
                    handler: r,
                    guid: r.guid,
                    selector: s,
                    needsContext: s && v.expr.match.needsContext.test(s),
                    namespace: h.join(".")
                }, d), m = a[c];
                if (!m) {
                    m = a[c] = [], m.delegateCount = 0;
                    if (!g.setup || g.setup.call(e, i, h, u) === !1) e.addEventListener ? e.addEventListener(c, u, !1) : e.attachEvent && e.attachEvent("on" + c, u)
                }
                g.add && (g.add.call(e, p), p.handler.guid || (p.handler.guid = r.guid)), s ? m.splice(m.delegateCount++, 0, p) : m.push(p), v.event.global[c] = !0
            }
            e = null
        },
        global: {},
        remove: function (e, t, n, r, i) {
            var s, o, u, a, f, l, c, h, p, d, m, g = v.hasData(e) && v._data(e);
            if (!g || !(h = g.events)) return;
            t = v.trim(Z(t || "")).split(" ");
            for (s = 0; s < t.length; s++) {
                o = J.exec(t[s]) || [], u = a = o[1], f = o[2];
                if (!u) {
                    for (u in h) v.event.remove(e, u + t[s], n, r, !0);
                    continue
                }
                p = v.event.special[u] || {}, u = (r ? p.delegateType : p.bindType) || u, d = h[u] || [], l = d.length, f = f ? new RegExp("(^|\\.)" + f.split(".").sort().join("\\.(?:.*\\.|)") + "(\\.|$)") : null;
                for (c = 0; c < d.length; c++) m = d[c], (i || a === m.origType) && (!n || n.guid === m.guid) && (!f || f.test(m.namespace)) && (!r || r === m.selector || r === "**" && m.selector) && (d.splice(c--, 1), m.selector && d.delegateCount--, p.remove && p.remove.call(e, m));
                d.length === 0 && l !== d.length && ((!p.teardown || p.teardown.call(e, f, g.handle) === !1) && v.removeEvent(e, u, g.handle), delete h[u])
            }
            v.isEmptyObject(h) && (delete g.handle, v.removeData(e, "events", !0))
        },
        customEvent: {getData: !0, setData: !0, changeData: !0},
        trigger: function (n, r, s, o) {
            if (!s || s.nodeType !== 3 && s.nodeType !== 8) {
                var u, a, f, l, c, h, p, d, m, g, y = n.type || n, b = [];
                if (Y.test(y + v.event.triggered)) return;
                y.indexOf("!") >= 0 && (y = y.slice(0, -1), a = !0), y.indexOf(".") >= 0 && (b = y.split("."), y = b.shift(), b.sort());
                if ((!s || v.event.customEvent[y]) && !v.event.global[y]) return;
                n = typeof n == "object" ? n[v.expando] ? n : new v.Event(y, n) : new v.Event(y), n.type = y, n.isTrigger = !0, n.exclusive = a, n.namespace = b.join("."), n.namespace_re = n.namespace ? new RegExp("(^|\\.)" + b.join("\\.(?:.*\\.|)") + "(\\.|$)") : null, h = y.indexOf(":") < 0 ? "on" + y : "";
                if (!s) {
                    u = v.cache;
                    for (f in u) u[f].events && u[f].events[y] && v.event.trigger(n, r, u[f].handle.elem, !0);
                    return
                }
                n.result = t, n.target || (n.target = s), r = r != null ? v.makeArray(r) : [], r.unshift(n), p = v.event.special[y] || {};
                if (p.trigger && p.trigger.apply(s, r) === !1) return;
                m = [[s, p.bindType || y]];
                if (!o && !p.noBubble && !v.isWindow(s)) {
                    g = p.delegateType || y, l = Y.test(g + y) ? s : s.parentNode;
                    for (c = s; l; l = l.parentNode) m.push([l, g]), c = l;
                    c === (s.ownerDocument || i) && m.push([c.defaultView || c.parentWindow || e, g])
                }
                for (f = 0; f < m.length && !n.isPropagationStopped(); f++) l = m[f][0], n.type = m[f][1], d = (v._data(l, "events") || {})[n.type] && v._data(l, "handle"), d && d.apply(l, r), d = h && l[h], d && v.acceptData(l) && d.apply && d.apply(l, r) === !1 && n.preventDefault();
                return n.type = y, !o && !n.isDefaultPrevented() && (!p._default || p._default.apply(s.ownerDocument, r) === !1) && (y !== "click" || !v.nodeName(s, "a")) && v.acceptData(s) && h && s[y] && (y !== "focus" && y !== "blur" || n.target.offsetWidth !== 0) && !v.isWindow(s) && (c = s[h], c && (s[h] = null), v.event.triggered = y, s[y](), v.event.triggered = t, c && (s[h] = c)), n.result
            }
            return
        },
        dispatch: function (n) {
            n = v.event.fix(n || e.event);
            var r, i, s, o, u, a, f, c, h, p, d = (v._data(this, "events") || {})[n.type] || [], m = d.delegateCount,
                g = l.call(arguments), y = !n.exclusive && !n.namespace, b = v.event.special[n.type] || {}, w = [];
            g[0] = n, n.delegateTarget = this;
            if (b.preDispatch && b.preDispatch.call(this, n) === !1) return;
            if (m && (!n.button || n.type !== "click")) for (s = n.target; s != this; s = s.parentNode || this) if (s.disabled !== !0 || n.type !== "click") {
                u = {}, f = [];
                for (r = 0; r < m; r++) c = d[r], h = c.selector, u[h] === t && (u[h] = c.needsContext ? v(h, this).index(s) >= 0 : v.find(h, this, null, [s]).length), u[h] && f.push(c);
                f.length && w.push({elem: s, matches: f})
            }
            d.length > m && w.push({elem: this, matches: d.slice(m)});
            for (r = 0; r < w.length && !n.isPropagationStopped(); r++) {
                a = w[r], n.currentTarget = a.elem;
                for (i = 0; i < a.matches.length && !n.isImmediatePropagationStopped(); i++) {
                    c = a.matches[i];
                    if (y || !n.namespace && !c.namespace || n.namespace_re && n.namespace_re.test(c.namespace)) n.data = c.data, n.handleObj = c, o = ((v.event.special[c.origType] || {}).handle || c.handler).apply(a.elem, g), o !== t && (n.result = o, o === !1 && (n.preventDefault(), n.stopPropagation()))
                }
            }
            return b.postDispatch && b.postDispatch.call(this, n), n.result
        },
        props: "attrChange attrName relatedNode srcElement altKey bubbles cancelable ctrlKey currentTarget eventPhase metaKey relatedTarget shiftKey target timeStamp view which".split(" "),
        fixHooks: {},
        keyHooks: {
            props: "char charCode key keyCode".split(" "), filter: function (e, t) {
                return e.which == null && (e.which = t.charCode != null ? t.charCode : t.keyCode), e
            }
        },
        mouseHooks: {
            props: "button buttons clientX clientY fromElement offsetX offsetY pageX pageY screenX screenY toElement".split(" "),
            filter: function (e, n) {
                var r, s, o, u = n.button, a = n.fromElement;
                return e.pageX == null && n.clientX != null && (r = e.target.ownerDocument || i, s = r.documentElement, o = r.body, e.pageX = n.clientX + (s && s.scrollLeft || o && o.scrollLeft || 0) - (s && s.clientLeft || o && o.clientLeft || 0), e.pageY = n.clientY + (s && s.scrollTop || o && o.scrollTop || 0) - (s && s.clientTop || o && o.clientTop || 0)), !e.relatedTarget && a && (e.relatedTarget = a === e.target ? n.toElement : a), !e.which && u !== t && (e.which = u & 1 ? 1 : u & 2 ? 3 : u & 4 ? 2 : 0), e
            }
        },
        fix: function (e) {
            if (e[v.expando]) return e;
            var t, n, r = e, s = v.event.fixHooks[e.type] || {}, o = s.props ? this.props.concat(s.props) : this.props;
            e = v.Event(r);
            for (t = o.length; t;) n = o[--t], e[n] = r[n];
            return e.target || (e.target = r.srcElement || i), e.target.nodeType === 3 && (e.target = e.target.parentNode), e.metaKey = !!e.metaKey, s.filter ? s.filter(e, r) : e
        },
        special: {
            load: {noBubble: !0},
            focus: {delegateType: "focusin"},
            blur: {delegateType: "focusout"},
            beforeunload: {
                setup: function (e, t, n) {
                    v.isWindow(this) && (this.onbeforeunload = n)
                }, teardown: function (e, t) {
                    this.onbeforeunload === t && (this.onbeforeunload = null)
                }
            }
        },
        simulate: function (e, t, n, r) {
            var i = v.extend(new v.Event, n, {type: e, isSimulated: !0, originalEvent: {}});
            r ? v.event.trigger(i, null, t) : v.event.dispatch.call(t, i), i.isDefaultPrevented() && n.preventDefault()
        }
    }, v.event.handle = v.event.dispatch, v.removeEvent = i.removeEventListener ? function (e, t, n) {
        e.removeEventListener && e.removeEventListener(t, n, !1)
    } : function (e, t, n) {
        var r = "on" + t;
        e.detachEvent && (typeof e[r] == "undefined" && (e[r] = null), e.detachEvent(r, n))
    }, v.Event = function (e, t) {
        if (!(this instanceof v.Event)) return new v.Event(e, t);
        e && e.type ? (this.originalEvent = e, this.type = e.type, this.isDefaultPrevented = e.defaultPrevented || e.returnValue === !1 || e.getPreventDefault && e.getPreventDefault() ? tt : et) : this.type = e, t && v.extend(this, t), this.timeStamp = e && e.timeStamp || v.now(), this[v.expando] = !0
    }, v.Event.prototype = {
        preventDefault: function () {
            this.isDefaultPrevented = tt;
            var e = this.originalEvent;
            if (!e) return;
            e.preventDefault ? e.preventDefault() : e.returnValue = !1
        }, stopPropagation: function () {
            this.isPropagationStopped = tt;
            var e = this.originalEvent;
            if (!e) return;
            e.stopPropagation && e.stopPropagation(), e.cancelBubble = !0
        }, stopImmediatePropagation: function () {
            this.isImmediatePropagationStopped = tt, this.stopPropagation()
        }, isDefaultPrevented: et, isPropagationStopped: et, isImmediatePropagationStopped: et
    }, v.each({mouseenter: "mouseover", mouseleave: "mouseout"}, function (e, t) {
        v.event.special[e] = {
            delegateType: t, bindType: t, handle: function (e) {
                var n, r = this, i = e.relatedTarget, s = e.handleObj, o = s.selector;
                if (!i || i !== r && !v.contains(r, i)) e.type = s.origType, n = s.handler.apply(this, arguments), e.type = t;
                return n
            }
        }
    }), v.support.submitBubbles || (v.event.special.submit = {
        setup: function () {
            if (v.nodeName(this, "form")) return !1;
            v.event.add(this, "click._submit keypress._submit", function (e) {
                var n = e.target, r = v.nodeName(n, "input") || v.nodeName(n, "button") ? n.form : t;
                r && !v._data(r, "_submit_attached") && (v.event.add(r, "submit._submit", function (e) {
                    e._submit_bubble = !0
                }), v._data(r, "_submit_attached", !0))
            })
        }, postDispatch: function (e) {
            e._submit_bubble && (delete e._submit_bubble, this.parentNode && !e.isTrigger && v.event.simulate("submit", this.parentNode, e, !0))
        }, teardown: function () {
            if (v.nodeName(this, "form")) return !1;
            v.event.remove(this, "._submit")
        }
    }), v.support.changeBubbles || (v.event.special.change = {
        setup: function () {
            if ($.test(this.nodeName)) {
                if (this.type === "checkbox" || this.type === "radio") v.event.add(this, "propertychange._change", function (e) {
                    e.originalEvent.propertyName === "checked" && (this._just_changed = !0)
                }), v.event.add(this, "click._change", function (e) {
                    this._just_changed && !e.isTrigger && (this._just_changed = !1), v.event.simulate("change", this, e, !0)
                });
                return !1
            }
            v.event.add(this, "beforeactivate._change", function (e) {
                var t = e.target;
                $.test(t.nodeName) && !v._data(t, "_change_attached") && (v.event.add(t, "change._change", function (e) {
                    this.parentNode && !e.isSimulated && !e.isTrigger && v.event.simulate("change", this.parentNode, e, !0)
                }), v._data(t, "_change_attached", !0))
            })
        }, handle: function (e) {
            var t = e.target;
            if (this !== t || e.isSimulated || e.isTrigger || t.type !== "radio" && t.type !== "checkbox") return e.handleObj.handler.apply(this, arguments)
        }, teardown: function () {
            return v.event.remove(this, "._change"), !$.test(this.nodeName)
        }
    }), v.support.focusinBubbles || v.each({focus: "focusin", blur: "focusout"}, function (e, t) {
        var n = 0, r = function (e) {
            v.event.simulate(t, e.target, v.event.fix(e), !0)
        };
        v.event.special[t] = {
            setup: function () {
                n++ === 0 && i.addEventListener(e, r, !0)
            }, teardown: function () {
                --n === 0 && i.removeEventListener(e, r, !0)
            }
        }
    }), v.fn.extend({
        on: function (e, n, r, i, s) {
            var o, u;
            if (typeof e == "object") {
                typeof n != "string" && (r = r || n, n = t);
                for (u in e) this.on(u, n, r, e[u], s);
                return this
            }
            r == null && i == null ? (i = n, r = n = t) : i == null && (typeof n == "string" ? (i = r, r = t) : (i = r, r = n, n = t));
            if (i === !1) i = et; else if (!i) return this;
            return s === 1 && (o = i, i = function (e) {
                return v().off(e), o.apply(this, arguments)
            }, i.guid = o.guid || (o.guid = v.guid++)), this.each(function () {
                v.event.add(this, e, i, r, n)
            })
        }, one: function (e, t, n, r) {
            return this.on(e, t, n, r, 1)
        }, off: function (e, n, r) {
            var i, s;
            if (e && e.preventDefault && e.handleObj) return i = e.handleObj, v(e.delegateTarget).off(i.namespace ? i.origType + "." + i.namespace : i.origType, i.selector, i.handler), this;
            if (typeof e == "object") {
                for (s in e) this.off(s, n, e[s]);
                return this
            }
            if (n === !1 || typeof n == "function") r = n, n = t;
            return r === !1 && (r = et), this.each(function () {
                v.event.remove(this, e, r, n)
            })
        }, bind: function (e, t, n) {
            return this.on(e, null, t, n)
        }, unbind: function (e, t) {
            return this.off(e, null, t)
        }, live: function (e, t, n) {
            return v(this.context).on(e, this.selector, t, n), this
        }, die: function (e, t) {
            return v(this.context).off(e, this.selector || "**", t), this
        }, delegate: function (e, t, n, r) {
            return this.on(t, e, n, r)
        }, undelegate: function (e, t, n) {
            return arguments.length === 1 ? this.off(e, "**") : this.off(t, e || "**", n)
        }, trigger: function (e, t) {
            return this.each(function () {
                v.event.trigger(e, t, this)
            })
        }, triggerHandler: function (e, t) {
            if (this[0]) return v.event.trigger(e, t, this[0], !0)
        }, toggle: function (e) {
            var t = arguments, n = e.guid || v.guid++, r = 0, i = function (n) {
                var i = (v._data(this, "lastToggle" + e.guid) || 0) % r;
                return v._data(this, "lastToggle" + e.guid, i + 1), n.preventDefault(), t[i].apply(this, arguments) || !1
            };
            i.guid = n;
            while (r < t.length) t[r++].guid = n;
            return this.click(i)
        }, hover: function (e, t) {
            return this.mouseenter(e).mouseleave(t || e)
        }
    }), v.each("blur focus focusin focusout load resize scroll unload click dblclick mousedown mouseup mousemove mouseover mouseout mouseenter mouseleave change select submit keydown keypress keyup error contextmenu".split(" "), function (e, t) {
        v.fn[t] = function (e, n) {
            return n == null && (n = e, e = null), arguments.length > 0 ? this.on(t, null, e, n) : this.trigger(t)
        }, Q.test(t) && (v.event.fixHooks[t] = v.event.keyHooks), G.test(t) && (v.event.fixHooks[t] = v.event.mouseHooks)
    }), function (e, t) {
        function nt(e, t, n, r) {
            n = n || [], t = t || g;
            var i, s, a, f, l = t.nodeType;
            if (!e || typeof e != "string") return n;
            if (l !== 1 && l !== 9) return [];
            a = o(t);
            if (!a && !r) if (i = R.exec(e)) if (f = i[1]) {
                if (l === 9) {
                    s = t.getElementById(f);
                    if (!s || !s.parentNode) return n;
                    if (s.id === f) return n.push(s), n
                } else if (t.ownerDocument && (s = t.ownerDocument.getElementById(f)) && u(t, s) && s.id === f) return n.push(s), n
            } else {
                if (i[2]) return S.apply(n, x.call(t.getElementsByTagName(e), 0)), n;
                if ((f = i[3]) && Z && t.getElementsByClassName) return S.apply(n, x.call(t.getElementsByClassName(f), 0)), n
            }
            return vt(e.replace(j, "$1"), t, n, r, a)
        }

        function rt(e) {
            return function (t) {
                var n = t.nodeName.toLowerCase();
                return n === "input" && t.type === e
            }
        }

        function it(e) {
            return function (t) {
                var n = t.nodeName.toLowerCase();
                return (n === "input" || n === "button") && t.type === e
            }
        }

        function st(e) {
            return N(function (t) {
                return t = +t, N(function (n, r) {
                    var i, s = e([], n.length, t), o = s.length;
                    while (o--) n[i = s[o]] && (n[i] = !(r[i] = n[i]))
                })
            })
        }

        function ot(e, t, n) {
            if (e === t) return n;
            var r = e.nextSibling;
            while (r) {
                if (r === t) return -1;
                r = r.nextSibling
            }
            return 1
        }

        function ut(e, t) {
            var n, r, s, o, u, a, f, l = L[d][e + " "];
            if (l) return t ? 0 : l.slice(0);
            u = e, a = [], f = i.preFilter;
            while (u) {
                if (!n || (r = F.exec(u))) r && (u = u.slice(r[0].length) || u), a.push(s = []);
                n = !1;
                if (r = I.exec(u)) s.push(n = new m(r.shift())), u = u.slice(n.length), n.type = r[0].replace(j, " ");
                for (o in i.filter) (r = J[o].exec(u)) && (!f[o] || (r = f[o](r))) && (s.push(n = new m(r.shift())), u = u.slice(n.length), n.type = o, n.matches = r);
                if (!n) break
            }
            return t ? u.length : u ? nt.error(e) : L(e, a).slice(0)
        }

        function at(e, t, r) {
            var i = t.dir, s = r && t.dir === "parentNode", o = w++;
            return t.first ? function (t, n, r) {
                while (t = t[i]) if (s || t.nodeType === 1) return e(t, n, r)
            } : function (t, r, u) {
                if (!u) {
                    var a, f = b + " " + o + " ", l = f + n;
                    while (t = t[i]) if (s || t.nodeType === 1) {
                        if ((a = t[d]) === l) return t.sizset;
                        if (typeof a == "string" && a.indexOf(f) === 0) {
                            if (t.sizset) return t
                        } else {
                            t[d] = l;
                            if (e(t, r, u)) return t.sizset = !0, t;
                            t.sizset = !1
                        }
                    }
                } else while (t = t[i]) if (s || t.nodeType === 1) if (e(t, r, u)) return t
            }
        }

        function ft(e) {
            return e.length > 1 ? function (t, n, r) {
                var i = e.length;
                while (i--) if (!e[i](t, n, r)) return !1;
                return !0
            } : e[0]
        }

        function lt(e, t, n, r, i) {
            var s, o = [], u = 0, a = e.length, f = t != null;
            for (; u < a; u++) if (s = e[u]) if (!n || n(s, r, i)) o.push(s), f && t.push(u);
            return o
        }

        function ct(e, t, n, r, i, s) {
            return r && !r[d] && (r = ct(r)), i && !i[d] && (i = ct(i, s)), N(function (s, o, u, a) {
                var f, l, c, h = [], p = [], d = o.length, v = s || dt(t || "*", u.nodeType ? [u] : u, []),
                    m = e && (s || !t) ? lt(v, h, e, u, a) : v, g = n ? i || (s ? e : d || r) ? [] : o : m;
                n && n(m, g, u, a);
                if (r) {
                    f = lt(g, p), r(f, [], u, a), l = f.length;
                    while (l--) if (c = f[l]) g[p[l]] = !(m[p[l]] = c)
                }
                if (s) {
                    if (i || e) {
                        if (i) {
                            f = [], l = g.length;
                            while (l--) (c = g[l]) && f.push(m[l] = c);
                            i(null, g = [], f, a)
                        }
                        l = g.length;
                        while (l--) (c = g[l]) && (f = i ? T.call(s, c) : h[l]) > -1 && (s[f] = !(o[f] = c))
                    }
                } else g = lt(g === o ? g.splice(d, g.length) : g), i ? i(null, o, g, a) : S.apply(o, g)
            })
        }

        function ht(e) {
            var t, n, r, s = e.length, o = i.relative[e[0].type], u = o || i.relative[" "], a = o ? 1 : 0,
                f = at(function (e) {
                    return e === t
                }, u, !0), l = at(function (e) {
                    return T.call(t, e) > -1
                }, u, !0), h = [function (e, n, r) {
                    return !o && (r || n !== c) || ((t = n).nodeType ? f(e, n, r) : l(e, n, r))
                }];
            for (; a < s; a++) if (n = i.relative[e[a].type]) h = [at(ft(h), n)]; else {
                n = i.filter[e[a].type].apply(null, e[a].matches);
                if (n[d]) {
                    r = ++a;
                    for (; r < s; r++) if (i.relative[e[r].type]) break;
                    return ct(a > 1 && ft(h), a > 1 && e.slice(0, a - 1).join("").replace(j, "$1"), n, a < r && ht(e.slice(a, r)), r < s && ht(e = e.slice(r)), r < s && e.join(""))
                }
                h.push(n)
            }
            return ft(h)
        }

        function pt(e, t) {
            var r = t.length > 0, s = e.length > 0, o = function (u, a, f, l, h) {
                var p, d, v, m = [], y = 0, w = "0", x = u && [], T = h != null, N = c,
                    C = u || s && i.find.TAG("*", h && a.parentNode || a), k = b += N == null ? 1 : Math.E;
                T && (c = a !== g && a, n = o.el);
                for (; (p = C[w]) != null; w++) {
                    if (s && p) {
                        for (d = 0; v = e[d]; d++) if (v(p, a, f)) {
                            l.push(p);
                            break
                        }
                        T && (b = k, n = ++o.el)
                    }
                    r && ((p = !v && p) && y--, u && x.push(p))
                }
                y += w;
                if (r && w !== y) {
                    for (d = 0; v = t[d]; d++) v(x, m, a, f);
                    if (u) {
                        if (y > 0) while (w--) !x[w] && !m[w] && (m[w] = E.call(l));
                        m = lt(m)
                    }
                    S.apply(l, m), T && !u && m.length > 0 && y + t.length > 1 && nt.uniqueSort(l)
                }
                return T && (b = k, c = N), x
            };
            return o.el = 0, r ? N(o) : o
        }

        function dt(e, t, n) {
            var r = 0, i = t.length;
            for (; r < i; r++) nt(e, t[r], n);
            return n
        }

        function vt(e, t, n, r, s) {
            var o, u, f, l, c, h = ut(e), p = h.length;
            if (!r && h.length === 1) {
                u = h[0] = h[0].slice(0);
                if (u.length > 2 && (f = u[0]).type === "ID" && t.nodeType === 9 && !s && i.relative[u[1].type]) {
                    t = i.find.ID(f.matches[0].replace($, ""), t, s)[0];
                    if (!t) return n;
                    e = e.slice(u.shift().length)
                }
                for (o = J.POS.test(e) ? -1 : u.length - 1; o >= 0; o--) {
                    f = u[o];
                    if (i.relative[l = f.type]) break;
                    if (c = i.find[l]) if (r = c(f.matches[0].replace($, ""), z.test(u[0].type) && t.parentNode || t, s)) {
                        u.splice(o, 1), e = r.length && u.join("");
                        if (!e) return S.apply(n, x.call(r, 0)), n;
                        break
                    }
                }
            }
            return a(e, h)(r, t, s, n, z.test(e)), n
        }

        function mt() {
        }

        var n, r, i, s, o, u, a, f, l, c, h = !0, p = "undefined", d = ("sizcache" + Math.random()).replace(".", ""),
            m = String, g = e.document, y = g.documentElement, b = 0, w = 0, E = [].pop, S = [].push, x = [].slice,
            T = [].indexOf || function (e) {
                var t = 0, n = this.length;
                for (; t < n; t++) if (this[t] === e) return t;
                return -1
            }, N = function (e, t) {
                return e[d] = t == null || t, e
            }, C = function () {
                var e = {}, t = [];
                return N(function (n, r) {
                    return t.push(n) > i.cacheLength && delete e[t.shift()], e[n + " "] = r
                }, e)
            }, k = C(), L = C(), A = C(), O = "[\\x20\\t\\r\\n\\f]", M = "(?:\\\\.|[-\\w]|[^\\x00-\\xa0])+",
            _ = M.replace("w", "w#"), D = "([*^$|!~]?=)",
            P = "\\[" + O + "*(" + M + ")" + O + "*(?:" + D + O + "*(?:(['\"])((?:\\\\.|[^\\\\])*?)\\3|(" + _ + ")|)|)" + O + "*\\]",
            H = ":(" + M + ")(?:\\((?:(['\"])((?:\\\\.|[^\\\\])*?)\\2|([^()[\\]]*|(?:(?:" + P + ")|[^:]|\\\\.)*|.*))\\)|)",
            B = ":(even|odd|eq|gt|lt|nth|first|last)(?:\\(" + O + "*((?:-\\d)?\\d*)" + O + "*\\)|)(?=[^-]|$)",
            j = new RegExp("^" + O + "+|((?:^|[^\\\\])(?:\\\\.)*)" + O + "+$", "g"),
            F = new RegExp("^" + O + "*," + O + "*"), I = new RegExp("^" + O + "*([\\x20\\t\\r\\n\\f>+~])" + O + "*"),
            q = new RegExp(H), R = /^(?:#([\w\-]+)|(\w+)|\.([\w\-]+))$/, U = /^:not/, z = /[\x20\t\r\n\f]*[+~]/,
            W = /:not\($/, X = /h\d/i, V = /input|select|textarea|button/i, $ = /\\(?!\\)/g, J = {
                ID: new RegExp("^#(" + M + ")"),
                CLASS: new RegExp("^\\.(" + M + ")"),
                NAME: new RegExp("^\\[name=['\"]?(" + M + ")['\"]?\\]"),
                TAG: new RegExp("^(" + M.replace("w", "w*") + ")"),
                ATTR: new RegExp("^" + P),
                PSEUDO: new RegExp("^" + H),
                POS: new RegExp(B, "i"),
                CHILD: new RegExp("^:(only|nth|first|last)-child(?:\\(" + O + "*(even|odd|(([+-]|)(\\d*)n|)" + O + "*(?:([+-]|)" + O + "*(\\d+)|))" + O + "*\\)|)", "i"),
                needsContext: new RegExp("^" + O + "*[>+~]|" + B, "i")
            }, K = function (e) {
                var t = g.createElement("div");
                try {
                    return e(t)
                } catch (n) {
                    return !1
                } finally {
                    t = null
                }
            }, Q = K(function (e) {
                return e.appendChild(g.createComment("")), !e.getElementsByTagName("*").length
            }), G = K(function (e) {
                return e.innerHTML = "<a href='#'></a>", e.firstChild && typeof e.firstChild.getAttribute !== p && e.firstChild.getAttribute("href") === "#"
            }), Y = K(function (e) {
                e.innerHTML = "<select></select>";
                var t = typeof e.lastChild.getAttribute("multiple");
                return t !== "boolean" && t !== "string"
            }), Z = K(function (e) {
                return e.innerHTML = "<div class='hidden e'></div><div class='hidden'></div>", !e.getElementsByClassName || !e.getElementsByClassName("e").length ? !1 : (e.lastChild.className = "e", e.getElementsByClassName("e").length === 2)
            }), et = K(function (e) {
                e.id = d + 0, e.innerHTML = "<a name='" + d + "'></a><div name='" + d + "'></div>", y.insertBefore(e, y.firstChild);
                var t = g.getElementsByName && g.getElementsByName(d).length === 2 + g.getElementsByName(d + 0).length;
                return r = !g.getElementById(d), y.removeChild(e), t
            });
        try {
            x.call(y.childNodes, 0)[0].nodeType
        } catch (tt) {
            x = function (e) {
                var t, n = [];
                for (; t = this[e]; e++) n.push(t);
                return n
            }
        }
        nt.matches = function (e, t) {
            return nt(e, null, null, t)
        }, nt.matchesSelector = function (e, t) {
            return nt(t, null, null, [e]).length > 0
        }, s = nt.getText = function (e) {
            var t, n = "", r = 0, i = e.nodeType;
            if (i) {
                if (i === 1 || i === 9 || i === 11) {
                    if (typeof e.textContent == "string") return e.textContent;
                    for (e = e.firstChild; e; e = e.nextSibling) n += s(e)
                } else if (i === 3 || i === 4) return e.nodeValue
            } else for (; t = e[r]; r++) n += s(t);
            return n
        }, o = nt.isXML = function (e) {
            var t = e && (e.ownerDocument || e).documentElement;
            return t ? t.nodeName !== "HTML" : !1
        }, u = nt.contains = y.contains ? function (e, t) {
            var n = e.nodeType === 9 ? e.documentElement : e, r = t && t.parentNode;
            return e === r || !!(r && r.nodeType === 1 && n.contains && n.contains(r))
        } : y.compareDocumentPosition ? function (e, t) {
            return t && !!(e.compareDocumentPosition(t) & 16)
        } : function (e, t) {
            while (t = t.parentNode) if (t === e) return !0;
            return !1
        }, nt.attr = function (e, t) {
            var n, r = o(e);
            return r || (t = t.toLowerCase()), (n = i.attrHandle[t]) ? n(e) : r || Y ? e.getAttribute(t) : (n = e.getAttributeNode(t), n ? typeof e[t] == "boolean" ? e[t] ? t : null : n.specified ? n.value : null : null)
        }, i = nt.selectors = {
            cacheLength: 50,
            createPseudo: N,
            match: J,
            attrHandle: G ? {} : {
                href: function (e) {
                    return e.getAttribute("href", 2)
                }, type: function (e) {
                    return e.getAttribute("type")
                }
            },
            find: {
                ID: r ? function (e, t, n) {
                    if (typeof t.getElementById !== p && !n) {
                        var r = t.getElementById(e);
                        return r && r.parentNode ? [r] : []
                    }
                } : function (e, n, r) {
                    if (typeof n.getElementById !== p && !r) {
                        var i = n.getElementById(e);
                        return i ? i.id === e || typeof i.getAttributeNode !== p && i.getAttributeNode("id").value === e ? [i] : t : []
                    }
                }, TAG: Q ? function (e, t) {
                    if (typeof t.getElementsByTagName !== p) return t.getElementsByTagName(e)
                } : function (e, t) {
                    var n = t.getElementsByTagName(e);
                    if (e === "*") {
                        var r, i = [], s = 0;
                        for (; r = n[s]; s++) r.nodeType === 1 && i.push(r);
                        return i
                    }
                    return n
                }, NAME: et && function (e, t) {
                    if (typeof t.getElementsByName !== p) return t.getElementsByName(name)
                }, CLASS: Z && function (e, t, n) {
                    if (typeof t.getElementsByClassName !== p && !n) return t.getElementsByClassName(e)
                }
            },
            relative: {
                ">": {dir: "parentNode", first: !0},
                " ": {dir: "parentNode"},
                "+": {dir: "previousSibling", first: !0},
                "~": {dir: "previousSibling"}
            },
            preFilter: {
                ATTR: function (e) {
                    return e[1] = e[1].replace($, ""), e[3] = (e[4] || e[5] || "").replace($, ""), e[2] === "~=" && (e[3] = " " + e[3] + " "), e.slice(0, 4)
                }, CHILD: function (e) {
                    return e[1] = e[1].toLowerCase(), e[1] === "nth" ? (e[2] || nt.error(e[0]), e[3] = +(e[3] ? e[4] + (e[5] || 1) : 2 * (e[2] === "even" || e[2] === "odd")), e[4] = +(e[6] + e[7] || e[2] === "odd")) : e[2] && nt.error(e[0]), e
                }, PSEUDO: function (e) {
                    var t, n;
                    if (J.CHILD.test(e[0])) return null;
                    if (e[3]) e[2] = e[3]; else if (t = e[4]) q.test(t) && (n = ut(t, !0)) && (n = t.indexOf(")", t.length - n) - t.length) && (t = t.slice(0, n), e[0] = e[0].slice(0, n)), e[2] = t;
                    return e.slice(0, 3)
                }
            },
            filter: {
                ID: r ? function (e) {
                    return e = e.replace($, ""), function (t) {
                        return t.getAttribute("id") === e
                    }
                } : function (e) {
                    return e = e.replace($, ""), function (t) {
                        var n = typeof t.getAttributeNode !== p && t.getAttributeNode("id");
                        return n && n.value === e
                    }
                }, TAG: function (e) {
                    return e === "*" ? function () {
                        return !0
                    } : (e = e.replace($, "").toLowerCase(), function (t) {
                        return t.nodeName && t.nodeName.toLowerCase() === e
                    })
                }, CLASS: function (e) {
                    var t = k[d][e + " "];
                    return t || (t = new RegExp("(^|" + O + ")" + e + "(" + O + "|$)")) && k(e, function (e) {
                        return t.test(e.className || typeof e.getAttribute !== p && e.getAttribute("class") || "")
                    })
                }, ATTR: function (e, t, n) {
                    return function (r, i) {
                        var s = nt.attr(r, e);
                        return s == null ? t === "!=" : t ? (s += "", t === "=" ? s === n : t === "!=" ? s !== n : t === "^=" ? n && s.indexOf(n) === 0 : t === "*=" ? n && s.indexOf(n) > -1 : t === "$=" ? n && s.substr(s.length - n.length) === n : t === "~=" ? (" " + s + " ").indexOf(n) > -1 : t === "|=" ? s === n || s.substr(0, n.length + 1) === n + "-" : !1) : !0
                    }
                }, CHILD: function (e, t, n, r) {
                    return e === "nth" ? function (e) {
                        var t, i, s = e.parentNode;
                        if (n === 1 && r === 0) return !0;
                        if (s) {
                            i = 0;
                            for (t = s.firstChild; t; t = t.nextSibling) if (t.nodeType === 1) {
                                i++;
                                if (e === t) break
                            }
                        }
                        return i -= r, i === n || i % n === 0 && i / n >= 0
                    } : function (t) {
                        var n = t;
                        switch (e) {
                            case"only":
                            case"first":
                                while (n = n.previousSibling) if (n.nodeType === 1) return !1;
                                if (e === "first") return !0;
                                n = t;
                            case"last":
                                while (n = n.nextSibling) if (n.nodeType === 1) return !1;
                                return !0
                        }
                    }
                }, PSEUDO: function (e, t) {
                    var n, r = i.pseudos[e] || i.setFilters[e.toLowerCase()] || nt.error("unsupported pseudo: " + e);
                    return r[d] ? r(t) : r.length > 1 ? (n = [e, e, "", t], i.setFilters.hasOwnProperty(e.toLowerCase()) ? N(function (e, n) {
                        var i, s = r(e, t), o = s.length;
                        while (o--) i = T.call(e, s[o]), e[i] = !(n[i] = s[o])
                    }) : function (e) {
                        return r(e, 0, n)
                    }) : r
                }
            },
            pseudos: {
                not: N(function (e) {
                    var t = [], n = [], r = a(e.replace(j, "$1"));
                    return r[d] ? N(function (e, t, n, i) {
                        var s, o = r(e, null, i, []), u = e.length;
                        while (u--) if (s = o[u]) e[u] = !(t[u] = s)
                    }) : function (e, i, s) {
                        return t[0] = e, r(t, null, s, n), !n.pop()
                    }
                }),
                has: N(function (e) {
                    return function (t) {
                        return nt(e, t).length > 0
                    }
                }),
                contains: N(function (e) {
                    return function (t) {
                        return (t.textContent || t.innerText || s(t)).indexOf(e) > -1
                    }
                }),
                enabled: function (e) {
                    return e.disabled === !1
                },
                disabled: function (e) {
                    return e.disabled === !0
                },
                checked: function (e) {
                    var t = e.nodeName.toLowerCase();
                    return t === "input" && !!e.checked || t === "option" && !!e.selected
                },
                selected: function (e) {
                    return e.parentNode && e.parentNode.selectedIndex, e.selected === !0
                },
                parent: function (e) {
                    return !i.pseudos.empty(e)
                },
                empty: function (e) {
                    var t;
                    e = e.firstChild;
                    while (e) {
                        if (e.nodeName > "@" || (t = e.nodeType) === 3 || t === 4) return !1;
                        e = e.nextSibling
                    }
                    return !0
                },
                header: function (e) {
                    return X.test(e.nodeName)
                },
                text: function (e) {
                    var t, n;
                    return e.nodeName.toLowerCase() === "input" && (t = e.type) === "text" && ((n = e.getAttribute("type")) == null || n.toLowerCase() === t)
                },
                radio: rt("radio"),
                checkbox: rt("checkbox"),
                file: rt("file"),
                password: rt("password"),
                image: rt("image"),
                submit: it("submit"),
                reset: it("reset"),
                button: function (e) {
                    var t = e.nodeName.toLowerCase();
                    return t === "input" && e.type === "button" || t === "button"
                },
                input: function (e) {
                    return V.test(e.nodeName)
                },
                focus: function (e) {
                    var t = e.ownerDocument;
                    return e === t.activeElement && (!t.hasFocus || t.hasFocus()) && !!(e.type || e.href || ~e.tabIndex)
                },
                active: function (e) {
                    return e === e.ownerDocument.activeElement
                },
                first: st(function () {
                    return [0]
                }),
                last: st(function (e, t) {
                    return [t - 1]
                }),
                eq: st(function (e, t, n) {
                    return [n < 0 ? n + t : n]
                }),
                even: st(function (e, t) {
                    for (var n = 0; n < t; n += 2) e.push(n);
                    return e
                }),
                odd: st(function (e, t) {
                    for (var n = 1; n < t; n += 2) e.push(n);
                    return e
                }),
                lt: st(function (e, t, n) {
                    for (var r = n < 0 ? n + t : n; --r >= 0;) e.push(r);
                    return e
                }),
                gt: st(function (e, t, n) {
                    for (var r = n < 0 ? n + t : n; ++r < t;) e.push(r);
                    return e
                })
            }
        }, f = y.compareDocumentPosition ? function (e, t) {
            return e === t ? (l = !0, 0) : (!e.compareDocumentPosition || !t.compareDocumentPosition ? e.compareDocumentPosition : e.compareDocumentPosition(t) & 4) ? -1 : 1
        } : function (e, t) {
            if (e === t) return l = !0, 0;
            if (e.sourceIndex && t.sourceIndex) return e.sourceIndex - t.sourceIndex;
            var n, r, i = [], s = [], o = e.parentNode, u = t.parentNode, a = o;
            if (o === u) return ot(e, t);
            if (!o) return -1;
            if (!u) return 1;
            while (a) i.unshift(a), a = a.parentNode;
            a = u;
            while (a) s.unshift(a), a = a.parentNode;
            n = i.length, r = s.length;
            for (var f = 0; f < n && f < r; f++) if (i[f] !== s[f]) return ot(i[f], s[f]);
            return f === n ? ot(e, s[f], -1) : ot(i[f], t, 1)
        }, [0, 0].sort(f), h = !l, nt.uniqueSort = function (e) {
            var t, n = [], r = 1, i = 0;
            l = h, e.sort(f);
            if (l) {
                for (; t = e[r]; r++) t === e[r - 1] && (i = n.push(r));
                while (i--) e.splice(n[i], 1)
            }
            return e
        }, nt.error = function (e) {
            throw new Error("Syntax error, unrecognized expression: " + e)
        }, a = nt.compile = function (e, t) {
            var n, r = [], i = [], s = A[d][e + " "];
            if (!s) {
                t || (t = ut(e)), n = t.length;
                while (n--) s = ht(t[n]), s[d] ? r.push(s) : i.push(s);
                s = A(e, pt(i, r))
            }
            return s
        }, g.querySelectorAll && function () {
            var e, t = vt, n = /'|\\/g, r = /\=[\x20\t\r\n\f]*([^'"\]]*)[\x20\t\r\n\f]*\]/g, i = [":focus"],
                s = [":active"],
                u = y.matchesSelector || y.mozMatchesSelector || y.webkitMatchesSelector || y.oMatchesSelector || y.msMatchesSelector;
            K(function (e) {
                e.innerHTML = "<select><option selected=''></option></select>", e.querySelectorAll("[selected]").length || i.push("\\[" + O + "*(?:checked|disabled|ismap|multiple|readonly|selected|value)"), e.querySelectorAll(":checked").length || i.push(":checked")
            }), K(function (e) {
                e.innerHTML = "<p test=''></p>", e.querySelectorAll("[test^='']").length && i.push("[*^$]=" + O + "*(?:\"\"|'')"), e.innerHTML = "<input type='hidden'/>", e.querySelectorAll(":enabled").length || i.push(":enabled", ":disabled")
            }), i = new RegExp(i.join("|")), vt = function (e, r, s, o, u) {
                if (!o && !u && !i.test(e)) {
                    var a, f, l = !0, c = d, h = r, p = r.nodeType === 9 && e;
                    if (r.nodeType === 1 && r.nodeName.toLowerCase() !== "object") {
                        a = ut(e), (l = r.getAttribute("id")) ? c = l.replace(n, "\\$&") : r.setAttribute("id", c), c = "[id='" + c + "'] ", f = a.length;
                        while (f--) a[f] = c + a[f].join("");
                        h = z.test(e) && r.parentNode || r, p = a.join(",")
                    }
                    if (p) try {
                        return S.apply(s, x.call(h.querySelectorAll(p), 0)), s
                    } catch (v) {
                    } finally {
                        l || r.removeAttribute("id")
                    }
                }
                return t(e, r, s, o, u)
            }, u && (K(function (t) {
                e = u.call(t, "div");
                try {
                    u.call(t, "[test!='']:sizzle"), s.push("!=", H)
                } catch (n) {
                }
            }), s = new RegExp(s.join("|")), nt.matchesSelector = function (t, n) {
                n = n.replace(r, "='$1']");
                if (!o(t) && !s.test(n) && !i.test(n)) try {
                    var a = u.call(t, n);
                    if (a || e || t.document && t.document.nodeType !== 11) return a
                } catch (f) {
                }
                return nt(n, null, null, [t]).length > 0
            })
        }(), i.pseudos.nth = i.pseudos.eq, i.filters = mt.prototype = i.pseudos, i.setFilters = new mt, nt.attr = v.attr, v.find = nt, v.expr = nt.selectors, v.expr[":"] = v.expr.pseudos, v.unique = nt.uniqueSort, v.text = nt.getText, v.isXMLDoc = nt.isXML, v.contains = nt.contains
    }(e);
    var nt = /Until$/, rt = /^(?:parents|prev(?:Until|All))/, it = /^.[^:#\[\.,]*$/, st = v.expr.match.needsContext,
        ot = {children: !0, contents: !0, next: !0, prev: !0};
    v.fn.extend({
        find: function (e) {
            var t, n, r, i, s, o, u = this;
            if (typeof e != "string") return v(e).filter(function () {
                for (t = 0, n = u.length; t < n; t++) if (v.contains(u[t], this)) return !0
            });
            o = this.pushStack("", "find", e);
            for (t = 0, n = this.length; t < n; t++) {
                r = o.length, v.find(e, this[t], o);
                if (t > 0) for (i = r; i < o.length; i++) for (s = 0; s < r; s++) if (o[s] === o[i]) {
                    o.splice(i--, 1);
                    break
                }
            }
            return o
        }, has: function (e) {
            var t, n = v(e, this), r = n.length;
            return this.filter(function () {
                for (t = 0; t < r; t++) if (v.contains(this, n[t])) return !0
            })
        }, not: function (e) {
            return this.pushStack(ft(this, e, !1), "not", e)
        }, filter: function (e) {
            return this.pushStack(ft(this, e, !0), "filter", e)
        }, is: function (e) {
            return !!e && (typeof e == "string" ? st.test(e) ? v(e, this.context).index(this[0]) >= 0 : v.filter(e, this).length > 0 : this.filter(e).length > 0)
        }, closest: function (e, t) {
            var n, r = 0, i = this.length, s = [], o = st.test(e) || typeof e != "string" ? v(e, t || this.context) : 0;
            for (; r < i; r++) {
                n = this[r];
                while (n && n.ownerDocument && n !== t && n.nodeType !== 11) {
                    if (o ? o.index(n) > -1 : v.find.matchesSelector(n, e)) {
                        s.push(n);
                        break
                    }
                    n = n.parentNode
                }
            }
            return s = s.length > 1 ? v.unique(s) : s, this.pushStack(s, "closest", e)
        }, index: function (e) {
            return e ? typeof e == "string" ? v.inArray(this[0], v(e)) : v.inArray(e.jquery ? e[0] : e, this) : this[0] && this[0].parentNode ? this.prevAll().length : -1
        }, add: function (e, t) {
            var n = typeof e == "string" ? v(e, t) : v.makeArray(e && e.nodeType ? [e] : e), r = v.merge(this.get(), n);
            return this.pushStack(ut(n[0]) || ut(r[0]) ? r : v.unique(r))
        }, addBack: function (e) {
            return this.add(e == null ? this.prevObject : this.prevObject.filter(e))
        }
    }), v.fn.andSelf = v.fn.addBack, v.each({
        parent: function (e) {
            var t = e.parentNode;
            return t && t.nodeType !== 11 ? t : null
        }, parents: function (e) {
            return v.dir(e, "parentNode")
        }, parentsUntil: function (e, t, n) {
            return v.dir(e, "parentNode", n)
        }, next: function (e) {
            return at(e, "nextSibling")
        }, prev: function (e) {
            return at(e, "previousSibling")
        }, nextAll: function (e) {
            return v.dir(e, "nextSibling")
        }, prevAll: function (e) {
            return v.dir(e, "previousSibling")
        }, nextUntil: function (e, t, n) {
            return v.dir(e, "nextSibling", n)
        }, prevUntil: function (e, t, n) {
            return v.dir(e, "previousSibling", n)
        }, siblings: function (e) {
            return v.sibling((e.parentNode || {}).firstChild, e)
        }, children: function (e) {
            return v.sibling(e.firstChild)
        }, contents: function (e) {
            return v.nodeName(e, "iframe") ? e.contentDocument || e.contentWindow.document : v.merge([], e.childNodes)
        }
    }, function (e, t) {
        v.fn[e] = function (n, r) {
            var i = v.map(this, t, n);
            return nt.test(e) || (r = n), r && typeof r == "string" && (i = v.filter(r, i)), i = this.length > 1 && !ot[e] ? v.unique(i) : i, this.length > 1 && rt.test(e) && (i = i.reverse()), this.pushStack(i, e, l.call(arguments).join(","))
        }
    }), v.extend({
        filter: function (e, t, n) {
            return n && (e = ":not(" + e + ")"), t.length === 1 ? v.find.matchesSelector(t[0], e) ? [t[0]] : [] : v.find.matches(e, t)
        }, dir: function (e, n, r) {
            var i = [], s = e[n];
            while (s && s.nodeType !== 9 && (r === t || s.nodeType !== 1 || !v(s).is(r))) s.nodeType === 1 && i.push(s), s = s[n];
            return i
        }, sibling: function (e, t) {
            var n = [];
            for (; e; e = e.nextSibling) e.nodeType === 1 && e !== t && n.push(e);
            return n
        }
    });
    var ct = "abbr|article|aside|audio|bdi|canvas|data|datalist|details|figcaption|figure|footer|header|hgroup|mark|meter|nav|output|progress|section|summary|time|video",
        ht = / jQuery\d+="(?:null|\d+)"/g, pt = /^\s+/,
        dt = /<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/gi, vt = /<([\w:]+)/,
        mt = /<tbody/i, gt = /<|&#?\w+;/, yt = /<(?:script|style|link)/i, bt = /<(?:script|object|embed|option|style)/i,
        wt = new RegExp("<(?:" + ct + ")[\\s/>]", "i"), Et = /^(?:checkbox|radio)$/,
        St = /checked\s*(?:[^=]|=\s*.checked.)/i, xt = /\/(java|ecma)script/i,
        Tt = /^\s*<!(?:\[CDATA\[|\-\-)|[\]\-]{2}>\s*$/g, Nt = {
            option: [1, "<select multiple='multiple'>", "</select>"],
            legend: [1, "<fieldset>", "</fieldset>"],
            thead: [1, "<table>", "</table>"],
            tr: [2, "<table><tbody>", "</tbody></table>"],
            td: [3, "<table><tbody><tr>", "</tr></tbody></table>"],
            col: [2, "<table><tbody></tbody><colgroup>", "</colgroup></table>"],
            area: [1, "<map>", "</map>"],
            _default: [0, "", ""]
        }, Ct = lt(i), kt = Ct.appendChild(i.createElement("div"));
    Nt.optgroup = Nt.option, Nt.tbody = Nt.tfoot = Nt.colgroup = Nt.caption = Nt.thead, Nt.th = Nt.td, v.support.htmlSerialize || (Nt._default = [1, "X<div>", "</div>"]), v.fn.extend({
        text: function (e) {
            return v.access(this, function (e) {
                return e === t ? v.text(this) : this.empty().append((this[0] && this[0].ownerDocument || i).createTextNode(e))
            }, null, e, arguments.length)
        }, wrapAll: function (e) {
            if (v.isFunction(e)) return this.each(function (t) {
                v(this).wrapAll(e.call(this, t))
            });
            if (this[0]) {
                var t = v(e, this[0].ownerDocument).eq(0).clone(!0);
                this[0].parentNode && t.insertBefore(this[0]), t.map(function () {
                    var e = this;
                    while (e.firstChild && e.firstChild.nodeType === 1) e = e.firstChild;
                    return e
                }).append(this)
            }
            return this
        }, wrapInner: function (e) {
            return v.isFunction(e) ? this.each(function (t) {
                v(this).wrapInner(e.call(this, t))
            }) : this.each(function () {
                var t = v(this), n = t.contents();
                n.length ? n.wrapAll(e) : t.append(e)
            })
        }, wrap: function (e) {
            var t = v.isFunction(e);
            return this.each(function (n) {
                v(this).wrapAll(t ? e.call(this, n) : e)
            })
        }, unwrap: function () {
            return this.parent().each(function () {
                v.nodeName(this, "body") || v(this).replaceWith(this.childNodes)
            }).end()
        }, append: function () {
            return this.domManip(arguments, !0, function (e) {
                (this.nodeType === 1 || this.nodeType === 11) && this.appendChild(e)
            })
        }, prepend: function () {
            return this.domManip(arguments, !0, function (e) {
                (this.nodeType === 1 || this.nodeType === 11) && this.insertBefore(e, this.firstChild)
            })
        }, before: function () {
            if (!ut(this[0])) return this.domManip(arguments, !1, function (e) {
                this.parentNode.insertBefore(e, this)
            });
            if (arguments.length) {
                var e = v.clean(arguments);
                return this.pushStack(v.merge(e, this), "before", this.selector)
            }
        }, after: function () {
            if (!ut(this[0])) return this.domManip(arguments, !1, function (e) {
                this.parentNode.insertBefore(e, this.nextSibling)
            });
            if (arguments.length) {
                var e = v.clean(arguments);
                return this.pushStack(v.merge(this, e), "after", this.selector)
            }
        }, remove: function (e, t) {
            var n, r = 0;
            for (; (n = this[r]) != null; r++) if (!e || v.filter(e, [n]).length) !t && n.nodeType === 1 && (v.cleanData(n.getElementsByTagName("*")), v.cleanData([n])), n.parentNode && n.parentNode.removeChild(n);
            return this
        }, empty: function () {
            var e, t = 0;
            for (; (e = this[t]) != null; t++) {
                e.nodeType === 1 && v.cleanData(e.getElementsByTagName("*"));
                while (e.firstChild) e.removeChild(e.firstChild)
            }
            return this
        }, clone: function (e, t) {
            return e = e == null ? !1 : e, t = t == null ? e : t, this.map(function () {
                return v.clone(this, e, t)
            })
        }, html: function (e) {
            return v.access(this, function (e) {
                var n = this[0] || {}, r = 0, i = this.length;
                if (e === t) return n.nodeType === 1 ? n.innerHTML.replace(ht, "") : t;
                if (typeof e == "string" && !yt.test(e) && (v.support.htmlSerialize || !wt.test(e)) && (v.support.leadingWhitespace || !pt.test(e)) && !Nt[(vt.exec(e) || ["", ""])[1].toLowerCase()]) {
                    e = e.replace(dt, "<$1></$2>");
                    try {
                        for (; r < i; r++) n = this[r] || {}, n.nodeType === 1 && (v.cleanData(n.getElementsByTagName("*")), n.innerHTML = e);
                        n = 0
                    } catch (s) {
                    }
                }
                n && this.empty().append(e)
            }, null, e, arguments.length)
        }, replaceWith: function (e) {
            return ut(this[0]) ? this.length ? this.pushStack(v(v.isFunction(e) ? e() : e), "replaceWith", e) : this : v.isFunction(e) ? this.each(function (t) {
                var n = v(this), r = n.html();
                n.replaceWith(e.call(this, t, r))
            }) : (typeof e != "string" && (e = v(e).detach()), this.each(function () {
                var t = this.nextSibling, n = this.parentNode;
                v(this).remove(), t ? v(t).before(e) : v(n).append(e)
            }))
        }, detach: function (e) {
            return this.remove(e, !0)
        }, domManip: function (e, n, r) {
            e = [].concat.apply([], e);
            var i, s, o, u, a = 0, f = e[0], l = [], c = this.length;
            if (!v.support.checkClone && c > 1 && typeof f == "string" && St.test(f)) return this.each(function () {
                v(this).domManip(e, n, r)
            });
            if (v.isFunction(f)) return this.each(function (i) {
                var s = v(this);
                e[0] = f.call(this, i, n ? s.html() : t), s.domManip(e, n, r)
            });
            if (this[0]) {
                i = v.buildFragment(e, this, l), o = i.fragment, s = o.firstChild, o.childNodes.length === 1 && (o = s);
                if (s) {
                    n = n && v.nodeName(s, "tr");
                    for (u = i.cacheable || c - 1; a < c; a++) r.call(n && v.nodeName(this[a], "table") ? Lt(this[a], "tbody") : this[a], a === u ? o : v.clone(o, !0, !0))
                }
                o = s = null, l.length && v.each(l, function (e, t) {
                    t.src ? v.ajax ? v.ajax({
                        url: t.src,
                        type: "GET",
                        dataType: "script",
                        async: !1,
                        global: !1,
                        "throws": !0
                    }) : v.error("no ajax") : v.globalEval((t.text || t.textContent || t.innerHTML || "").replace(Tt, "")), t.parentNode && t.parentNode.removeChild(t)
                })
            }
            return this
        }
    }), v.buildFragment = function (e, n, r) {
        var s, o, u, a = e[0];
        return n = n || i, n = !n.nodeType && n[0] || n, n = n.ownerDocument || n, e.length === 1 && typeof a == "string" && a.length < 512 && n === i && a.charAt(0) === "<" && !bt.test(a) && (v.support.checkClone || !St.test(a)) && (v.support.html5Clone || !wt.test(a)) && (o = !0, s = v.fragments[a], u = s !== t), s || (s = n.createDocumentFragment(), v.clean(e, n, s, r), o && (v.fragments[a] = u && s)), {
            fragment: s,
            cacheable: o
        }
    }, v.fragments = {}, v.each({
        appendTo: "append",
        prependTo: "prepend",
        insertBefore: "before",
        insertAfter: "after",
        replaceAll: "replaceWith"
    }, function (e, t) {
        v.fn[e] = function (n) {
            var r, i = 0, s = [], o = v(n), u = o.length, a = this.length === 1 && this[0].parentNode;
            if ((a == null || a && a.nodeType === 11 && a.childNodes.length === 1) && u === 1) return o[t](this[0]), this;
            for (; i < u; i++) r = (i > 0 ? this.clone(!0) : this).get(), v(o[i])[t](r), s = s.concat(r);
            return this.pushStack(s, e, o.selector)
        }
    }), v.extend({
        clone: function (e, t, n) {
            var r, i, s, o;
            v.support.html5Clone || v.isXMLDoc(e) || !wt.test("<" + e.nodeName + ">") ? o = e.cloneNode(!0) : (kt.innerHTML = e.outerHTML, kt.removeChild(o = kt.firstChild));
            if ((!v.support.noCloneEvent || !v.support.noCloneChecked) && (e.nodeType === 1 || e.nodeType === 11) && !v.isXMLDoc(e)) {
                Ot(e, o), r = Mt(e), i = Mt(o);
                for (s = 0; r[s]; ++s) i[s] && Ot(r[s], i[s])
            }
            if (t) {
                At(e, o);
                if (n) {
                    r = Mt(e), i = Mt(o);
                    for (s = 0; r[s]; ++s) At(r[s], i[s])
                }
            }
            return r = i = null, o
        }, clean: function (e, t, n, r) {
            var s, o, u, a, f, l, c, h, p, d, m, g, y = t === i && Ct, b = [];
            if (!t || typeof t.createDocumentFragment == "undefined") t = i;
            for (s = 0; (u = e[s]) != null; s++) {
                typeof u == "number" && (u += "");
                if (!u) continue;
                if (typeof u == "string") if (!gt.test(u)) u = t.createTextNode(u); else {
                    y = y || lt(t), c = t.createElement("div"), y.appendChild(c), u = u.replace(dt, "<$1></$2>"), a = (vt.exec(u) || ["", ""])[1].toLowerCase(), f = Nt[a] || Nt._default, l = f[0], c.innerHTML = f[1] + u + f[2];
                    while (l--) c = c.lastChild;
                    if (!v.support.tbody) {
                        h = mt.test(u), p = a === "table" && !h ? c.firstChild && c.firstChild.childNodes : f[1] === "<table>" && !h ? c.childNodes : [];
                        for (o = p.length - 1; o >= 0; --o) v.nodeName(p[o], "tbody") && !p[o].childNodes.length && p[o].parentNode.removeChild(p[o])
                    }
                    !v.support.leadingWhitespace && pt.test(u) && c.insertBefore(t.createTextNode(pt.exec(u)[0]), c.firstChild), u = c.childNodes, c.parentNode.removeChild(c)
                }
                u.nodeType ? b.push(u) : v.merge(b, u)
            }
            c && (u = c = y = null);
            if (!v.support.appendChecked) for (s = 0; (u = b[s]) != null; s++) v.nodeName(u, "input") ? _t(u) : typeof u.getElementsByTagName != "undefined" && v.grep(u.getElementsByTagName("input"), _t);
            if (n) {
                m = function (e) {
                    if (!e.type || xt.test(e.type)) return r ? r.push(e.parentNode ? e.parentNode.removeChild(e) : e) : n.appendChild(e)
                };
                for (s = 0; (u = b[s]) != null; s++) if (!v.nodeName(u, "script") || !m(u)) n.appendChild(u), typeof u.getElementsByTagName != "undefined" && (g = v.grep(v.merge([], u.getElementsByTagName("script")), m), b.splice.apply(b, [s + 1, 0].concat(g)), s += g.length)
            }
            return b
        }, cleanData: function (e, t) {
            var n, r, i, s, o = 0, u = v.expando, a = v.cache, f = v.support.deleteExpando, l = v.event.special;
            for (; (i = e[o]) != null; o++) if (t || v.acceptData(i)) {
                r = i[u], n = r && a[r];
                if (n) {
                    if (n.events) for (s in n.events) l[s] ? v.event.remove(i, s) : v.removeEvent(i, s, n.handle);
                    a[r] && (delete a[r], f ? delete i[u] : i.removeAttribute ? i.removeAttribute(u) : i[u] = null, v.deletedIds.push(r))
                }
            }
        }
    }), function () {
        var e, t;
        v.uaMatch = function (e) {
            e = e.toLowerCase();
            var t = /(chrome)[ \/]([\w.]+)/.exec(e) || /(webkit)[ \/]([\w.]+)/.exec(e) || /(opera)(?:.*version|)[ \/]([\w.]+)/.exec(e) || /(msie) ([\w.]+)/.exec(e) || e.indexOf("compatible") < 0 && /(mozilla)(?:.*? rv:([\w.]+)|)/.exec(e) || [];
            return {browser: t[1] || "", version: t[2] || "0"}
        }, e = v.uaMatch(o.userAgent), t = {}, e.browser && (t[e.browser] = !0, t.version = e.version), t.chrome ? t.webkit = !0 : t.webkit && (t.safari = !0), v.browser = t, v.sub = function () {
            function e(t, n) {
                return new e.fn.init(t, n)
            }

            v.extend(!0, e, this), e.superclass = this, e.fn = e.prototype = this(), e.fn.constructor = e, e.sub = this.sub, e.fn.init = function (r, i) {
                return i && i instanceof v && !(i instanceof e) && (i = e(i)), v.fn.init.call(this, r, i, t)
            }, e.fn.init.prototype = e.fn;
            var t = e(i);
            return e
        }
    }();
    var Dt, Pt, Ht, Bt = /alpha\([^)]*\)/i, jt = /opacity=([^)]*)/, Ft = /^(top|right|bottom|left)$/,
        It = /^(none|table(?!-c[ea]).+)/, qt = /^margin/, Rt = new RegExp("^(" + m + ")(.*)$", "i"),
        Ut = new RegExp("^(" + m + ")(?!px)[a-z%]+$", "i"), zt = new RegExp("^([-+])=(" + m + ")", "i"),
        Wt = {BODY: "block"}, Xt = {position: "absolute", visibility: "hidden", display: "block"},
        Vt = {letterSpacing: 0, fontWeight: 400}, $t = ["Top", "Right", "Bottom", "Left"],
        Jt = ["Webkit", "O", "Moz", "ms"], Kt = v.fn.toggle;
    v.fn.extend({
        css: function (e, n) {
            return v.access(this, function (e, n, r) {
                return r !== t ? v.style(e, n, r) : v.css(e, n)
            }, e, n, arguments.length > 1)
        }, show: function () {
            return Yt(this, !0)
        }, hide: function () {
            return Yt(this)
        }, toggle: function (e, t) {
            var n = typeof e == "boolean";
            return v.isFunction(e) && v.isFunction(t) ? Kt.apply(this, arguments) : this.each(function () {
                (n ? e : Gt(this)) ? v(this).show() : v(this).hide()
            })
        }
    }), v.extend({
        cssHooks: {
            opacity: {
                get: function (e, t) {
                    if (t) {
                        var n = Dt(e, "opacity");
                        return n === "" ? "1" : n
                    }
                }
            }
        },
        cssNumber: {
            fillOpacity: !0,
            fontWeight: !0,
            lineHeight: !0,
            opacity: !0,
            orphans: !0,
            widows: !0,
            zIndex: !0,
            zoom: !0
        },
        cssProps: {"float": v.support.cssFloat ? "cssFloat" : "styleFloat"},
        style: function (e, n, r, i) {
            if (!e || e.nodeType === 3 || e.nodeType === 8 || !e.style) return;
            var s, o, u, a = v.camelCase(n), f = e.style;
            n = v.cssProps[a] || (v.cssProps[a] = Qt(f, a)), u = v.cssHooks[n] || v.cssHooks[a];
            if (r === t) return u && "get" in u && (s = u.get(e, !1, i)) !== t ? s : f[n];
            o = typeof r, o === "string" && (s = zt.exec(r)) && (r = (s[1] + 1) * s[2] + parseFloat(v.css(e, n)), o = "number");
            if (r == null || o === "number" && isNaN(r)) return;
            o === "number" && !v.cssNumber[a] && (r += "px");
            if (!u || !("set" in u) || (r = u.set(e, r, i)) !== t) try {
                f[n] = r
            } catch (l) {
            }
        },
        css: function (e, n, r, i) {
            var s, o, u, a = v.camelCase(n);
            return n = v.cssProps[a] || (v.cssProps[a] = Qt(e.style, a)), u = v.cssHooks[n] || v.cssHooks[a], u && "get" in u && (s = u.get(e, !0, i)), s === t && (s = Dt(e, n)), s === "normal" && n in Vt && (s = Vt[n]), r || i !== t ? (o = parseFloat(s), r || v.isNumeric(o) ? o || 0 : s) : s
        },
        swap: function (e, t, n) {
            var r, i, s = {};
            for (i in t) s[i] = e.style[i], e.style[i] = t[i];
            r = n.call(e);
            for (i in t) e.style[i] = s[i];
            return r
        }
    }), e.getComputedStyle ? Dt = function (t, n) {
        var r, i, s, o, u = e.getComputedStyle(t, null), a = t.style;
        return u && (r = u.getPropertyValue(n) || u[n], r === "" && !v.contains(t.ownerDocument, t) && (r = v.style(t, n)), Ut.test(r) && qt.test(n) && (i = a.width, s = a.minWidth, o = a.maxWidth, a.minWidth = a.maxWidth = a.width = r, r = u.width, a.width = i, a.minWidth = s, a.maxWidth = o)), r
    } : i.documentElement.currentStyle && (Dt = function (e, t) {
        var n, r, i = e.currentStyle && e.currentStyle[t], s = e.style;
        return i == null && s && s[t] && (i = s[t]), Ut.test(i) && !Ft.test(t) && (n = s.left, r = e.runtimeStyle && e.runtimeStyle.left, r && (e.runtimeStyle.left = e.currentStyle.left), s.left = t === "fontSize" ? "1em" : i, i = s.pixelLeft + "px", s.left = n, r && (e.runtimeStyle.left = r)), i === "" ? "auto" : i
    }), v.each(["height", "width"], function (e, t) {
        v.cssHooks[t] = {
            get: function (e, n, r) {
                if (n) return e.offsetWidth === 0 && It.test(Dt(e, "display")) ? v.swap(e, Xt, function () {
                    return tn(e, t, r)
                }) : tn(e, t, r)
            }, set: function (e, n, r) {
                return Zt(e, n, r ? en(e, t, r, v.support.boxSizing && v.css(e, "boxSizing") === "border-box") : 0)
            }
        }
    }), v.support.opacity || (v.cssHooks.opacity = {
        get: function (e, t) {
            return jt.test((t && e.currentStyle ? e.currentStyle.filter : e.style.filter) || "") ? .01 * parseFloat(RegExp.$1) + "" : t ? "1" : ""
        }, set: function (e, t) {
            var n = e.style, r = e.currentStyle, i = v.isNumeric(t) ? "alpha(opacity=" + t * 100 + ")" : "",
                s = r && r.filter || n.filter || "";
            n.zoom = 1;
            if (t >= 1 && v.trim(s.replace(Bt, "")) === "" && n.removeAttribute) {
                n.removeAttribute("filter");
                if (r && !r.filter) return
            }
            n.filter = Bt.test(s) ? s.replace(Bt, i) : s + " " + i
        }
    }), v(function () {
        v.support.reliableMarginRight || (v.cssHooks.marginRight = {
            get: function (e, t) {
                return v.swap(e, {display: "inline-block"}, function () {
                    if (t) return Dt(e, "marginRight")
                })
            }
        }), !v.support.pixelPosition && v.fn.position && v.each(["top", "left"], function (e, t) {
            v.cssHooks[t] = {
                get: function (e, n) {
                    if (n) {
                        var r = Dt(e, t);
                        return Ut.test(r) ? v(e).position()[t] + "px" : r
                    }
                }
            }
        })
    }), v.expr && v.expr.filters && (v.expr.filters.hidden = function (e) {
        return e.offsetWidth === 0 && e.offsetHeight === 0 || !v.support.reliableHiddenOffsets && (e.style && e.style.display || Dt(e, "display")) === "none"
    }, v.expr.filters.visible = function (e) {
        return !v.expr.filters.hidden(e)
    }), v.each({margin: "", padding: "", border: "Width"}, function (e, t) {
        v.cssHooks[e + t] = {
            expand: function (n) {
                var r, i = typeof n == "string" ? n.split(" ") : [n], s = {};
                for (r = 0; r < 4; r++) s[e + $t[r] + t] = i[r] || i[r - 2] || i[0];
                return s
            }
        }, qt.test(e) || (v.cssHooks[e + t].set = Zt)
    });
    var rn = /%20/g, sn = /\[\]$/, on = /\r?\n/g,
        un = /^(?:color|date|datetime|datetime-local|email|hidden|month|number|password|range|search|tel|text|time|url|week)$/i,
        an = /^(?:select|textarea)/i;
    v.fn.extend({
        serialize: function () {
            return v.param(this.serializeArray())
        }, serializeArray: function () {
            return this.map(function () {
                return this.elements ? v.makeArray(this.elements) : this
            }).filter(function () {
                return this.name && !this.disabled && (this.checked || an.test(this.nodeName) || un.test(this.type))
            }).map(function (e, t) {
                var n = v(this).val();
                return n == null ? null : v.isArray(n) ? v.map(n, function (e, n) {
                    return {name: t.name, value: e.replace(on, "\r\n")}
                }) : {name: t.name, value: n.replace(on, "\r\n")}
            }).get()
        }
    }), v.param = function (e, n) {
        var r, i = [], s = function (e, t) {
            t = v.isFunction(t) ? t() : t == null ? "" : t, i[i.length] = encodeURIComponent(e) + "=" + encodeURIComponent(t)
        };
        n === t && (n = v.ajaxSettings && v.ajaxSettings.traditional);
        if (v.isArray(e) || e.jquery && !v.isPlainObject(e)) v.each(e, function () {
            s(this.name, this.value)
        }); else for (r in e) fn(r, e[r], n, s);
        return i.join("&").replace(rn, "+")
    };
    var ln, cn, hn = /#.*$/, pn = /^(.*?):[ \t]*([^\r\n]*)\r?$/mg,
        dn = /^(?:about|app|app\-storage|.+\-extension|file|res|widget):$/, vn = /^(?:GET|HEAD)$/, mn = /^\/\//,
        gn = /\?/, yn = /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, bn = /([?&])_=[^&]*/,
        wn = /^([\w\+\.\-]+:)(?:\/\/([^\/?#:]*)(?::(\d+)|)|)/, En = v.fn.load, Sn = {}, xn = {}, Tn = ["*/"] + ["*"];
    try {
        cn = s.href
    } catch (Nn) {
        cn = i.createElement("a"), cn.href = "", cn = cn.href
    }
    ln = wn.exec(cn.toLowerCase()) || [], v.fn.load = function (e, n, r) {
        if (typeof e != "string" && En) return En.apply(this, arguments);
        if (!this.length) return this;
        var i, s, o, u = this, a = e.indexOf(" ");
        return a >= 0 && (i = e.slice(a, e.length), e = e.slice(0, a)), v.isFunction(n) ? (r = n, n = t) : n && typeof n == "object" && (s = "POST"), v.ajax({
            url: e,
            type: s,
            dataType: "html",
            data: n,
            complete: function (e, t) {
                r && u.each(r, o || [e.responseText, t, e])
            }
        }).done(function (e) {
            o = arguments, u.html(i ? v("<div>").append(e.replace(yn, "")).find(i) : e)
        }), this
    }, v.each("ajaxStart ajaxStop ajaxComplete ajaxError ajaxSuccess ajaxSend".split(" "), function (e, t) {
        v.fn[t] = function (e) {
            return this.on(t, e)
        }
    }), v.each(["get", "post"], function (e, n) {
        v[n] = function (e, r, i, s) {
            return v.isFunction(r) && (s = s || i, i = r, r = t), v.ajax({
                type: n,
                url: e,
                data: r,
                success: i,
                dataType: s
            })
        }
    }), v.extend({
        getScript: function (e, n) {
            return v.get(e, t, n, "script")
        },
        getJSON: function (e, t, n) {
            return v.get(e, t, n, "json")
        },
        ajaxSetup: function (e, t) {
            return t ? Ln(e, v.ajaxSettings) : (t = e, e = v.ajaxSettings), Ln(e, t), e
        },
        ajaxSettings: {
            url: cn,
            isLocal: dn.test(ln[1]),
            global: !0,
            type: "GET",
            contentType: "application/x-www-form-urlencoded; charset=UTF-8",
            processData: !0,
            async: !0,
            accepts: {
                xml: "application/xml, text/xml",
                html: "text/html",
                text: "text/plain",
                json: "application/json, text/javascript",
                "*": Tn
            },
            contents: {xml: /xml/, html: /html/, json: /json/},
            responseFields: {xml: "responseXML", text: "responseText"},
            converters: {"* text": e.String, "text html": !0, "text json": v.parseJSON, "text xml": v.parseXML},
            flatOptions: {context: !0, url: !0}
        },
        ajaxPrefilter: Cn(Sn),
        ajaxTransport: Cn(xn),
        ajax: function (e, n) {
            function T(e, n, s, a) {
                var l, y, b, w, S, T = n;
                if (E === 2) return;
                E = 2, u && clearTimeout(u), o = t, i = a || "", x.readyState = e > 0 ? 4 : 0, s && (w = An(c, x, s));
                if (e >= 200 && e < 300 || e === 304) c.ifModified && (S = x.getResponseHeader("Last-Modified"), S && (v.lastModified[r] = S), S = x.getResponseHeader("Etag"), S && (v.etag[r] = S)), e === 304 ? (T = "notmodified", l = !0) : (l = On(c, w), T = l.state, y = l.data, b = l.error, l = !b); else {
                    b = T;
                    if (!T || e) T = "error", e < 0 && (e = 0)
                }
                x.status = e, x.statusText = (n || T) + "", l ? d.resolveWith(h, [y, T, x]) : d.rejectWith(h, [x, T, b]), x.statusCode(g), g = t, f && p.trigger("ajax" + (l ? "Success" : "Error"), [x, c, l ? y : b]), m.fireWith(h, [x, T]), f && (p.trigger("ajaxComplete", [x, c]), --v.active || v.event.trigger("ajaxStop"))
            }

            typeof e == "object" && (n = e, e = t), n = n || {};
            var r, i, s, o, u, a, f, l, c = v.ajaxSetup({}, n), h = c.context || c,
                p = h !== c && (h.nodeType || h instanceof v) ? v(h) : v.event, d = v.Deferred(),
                m = v.Callbacks("once memory"), g = c.statusCode || {}, b = {}, w = {}, E = 0, S = "canceled", x = {
                    readyState: 0, setRequestHeader: function (e, t) {
                        if (!E) {
                            var n = e.toLowerCase();
                            e = w[n] = w[n] || e, b[e] = t
                        }
                        return this
                    }, getAllResponseHeaders: function () {
                        return E === 2 ? i : null
                    }, getResponseHeader: function (e) {
                        var n;
                        if (E === 2) {
                            if (!s) {
                                s = {};
                                while (n = pn.exec(i)) s[n[1].toLowerCase()] = n[2]
                            }
                            n = s[e.toLowerCase()]
                        }
                        return n === t ? null : n
                    }, overrideMimeType: function (e) {
                        return E || (c.mimeType = e), this
                    }, abort: function (e) {
                        return e = e || S, o && o.abort(e), T(0, e), this
                    }
                };
            d.promise(x), x.success = x.done, x.error = x.fail, x.complete = m.add, x.statusCode = function (e) {
                if (e) {
                    var t;
                    if (E < 2) for (t in e) g[t] = [g[t], e[t]]; else t = e[x.status], x.always(t)
                }
                return this
            }, c.url = ((e || c.url) + "").replace(hn, "").replace(mn, ln[1] + "//"), c.dataTypes = v.trim(c.dataType || "*").toLowerCase().split(y), c.crossDomain == null && (a = wn.exec(c.url.toLowerCase()), c.crossDomain = !(!a || a[1] === ln[1] && a[2] === ln[2] && (a[3] || (a[1] === "http:" ? 80 : 443)) == (ln[3] || (ln[1] === "http:" ? 80 : 443)))), c.data && c.processData && typeof c.data != "string" && (c.data = v.param(c.data, c.traditional)), kn(Sn, c, n, x);
            if (E === 2) return x;
            f = c.global, c.type = c.type.toUpperCase(), c.hasContent = !vn.test(c.type), f && v.active++ === 0 && v.event.trigger("ajaxStart");
            if (!c.hasContent) {
                c.data && (c.url += (gn.test(c.url) ? "&" : "?") + c.data, delete c.data), r = c.url;
                if (c.cache === !1) {
                    var N = v.now(), C = c.url.replace(bn, "$1_=" + N);
                    c.url = C + (C === c.url ? (gn.test(c.url) ? "&" : "?") + "_=" + N : "")
                }
            }
            (c.data && c.hasContent && c.contentType !== !1 || n.contentType) && x.setRequestHeader("Content-Type", c.contentType), c.ifModified && (r = r || c.url, v.lastModified[r] && x.setRequestHeader("If-Modified-Since", v.lastModified[r]), v.etag[r] && x.setRequestHeader("If-None-Match", v.etag[r])), x.setRequestHeader("Accept", c.dataTypes[0] && c.accepts[c.dataTypes[0]] ? c.accepts[c.dataTypes[0]] + (c.dataTypes[0] !== "*" ? ", " + Tn + "; q=0.01" : "") : c.accepts["*"]);
            for (l in c.headers) x.setRequestHeader(l, c.headers[l]);
            if (!c.beforeSend || c.beforeSend.call(h, x, c) !== !1 && E !== 2) {
                S = "abort";
                for (l in {success: 1, error: 1, complete: 1}) x[l](c[l]);
                o = kn(xn, c, n, x);
                if (!o) T(-1, "No Transport"); else {
                    x.readyState = 1, f && p.trigger("ajaxSend", [x, c]), c.async && c.timeout > 0 && (u = setTimeout(function () {
                        x.abort("timeout")
                    }, c.timeout));
                    try {
                        E = 1, o.send(b, T)
                    } catch (k) {
                        if (!(E < 2)) throw k;
                        T(-1, k)
                    }
                }
                return x
            }
            return x.abort()
        },
        active: 0,
        lastModified: {},
        etag: {}
    });
    var Mn = [], _n = /\?/, Dn = /(=)\?(?=&|$)|\?\?/, Pn = v.now();
    v.ajaxSetup({
        jsonp: "callback", jsonpCallback: function () {
            var e = Mn.pop() || v.expando + "_" + Pn++;
            return this[e] = !0, e
        }
    }), v.ajaxPrefilter("json jsonp", function (n, r, i) {
        var s, o, u, a = n.data, f = n.url, l = n.jsonp !== !1, c = l && Dn.test(f),
            h = l && !c && typeof a == "string" && !(n.contentType || "").indexOf("application/x-www-form-urlencoded") && Dn.test(a);
        if (n.dataTypes[0] === "jsonp" || c || h) return s = n.jsonpCallback = v.isFunction(n.jsonpCallback) ? n.jsonpCallback() : n.jsonpCallback, o = e[s], c ? n.url = f.replace(Dn, "$1" + s) : h ? n.data = a.replace(Dn, "$1" + s) : l && (n.url += (_n.test(f) ? "&" : "?") + n.jsonp + "=" + s), n.converters["script json"] = function () {
            return u || v.error(s + " was not called"), u[0]
        }, n.dataTypes[0] = "json", e[s] = function () {
            u = arguments
        }, i.always(function () {
            e[s] = o, n[s] && (n.jsonpCallback = r.jsonpCallback, Mn.push(s)), u && v.isFunction(o) && o(u[0]), u = o = t
        }), "script"
    }), v.ajaxSetup({
        accepts: {script: "text/javascript, application/javascript, application/ecmascript, application/x-ecmascript"},
        contents: {script: /javascript|ecmascript/},
        converters: {
            "text script": function (e) {
                return v.globalEval(e), e
            }
        }
    }), v.ajaxPrefilter("script", function (e) {
        e.cache === t && (e.cache = !1), e.crossDomain && (e.type = "GET", e.global = !1)
    }), v.ajaxTransport("script", function (e) {
        if (e.crossDomain) {
            var n, r = i.head || i.getElementsByTagName("head")[0] || i.documentElement;
            return {
                send: function (s, o) {
                    n = i.createElement("script"), n.async = "async", e.scriptCharset && (n.charset = e.scriptCharset), n.src = e.url, n.onload = n.onreadystatechange = function (e, i) {
                        if (i || !n.readyState || /loaded|complete/.test(n.readyState)) n.onload = n.onreadystatechange = null, r && n.parentNode && r.removeChild(n), n = t, i || o(200, "success")
                    }, r.insertBefore(n, r.firstChild)
                }, abort: function () {
                    n && n.onload(0, 1)
                }
            }
        }
    });
    var Hn, Bn = e.ActiveXObject ? function () {
        for (var e in Hn) Hn[e](0, 1)
    } : !1, jn = 0;
    v.ajaxSettings.xhr = e.ActiveXObject ? function () {
        return !this.isLocal && Fn() || In()
    } : Fn, function (e) {
        v.extend(v.support, {ajax: !!e, cors: !!e && "withCredentials" in e})
    }(v.ajaxSettings.xhr()), v.support.ajax && v.ajaxTransport(function (n) {
        if (!n.crossDomain || v.support.cors) {
            var r;
            return {
                send: function (i, s) {
                    var o, u, a = n.xhr();
                    n.username ? a.open(n.type, n.url, n.async, n.username, n.password) : a.open(n.type, n.url, n.async);
                    if (n.xhrFields) for (u in n.xhrFields) a[u] = n.xhrFields[u];
                    n.mimeType && a.overrideMimeType && a.overrideMimeType(n.mimeType), !n.crossDomain && !i["X-Requested-With"] && (i["X-Requested-With"] = "XMLHttpRequest");
                    try {
                        for (u in i) a.setRequestHeader(u, i[u])
                    } catch (f) {
                    }
                    a.send(n.hasContent && n.data || null), r = function (e, i) {
                        var u, f, l, c, h;
                        try {
                            if (r && (i || a.readyState === 4)) {
                                r = t, o && (a.onreadystatechange = v.noop, Bn && delete Hn[o]);
                                if (i) a.readyState !== 4 && a.abort(); else {
                                    u = a.status, l = a.getAllResponseHeaders(), c = {}, h = a.responseXML, h && h.documentElement && (c.xml = h);
                                    try {
                                        c.text = a.responseText
                                    } catch (p) {
                                    }
                                    try {
                                        f = a.statusText
                                    } catch (p) {
                                        f = ""
                                    }
                                    !u && n.isLocal && !n.crossDomain ? u = c.text ? 200 : 404 : u === 1223 && (u = 204)
                                }
                            }
                        } catch (d) {
                            i || s(-1, d)
                        }
                        c && s(u, f, c, l)
                    }, n.async ? a.readyState === 4 ? setTimeout(r, 0) : (o = ++jn, Bn && (Hn || (Hn = {}, v(e).unload(Bn)), Hn[o] = r), a.onreadystatechange = r) : r()
                }, abort: function () {
                    r && r(0, 1)
                }
            }
        }
    });
    var qn, Rn, Un = /^(?:toggle|show|hide)$/, zn = new RegExp("^(?:([-+])=|)(" + m + ")([a-z%]*)$", "i"),
        Wn = /queueHooks$/, Xn = [Gn], Vn = {
            "*": [function (e, t) {
                var n, r, i = this.createTween(e, t), s = zn.exec(t), o = i.cur(), u = +o || 0, a = 1, f = 20;
                if (s) {
                    n = +s[2], r = s[3] || (v.cssNumber[e] ? "" : "px");
                    if (r !== "px" && u) {
                        u = v.css(i.elem, e, !0) || n || 1;
                        do a = a || ".5", u /= a, v.style(i.elem, e, u + r); while (a !== (a = i.cur() / o) && a !== 1 && --f)
                    }
                    i.unit = r, i.start = u, i.end = s[1] ? u + (s[1] + 1) * n : n
                }
                return i
            }]
        };
    v.Animation = v.extend(Kn, {
        tweener: function (e, t) {
            v.isFunction(e) ? (t = e, e = ["*"]) : e = e.split(" ");
            var n, r = 0, i = e.length;
            for (; r < i; r++) n = e[r], Vn[n] = Vn[n] || [], Vn[n].unshift(t)
        }, prefilter: function (e, t) {
            t ? Xn.unshift(e) : Xn.push(e)
        }
    }), v.Tween = Yn, Yn.prototype = {
        constructor: Yn, init: function (e, t, n, r, i, s) {
            this.elem = e, this.prop = n, this.easing = i || "swing", this.options = t, this.start = this.now = this.cur(), this.end = r, this.unit = s || (v.cssNumber[n] ? "" : "px")
        }, cur: function () {
            var e = Yn.propHooks[this.prop];
            return e && e.get ? e.get(this) : Yn.propHooks._default.get(this)
        }, run: function (e) {
            var t, n = Yn.propHooks[this.prop];
            return this.options.duration ? this.pos = t = v.easing[this.easing](e, this.options.duration * e, 0, 1, this.options.duration) : this.pos = t = e, this.now = (this.end - this.start) * t + this.start, this.options.step && this.options.step.call(this.elem, this.now, this), n && n.set ? n.set(this) : Yn.propHooks._default.set(this), this
        }
    }, Yn.prototype.init.prototype = Yn.prototype, Yn.propHooks = {
        _default: {
            get: function (e) {
                var t;
                return e.elem[e.prop] == null || !!e.elem.style && e.elem.style[e.prop] != null ? (t = v.css(e.elem, e.prop, !1, ""), !t || t === "auto" ? 0 : t) : e.elem[e.prop]
            }, set: function (e) {
                v.fx.step[e.prop] ? v.fx.step[e.prop](e) : e.elem.style && (e.elem.style[v.cssProps[e.prop]] != null || v.cssHooks[e.prop]) ? v.style(e.elem, e.prop, e.now + e.unit) : e.elem[e.prop] = e.now
            }
        }
    }, Yn.propHooks.scrollTop = Yn.propHooks.scrollLeft = {
        set: function (e) {
            e.elem.nodeType && e.elem.parentNode && (e.elem[e.prop] = e.now)
        }
    }, v.each(["toggle", "show", "hide"], function (e, t) {
        var n = v.fn[t];
        v.fn[t] = function (r, i, s) {
            return r == null || typeof r == "boolean" || !e && v.isFunction(r) && v.isFunction(i) ? n.apply(this, arguments) : this.animate(Zn(t, !0), r, i, s)
        }
    }), v.fn.extend({
        fadeTo: function (e, t, n, r) {
            return this.filter(Gt).css("opacity", 0).show().end().animate({opacity: t}, e, n, r)
        }, animate: function (e, t, n, r) {
            var i = v.isEmptyObject(e), s = v.speed(t, n, r), o = function () {
                var t = Kn(this, v.extend({}, e), s);
                i && t.stop(!0)
            };
            return i || s.queue === !1 ? this.each(o) : this.queue(s.queue, o)
        }, stop: function (e, n, r) {
            var i = function (e) {
                var t = e.stop;
                delete e.stop, t(r)
            };
            return typeof e != "string" && (r = n, n = e, e = t), n && e !== !1 && this.queue(e || "fx", []), this.each(function () {
                var t = !0, n = e != null && e + "queueHooks", s = v.timers, o = v._data(this);
                if (n) o[n] && o[n].stop && i(o[n]); else for (n in o) o[n] && o[n].stop && Wn.test(n) && i(o[n]);
                for (n = s.length; n--;) s[n].elem === this && (e == null || s[n].queue === e) && (s[n].anim.stop(r), t = !1, s.splice(n, 1));
                (t || !r) && v.dequeue(this, e)
            })
        }
    }), v.each({
        slideDown: Zn("show"),
        slideUp: Zn("hide"),
        slideToggle: Zn("toggle"),
        fadeIn: {opacity: "show"},
        fadeOut: {opacity: "hide"},
        fadeToggle: {opacity: "toggle"}
    }, function (e, t) {
        v.fn[e] = function (e, n, r) {
            return this.animate(t, e, n, r)
        }
    }), v.speed = function (e, t, n) {
        var r = e && typeof e == "object" ? v.extend({}, e) : {
            complete: n || !n && t || v.isFunction(e) && e,
            duration: e,
            easing: n && t || t && !v.isFunction(t) && t
        };
        r.duration = v.fx.off ? 0 : typeof r.duration == "number" ? r.duration : r.duration in v.fx.speeds ? v.fx.speeds[r.duration] : v.fx.speeds._default;
        if (r.queue == null || r.queue === !0) r.queue = "fx";
        return r.old = r.complete, r.complete = function () {
            v.isFunction(r.old) && r.old.call(this), r.queue && v.dequeue(this, r.queue)
        }, r
    }, v.easing = {
        linear: function (e) {
            return e
        }, swing: function (e) {
            return .5 - Math.cos(e * Math.PI) / 2
        }
    }, v.timers = [], v.fx = Yn.prototype.init, v.fx.tick = function () {
        var e, n = v.timers, r = 0;
        qn = v.now();
        for (; r < n.length; r++) e = n[r], !e() && n[r] === e && n.splice(r--, 1);
        n.length || v.fx.stop(), qn = t
    }, v.fx.timer = function (e) {
        e() && v.timers.push(e) && !Rn && (Rn = setInterval(v.fx.tick, v.fx.interval))
    }, v.fx.interval = 13, v.fx.stop = function () {
        clearInterval(Rn), Rn = null
    }, v.fx.speeds = {
        slow: 600,
        fast: 200,
        _default: 400
    }, v.fx.step = {}, v.expr && v.expr.filters && (v.expr.filters.animated = function (e) {
        return v.grep(v.timers, function (t) {
            return e === t.elem
        }).length
    });
    var er = /^(?:body|html)$/i;
    v.fn.offset = function (e) {
        if (arguments.length) return e === t ? this : this.each(function (t) {
            v.offset.setOffset(this, e, t)
        });
        var n, r, i, s, o, u, a, f = {top: 0, left: 0}, l = this[0], c = l && l.ownerDocument;
        if (!c) return;
        return (r = c.body) === l ? v.offset.bodyOffset(l) : (n = c.documentElement, v.contains(n, l) ? (typeof l.getBoundingClientRect != "undefined" && (f = l.getBoundingClientRect()), i = tr(c), s = n.clientTop || r.clientTop || 0, o = n.clientLeft || r.clientLeft || 0, u = i.pageYOffset || n.scrollTop, a = i.pageXOffset || n.scrollLeft, {
            top: f.top + u - s,
            left: f.left + a - o
        }) : f)
    }, v.offset = {
        bodyOffset: function (e) {
            var t = e.offsetTop, n = e.offsetLeft;
            return v.support.doesNotIncludeMarginInBodyOffset && (t += parseFloat(v.css(e, "marginTop")) || 0, n += parseFloat(v.css(e, "marginLeft")) || 0), {
                top: t,
                left: n
            }
        }, setOffset: function (e, t, n) {
            var r = v.css(e, "position");
            r === "static" && (e.style.position = "relative");
            var i = v(e), s = i.offset(), o = v.css(e, "top"), u = v.css(e, "left"),
                a = (r === "absolute" || r === "fixed") && v.inArray("auto", [o, u]) > -1, f = {}, l = {}, c, h;
            a ? (l = i.position(), c = l.top, h = l.left) : (c = parseFloat(o) || 0, h = parseFloat(u) || 0), v.isFunction(t) && (t = t.call(e, n, s)), t.top != null && (f.top = t.top - s.top + c), t.left != null && (f.left = t.left - s.left + h), "using" in t ? t.using.call(e, f) : i.css(f)
        }
    }, v.fn.extend({
        position: function () {
            if (!this[0]) return;
            var e = this[0], t = this.offsetParent(), n = this.offset(),
                r = er.test(t[0].nodeName) ? {top: 0, left: 0} : t.offset();
            return n.top -= parseFloat(v.css(e, "marginTop")) || 0, n.left -= parseFloat(v.css(e, "marginLeft")) || 0, r.top += parseFloat(v.css(t[0], "borderTopWidth")) || 0, r.left += parseFloat(v.css(t[0], "borderLeftWidth")) || 0, {
                top: n.top - r.top,
                left: n.left - r.left
            }
        }, offsetParent: function () {
            return this.map(function () {
                var e = this.offsetParent || i.body;
                while (e && !er.test(e.nodeName) && v.css(e, "position") === "static") e = e.offsetParent;
                return e || i.body
            })
        }
    }), v.each({scrollLeft: "pageXOffset", scrollTop: "pageYOffset"}, function (e, n) {
        var r = /Y/.test(n);
        v.fn[e] = function (i) {
            return v.access(this, function (e, i, s) {
                var o = tr(e);
                if (s === t) return o ? n in o ? o[n] : o.document.documentElement[i] : e[i];
                o ? o.scrollTo(r ? v(o).scrollLeft() : s, r ? s : v(o).scrollTop()) : e[i] = s
            }, e, i, arguments.length, null)
        }
    }), v.each({Height: "height", Width: "width"}, function (e, n) {
        v.each({padding: "inner" + e, content: n, "": "outer" + e}, function (r, i) {
            v.fn[i] = function (i, s) {
                var o = arguments.length && (r || typeof i != "boolean"),
                    u = r || (i === !0 || s === !0 ? "margin" : "border");
                return v.access(this, function (n, r, i) {
                    var s;
                    return v.isWindow(n) ? n.document.documentElement["client" + e] : n.nodeType === 9 ? (s = n.documentElement, Math.max(n.body["scroll" + e], s["scroll" + e], n.body["offset" + e], s["offset" + e], s["client" + e])) : i === t ? v.css(n, r, i, u) : v.style(n, r, i, u)
                }, n, o ? i : t, o, null)
            }
        })
    }), e.jQuery = e.$ = v, typeof define == "function" && define.amd && define.amd.jQuery && define("jquery", [], function () {
        return v
    })
})(window);
/*! jQuery UI - v1.9.2 - 2013-05-03
* http://jqueryui.com
* Includes: jquery.ui.core.js
* Copyright 2013 jQuery Foundation and other contributors; Licensed MIT */
(function (e, t) {
    function i(t, n) {
        var r, i, o, u = t.nodeName.toLowerCase();
        return "area" === u ? (r = t.parentNode, i = r.name, !t.href || !i || r.nodeName.toLowerCase() !== "map" ? !1 : (o = e("img[usemap=#" + i + "]")[0], !!o && s(o))) : (/input|select|textarea|button|object/.test(u) ? !t.disabled : "a" === u ? t.href || n : n) && s(t)
    }

    function s(t) {
        return e.expr.filters.visible(t) && !e(t).parents().andSelf().filter(function () {
            return e.css(this, "visibility") === "hidden"
        }).length
    }

    var n = 0, r = /^ui-id-\d+$/;
    e.ui = e.ui || {};
    if (e.ui.version) return;
    e.extend(e.ui, {
        version: "1.9.2",
        keyCode: {
            BACKSPACE: 8,
            COMMA: 188,
            DELETE: 46,
            DOWN: 40,
            END: 35,
            ENTER: 13,
            ESCAPE: 27,
            HOME: 36,
            LEFT: 37,
            NUMPAD_ADD: 107,
            NUMPAD_DECIMAL: 110,
            NUMPAD_DIVIDE: 111,
            NUMPAD_ENTER: 108,
            NUMPAD_MULTIPLY: 106,
            NUMPAD_SUBTRACT: 109,
            PAGE_DOWN: 34,
            PAGE_UP: 33,
            PERIOD: 190,
            RIGHT: 39,
            SPACE: 32,
            TAB: 9,
            UP: 38
        }
    }), e.fn.extend({
        _focus: e.fn.focus, focus: function (t, n) {
            return typeof t == "number" ? this.each(function () {
                var r = this;
                setTimeout(function () {
                    e(r).focus(), n && n.call(r)
                }, t)
            }) : this._focus.apply(this, arguments)
        }, scrollParent: function () {
            var t;
            return e.ui.ie && /(static|relative)/.test(this.css("position")) || /absolute/.test(this.css("position")) ? t = this.parents().filter(function () {
                return /(relative|absolute|fixed)/.test(e.css(this, "position")) && /(auto|scroll)/.test(e.css(this, "overflow") + e.css(this, "overflow-y") + e.css(this, "overflow-x"))
            }).eq(0) : t = this.parents().filter(function () {
                return /(auto|scroll)/.test(e.css(this, "overflow") + e.css(this, "overflow-y") + e.css(this, "overflow-x"))
            }).eq(0), /fixed/.test(this.css("position")) || !t.length ? e(document) : t
        }, zIndex: function (n) {
            if (n !== t) return this.css("zIndex", n);
            if (this.length) {
                var r = e(this[0]), i, s;
                while (r.length && r[0] !== document) {
                    i = r.css("position");
                    if (i === "absolute" || i === "relative" || i === "fixed") {
                        s = parseInt(r.css("zIndex"), 10);
                        if (!isNaN(s) && s !== 0) return s
                    }
                    r = r.parent()
                }
            }
            return 0
        }, uniqueId: function () {
            return this.each(function () {
                this.id || (this.id = "ui-id-" + ++n)
            })
        }, removeUniqueId: function () {
            return this.each(function () {
                r.test(this.id) && e(this).removeAttr("id")
            })
        }
    }), e.extend(e.expr[":"], {
        data: e.expr.createPseudo ? e.expr.createPseudo(function (t) {
            return function (n) {
                return !!e.data(n, t)
            }
        }) : function (t, n, r) {
            return !!e.data(t, r[3])
        }, focusable: function (t) {
            return i(t, !isNaN(e.attr(t, "tabindex")))
        }, tabbable: function (t) {
            var n = e.attr(t, "tabindex"), r = isNaN(n);
            return (r || n >= 0) && i(t, !r)
        }
    }), e(function () {
        var t = document.body, n = t.appendChild(n = document.createElement("div"));
        n.offsetHeight, e.extend(n.style, {
            minHeight: "100px",
            height: "auto",
            padding: 0,
            borderWidth: 0
        }), e.support.minHeight = n.offsetHeight === 100, e.support.selectstart = "onselectstart" in n, t.removeChild(n).style.display = "none"
    }), e("<a>").outerWidth(1).jquery || e.each(["Width", "Height"], function (n, r) {
        function u(t, n, r, s) {
            return e.each(i, function () {
                n -= parseFloat(e.css(t, "padding" + this)) || 0, r && (n -= parseFloat(e.css(t, "border" + this + "Width")) || 0), s && (n -= parseFloat(e.css(t, "margin" + this)) || 0)
            }), n
        }

        var i = r === "Width" ? ["Left", "Right"] : ["Top", "Bottom"], s = r.toLowerCase(), o = {
            innerWidth: e.fn.innerWidth,
            innerHeight: e.fn.innerHeight,
            outerWidth: e.fn.outerWidth,
            outerHeight: e.fn.outerHeight
        };
        e.fn["inner" + r] = function (n) {
            return n === t ? o["inner" + r].call(this) : this.each(function () {
                e(this).css(s, u(this, n) + "px")
            })
        }, e.fn["outer" + r] = function (t, n) {
            return typeof t != "number" ? o["outer" + r].call(this, t) : this.each(function () {
                e(this).css(s, u(this, t, !0, n) + "px")
            })
        }
    }), e("<a>").data("a-b", "a").removeData("a-b").data("a-b") && (e.fn.removeData = function (t) {
        return function (n) {
            return arguments.length ? t.call(this, e.camelCase(n)) : t.call(this)
        }
    }(e.fn.removeData)), function () {
        var t = /msie ([\w.]+)/.exec(navigator.userAgent.toLowerCase()) || [];
        e.ui.ie = t.length ? !0 : !1, e.ui.ie6 = parseFloat(t[1], 10) === 6
    }(), e.fn.extend({
        disableSelection: function () {
            return this.bind((e.support.selectstart ? "selectstart" : "mousedown") + ".ui-disableSelection", function (e) {
                e.preventDefault()
            })
        }, enableSelection: function () {
            return this.unbind(".ui-disableSelection")
        }
    }), e.extend(e.ui, {
        plugin: {
            add: function (t, n, r) {
                var i, s = e.ui[t].prototype;
                for (i in r) s.plugins[i] = s.plugins[i] || [], s.plugins[i].push([n, r[i]])
            }, call: function (e, t, n) {
                var r, i = e.plugins[t];
                if (!i || !e.element[0].parentNode || e.element[0].parentNode.nodeType === 11) return;
                for (r = 0; r < i.length; r++) e.options[i[r][0]] && i[r][1].apply(e.element, n)
            }
        }, contains: e.contains, hasScroll: function (t, n) {
            if (e(t).css("overflow") === "hidden") return !1;
            var r = n && n === "left" ? "scrollLeft" : "scrollTop", i = !1;
            return t[r] > 0 ? !0 : (t[r] = 1, i = t[r] > 0, t[r] = 0, i)
        }, isOverAxis: function (e, t, n) {
            return e > t && e < t + n
        }, isOver: function (t, n, r, i, s, o) {
            return e.ui.isOverAxis(t, r, s) && e.ui.isOverAxis(n, i, o)
        }
    })
})(jQuery);
/*! jQuery UI - v1.9.2 - 2013-05-03
* http://jqueryui.com
* Includes: jquery.ui.datepicker.js
* Copyright 2013 jQuery Foundation and other contributors; Licensed MIT */
(function ($, undefined) {
    function Datepicker() {
        this.debug = !1, this._curInst = null, this._keyEvent = !1, this._disabledInputs = [], this._datepickerShowing = !1, this._inDialog = !1, this._mainDivId = "ui-datepicker-div", this._inlineClass = "ui-datepicker-inline", this._appendClass = "ui-datepicker-append", this._triggerClass = "ui-datepicker-trigger", this._dialogClass = "ui-datepicker-dialog", this._disableClass = "ui-datepicker-disabled", this._unselectableClass = "ui-datepicker-unselectable", this._selectedClass = "ui-datepicker-selected", this._currentClass = "ui-datepicker-current-day", this._dayOverClass = "ui-datepicker-days-cell-over", this.regional = [], this.regional[""] = {
            closeText: "Done",
            prevText: "Prev",
            nextText: "Next",
            currentText: "Today",
            monthNames: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
            monthNamesShort: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
            dayNames: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
            dayNamesShort: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
            dayNamesMin: ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"],
            weekHeader: "Wk",
            dateFormat: "mm/dd/yy",
            firstDay: 0,
            isRTL: !1,
            showMonthAfterYear: !1,
            yearSuffix: ""
        }, this._defaults = {
            showOn: "focus",
            showAnim: "fadeIn",
            showOptions: {},
            defaultDate: null,
            appendText: "",
            buttonText: "...",
            buttonImage: "",
            buttonImageOnly: !1,
            hideIfNoPrevNext: !1,
            navigationAsDateFormat: !1,
            gotoCurrent: !1,
            changeMonth: !1,
            changeYear: !1,
            yearRange: "c-10:c+10",
            showOtherMonths: !1,
            selectOtherMonths: !1,
            showWeek: !1,
            calculateWeek: this.iso8601Week,
            shortYearCutoff: "+10",
            minDate: null,
            maxDate: null,
            duration: "fast",
            beforeShowDay: null,
            beforeShow: null,
            onSelect: null,
            onChangeMonthYear: null,
            onClose: null,
            numberOfMonths: 1,
            showCurrentAtPos: 0,
            stepMonths: 1,
            stepBigMonths: 12,
            altField: "",
            altFormat: "",
            constrainInput: !0,
            showButtonPanel: !1,
            autoSize: !1,
            disabled: !1
        }, $.extend(this._defaults, this.regional[""]), this.dpDiv = bindHover($('<div id="' + this._mainDivId + '" class="ui-datepicker ui-widget ui-widget-content ui-helper-clearfix ui-corner-all"></div>'))
    }

    function bindHover(e) {
        var t = "button, .ui-datepicker-prev, .ui-datepicker-next, .ui-datepicker-calendar td a";
        return e.delegate(t, "mouseout", function () {
            $(this).removeClass("ui-state-hover"), this.className.indexOf("ui-datepicker-prev") != -1 && $(this).removeClass("ui-datepicker-prev-hover"), this.className.indexOf("ui-datepicker-next") != -1 && $(this).removeClass("ui-datepicker-next-hover")
        }).delegate(t, "mouseover", function () {
            $.datepicker._isDisabledDatepicker(instActive.inline ? e.parent()[0] : instActive.input[0]) || ($(this).parents(".ui-datepicker-calendar").find("a").removeClass("ui-state-hover"), $(this).addClass("ui-state-hover"), this.className.indexOf("ui-datepicker-prev") != -1 && $(this).addClass("ui-datepicker-prev-hover"), this.className.indexOf("ui-datepicker-next") != -1 && $(this).addClass("ui-datepicker-next-hover"))
        })
    }

    function extendRemove(e, t) {
        $.extend(e, t);
        for (var n in t) if (t[n] == null || t[n] == undefined) e[n] = t[n];
        return e
    }

    $.extend($.ui, {datepicker: {version: "1.9.2"}});
    var PROP_NAME = "datepicker", dpuuid = (new Date).getTime(), instActive;
    $.extend(Datepicker.prototype, {
        markerClassName: "hasDatepicker",
        maxRows: 4,
        log: function () {
            this.debug && console.log.apply("", arguments)
        },
        _widgetDatepicker: function () {
            return this.dpDiv
        },
        setDefaults: function (e) {
            return extendRemove(this._defaults, e || {}), this
        },
        _attachDatepicker: function (target, settings) {
            var inlineSettings = null;
            for (var attrName in this._defaults) {
                var attrValue = target.getAttribute("date:" + attrName);
                if (attrValue) {
                    inlineSettings = inlineSettings || {};
                    try {
                        inlineSettings[attrName] = eval(attrValue)
                    } catch (err) {
                        inlineSettings[attrName] = attrValue
                    }
                }
            }
            var nodeName = target.nodeName.toLowerCase(), inline = nodeName == "div" || nodeName == "span";
            target.id || (this.uuid += 1, target.id = "dp" + this.uuid);
            var inst = this._newInst($(target), inline);
            inst.settings = $.extend({}, settings || {}, inlineSettings || {}), nodeName == "input" ? this._connectDatepicker(target, inst) : inline && this._inlineDatepicker(target, inst)
        },
        _newInst: function (e, t) {
            var n = e[0].id.replace(/([^A-Za-z0-9_-])/g, "\\\\$1");
            return {
                id: n,
                input: e,
                selectedDay: 0,
                selectedMonth: 0,
                selectedYear: 0,
                drawMonth: 0,
                drawYear: 0,
                inline: t,
                dpDiv: t ? bindHover($('<div class="' + this._inlineClass + ' ui-datepicker ui-widget ui-widget-content ui-helper-clearfix ui-corner-all"></div>')) : this.dpDiv
            }
        },
        _connectDatepicker: function (e, t) {
            var n = $(e);
            t.append = $([]), t.trigger = $([]);
            if (n.hasClass(this.markerClassName)) return;
            this._attachments(n, t), n.addClass(this.markerClassName).keydown(this._doKeyDown).keypress(this._doKeyPress).keyup(this._doKeyUp).bind("setData.datepicker", function (e, n, r) {
                t.settings[n] = r
            }).bind("getData.datepicker", function (e, n) {
                return this._get(t, n)
            }), this._autoSize(t), $.data(e, PROP_NAME, t), t.settings.disabled && this._disableDatepicker(e)
        },
        _attachments: function (e, t) {
            var n = this._get(t, "appendText"), r = this._get(t, "isRTL");
            t.append && t.append.remove(), n && (t.append = $('<span class="' + this._appendClass + '">' + n + "</span>"), e[r ? "before" : "after"](t.append)), e.unbind("focus", this._showDatepicker), t.trigger && t.trigger.remove();
            var i = this._get(t, "showOn");
            (i == "focus" || i == "both") && e.focus(this._showDatepicker);
            if (i == "button" || i == "both") {
                var s = this._get(t, "buttonText"), o = this._get(t, "buttonImage");
                t.trigger = $(this._get(t, "buttonImageOnly") ? $("<img/>").addClass(this._triggerClass).attr({
                    src: o,
                    alt: s,
                    title: s
                }) : $('<button type="button"></button>').addClass(this._triggerClass).html(o == "" ? s : $("<img/>").attr({
                    src: o,
                    alt: s,
                    title: s
                }))), e[r ? "before" : "after"](t.trigger), t.trigger.click(function () {
                    return $.datepicker._datepickerShowing && $.datepicker._lastInput == e[0] ? $.datepicker._hideDatepicker() : $.datepicker._datepickerShowing && $.datepicker._lastInput != e[0] ? ($.datepicker._hideDatepicker(), $.datepicker._showDatepicker(e[0])) : $.datepicker._showDatepicker(e[0]), !1
                })
            }
        },
        _autoSize: function (e) {
            if (this._get(e, "autoSize") && !e.inline) {
                var t = new Date(2009, 11, 20), n = this._get(e, "dateFormat");
                if (n.match(/[DM]/)) {
                    var r = function (e) {
                        var t = 0, n = 0;
                        for (var r = 0; r < e.length; r++) e[r].length > t && (t = e[r].length, n = r);
                        return n
                    };
                    t.setMonth(r(this._get(e, n.match(/MM/) ? "monthNames" : "monthNamesShort"))), t.setDate(r(this._get(e, n.match(/DD/) ? "dayNames" : "dayNamesShort")) + 20 - t.getDay())
                }
                e.input.attr("size", this._formatDate(e, t).length)
            }
        },
        _inlineDatepicker: function (e, t) {
            var n = $(e);
            if (n.hasClass(this.markerClassName)) return;
            n.addClass(this.markerClassName).append(t.dpDiv).bind("setData.datepicker", function (e, n, r) {
                t.settings[n] = r
            }).bind("getData.datepicker", function (e, n) {
                return this._get(t, n)
            }), $.data(e, PROP_NAME, t), this._setDate(t, this._getDefaultDate(t), !0), this._updateDatepicker(t), this._updateAlternate(t), t.settings.disabled && this._disableDatepicker(e), t.dpDiv.css("display", "block")
        },
        _dialogDatepicker: function (e, t, n, r, i) {
            var s = this._dialogInst;
            if (!s) {
                this.uuid += 1;
                var o = "dp" + this.uuid;
                this._dialogInput = $('<input type="text" id="' + o + '" style="position: absolute; top: -100px; width: 0px;"/>'), this._dialogInput.keydown(this._doKeyDown), $("body").append(this._dialogInput), s = this._dialogInst = this._newInst(this._dialogInput, !1), s.settings = {}, $.data(this._dialogInput[0], PROP_NAME, s)
            }
            extendRemove(s.settings, r || {}), t = t && t.constructor == Date ? this._formatDate(s, t) : t, this._dialogInput.val(t), this._pos = i ? i.length ? i : [i.pageX, i.pageY] : null;
            if (!this._pos) {
                var u = document.documentElement.clientWidth, a = document.documentElement.clientHeight,
                    f = document.documentElement.scrollLeft || document.body.scrollLeft,
                    l = document.documentElement.scrollTop || document.body.scrollTop;
                this._pos = [u / 2 - 100 + f, a / 2 - 150 + l]
            }
            return this._dialogInput.css("left", this._pos[0] + 20 + "px").css("top", this._pos[1] + "px"), s.settings.onSelect = n, this._inDialog = !0, this.dpDiv.addClass(this._dialogClass), this._showDatepicker(this._dialogInput[0]), $.blockUI && $.blockUI(this.dpDiv), $.data(this._dialogInput[0], PROP_NAME, s), this
        },
        _destroyDatepicker: function (e) {
            var t = $(e), n = $.data(e, PROP_NAME);
            if (!t.hasClass(this.markerClassName)) return;
            var r = e.nodeName.toLowerCase();
            $.removeData(e, PROP_NAME), r == "input" ? (n.append.remove(), n.trigger.remove(), t.removeClass(this.markerClassName).unbind("focus", this._showDatepicker).unbind("keydown", this._doKeyDown).unbind("keypress", this._doKeyPress).unbind("keyup", this._doKeyUp)) : (r == "div" || r == "span") && t.removeClass(this.markerClassName).empty()
        },
        _enableDatepicker: function (e) {
            var t = $(e), n = $.data(e, PROP_NAME);
            if (!t.hasClass(this.markerClassName)) return;
            var r = e.nodeName.toLowerCase();
            if (r == "input") e.disabled = !1, n.trigger.filter("button").each(function () {
                this.disabled = !1
            }).end().filter("img").css({opacity: "1.0", cursor: ""}); else if (r == "div" || r == "span") {
                var i = t.children("." + this._inlineClass);
                i.children().removeClass("ui-state-disabled"), i.find("select.ui-datepicker-month, select.ui-datepicker-year").prop("disabled", !1)
            }
            this._disabledInputs = $.map(this._disabledInputs, function (t) {
                return t == e ? null : t
            })
        },
        _disableDatepicker: function (e) {
            var t = $(e), n = $.data(e, PROP_NAME);
            if (!t.hasClass(this.markerClassName)) return;
            var r = e.nodeName.toLowerCase();
            if (r == "input") e.disabled = !0, n.trigger.filter("button").each(function () {
                this.disabled = !0
            }).end().filter("img").css({opacity: "0.5", cursor: "default"}); else if (r == "div" || r == "span") {
                var i = t.children("." + this._inlineClass);
                i.children().addClass("ui-state-disabled"), i.find("select.ui-datepicker-month, select.ui-datepicker-year").prop("disabled", !0)
            }
            this._disabledInputs = $.map(this._disabledInputs, function (t) {
                return t == e ? null : t
            }), this._disabledInputs[this._disabledInputs.length] = e
        },
        _isDisabledDatepicker: function (e) {
            if (!e) return !1;
            for (var t = 0; t < this._disabledInputs.length; t++) if (this._disabledInputs[t] == e) return !0;
            return !1
        },
        _getInst: function (e) {
            try {
                return $.data(e, PROP_NAME)
            } catch (t) {
                throw"Missing instance data for this datepicker"
            }
        },
        _optionDatepicker: function (e, t, n) {
            var r = this._getInst(e);
            if (arguments.length == 2 && typeof t == "string") return t == "defaults" ? $.extend({}, $.datepicker._defaults) : r ? t == "all" ? $.extend({}, r.settings) : this._get(r, t) : null;
            var i = t || {};
            typeof t == "string" && (i = {}, i[t] = n);
            if (r) {
                this._curInst == r && this._hideDatepicker();
                var s = this._getDateDatepicker(e, !0), o = this._getMinMaxDate(r, "min"),
                    u = this._getMinMaxDate(r, "max");
                extendRemove(r.settings, i), o !== null && i.dateFormat !== undefined && i.minDate === undefined && (r.settings.minDate = this._formatDate(r, o)), u !== null && i.dateFormat !== undefined && i.maxDate === undefined && (r.settings.maxDate = this._formatDate(r, u)), this._attachments($(e), r), this._autoSize(r), this._setDate(r, s), this._updateAlternate(r), this._updateDatepicker(r)
            }
        },
        _changeDatepicker: function (e, t, n) {
            this._optionDatepicker(e, t, n)
        },
        _refreshDatepicker: function (e) {
            var t = this._getInst(e);
            t && this._updateDatepicker(t)
        },
        _setDateDatepicker: function (e, t) {
            var n = this._getInst(e);
            n && (this._setDate(n, t), this._updateDatepicker(n), this._updateAlternate(n))
        },
        _getDateDatepicker: function (e, t) {
            var n = this._getInst(e);
            return n && !n.inline && this._setDateFromField(n, t), n ? this._getDate(n) : null
        },
        _doKeyDown: function (e) {
            var t = $.datepicker._getInst(e.target), n = !0, r = t.dpDiv.is(".ui-datepicker-rtl");
            t._keyEvent = !0;
            if ($.datepicker._datepickerShowing) switch (e.keyCode) {
                case 9:
                    $.datepicker._hideDatepicker(), n = !1;
                    break;
                case 13:
                    var i = $("td." + $.datepicker._dayOverClass + ":not(." + $.datepicker._currentClass + ")", t.dpDiv);
                    i[0] && $.datepicker._selectDay(e.target, t.selectedMonth, t.selectedYear, i[0]);
                    var s = $.datepicker._get(t, "onSelect");
                    if (s) {
                        var o = $.datepicker._formatDate(t);
                        s.apply(t.input ? t.input[0] : null, [o, t])
                    } else $.datepicker._hideDatepicker();
                    return !1;
                case 27:
                    $.datepicker._hideDatepicker();
                    break;
                case 33:
                    $.datepicker._adjustDate(e.target, e.ctrlKey ? -$.datepicker._get(t, "stepBigMonths") : -$.datepicker._get(t, "stepMonths"), "M");
                    break;
                case 34:
                    $.datepicker._adjustDate(e.target, e.ctrlKey ? +$.datepicker._get(t, "stepBigMonths") : +$.datepicker._get(t, "stepMonths"), "M");
                    break;
                case 35:
                    (e.ctrlKey || e.metaKey) && $.datepicker._clearDate(e.target), n = e.ctrlKey || e.metaKey;
                    break;
                case 36:
                    (e.ctrlKey || e.metaKey) && $.datepicker._gotoToday(e.target), n = e.ctrlKey || e.metaKey;
                    break;
                case 37:
                    (e.ctrlKey || e.metaKey) && $.datepicker._adjustDate(e.target, r ? 1 : -1, "D"), n = e.ctrlKey || e.metaKey, e.originalEvent.altKey && $.datepicker._adjustDate(e.target, e.ctrlKey ? -$.datepicker._get(t, "stepBigMonths") : -$.datepicker._get(t, "stepMonths"), "M");
                    break;
                case 38:
                    (e.ctrlKey || e.metaKey) && $.datepicker._adjustDate(e.target, -7, "D"), n = e.ctrlKey || e.metaKey;
                    break;
                case 39:
                    (e.ctrlKey || e.metaKey) && $.datepicker._adjustDate(e.target, r ? -1 : 1, "D"), n = e.ctrlKey || e.metaKey, e.originalEvent.altKey && $.datepicker._adjustDate(e.target, e.ctrlKey ? +$.datepicker._get(t, "stepBigMonths") : +$.datepicker._get(t, "stepMonths"), "M");
                    break;
                case 40:
                    (e.ctrlKey || e.metaKey) && $.datepicker._adjustDate(e.target, 7, "D"), n = e.ctrlKey || e.metaKey;
                    break;
                default:
                    n = !1
            } else e.keyCode == 36 && e.ctrlKey ? $.datepicker._showDatepicker(this) : n = !1;
            n && (e.preventDefault(), e.stopPropagation())
        },
        _doKeyPress: function (e) {
            var t = $.datepicker._getInst(e.target);
            if ($.datepicker._get(t, "constrainInput")) {
                var n = $.datepicker._possibleChars($.datepicker._get(t, "dateFormat")),
                    r = String.fromCharCode(e.charCode == undefined ? e.keyCode : e.charCode);
                return e.ctrlKey || e.metaKey || r < " " || !n || n.indexOf(r) > -1
            }
        },
        _doKeyUp: function (e) {
            var t = $.datepicker._getInst(e.target);
            if (t.input.val() != t.lastVal) try {
                var n = $.datepicker.parseDate($.datepicker._get(t, "dateFormat"), t.input ? t.input.val() : null, $.datepicker._getFormatConfig(t));
                n && ($.datepicker._setDateFromField(t), $.datepicker._updateAlternate(t), $.datepicker._updateDatepicker(t))
            } catch (r) {
                $.datepicker.log(r)
            }
            return !0
        },
        _showDatepicker: function (e) {
            e = e.target || e, e.nodeName.toLowerCase() != "input" && (e = $("input", e.parentNode)[0]);
            if ($.datepicker._isDisabledDatepicker(e) || $.datepicker._lastInput == e) return;
            var t = $.datepicker._getInst(e);
            $.datepicker._curInst && $.datepicker._curInst != t && ($.datepicker._curInst.dpDiv.stop(!0, !0), t && $.datepicker._datepickerShowing && $.datepicker._hideDatepicker($.datepicker._curInst.input[0]));
            var n = $.datepicker._get(t, "beforeShow"), r = n ? n.apply(e, [e, t]) : {};
            if (r === !1) return;
            extendRemove(t.settings, r), t.lastVal = null, $.datepicker._lastInput = e, $.datepicker._setDateFromField(t), $.datepicker._inDialog && (e.value = ""), $.datepicker._pos || ($.datepicker._pos = $.datepicker._findPos(e), $.datepicker._pos[1] += e.offsetHeight);
            var i = !1;
            $(e).parents().each(function () {
                return i |= $(this).css("position") == "fixed", !i
            });
            var s = {left: $.datepicker._pos[0], top: $.datepicker._pos[1]};
            $.datepicker._pos = null, t.dpDiv.empty(), t.dpDiv.css({
                position: "absolute",
                display: "block",
                top: "-1000px"
            }), $.datepicker._updateDatepicker(t), s = $.datepicker._checkOffset(t, s, i), t.dpDiv.css({
                position: $.datepicker._inDialog && $.blockUI ? "static" : i ? "fixed" : "absolute",
                display: "none",
                left: s.left + "px",
                top: s.top + "px"
            });
            if (!t.inline) {
                var o = $.datepicker._get(t, "showAnim"), u = $.datepicker._get(t, "duration"), a = function () {
                    var e = t.dpDiv.find("iframe.ui-datepicker-cover");
                    if (!!e.length) {
                        var n = $.datepicker._getBorders(t.dpDiv);
                        e.css({left: -n[0], top: -n[1], width: t.dpDiv.outerWidth(), height: t.dpDiv.outerHeight()})
                    }
                };
                t.dpDiv.zIndex($(e).zIndex() + 1), $.datepicker._datepickerShowing = !0, $.effects && ($.effects.effect[o] || $.effects[o]) ? t.dpDiv.show(o, $.datepicker._get(t, "showOptions"), u, a) : t.dpDiv[o || "show"](o ? u : null, a), (!o || !u) && a(), t.input.is(":visible") && !t.input.is(":disabled") && t.input.focus(), $.datepicker._curInst = t
            }
        },
        _updateDatepicker: function (e) {
            this.maxRows = 4;
            var t = $.datepicker._getBorders(e.dpDiv);
            instActive = e, e.dpDiv.empty().append(this._generateHTML(e)), this._attachHandlers(e);
            var n = e.dpDiv.find("iframe.ui-datepicker-cover");
            !n.length || n.css({
                left: -t[0],
                top: -t[1],
                width: e.dpDiv.outerWidth(),
                height: e.dpDiv.outerHeight()
            }), e.dpDiv.find("." + this._dayOverClass + " a").mouseover();
            var r = this._getNumberOfMonths(e), i = r[1], s = 17;
            e.dpDiv.removeClass("ui-datepicker-multi-2 ui-datepicker-multi-3 ui-datepicker-multi-4").width(""), i > 1 && e.dpDiv.addClass("ui-datepicker-multi-" + i).css("width", s * i + "em"), e.dpDiv[(r[0] != 1 || r[1] != 1 ? "add" : "remove") + "Class"]("ui-datepicker-multi"), e.dpDiv[(this._get(e, "isRTL") ? "add" : "remove") + "Class"]("ui-datepicker-rtl"), e == $.datepicker._curInst && $.datepicker._datepickerShowing && e.input && e.input.is(":visible") && !e.input.is(":disabled") && e.input[0] != document.activeElement && e.input.focus();
            if (e.yearshtml) {
                var o = e.yearshtml;
                setTimeout(function () {
                    o === e.yearshtml && e.yearshtml && e.dpDiv.find("select.ui-datepicker-year:first").replaceWith(e.yearshtml), o = e.yearshtml = null
                }, 0)
            }
        },
        _getBorders: function (e) {
            var t = function (e) {
                return {thin: 1, medium: 2, thick: 3}[e] || e
            };
            return [parseFloat(t(e.css("border-left-width"))), parseFloat(t(e.css("border-top-width")))]
        },
        _checkOffset: function (e, t, n) {
            var r = e.dpDiv.outerWidth(), i = e.dpDiv.outerHeight(), s = e.input ? e.input.outerWidth() : 0,
                o = e.input ? e.input.outerHeight() : 0,
                u = document.documentElement.clientWidth + (n ? 0 : $(document).scrollLeft()),
                a = document.documentElement.clientHeight + (n ? 0 : $(document).scrollTop());
            return t.left -= this._get(e, "isRTL") ? r - s : 0, t.left -= n && t.left == e.input.offset().left ? $(document).scrollLeft() : 0, t.top -= n && t.top == e.input.offset().top + o ? $(document).scrollTop() : 0, t.left -= Math.min(t.left, t.left + r > u && u > r ? Math.abs(t.left + r - u) : 0), t.top -= Math.min(t.top, t.top + i > a && a > i ? Math.abs(i + o) : 0), t
        },
        _findPos: function (e) {
            var t = this._getInst(e), n = this._get(t, "isRTL");
            while (e && (e.type == "hidden" || e.nodeType != 1 || $.expr.filters.hidden(e))) e = e[n ? "previousSibling" : "nextSibling"];
            var r = $(e).offset();
            return [r.left, r.top]
        },
        _hideDatepicker: function (e) {
            var t = this._curInst;
            if (!t || e && t != $.data(e, PROP_NAME)) return;
            if (this._datepickerShowing) {
                var n = this._get(t, "showAnim"), r = this._get(t, "duration"), i = function () {
                    $.datepicker._tidyDialog(t)
                };
                $.effects && ($.effects.effect[n] || $.effects[n]) ? t.dpDiv.hide(n, $.datepicker._get(t, "showOptions"), r, i) : t.dpDiv[n == "slideDown" ? "slideUp" : n == "fadeIn" ? "fadeOut" : "hide"](n ? r : null, i), n || i(), this._datepickerShowing = !1;
                var s = this._get(t, "onClose");
                s && s.apply(t.input ? t.input[0] : null, [t.input ? t.input.val() : "", t]), this._lastInput = null, this._inDialog && (this._dialogInput.css({
                    position: "absolute",
                    left: "0",
                    top: "-100px"
                }), $.blockUI && ($.unblockUI(), $("body").append(this.dpDiv))), this._inDialog = !1
            }
        },
        _tidyDialog: function (e) {
            e.dpDiv.removeClass(this._dialogClass).unbind(".ui-datepicker-calendar")
        },
        _checkExternalClick: function (e) {
            if (!$.datepicker._curInst) return;
            var t = $(e.target), n = $.datepicker._getInst(t[0]);
            (t[0].id != $.datepicker._mainDivId && t.parents("#" + $.datepicker._mainDivId).length == 0 && !t.hasClass($.datepicker.markerClassName) && !t.closest("." + $.datepicker._triggerClass).length && $.datepicker._datepickerShowing && (!$.datepicker._inDialog || !$.blockUI) || t.hasClass($.datepicker.markerClassName) && $.datepicker._curInst != n) && $.datepicker._hideDatepicker()
        },
        _adjustDate: function (e, t, n) {
            var r = $(e), i = this._getInst(r[0]);
            if (this._isDisabledDatepicker(r[0])) return;
            this._adjustInstDate(i, t + (n == "M" ? this._get(i, "showCurrentAtPos") : 0), n), this._updateDatepicker(i)
        },
        _gotoToday: function (e) {
            var t = $(e), n = this._getInst(t[0]);
            if (this._get(n, "gotoCurrent") && n.currentDay) n.selectedDay = n.currentDay, n.drawMonth = n.selectedMonth = n.currentMonth, n.drawYear = n.selectedYear = n.currentYear; else {
                var r = new Date;
                n.selectedDay = r.getDate(), n.drawMonth = n.selectedMonth = r.getMonth(), n.drawYear = n.selectedYear = r.getFullYear()
            }
            this._notifyChange(n), this._adjustDate(t)
        },
        _selectMonthYear: function (e, t, n) {
            var r = $(e), i = this._getInst(r[0]);
            i["selected" + (n == "M" ? "Month" : "Year")] = i["draw" + (n == "M" ? "Month" : "Year")] = parseInt(t.options[t.selectedIndex].value, 10), this._notifyChange(i), this._adjustDate(r)
        },
        _selectDay: function (e, t, n, r) {
            var i = $(e);
            if ($(r).hasClass(this._unselectableClass) || this._isDisabledDatepicker(i[0])) return;
            $(r).parent().parent().find('td.' + this._selectedClass).removeClass(this._selectedClass);
            $(r).addClass(this._selectedClass);
            var s = this._getInst(i[0]);
            s.selectedDay = s.currentDay = $("a", r).html(), s.selectedMonth = s.currentMonth = t, s.selectedYear = s.currentYear = n, this._selectDate(e, this._formatDate(s, s.currentDay, s.currentMonth, s.currentYear))
        },
        _clearDate: function (e) {
            var t = $(e), n = this._getInst(t[0]);
            this._selectDate(t, "")
        },
        _selectDate: function (e, t) {
            var n = $(e), r = this._getInst(n[0]);
            t = t != null ? t : this._formatDate(r), r.input && r.input.val(t), this._updateAlternate(r);
            var i = this._get(r, "onSelect");
            i ? i.apply(r.input ? r.input[0] : null, [t, r]) : r.input && r.input.trigger("change"), r.inline ? this._updateDatepicker(r) : (this._hideDatepicker(), this._lastInput = r.input[0], typeof r.input[0] != "object" && r.input.focus(), this._lastInput = null)
        },
        _updateAlternate: function (e) {
            var t = this._get(e, "altField");
            if (t) {
                var n = this._get(e, "altFormat") || this._get(e, "dateFormat"), r = this._getDate(e),
                    i = this.formatDate(n, r, this._getFormatConfig(e));
                $(t).each(function () {
                    $(this).val(i)
                })
            }
        },
        noWeekends: function (e) {
            var t = e.getDay();
            return [t > 0 && t < 6, ""]
        },
        iso8601Week: function (e) {
            var t = new Date(e.getTime());
            t.setDate(t.getDate() + 4 - (t.getDay() || 7));
            var n = t.getTime();
            return t.setMonth(0), t.setDate(1), Math.floor(Math.round((n - t) / 864e5) / 7) + 1
        },
        parseDate: function (e, t, n) {
            if (e == null || t == null) throw"Invalid arguments";
            t = typeof t == "object" ? t.toString() : t + "";
            if (t == "") return null;
            var r = (n ? n.shortYearCutoff : null) || this._defaults.shortYearCutoff;
            r = typeof r != "string" ? r : (new Date).getFullYear() % 100 + parseInt(r, 10);
            var i = (n ? n.dayNamesShort : null) || this._defaults.dayNamesShort,
                s = (n ? n.dayNames : null) || this._defaults.dayNames,
                o = (n ? n.monthNamesShort : null) || this._defaults.monthNamesShort,
                u = (n ? n.monthNames : null) || this._defaults.monthNames, a = -1, f = -1, l = -1, c = -1, h = !1,
                p = function (t) {
                    var n = y + 1 < e.length && e.charAt(y + 1) == t;
                    return n && y++, n
                }, d = function (e) {
                    var n = p(e), r = e == "@" ? 14 : e == "!" ? 20 : e == "y" && n ? 4 : e == "o" ? 3 : 2,
                        i = new RegExp("^\\d{1," + r + "}"), s = t.substring(g).match(i);
                    if (!s) throw"Missing number at position " + g;
                    return g += s[0].length, parseInt(s[0], 10)
                }, v = function (e, n, r) {
                    var i = $.map(p(e) ? r : n, function (e, t) {
                        return [[t, e]]
                    }).sort(function (e, t) {
                        return -(e[1].length - t[1].length)
                    }), s = -1;
                    $.each(i, function (e, n) {
                        var r = n[1];
                        if (t.substr(g, r.length).toLowerCase() == r.toLowerCase()) return s = n[0], g += r.length, !1
                    });
                    if (s != -1) return s + 1;
                    throw"Unknown name at position " + g
                }, m = function () {
                    if (t.charAt(g) != e.charAt(y)) throw"Unexpected literal at position " + g;
                    g++
                }, g = 0;
            for (var y = 0; y < e.length; y++) if (h) e.charAt(y) == "'" && !p("'") ? h = !1 : m(); else switch (e.charAt(y)) {
                case"d":
                    l = d("d");
                    break;
                case"D":
                    v("D", i, s);
                    break;
                case"o":
                    c = d("o");
                    break;
                case"m":
                    f = d("m");
                    break;
                case"M":
                    f = v("M", o, u);
                    break;
                case"y":
                    a = d("y");
                    break;
                case"@":
                    var b = new Date(d("@"));
                    a = b.getFullYear(), f = b.getMonth() + 1, l = b.getDate();
                    break;
                case"!":
                    var b = new Date((d("!") - this._ticksTo1970) / 1e4);
                    a = b.getFullYear(), f = b.getMonth() + 1, l = b.getDate();
                    break;
                case"'":
                    p("'") ? m() : h = !0;
                    break;
                default:
                    m()
            }
            if (g < t.length) {
                var w = t.substr(g);
                if (!/^\s+/.test(w)) throw"Extra/unparsed characters found in date: " + w
            }
            a == -1 ? a = (new Date).getFullYear() : a < 100 && (a += (new Date).getFullYear() - (new Date).getFullYear() % 100 + (a <= r ? 0 : -100));
            if (c > -1) {
                f = 1, l = c;
                do {
                    var E = this._getDaysInMonth(a, f - 1);
                    if (l <= E) break;
                    f++, l -= E
                } while (!0)
            }
            var b = this._daylightSavingAdjust(new Date(a, f - 1, l));
            if (b.getFullYear() != a || b.getMonth() + 1 != f || b.getDate() != l) throw"Invalid date";
            return b
        },
        ATOM: "yy-mm-dd",
        COOKIE: "D, dd M yy",
        ISO_8601: "yy-mm-dd",
        RFC_822: "D, d M y",
        RFC_850: "DD, dd-M-y",
        RFC_1036: "D, d M y",
        RFC_1123: "D, d M yy",
        RFC_2822: "D, d M yy",
        RSS: "D, d M y",
        TICKS: "!",
        TIMESTAMP: "@",
        W3C: "yy-mm-dd",
        _ticksTo1970: (718685 + Math.floor(492.5) - Math.floor(19.7) + Math.floor(4.925)) * 24 * 60 * 60 * 1e7,
        formatDate: function (e, t, n) {
            if (!t) return "";
            var r = (n ? n.dayNamesShort : null) || this._defaults.dayNamesShort,
                i = (n ? n.dayNames : null) || this._defaults.dayNames,
                s = (n ? n.monthNamesShort : null) || this._defaults.monthNamesShort,
                o = (n ? n.monthNames : null) || this._defaults.monthNames, u = function (t) {
                    var n = h + 1 < e.length && e.charAt(h + 1) == t;
                    return n && h++, n
                }, a = function (e, t, n) {
                    var r = "" + t;
                    if (u(e)) while (r.length < n) r = "0" + r;
                    return r
                }, f = function (e, t, n, r) {
                    return u(e) ? r[t] : n[t]
                }, l = "", c = !1;
            if (t) for (var h = 0; h < e.length; h++) if (c) e.charAt(h) == "'" && !u("'") ? c = !1 : l += e.charAt(h); else switch (e.charAt(h)) {
                case"d":
                    l += a("d", t.getDate(), 2);
                    break;
                case"D":
                    l += f("D", t.getDay(), r, i);
                    break;
                case"o":
                    l += a("o", Math.round(((new Date(t.getFullYear(), t.getMonth(), t.getDate())).getTime() - (new Date(t.getFullYear(), 0, 0)).getTime()) / 864e5), 3);
                    break;
                case"m":
                    l += a("m", t.getMonth() + 1, 2);
                    break;
                case"M":
                    l += f("M", t.getMonth(), s, o);
                    break;
                case"y":
                    l += u("y") ? t.getFullYear() : (t.getYear() % 100 < 10 ? "0" : "") + t.getYear() % 100;
                    break;
                case"@":
                    l += t.getTime();
                    break;
                case"!":
                    l += t.getTime() * 1e4 + this._ticksTo1970;
                    break;
                case"'":
                    u("'") ? l += "'" : c = !0;
                    break;
                default:
                    l += e.charAt(h)
            }
            return l
        },
        _possibleChars: function (e) {
            var t = "", n = !1, r = function (t) {
                var n = i + 1 < e.length && e.charAt(i + 1) == t;
                return n && i++, n
            };
            for (var i = 0; i < e.length; i++) if (n) e.charAt(i) == "'" && !r("'") ? n = !1 : t += e.charAt(i); else switch (e.charAt(i)) {
                case"d":
                case"m":
                case"y":
                case"@":
                    t += "0123456789";
                    break;
                case"D":
                case"M":
                    return null;
                case"'":
                    r("'") ? t += "'" : n = !0;
                    break;
                default:
                    t += e.charAt(i)
            }
            return t
        },
        _get: function (e, t) {
            return e.settings[t] !== undefined ? e.settings[t] : this._defaults[t]
        },
        _setDateFromField: function (e, t) {
            if (e.input.val() == e.lastVal) return;
            var n = this._get(e, "dateFormat"), r = e.lastVal = e.input ? e.input.val() : null, i, s;
            i = s = this._getDefaultDate(e);
            var o = this._getFormatConfig(e);
            try {
                i = this.parseDate(n, r, o) || s
            } catch (u) {
                this.log(u), r = t ? "" : r
            }
            e.selectedDay = i.getDate(), e.drawMonth = e.selectedMonth = i.getMonth(), e.drawYear = e.selectedYear = i.getFullYear(), e.currentDay = r ? i.getDate() : 0, e.currentMonth = r ? i.getMonth() : 0, e.currentYear = r ? i.getFullYear() : 0, this._adjustInstDate(e)
        },
        _getDefaultDate: function (e) {
            return this._restrictMinMax(e, this._determineDate(e, this._get(e, "defaultDate"), new Date))
        },
        _determineDate: function (e, t, n) {
            var r = function (e) {
                    var t = new Date;
                    return t.setDate(t.getDate() + e), t
                }, i = function (t) {
                    try {
                        return $.datepicker.parseDate($.datepicker._get(e, "dateFormat"), t, $.datepicker._getFormatConfig(e))
                    } catch (n) {
                    }
                    var r = (t.toLowerCase().match(/^c/) ? $.datepicker._getDate(e) : null) || new Date,
                        i = r.getFullYear(), s = r.getMonth(), o = r.getDate(), u = /([+-]?[0-9]+)\s*(d|D|w|W|m|M|y|Y)?/g,
                        a = u.exec(t);
                    while (a) {
                        switch (a[2] || "d") {
                            case"d":
                            case"D":
                                o += parseInt(a[1], 10);
                                break;
                            case"w":
                            case"W":
                                o += parseInt(a[1], 10) * 7;
                                break;
                            case"m":
                            case"M":
                                s += parseInt(a[1], 10), o = Math.min(o, $.datepicker._getDaysInMonth(i, s));
                                break;
                            case"y":
                            case"Y":
                                i += parseInt(a[1], 10), o = Math.min(o, $.datepicker._getDaysInMonth(i, s))
                        }
                        a = u.exec(t)
                    }
                    return new Date(i, s, o)
                },
                s = t == null || t === "" ? n : typeof t == "string" ? i(t) : typeof t == "number" ? isNaN(t) ? n : r(t) : new Date(t.getTime());
            return s = s && s.toString() == "Invalid Date" ? n : s, s && (s.setHours(0), s.setMinutes(0), s.setSeconds(0), s.setMilliseconds(0)), this._daylightSavingAdjust(s)
        },
        _daylightSavingAdjust: function (e) {
            return e ? (e.setHours(e.getHours() > 12 ? e.getHours() + 2 : 0), e) : null
        },
        _setDate: function (e, t, n) {
            var r = !t, i = e.selectedMonth, s = e.selectedYear,
                o = this._restrictMinMax(e, this._determineDate(e, t, new Date));
            e.selectedDay = e.currentDay = o.getDate(), e.drawMonth = e.selectedMonth = e.currentMonth = o.getMonth(), e.drawYear = e.selectedYear = e.currentYear = o.getFullYear(), (i != e.selectedMonth || s != e.selectedYear) && !n && this._notifyChange(e), this._adjustInstDate(e), e.input && e.input.val(r ? "" : this._formatDate(e))
        },
        _getDate: function (e) {
            var t = !e.currentYear || e.input && e.input.val() == "" ? null : this._daylightSavingAdjust(new Date(e.currentYear, e.currentMonth, e.currentDay));
            return t
        },
        _attachHandlers: function (e) {
            var t = this._get(e, "stepMonths"), n = "#" + e.id.replace(/\\\\/g, "\\");
            e.dpDiv.find("[data-handler]").map(function () {
                var e = {
                    prev: function () {
                        window["DP_jQuery_" + dpuuid].datepicker._adjustDate(n, -t, "M")
                    }, next: function () {
                        window["DP_jQuery_" + dpuuid].datepicker._adjustDate(n, +t, "M")
                    }, hide: function () {
                        window["DP_jQuery_" + dpuuid].datepicker._hideDatepicker()
                    }, today: function () {
                        window["DP_jQuery_" + dpuuid].datepicker._gotoToday(n)
                    }, selectDay: function () {
                        return window["DP_jQuery_" + dpuuid].datepicker._selectDay(n, +this.getAttribute("data-month"), +this.getAttribute("data-year"), this), !1
                    }, selectMonth: function () {
                        return window["DP_jQuery_" + dpuuid].datepicker._selectMonthYear(n, this, "M"), !1
                    }, selectYear: function () {
                        return window["DP_jQuery_" + dpuuid].datepicker._selectMonthYear(n, this, "Y"), !1
                    }
                };
                $(this).bind(this.getAttribute("data-event"), e[this.getAttribute("data-handler")])
            })
        },
        _generateHTML: function (e) {
            var t = new Date;
            t = this._daylightSavingAdjust(new Date(t.getFullYear(), t.getMonth(), t.getDate()));
            var n = this._get(e, "isRTL"), r = this._get(e, "showButtonPanel"), i = this._get(e, "hideIfNoPrevNext"),
                s = this._get(e, "navigationAsDateFormat"), o = this._getNumberOfMonths(e),
                u = this._get(e, "showCurrentAtPos"), a = this._get(e, "stepMonths"), f = o[0] != 1 || o[1] != 1,
                l = this._daylightSavingAdjust(e.currentDay ? new Date(e.currentYear, e.currentMonth, e.currentDay) : new Date(9999, 9, 9)),
                c = this._getMinMaxDate(e, "min"), h = this._getMinMaxDate(e, "max"), p = e.drawMonth - u,
                d = e.drawYear;
            p < 0 && (p += 12, d--);
            if (h) {
                var v = this._daylightSavingAdjust(new Date(h.getFullYear(), h.getMonth() - o[0] * o[1] + 1, h.getDate()));
                v = c && v < c ? c : v;
                while (this._daylightSavingAdjust(new Date(d, p, 1)) > v) p--, p < 0 && (p = 11, d--)
            }
            e.drawMonth = p, e.drawYear = d;
            var m = this._get(e, "prevText");
            m = s ? this.formatDate(m, this._daylightSavingAdjust(new Date(d, p - a, 1)), this._getFormatConfig(e)) : m;
            var g = this._canAdjustMonth(e, -1, d, p) ? '<a class="ui-datepicker-prev ui-corner-all" data-handler="prev" data-event="click" title="' + m + '"><span class="ui-icon ui-icon-circle-triangle-' + (n ? "e" : "w") + '">' + m + "</span></a>" : i ? "" : '<a class="ui-datepicker-prev ui-corner-all ui-state-disabled" title="' + m + '"><span class="ui-icon ui-icon-circle-triangle-' + (n ? "e" : "w") + '">' + m + "</span></a>",
                y = this._get(e, "nextText");
            y = s ? this.formatDate(y, this._daylightSavingAdjust(new Date(d, p + a, 1)), this._getFormatConfig(e)) : y;
            var b = this._canAdjustMonth(e, 1, d, p) ? '<a class="ui-datepicker-next ui-corner-all" data-handler="next" data-event="click" title="' + y + '"><span class="ui-icon ui-icon-circle-triangle-' + (n ? "w" : "e") + '">' + y + "</span></a>" : i ? "" : '<a class="ui-datepicker-next ui-corner-all ui-state-disabled" title="' + y + '"><span class="ui-icon ui-icon-circle-triangle-' + (n ? "w" : "e") + '">' + y + "</span></a>",
                w = this._get(e, "currentText"), E = this._get(e, "gotoCurrent") && e.currentDay ? l : t;
            w = s ? this.formatDate(w, E, this._getFormatConfig(e)) : w;
            var S = e.inline ? "" : '<button type="button" class="ui-datepicker-close ui-state-default ui-priority-primary ui-corner-all" data-handler="hide" data-event="click">' + this._get(e, "closeText") + "</button>",
                x = r ? '<div class="ui-datepicker-buttonpane ui-widget-content">' + (n ? S : "") + (this._isInRange(e, E) ? '<button type="button" class="ui-datepicker-current ui-state-default ui-priority-secondary ui-corner-all" data-handler="today" data-event="click">' + w + "</button>" : "") + (n ? "" : S) + "</div>" : "",
                T = parseInt(this._get(e, "firstDay"), 10);
            T = isNaN(T) ? 0 : T;
            var N = this._get(e, "showWeek"), C = this._get(e, "dayNames"), k = this._get(e, "dayNamesShort"),
                L = this._get(e, "dayNamesMin"), A = this._get(e, "monthNames"), O = this._get(e, "monthNamesShort"),
                M = this._get(e, "beforeShowDay"), _ = this._get(e, "showOtherMonths"),
                D = this._get(e, "selectOtherMonths"), P = this._get(e, "calculateWeek") || this.iso8601Week,
                H = this._getDefaultDate(e), B = "";
            for (var j = 0; j < o[0]; j++) {
                var F = "";
                this.maxRows = 4;
                for (var I = 0; I < o[1]; I++) {
                    var q = this._daylightSavingAdjust(new Date(d, p, e.selectedDay)), R = " ui-corner-all", U = "";
                    if (f) {
                        U += '<div class="ui-datepicker-group';
                        if (o[1] > 1) switch (I) {
                            case 0:
                                U += " ui-datepicker-group-first", R = " ui-corner-" + (n ? "right" : "left");
                                break;
                            case o[1] - 1:
                                U += " ui-datepicker-group-last", R = " ui-corner-" + (n ? "left" : "right");
                                break;
                            default:
                                U += " ui-datepicker-group-middle", R = ""
                        }
                        U += '">'
                    }
                    U += '<div class="ui-datepicker-header ui-widget-header ui-helper-clearfix' + R + '">' + (/all|left/.test(R) && j == 0 ? n ? b : g : "") + (/all|right/.test(R) && j == 0 ? n ? g : b : "") + this._generateMonthYearHeader(e, p, d, c, h, j > 0 || I > 0, A, O) + '</div><table class="ui-datepicker-calendar"><thead>' + "<tr>";
                    var z = N ? '<th class="ui-datepicker-week-col">' + this._get(e, "weekHeader") + "</th>" : "";
                    for (var W = 0; W < 7; W++) {
                        var X = (W + T) % 7;
                        z += "<th" + ((W + T + 6) % 7 >= 5 ? ' class="ui-datepicker-week-end"' : "") + ">" + '<span title="' + C[X] + '">' + L[X] + "</span></th>"
                    }
                    U += z + "</tr></thead><tbody>";
                    var V = this._getDaysInMonth(d, p);
                    d == e.selectedYear && p == e.selectedMonth && (e.selectedDay = Math.min(e.selectedDay, V));
                    var J = (this._getFirstDayOfMonth(d, p) - T + 7) % 7, K = Math.ceil((J + V) / 7),
                        Q = f ? this.maxRows > K ? this.maxRows : K : K;
                    this.maxRows = Q;
                    var G = this._daylightSavingAdjust(new Date(d, p, 1 - J));
                    for (var Y = 0; Y < Q; Y++) {
                        U += "<tr>";
                        var Z = N ? '<td class="ui-datepicker-week-col">' + this._get(e, "calculateWeek")(G) + "</td>" : "";
                        for (var W = 0; W < 7; W++) {
                            var et = M ? M.apply(e.input ? e.input[0] : null, [G]) : [!0, ""], tt = G.getMonth() != p,
                                nt = tt && !D || !et[0] || c && G < c || h && G > h;
                            Z += '<td class="' + ((W + T + 6) % 7 >= 5 ? " ui-datepicker-week-end" : "") + (tt ? " ui-datepicker-other-month" : "") + (G.getTime() == q.getTime() && p == e.selectedMonth && e._keyEvent || H.getTime() == G.getTime() && H.getTime() == q.getTime() ? " " + this._dayOverClass : "") + (nt ? " " + this._unselectableClass + " ui-state-disabled" : "") + (tt && !_ ? "" : " " + et[1] + (G.getTime() == l.getTime() ? " " + this._currentClass : "") + (G.getTime() == t.getTime() ? " ui-datepicker-today" : "")) + '"' + ((!tt || _) && et[2] ? ' title="' + et[2] + '"' : "") + (nt ? "" : ' data-handler="selectDay" data-event="click" data-month="' + G.getMonth() + '" data-year="' + G.getFullYear() + '"') + ">" + (tt && !_ ? "&#xa0;" : nt ? '<span class="ui-state-default">' + G.getDate() + "</span>" : '<a class="ui-state-default' + (G.getTime() == t.getTime() ? " ui-state-highlight" : "") + (G.getTime() == l.getTime() ? " ui-state-active" : "") + (tt ? " ui-priority-secondary" : "") + '" href="#">' + G.getDate() + "</a>") + "</td>", G.setDate(G.getDate() + 1), G = this._daylightSavingAdjust(G)
                        }
                        U += Z + "</tr>"
                    }
                    p++, p > 11 && (p = 0, d++), U += "</tbody></table>" + (f ? "</div>" + (o[0] > 0 && I == o[1] - 1 ? '<div class="ui-datepicker-row-break"></div>' : "") : ""), F += U
                }
                B += F
            }
            return B += x + ($.ui.ie6 && !e.inline ? '<iframe src="javascript:false;" class="ui-datepicker-cover" frameborder="0"></iframe>' : ""), e._keyEvent = !1, B
        },
        _generateMonthYearHeader: function (e, t, n, r, i, s, o, u) {
            var a = this._get(e, "changeMonth"), f = this._get(e, "changeYear"), l = this._get(e, "showMonthAfterYear"),
                c = '<div class="ui-datepicker-title">', h = "";
            if (s || !a) h += '<span class="ui-datepicker-month">' + o[t] + "</span>"; else {
                var p = r && r.getFullYear() == n, d = i && i.getFullYear() == n;
                h += '<select class="ui-datepicker-month" data-handler="selectMonth" data-event="change">';
                for (var v = 0; v < 12; v++) (!p || v >= r.getMonth()) && (!d || v <= i.getMonth()) && (h += '<option value="' + v + '"' + (v == t ? ' selected="selected"' : "") + ">" + u[v] + "</option>");
                h += "</select>"
            }
            l || (c += h + (s || !a || !f ? "&#xa0;" : ""));
            if (!e.yearshtml) {
                e.yearshtml = "";
                if (s || !f) c += '<span class="ui-datepicker-year">' + n + "</span>"; else {
                    var m = this._get(e, "yearRange").split(":"), g = (new Date).getFullYear(), y = function (e) {
                        var t = e.match(/c[+-].*/) ? n + parseInt(e.substring(1), 10) : e.match(/[+-].*/) ? g + parseInt(e, 10) : parseInt(e, 10);
                        return isNaN(t) ? g : t
                    }, b = y(m[0]), w = Math.max(b, y(m[1] || ""));
                    b = r ? Math.max(b, r.getFullYear()) : b, w = i ? Math.min(w, i.getFullYear()) : w, e.yearshtml += '<select class="ui-datepicker-year" data-handler="selectYear" data-event="change">';
                    for (; b <= w; b++) e.yearshtml += '<option value="' + b + '"' + (b == n ? ' selected="selected"' : "") + ">" + b + "</option>";
                    e.yearshtml += "</select>", c += e.yearshtml, e.yearshtml = null
                }
            }
            return c += this._get(e, "yearSuffix"), l && (c += (s || !a || !f ? "&#xa0;" : "") + h), c += "</div>", c
        },
        _adjustInstDate: function (e, t, n) {
            var r = e.drawYear + (n == "Y" ? t : 0), i = e.drawMonth + (n == "M" ? t : 0),
                s = Math.min(e.selectedDay, this._getDaysInMonth(r, i)) + (n == "D" ? t : 0),
                o = this._restrictMinMax(e, this._daylightSavingAdjust(new Date(r, i, s)));
            e.selectedDay = o.getDate(), e.drawMonth = e.selectedMonth = o.getMonth(), e.drawYear = e.selectedYear = o.getFullYear(), (n == "M" || n == "Y") && this._notifyChange(e)
        },
        _restrictMinMax: function (e, t) {
            var n = this._getMinMaxDate(e, "min"), r = this._getMinMaxDate(e, "max"), i = n && t < n ? n : t;
            return i = r && i > r ? r : i, i
        },
        _notifyChange: function (e) {
            var t = this._get(e, "onChangeMonthYear");
            t && t.apply(e.input ? e.input[0] : null, [e.selectedYear, e.selectedMonth + 1, e])
        },
        _getNumberOfMonths: function (e) {
            var t = this._get(e, "numberOfMonths");
            return t == null ? [1, 1] : typeof t == "number" ? [1, t] : t
        },
        _getMinMaxDate: function (e, t) {
            return this._determineDate(e, this._get(e, t + "Date"), null)
        },
        _getDaysInMonth: function (e, t) {
            return 32 - this._daylightSavingAdjust(new Date(e, t, 32)).getDate()
        },
        _getFirstDayOfMonth: function (e, t) {
            return (new Date(e, t, 1)).getDay()
        },
        _canAdjustMonth: function (e, t, n, r) {
            var i = this._getNumberOfMonths(e),
                s = this._daylightSavingAdjust(new Date(n, r + (t < 0 ? t : i[0] * i[1]), 1));
            return t < 0 && s.setDate(this._getDaysInMonth(s.getFullYear(), s.getMonth())), this._isInRange(e, s)
        },
        _isInRange: function (e, t) {
            var n = this._getMinMaxDate(e, "min"), r = this._getMinMaxDate(e, "max");
            return (!n || t.getTime() >= n.getTime()) && (!r || t.getTime() <= r.getTime())
        },
        _getFormatConfig: function (e) {
            var t = this._get(e, "shortYearCutoff");
            return t = typeof t != "string" ? t : (new Date).getFullYear() % 100 + parseInt(t, 10), {
                shortYearCutoff: t,
                dayNamesShort: this._get(e, "dayNamesShort"),
                dayNames: this._get(e, "dayNames"),
                monthNamesShort: this._get(e, "monthNamesShort"),
                monthNames: this._get(e, "monthNames")
            }
        },
        _formatDate: function (e, t, n, r) {
            t || (e.currentDay = e.selectedDay, e.currentMonth = e.selectedMonth, e.currentYear = e.selectedYear);
            var i = t ? typeof t == "object" ? t : this._daylightSavingAdjust(new Date(r, n, t)) : this._daylightSavingAdjust(new Date(e.currentYear, e.currentMonth, e.currentDay));
            return this.formatDate(this._get(e, "dateFormat"), i, this._getFormatConfig(e))
        }
    }), $.fn.datepicker = function (e) {
        if (!this.length) return this;
        $.datepicker.initialized || ($(document).mousedown($.datepicker._checkExternalClick).find(document.body).append($.datepicker.dpDiv), $.datepicker.initialized = !0);
        var t = Array.prototype.slice.call(arguments, 1);
        return typeof e != "string" || e != "isDisabled" && e != "getDate" && e != "widget" ? e == "option" && arguments.length == 2 && typeof arguments[1] == "string" ? $.datepicker["_" + e + "Datepicker"].apply($.datepicker, [this[0]].concat(t)) : this.each(function () {
            typeof e == "string" ? $.datepicker["_" + e + "Datepicker"].apply($.datepicker, [this].concat(t)) : $.datepicker._attachDatepicker(this, e)
        }) : $.datepicker["_" + e + "Datepicker"].apply($.datepicker, [this[0]].concat(t))
    }, $.datepicker = new Datepicker, $.datepicker.initialized = !1, $.datepicker.uuid = (new Date).getTime(), $.datepicker.version = "1.9.2", window["DP_jQuery_" + dpuuid] = $
})(jQuery);
/*
 * jQuery timepicker addon
 * By: Trent Richardson [http://trentrichardson.com]
 * Version 1.2.2
 * Last Modified: 04/13/2013
 *
 * Copyright 2013 Trent Richardson
 * You may use this project under MIT or GPL licenses.
 * http://trentrichardson.com/Impromptu/GPL-LICENSE.txt
 * http://trentrichardson.com/Impromptu/MIT-LICENSE.txt
 */
(function ($) {
    $.ui.timepicker = $.ui.timepicker || {};
    if (!$.ui.timepicker.version) {
        $.extend($.ui, {timepicker: {version: "1.2.2"}});
        var Timepicker = function () {
            this.regional = [], this.regional[""] = {
                currentText: "Now",
                closeText: "Done",
                amNames: ["AM", "A"],
                pmNames: ["PM", "P"],
                timeFormat: "HH:mm",
                timeSuffix: "",
                timeOnlyTitle: "Choose Time",
                timeText: "Time",
                hourText: "Hour",
                minuteText: "Minute",
                secondText: "Second",
                millisecText: "Millisecond",
                timezoneText: "Time Zone",
                isRTL: !1
            }, this._defaults = {
                showButtonPanel: !0,
                timeOnly: !1,
                showHour: !0,
                showMinute: !0,
                showSecond: !1,
                showMillisec: !1,
                showTimezone: !1,
                showTime: !0,
                stepHour: 1,
                stepMinute: 1,
                stepSecond: 1,
                stepMillisec: 1,
                hour: 0,
                minute: 0,
                second: 0,
                millisec: 0,
                timezone: null,
                useLocalTimezone: !1,
                defaultTimezone: "+0000",
                hourMin: 0,
                minuteMin: 0,
                secondMin: 0,
                millisecMin: 0,
                hourMax: 23,
                minuteMax: 59,
                secondMax: 59,
                millisecMax: 999,
                minDateTime: null,
                maxDateTime: null,
                onSelect: null,
                hourGrid: 0,
                minuteGrid: 0,
                secondGrid: 0,
                millisecGrid: 0,
                alwaysSetTime: !0,
                separator: " ",
                altFieldTimeOnly: !0,
                altTimeFormat: null,
                altSeparator: null,
                altTimeSuffix: null,
                pickerTimeFormat: null,
                pickerTimeSuffix: null,
                showTimepicker: !0,
                timezoneIso8601: !1,
                timezoneList: null,
                addSliderAccess: !1,
                sliderAccessArgs: null,
                controlType: "slider",
                defaultValue: null,
                parse: "strict"
            }, $.extend(this._defaults, this.regional[""])
        };
        $.extend(Timepicker.prototype, {
            $input: null,
            $altInput: null,
            $timeObj: null,
            inst: null,
            hour_slider: null,
            minute_slider: null,
            second_slider: null,
            millisec_slider: null,
            timezone_select: null,
            hour: 0,
            minute: 0,
            second: 0,
            millisec: 0,
            timezone: null,
            defaultTimezone: "+0000",
            hourMinOriginal: null,
            minuteMinOriginal: null,
            secondMinOriginal: null,
            millisecMinOriginal: null,
            hourMaxOriginal: null,
            minuteMaxOriginal: null,
            secondMaxOriginal: null,
            millisecMaxOriginal: null,
            ampm: "",
            formattedDate: "",
            formattedTime: "",
            formattedDateTime: "",
            timezoneList: null,
            units: ["hour", "minute", "second", "millisec"],
            control: null,
            setDefaults: function (a) {
                extendRemove(this._defaults, a || {});
                return this
            },
            _newInst: function ($input, o) {
                var tp_inst = new Timepicker, inlineSettings = {}, fns = {}, overrides, i;
                for (var attrName in this._defaults) if (this._defaults.hasOwnProperty(attrName)) {
                    var attrValue = $input.attr("time:" + attrName);
                    if (attrValue) try {
                        inlineSettings[attrName] = eval(attrValue)
                    } catch (err) {
                        inlineSettings[attrName] = attrValue
                    }
                }
                overrides = {
                    beforeShow: function (a, b) {
                        if ($.isFunction(tp_inst._defaults.evnts.beforeShow)) return tp_inst._defaults.evnts.beforeShow.call($input[0], a, b, tp_inst)
                    }, onChangeMonthYear: function (a, b, c) {
                        tp_inst._updateDateTime(c), $.isFunction(tp_inst._defaults.evnts.onChangeMonthYear) && tp_inst._defaults.evnts.onChangeMonthYear.call($input[0], a, b, c, tp_inst)
                    }, onClose: function (a, b) {
                        tp_inst.timeDefined === !0 && $input.val() !== "" && tp_inst._updateDateTime(b), $.isFunction(tp_inst._defaults.evnts.onClose) && tp_inst._defaults.evnts.onClose.call($input[0], a, b, tp_inst)
                    }
                };
                for (i in overrides) overrides.hasOwnProperty(i) && (fns[i] = o[i] || null);
                tp_inst._defaults = $.extend({}, this._defaults, inlineSettings, o, overrides, {
                    evnts: fns,
                    timepicker: tp_inst
                }), tp_inst.amNames = $.map(tp_inst._defaults.amNames, function (a) {
                    return a.toUpperCase()
                }), tp_inst.pmNames = $.map(tp_inst._defaults.pmNames, function (a) {
                    return a.toUpperCase()
                }), typeof tp_inst._defaults.controlType == "string" ? ($.fn[tp_inst._defaults.controlType] === undefined && (tp_inst._defaults.controlType = "select"), tp_inst.control = tp_inst._controls[tp_inst._defaults.controlType]) : tp_inst.control = tp_inst._defaults.controlType;
                if (tp_inst._defaults.timezoneList === null) {
                    var timezoneList = ["-1200", "-1100", "-1000", "-0930", "-0900", "-0800", "-0700", "-0600", "-0500", "-0430", "-0400", "-0330", "-0300", "-0200", "-0100", "+0000", "+0100", "+0200", "+0300", "+0330", "+0400", "+0430", "+0500", "+0530", "+0545", "+0600", "+0630", "+0700", "+0800", "+0845", "+0900", "+0930", "+1000", "+1030", "+1100", "+1130", "+1200", "+1245", "+1300", "+1400"];
                    tp_inst._defaults.timezoneIso8601 && (timezoneList = $.map(timezoneList, function (a) {
                        return a == "+0000" ? "Z" : a.substring(0, 3) + ":" + a.substring(3)
                    })), tp_inst._defaults.timezoneList = timezoneList
                }
                tp_inst.timezone = tp_inst._defaults.timezone, tp_inst.hour = tp_inst._defaults.hour < tp_inst._defaults.hourMin ? tp_inst._defaults.hourMin : tp_inst._defaults.hour > tp_inst._defaults.hourMax ? tp_inst._defaults.hourMax : tp_inst._defaults.hour, tp_inst.minute = tp_inst._defaults.minute < tp_inst._defaults.minuteMin ? tp_inst._defaults.minuteMin : tp_inst._defaults.minute > tp_inst._defaults.minuteMax ? tp_inst._defaults.minuteMax : tp_inst._defaults.minute, tp_inst.second = tp_inst._defaults.second < tp_inst._defaults.secondMin ? tp_inst._defaults.secondMin : tp_inst._defaults.second > tp_inst._defaults.secondMax ? tp_inst._defaults.secondMax : tp_inst._defaults.second, tp_inst.millisec = tp_inst._defaults.millisec < tp_inst._defaults.millisecMin ? tp_inst._defaults.millisecMin : tp_inst._defaults.millisec > tp_inst._defaults.millisecMax ? tp_inst._defaults.millisecMax : tp_inst._defaults.millisec, tp_inst.ampm = "", tp_inst.$input = $input, o.altField && (tp_inst.$altInput = $(o.altField).css({cursor: "pointer"}).focus(function () {
                    $input.trigger("focus")
                }));
                if (tp_inst._defaults.minDate === 0 || tp_inst._defaults.minDateTime === 0) tp_inst._defaults.minDate = new Date;
                if (tp_inst._defaults.maxDate === 0 || tp_inst._defaults.maxDateTime === 0) tp_inst._defaults.maxDate = new Date;
                tp_inst._defaults.minDate !== undefined && tp_inst._defaults.minDate instanceof Date && (tp_inst._defaults.minDateTime = new Date(tp_inst._defaults.minDate.getTime())), tp_inst._defaults.minDateTime !== undefined && tp_inst._defaults.minDateTime instanceof Date && (tp_inst._defaults.minDate = new Date(tp_inst._defaults.minDateTime.getTime())), tp_inst._defaults.maxDate !== undefined && tp_inst._defaults.maxDate instanceof Date && (tp_inst._defaults.maxDateTime = new Date(tp_inst._defaults.maxDate.getTime())), tp_inst._defaults.maxDateTime !== undefined && tp_inst._defaults.maxDateTime instanceof Date && (tp_inst._defaults.maxDate = new Date(tp_inst._defaults.maxDateTime.getTime()));
                return tp_inst
            },
            _addTimePicker: function (a) {
                var b = this.$altInput && this._defaults.altFieldTimeOnly ? this.$input.val() + " " + this.$altInput.val() : this.$input.val();
                this.timeDefined = this._parseTime(b), this._limitMinMaxDateTime(a, !1), this._injectTimePicker()
            },
            _parseTime: function (a, b) {
                this.inst || (this.inst = $.datepicker._getInst(this.$input[0]));
                if (b || !this._defaults.timeOnly) {
                    var c = $.datepicker._get(this.inst, "dateFormat");
                    try {
                        var d = parseDateTimeInternal(c, this._defaults.timeFormat, a, $.datepicker._getFormatConfig(this.inst), this._defaults);
                        if (!d.timeObj) return !1;
                        $.extend(this, d.timeObj)
                    } catch (e) {
                        $.timepicker.log("Error parsing the date/time string: " + e + "\ndate/time string = " + a + "\ntimeFormat = " + this._defaults.timeFormat + "\ndateFormat = " + c);
                        return !1
                    }
                    return !0
                }
                var f = $.datepicker.parseTime(this._defaults.timeFormat, a, this._defaults);
                if (!f) return !1;
                $.extend(this, f);
                return !0
            },
            _injectTimePicker: function () {
                var a = this.inst.dpDiv, b = this.inst.settings, c = this, d = "", e = "", f = {}, g = {}, h = null,
                    i = 0, j = 0;
                if (a.find("div.ui-timepicker-div").length === 0 && b.showTimepicker) {
                    var k = ' style="display:none;"',
                        l = '<div class="ui-timepicker-div' + (b.isRTL ? " ui-timepicker-rtl" : "") + '"><dl>' + '<dt class="ui_tpicker_time_label"' + (b.showTime ? "" : k) + ">" + b.timeText + "</dt>" + '<dd class="ui_tpicker_time"' + (b.showTime ? "" : k) + "></dd>";
                    for (i = 0, j = this.units.length; i < j; i++) {
                        d = this.units[i], e = d.substr(0, 1).toUpperCase() + d.substr(1), f[d] = parseInt(b[d + "Max"] - (b[d + "Max"] - b[d + "Min"]) % b["step" + e], 10), g[d] = 0, l += '<dt class="ui_tpicker_' + d + '_label"' + (b["show" + e] ? "" : k) + ">" + b[d + "Text"] + "</dt>" + '<dd class="ui_tpicker_' + d + '"><div class="ui_tpicker_' + d + '_slider"' + (b["show" + e] ? "" : k) + "></div>";
                        if (b["show" + e] && b[d + "Grid"] > 0) {
                            l += '<div style="padding-left: 1px"><table class="ui-tpicker-grid-label"><tr>';
                            if (d == "hour") for (var m = b[d + "Min"]; m <= f[d]; m += parseInt(b[d + "Grid"], 10)) {
                                g[d]++;
                                var n = $.datepicker.formatTime(useAmpm(b.pickerTimeFormat || b.timeFormat) ? "hht" : "HH", {hour: m}, b);
                                l += '<td data-for="' + d + '">' + n + "</td>"
                            } else for (var o = b[d + "Min"]; o <= f[d]; o += parseInt(b[d + "Grid"], 10)) g[d]++, l += '<td data-for="' + d + '">' + (o < 10 ? "0" : "") + o + "</td>";
                            l += "</tr></table></div>"
                        }
                        l += "</dd>"
                    }
                    l += '<dt class="ui_tpicker_timezone_label"' + (b.showTimezone ? "" : k) + ">" + b.timezoneText + "</dt>", l += '<dd class="ui_tpicker_timezone" ' + (b.showTimezone ? "" : k) + "></dd>", l += "</dl></div>";
                    var p = $(l);
                    b.timeOnly === !0 && (p.prepend('<div class="ui-widget-header ui-helper-clearfix ui-corner-all"><div class="ui-datepicker-title">' + b.timeOnlyTitle + "</div>" + "</div>"), a.find(".ui-datepicker-header, .ui-datepicker-calendar").hide());
                    for (i = 0, j = c.units.length; i < j; i++) d = c.units[i], e = d.substr(0, 1).toUpperCase() + d.substr(1), c[d + "_slider"] = c.control.create(c, p.find(".ui_tpicker_" + d + "_slider"), d, c[d], b[d + "Min"], f[d], b["step" + e]), b["show" + e] && b[d + "Grid"] > 0 && (h = 100 * g[d] * b[d + "Grid"] / (f[d] - b[d + "Min"]), p.find(".ui_tpicker_" + d + " table").css({
                        width: h + "%",
                        marginLeft: b.isRTL ? "0" : h / (-2 * g[d]) + "%",
                        marginRight: b.isRTL ? h / (-2 * g[d]) + "%" : "0",
                        borderCollapse: "collapse"
                    }).find("td").click(function (a) {
                        var b = $(this), e = b.html(), f = parseInt(e.replace(/[^0-9]/g), 10),
                            g = e.replace(/[^apm]/ig), h = b.data("for");
                        h == "hour" && (g.indexOf("p") !== -1 && f < 12 ? f += 12 : g.indexOf("a") !== -1 && f === 12 && (f = 0)), c.control.value(c, c[h + "_slider"], d, f), c._onTimeChange(), c._onSelectHandler()
                    }).css({cursor: "pointer", width: 100 / g[d] + "%", textAlign: "center", overflow: "hidden"}));
                    this.timezone_select = p.find(".ui_tpicker_timezone").append("<select></select>").find("select"), $.fn.append.apply(this.timezone_select, $.map(b.timezoneList, function (a, b) {
                        return $("<option />").val(typeof a == "object" ? a.value : a).text(typeof a == "object" ? a.label : a)
                    }));
                    if (typeof this.timezone != "undefined" && this.timezone !== null && this.timezone !== "") {
                        var q = new Date(this.inst.selectedYear, this.inst.selectedMonth, this.inst.selectedDay, 12),
                            r = $.timepicker.timeZoneOffsetString(q);
                        r == this.timezone ? selectLocalTimeZone(c) : this.timezone_select.val(this.timezone)
                    } else typeof this.hour != "undefined" && this.hour !== null && this.hour !== "" ? this.timezone_select.val(b.defaultTimezone) : selectLocalTimeZone(c);
                    this.timezone_select.change(function () {
                        c._defaults.useLocalTimezone = !1, c._onTimeChange(), c._onSelectHandler()
                    });
                    var s = a.find(".ui-datepicker-buttonpane");
                    s.length ? s.before(p) : a.append(p), this.$timeObj = p.find(".ui_tpicker_time");
                    if (this.inst !== null) {
                        var t = this.timeDefined;
                        this._onTimeChange(), this.timeDefined = t
                    }
                    if (this._defaults.addSliderAccess) {
                        var u = this._defaults.sliderAccessArgs, v = this._defaults.isRTL;
                        u.isRTL = v, setTimeout(function () {
                            if (p.find(".ui-slider-access").length === 0) {
                                p.find(".ui-slider:visible").sliderAccess(u);
                                var a = p.find(".ui-slider-access:eq(0)").outerWidth(!0);
                                a && p.find("table:visible").each(function () {
                                    var b = $(this), c = b.outerWidth(),
                                        d = b.css(v ? "marginRight" : "marginLeft").toString().replace("%", ""),
                                        e = c - a, f = d * e / c + "%", g = {width: e, marginRight: 0, marginLeft: 0};
                                    g[v ? "marginRight" : "marginLeft"] = f, b.css(g)
                                })
                            }
                        }, 10)
                    }
                }
            },
            _limitMinMaxDateTime: function (a, b) {
                var c = this._defaults, d = new Date(a.selectedYear, a.selectedMonth, a.selectedDay);
                if (!!this._defaults.showTimepicker) {
                    if ($.datepicker._get(a, "minDateTime") !== null && $.datepicker._get(a, "minDateTime") !== undefined && d) {
                        var e = $.datepicker._get(a, "minDateTime"),
                            f = new Date(e.getFullYear(), e.getMonth(), e.getDate(), 0, 0, 0, 0);
                        if (this.hourMinOriginal === null || this.minuteMinOriginal === null || this.secondMinOriginal === null || this.millisecMinOriginal === null) this.hourMinOriginal = c.hourMin, this.minuteMinOriginal = c.minuteMin, this.secondMinOriginal = c.secondMin, this.millisecMinOriginal = c.millisecMin;
                        a.settings.timeOnly || f.getTime() == d.getTime() ? (this._defaults.hourMin = e.getHours(), this.hour <= this._defaults.hourMin ? (this.hour = this._defaults.hourMin, this._defaults.minuteMin = e.getMinutes(), this.minute <= this._defaults.minuteMin ? (this.minute = this._defaults.minuteMin, this._defaults.secondMin = e.getSeconds(), this.second <= this._defaults.secondMin ? (this.second = this._defaults.secondMin, this._defaults.millisecMin = e.getMilliseconds()) : (this.millisec < this._defaults.millisecMin && (this.millisec = this._defaults.millisecMin), this._defaults.millisecMin = this.millisecMinOriginal)) : (this._defaults.secondMin = this.secondMinOriginal, this._defaults.millisecMin = this.millisecMinOriginal)) : (this._defaults.minuteMin = this.minuteMinOriginal, this._defaults.secondMin = this.secondMinOriginal, this._defaults.millisecMin = this.millisecMinOriginal)) : (this._defaults.hourMin = this.hourMinOriginal, this._defaults.minuteMin = this.minuteMinOriginal, this._defaults.secondMin = this.secondMinOriginal, this._defaults.millisecMin = this.millisecMinOriginal)
                    }
                    if ($.datepicker._get(a, "maxDateTime") !== null && $.datepicker._get(a, "maxDateTime") !== undefined && d) {
                        var g = $.datepicker._get(a, "maxDateTime"),
                            h = new Date(g.getFullYear(), g.getMonth(), g.getDate(), 0, 0, 0, 0);
                        if (this.hourMaxOriginal === null || this.minuteMaxOriginal === null || this.secondMaxOriginal === null) this.hourMaxOriginal = c.hourMax, this.minuteMaxOriginal = c.minuteMax, this.secondMaxOriginal = c.secondMax, this.millisecMaxOriginal = c.millisecMax;
                        a.settings.timeOnly || h.getTime() == d.getTime() ? (this._defaults.hourMax = g.getHours(), this.hour >= this._defaults.hourMax ? (this.hour = this._defaults.hourMax, this._defaults.minuteMax = g.getMinutes(), this.minute >= this._defaults.minuteMax ? (this.minute = this._defaults.minuteMax, this._defaults.secondMax = g.getSeconds(), this.second >= this._defaults.secondMax ? (this.second = this._defaults.secondMax, this._defaults.millisecMax = g.getMilliseconds()) : (this.millisec > this._defaults.millisecMax && (this.millisec = this._defaults.millisecMax), this._defaults.millisecMax = this.millisecMaxOriginal)) : (this._defaults.secondMax = this.secondMaxOriginal, this._defaults.millisecMax = this.millisecMaxOriginal)) : (this._defaults.minuteMax = this.minuteMaxOriginal, this._defaults.secondMax = this.secondMaxOriginal, this._defaults.millisecMax = this.millisecMaxOriginal)) : (this._defaults.hourMax = this.hourMaxOriginal, this._defaults.minuteMax = this.minuteMaxOriginal, this._defaults.secondMax = this.secondMaxOriginal, this._defaults.millisecMax = this.millisecMaxOriginal)
                    }
                    if (b !== undefined && b === !0) {
                        var i = parseInt(this._defaults.hourMax - (this._defaults.hourMax - this._defaults.hourMin) % this._defaults.stepHour, 10),
                            j = parseInt(this._defaults.minuteMax - (this._defaults.minuteMax - this._defaults.minuteMin) % this._defaults.stepMinute, 10),
                            k = parseInt(this._defaults.secondMax - (this._defaults.secondMax - this._defaults.secondMin) % this._defaults.stepSecond, 10),
                            l = parseInt(this._defaults.millisecMax - (this._defaults.millisecMax - this._defaults.millisecMin) % this._defaults.stepMillisec, 10);
                        this.hour_slider && (this.control.options(this, this.hour_slider, "hour", {
                            min: this._defaults.hourMin,
                            max: i
                        }), this.control.value(this, this.hour_slider, "hour", this.hour - this.hour % this._defaults.stepHour)), this.minute_slider && (this.control.options(this, this.minute_slider, "minute", {
                            min: this._defaults.minuteMin,
                            max: j
                        }), this.control.value(this, this.minute_slider, "minute", this.minute - this.minute % this._defaults.stepMinute)), this.second_slider && (this.control.options(this, this.second_slider, "second", {
                            min: this._defaults.secondMin,
                            max: k
                        }), this.control.value(this, this.second_slider, "second", this.second - this.second % this._defaults.stepSecond)), this.millisec_slider && (this.control.options(this, this.millisec_slider, "millisec", {
                            min: this._defaults.millisecMin,
                            max: l
                        }), this.control.value(this, this.millisec_slider, "millisec", this.millisec - this.millisec % this._defaults.stepMillisec))
                    }
                }
            },
            _onTimeChange: function () {
                var a = this.hour_slider ? this.control.value(this, this.hour_slider, "hour") : !1,
                    b = this.minute_slider ? this.control.value(this, this.minute_slider, "minute") : !1,
                    c = this.second_slider ? this.control.value(this, this.second_slider, "second") : !1,
                    d = this.millisec_slider ? this.control.value(this, this.millisec_slider, "millisec") : !1,
                    e = this.timezone_select ? this.timezone_select.val() : !1, f = this._defaults,
                    g = f.pickerTimeFormat || f.timeFormat, h = f.pickerTimeSuffix || f.timeSuffix;
                typeof a == "object" && (a = !1), typeof b == "object" && (b = !1), typeof c == "object" && (c = !1), typeof d == "object" && (d = !1), typeof e == "object" && (e = !1), a !== !1 && (a = parseInt(a, 10)), b !== !1 && (b = parseInt(b, 10)), c !== !1 && (c = parseInt(c, 10)), d !== !1 && (d = parseInt(d, 10));
                var i = f[a < 12 ? "amNames" : "pmNames"][0],
                    j = a != this.hour || b != this.minute || c != this.second || d != this.millisec || this.ampm.length > 0 && a < 12 != ($.inArray(this.ampm.toUpperCase(), this.amNames) !== -1) || this.timezone === null && e != this.defaultTimezone || this.timezone !== null && e != this.timezone;
                j && (a !== !1 && (this.hour = a), b !== !1 && (this.minute = b), c !== !1 && (this.second = c), d !== !1 && (this.millisec = d), e !== !1 && (this.timezone = e), this.inst || (this.inst = $.datepicker._getInst(this.$input[0])), this._limitMinMaxDateTime(this.inst, !0)), useAmpm(f.timeFormat) && (this.ampm = i), this.formattedTime = $.datepicker.formatTime(f.timeFormat, this, f), this.$timeObj && (g === f.timeFormat ? this.$timeObj.text(this.formattedTime + h) : this.$timeObj.text($.datepicker.formatTime(g, this, f) + h)), this.timeDefined = !0, j && this._updateDateTime()
            },
            _onSelectHandler: function () {
                var a = this._defaults.onSelect || this.inst.settings.onSelect, b = this.$input ? this.$input[0] : null;
                a && b && a.apply(b, [this.formattedDateTime, this])
            },
            _updateDateTime: function (a) {
                a = this.inst || a;
                var b = $.datepicker._daylightSavingAdjust(new Date(a.selectedYear, a.selectedMonth, a.selectedDay)),
                    c = $.datepicker._get(a, "dateFormat"), d = $.datepicker._getFormatConfig(a),
                    e = b !== null && this.timeDefined;
                this.formattedDate = $.datepicker.formatDate(c, b === null ? new Date : b, d);
                var f = this.formattedDate;
                a.lastVal === "" && (a.currentYear = a.selectedYear, a.currentMonth = a.selectedMonth, a.currentDay = a.selectedDay), this._defaults.timeOnly === !0 ? f = this.formattedTime : this._defaults.timeOnly !== !0 && (this._defaults.alwaysSetTime || e) && (f += this._defaults.separator + this.formattedTime + this._defaults.timeSuffix), this.formattedDateTime = f;
                if (!this._defaults.showTimepicker) this.$input.val(this.formattedDate); else if (this.$altInput && this._defaults.altFieldTimeOnly === !0) this.$altInput.val(this.formattedTime), this.$input.val(this.formattedDate); else if (this.$altInput) {
                    this.$input.val(f);
                    var g = "",
                        h = this._defaults.altSeparator ? this._defaults.altSeparator : this._defaults.separator,
                        i = this._defaults.altTimeSuffix ? this._defaults.altTimeSuffix : this._defaults.timeSuffix;
                    this._defaults.altFormat ? g = $.datepicker.formatDate(this._defaults.altFormat, b === null ? new Date : b, d) : g = this.formattedDate, g && (g += h), this._defaults.altTimeFormat ? g += $.datepicker.formatTime(this._defaults.altTimeFormat, this, this._defaults) + i : g += this.formattedTime + i, this.$altInput.val(g)
                } else this.$input.val(f);
                this.$input.trigger("change")
            },
            _onFocus: function () {
                if (!this.$input.val() && this._defaults.defaultValue) {
                    this.$input.val(this._defaults.defaultValue);
                    var a = $.datepicker._getInst(this.$input.get(0)), b = $.datepicker._get(a, "timepicker");
                    if (b && b._defaults.timeOnly && a.input.val() != a.lastVal) try {
                        $.datepicker._updateDatepicker(a)
                    } catch (c) {
                        $.timepicker.log(c)
                    }
                }
            },
            _controls: {
                slider: {
                    create: function (a, b, c, d, e, f, g) {
                        var h = a._defaults.isRTL;
                        return b.prop("slide", null).slider({
                            orientation: "horizontal",
                            value: h ? d * -1 : d,
                            min: h ? f * -1 : e,
                            max: h ? e * -1 : f,
                            step: g,
                            slide: function (b, d) {
                                a.control.value(a, $(this), c, h ? d.value * -1 : d.value), a._onTimeChange()
                            },
                            stop: function (b, c) {
                                a._onSelectHandler()
                            }
                        })
                    }, options: function (a, b, c, d, e) {
                        if (a._defaults.isRTL) {
                            if (typeof d == "string") {
                                if (d == "min" || d == "max") {
                                    if (e !== undefined) return b.slider(d, e * -1);
                                    return Math.abs(b.slider(d))
                                }
                                return b.slider(d)
                            }
                            var f = d.min, g = d.max;
                            d.min = d.max = null, f !== undefined && (d.max = f * -1), g !== undefined && (d.min = g * -1);
                            return b.slider(d)
                        }
                        if (typeof d == "string" && e !== undefined) return b.slider(d, e);
                        return b.slider(d)
                    }, value: function (a, b, c, d) {
                        if (a._defaults.isRTL) {
                            if (d !== undefined) return b.slider("value", d * -1);
                            return Math.abs(b.slider("value"))
                        }
                        if (d !== undefined) return b.slider("value", d);
                        return b.slider("value")
                    }
                }, select: {
                    create: function (a, b, c, d, e, f, g) {
                        var h = '<select class="ui-timepicker-select" data-unit="' + c + '" data-min="' + e + '" data-max="' + f + '" data-step="' + g + '">',
                            i = a._defaults.pickerTimeFormat || a._defaults.timeFormat;
                        for (var j = e; j <= f; j += g) h += '<option value="' +
                            j + '"' + (j == d ? " selected" : "") + ">", c == "hour" ? h += $.datepicker.formatTime($.trim(i.replace(/[^ht ]/ig, "")), {hour: j}, a._defaults) : c == "millisec" || j >= 10 ? h += j : h += "0" + j.toString(), h += "</option>";
                        h += "</select>", b.children("select").remove(), $(h).appendTo(b).change(function (b) {
                            a._onTimeChange(), a._onSelectHandler()
                        });
                        return b
                    }, options: function (a, b, c, d, e) {
                        var f = {}, g = b.children("select");
                        if (typeof d == "string") {
                            if (e === undefined) return g.data(d);
                            f[d] = e
                        } else f = d;
                        return a.control.create(a, b, g.data("unit"), g.val(), f.min || g.data("min"), f.max || g.data("max"), f.step || g.data("step"))
                    }, value: function (a, b, c, d) {
                        var e = b.children("select");
                        if (d !== undefined) return e.val(d);
                        return e.val()
                    }
                }
            }
        }), $.fn.extend({
            timepicker: function (a) {
                a = a || {};
                var b = Array.prototype.slice.call(arguments);
                typeof a == "object" && (b[0] = $.extend(a, {timeOnly: !0}));
                return $(this).each(function () {
                    $.fn.datetimepicker.apply($(this), b)
                })
            }, datetimepicker: function (a) {
                a = a || {};
                var b = arguments;
                return typeof a == "string" ? a == "getDate" ? $.fn.datepicker.apply($(this[0]), b) : this.each(function () {
                    var a = $(this);
                    a.datepicker.apply(a, b)
                }) : this.each(function () {
                    var b = $(this);
                    b.datepicker($.timepicker._newInst(b, a)._defaults)
                })
            }
        }), $.datepicker.parseDateTime = function (a, b, c, d, e) {
            var f = parseDateTimeInternal(a, b, c, d, e);
            if (f.timeObj) {
                var g = f.timeObj;
                f.date.setHours(g.hour, g.minute, g.second, g.millisec)
            }
            return f.date
        }, $.datepicker.parseTime = function (a, b, c) {
            var d = extendRemove(extendRemove({}, $.timepicker._defaults), c || {}), e = function (a, b, c) {
                var d = function (a, b) {
                    var c = [];
                    a && $.merge(c, a), b && $.merge(c, b), c = $.map(c, function (a) {
                        return a.replace(/[.*+?|()\[\]{}\\]/g, "\\$&")
                    });
                    return "(" + c.join("|") + ")?"
                }, e = function (a) {
                    var b = a.toLowerCase().match(/(h{1,2}|m{1,2}|s{1,2}|l{1}|t{1,2}|z|'.*?')/g),
                        c = {h: -1, m: -1, s: -1, l: -1, t: -1, z: -1};
                    if (b) for (var d = 0; d < b.length; d++) c[b[d].toString().charAt(0)] == -1 && (c[b[d].toString().charAt(0)] = d + 1);
                    return c
                }, f = "^" + a.toString().replace(/([hH]{1,2}|mm?|ss?|[tT]{1,2}|[lz]|'.*?')/g, function (a) {
                    var b = a.length;
                    switch (a.charAt(0).toLowerCase()) {
                        case"h":
                            return b === 1 ? "(\\d?\\d)" : "(\\d{" + b + "})";
                        case"m":
                            return b === 1 ? "(\\d?\\d)" : "(\\d{" + b + "})";
                        case"s":
                            return b === 1 ? "(\\d?\\d)" : "(\\d{" + b + "})";
                        case"l":
                            return "(\\d?\\d?\\d)";
                        case"z":
                            return "(z|[-+]\\d\\d:?\\d\\d|\\S+)?";
                        case"t":
                            return d(c.amNames, c.pmNames);
                        default:
                            return "(" + a.replace(/\'/g, "").replace(/(\.|\$|\^|\\|\/|\(|\)|\[|\]|\?|\+|\*)/g, function (a) {
                                return "\\" + a
                            }) + ")?"
                    }
                }).replace(/\s/g, "\\s?") + c.timeSuffix + "$", g = e(a), h = "", i;
                i = b.match(new RegExp(f, "i"));
                var j = {hour: 0, minute: 0, second: 0, millisec: 0};
                if (i) {
                    g.t !== -1 && (i[g.t] === undefined || i[g.t].length === 0 ? (h = "", j.ampm = "") : (h = $.inArray(i[g.t].toUpperCase(), c.amNames) !== -1 ? "AM" : "PM", j.ampm = c[h == "AM" ? "amNames" : "pmNames"][0])), g.h !== -1 && (h == "AM" && i[g.h] == "12" ? j.hour = 0 : h == "PM" && i[g.h] != "12" ? j.hour = parseInt(i[g.h], 10) + 12 : j.hour = Number(i[g.h])), g.m !== -1 && (j.minute = Number(i[g.m])), g.s !== -1 && (j.second = Number(i[g.s])), g.l !== -1 && (j.millisec = Number(i[g.l]));
                    if (g.z !== -1 && i[g.z] !== undefined) {
                        var k = i[g.z].toUpperCase();
                        switch (k.length) {
                            case 1:
                                k = c.timezoneIso8601 ? "Z" : "+0000";
                                break;
                            case 5:
                                c.timezoneIso8601 && (k = k.substring(1) == "0000" ? "Z" : k.substring(0, 3) + ":" + k.substring(3));
                                break;
                            case 6:
                                c.timezoneIso8601 ? k.substring(1) == "00:00" && (k = "Z") : k = k == "Z" || k.substring(1) == "00:00" ? "+0000" : k.replace(/:/, "")
                        }
                        j.timezone = k
                    }
                    return j
                }
                return !1
            }, f = function (a, b, c) {
                try {
                    var d = new Date("2012-01-01 " + b);
                    if (isNaN(d.getTime())) {
                        d = new Date("2012-01-01T" + b);
                        if (isNaN(d.getTime())) {
                            d = new Date("01/01/2012 " + b);
                            if (isNaN(d.getTime())) throw"Unable to parse time with native Date: " + b
                        }
                    }
                    return {
                        hour: d.getHours(),
                        minute: d.getMinutes(),
                        second: d.getSeconds(),
                        millisec: d.getMilliseconds(),
                        timezone: $.timepicker.timeZoneOffsetString(d)
                    }
                } catch (f) {
                    try {
                        return e(a, b, c)
                    } catch (g) {
                        $.timepicker.log("Unable to parse \ntimeString: " + b + "\ntimeFormat: " + a)
                    }
                }
                return !1
            };
            if (typeof d.parse == "function") return d.parse(a, b, d);
            if (d.parse === "loose") return f(a, b, d);
            return e(a, b, d)
        }, $.datepicker.formatTime = function (a, b, c) {
            c = c || {}, c = $.extend({}, $.timepicker._defaults, c), b = $.extend({
                hour: 0,
                minute: 0,
                second: 0,
                millisec: 0,
                timezone: "+0000"
            }, b);
            var d = a, e = c.amNames[0], f = parseInt(b.hour, 10);
            f > 11 && (e = c.pmNames[0]), d = d.replace(/(?:HH?|hh?|mm?|ss?|[tT]{1,2}|[lz]|('.*?'|".*?"))/g, function (a) {
                switch (a) {
                    case"HH":
                        return ("0" + f).slice(-2);
                    case"H":
                        return f;
                    case"hh":
                        return ("0" + convert24to12(f)).slice(-2);
                    case"h":
                        return convert24to12(f);
                    case"mm":
                        return ("0" + b.minute).slice(-2);
                    case"m":
                        return b.minute;
                    case"ss":
                        return ("0" + b.second).slice(-2);
                    case"s":
                        return b.second;
                    case"l":
                        return ("00" + b.millisec).slice(-3);
                    case"z":
                        return b.timezone === null ? c.defaultTimezone : b.timezone;
                    case"T":
                        return e.charAt(0).toUpperCase();
                    case"TT":
                        return e.toUpperCase();
                    case"t":
                        return e.charAt(0).toLowerCase();
                    case"tt":
                        return e.toLowerCase();
                    default:
                        return a.replace(/\'/g, "") || "'"
                }
            }), d = $.trim(d);
            return d
        }, $.datepicker._base_selectDate = $.datepicker._selectDate, $.datepicker._selectDate = function (a, b) {
            var c = this._getInst($(a)[0]), d = this._get(c, "timepicker");
            d ? (d._limitMinMaxDateTime(c, !0), c.inline = c.stay_open = !0, this._base_selectDate(a, b), c.inline = c.stay_open = !1, this._notifyChange(c)) : this._base_selectDate(a, b)
        }, $.datepicker._base_updateDatepicker = $.datepicker._updateDatepicker, $.datepicker._updateDatepicker = function (a) {
            var b = a.input[0];
            if (!$.datepicker._curInst || $.datepicker._curInst == a || !$.datepicker._datepickerShowing || $.datepicker._lastInput == b) if (typeof a.stay_open != "boolean" || a.stay_open === !1) {
                this._base_updateDatepicker(a);
                var c = this._get(a, "timepicker");
                c && c._addTimePicker(a)
            }
        }, $.datepicker._base_doKeyPress = $.datepicker._doKeyPress, $.datepicker._doKeyPress = function (a) {
            var b = $.datepicker._getInst(a.target), c = $.datepicker._get(b, "timepicker");
            if (c && $.datepicker._get(b, "constrainInput")) {
                var d = useAmpm(c._defaults.timeFormat),
                    e = $.datepicker._possibleChars($.datepicker._get(b, "dateFormat")),
                    f = c._defaults.timeFormat.toString().replace(/[hms]/g, "").replace(/TT/g, d ? "APM" : "").replace(/Tt/g, d ? "AaPpMm" : "").replace(/tT/g, d ? "AaPpMm" : "").replace(/T/g, d ? "AP" : "").replace(/tt/g, d ? "apm" : "").replace(/t/g, d ? "ap" : "") + " " + c._defaults.separator + c._defaults.timeSuffix + (c._defaults.showTimezone ? c._defaults.timezoneList.join("") : "") + c._defaults.amNames.join("") + c._defaults.pmNames.join("") + e,
                    g = String.fromCharCode(a.charCode === undefined ? a.keyCode : a.charCode);
                return a.ctrlKey || g < " " || !e || f.indexOf(g) > -1
            }
            return $.datepicker._base_doKeyPress(a)
        }, $.datepicker._base_updateAlternate = $.datepicker._updateAlternate, $.datepicker._updateAlternate = function (a) {
            var b = this._get(a, "timepicker");
            if (b) {
                var c = b._defaults.altField;
                if (c) {
                    var d = b._defaults.altFormat || b._defaults.dateFormat, e = this._getDate(a),
                        f = $.datepicker._getFormatConfig(a), g = "",
                        h = b._defaults.altSeparator ? b._defaults.altSeparator : b._defaults.separator,
                        i = b._defaults.altTimeSuffix ? b._defaults.altTimeSuffix : b._defaults.timeSuffix,
                        j = b._defaults.altTimeFormat !== null ? b._defaults.altTimeFormat : b._defaults.timeFormat;
                    g += $.datepicker.formatTime(j, b, b._defaults) + i, !b._defaults.timeOnly && !b._defaults.altFieldTimeOnly && e !== null && (b._defaults.altFormat ? g = $.datepicker.formatDate(b._defaults.altFormat, e, f) + h + g : g = b.formattedDate + h + g), $(c).val(g)
                }
            } else $.datepicker._base_updateAlternate(a)
        }, $.datepicker._base_doKeyUp = $.datepicker._doKeyUp, $.datepicker._doKeyUp = function (a) {
            var b = $.datepicker._getInst(a.target), c = $.datepicker._get(b, "timepicker");
            if (c && c._defaults.timeOnly && b.input.val() != b.lastVal) try {
                $.datepicker._updateDatepicker(b)
            } catch (d) {
                $.timepicker.log(d)
            }
            return $.datepicker._base_doKeyUp(a)
        }, $.datepicker._base_gotoToday = $.datepicker._gotoToday, $.datepicker._gotoToday = function (a) {
            var b = this._getInst($(a)[0]), c = b.dpDiv;
            this._base_gotoToday(a);
            var d = this._get(b, "timepicker");
            selectLocalTimeZone(d);
            var e = new Date;
            e.setSeconds(0), this._setTime(b, e), $(".ui-datepicker-today", c).click()
        }, $.datepicker._disableTimepickerDatepicker = function (a) {
            var b = this._getInst(a);
            if (!!b) {
                var c = this._get(b, "timepicker");
                $(a).datepicker("getDate"), c && (c._defaults.showTimepicker = !1, c._updateDateTime(b))
            }
        }, $.datepicker._enableTimepickerDatepicker = function (a) {
            var b = this._getInst(a);
            if (!!b) {
                var c = this._get(b, "timepicker");
                $(a).datepicker("getDate"), c && (c._defaults.showTimepicker = !0, c._addTimePicker(b), c._updateDateTime(b))
            }
        }, $.datepicker._setTime = function (a, b) {
            var c = this._get(a, "timepicker");
            if (c) {
                var d = c._defaults;
                c.hour = b ? b.getHours() : d.hour, c.minute = b ? b.getMinutes() : d.minute, c.second = b ? b.getSeconds() : d.second, c.millisec = b ? b.getMilliseconds() : d.millisec, c._limitMinMaxDateTime(a, !0), c._onTimeChange(), c._updateDateTime(a)
            }
        }, $.datepicker._setTimeDatepicker = function (a, b, c) {
            var d = this._getInst(a);
            if (!!d) {
                var e = this._get(d, "timepicker");
                if (e) {
                    this._setDateFromField(d);
                    var f;
                    b && (typeof b == "string" ? (e._parseTime(b, c), f = new Date, f.setHours(e.hour, e.minute, e.second, e.millisec)) : f = new Date(b.getTime()), f.toString() == "Invalid Date" && (f = undefined), this._setTime(d, f))
                }
            }
        }, $.datepicker._base_setDateDatepicker = $.datepicker._setDateDatepicker, $.datepicker._setDateDatepicker = function (a, b) {
            var c = this._getInst(a);
            if (!!c) {
                var d = b instanceof Date ? new Date(b.getTime()) : b;
                this._updateDatepicker(c), this._base_setDateDatepicker.apply(this, arguments), this._setTimeDatepicker(a, d, !0)
            }
        }, $.datepicker._base_getDateDatepicker = $.datepicker._getDateDatepicker, $.datepicker._getDateDatepicker = function (a, b) {
            var c = this._getInst(a);
            if (!!c) {
                var d = this._get(c, "timepicker");
                if (d) {
                    c.lastVal === undefined && this._setDateFromField(c, b);
                    var e = this._getDate(c);
                    e && d._parseTime($(a).val(), d.timeOnly) && e.setHours(d.hour, d.minute, d.second, d.millisec);
                    return e
                }
                return this._base_getDateDatepicker(a, b)
            }
        }, $.datepicker._base_parseDate = $.datepicker.parseDate, $.datepicker.parseDate = function (a, b, c) {
            var d;
            try {
                d = this._base_parseDate(a, b, c)
            } catch (e) {
                if (e.indexOf(":") >= 0) d = this._base_parseDate(a, b.substring(0, b.length - (e.length - e.indexOf(":") - 2)), c), $.timepicker.log("Error parsing the date string: " + e + "\ndate string = " + b + "\ndate format = " + a); else throw e
            }
            return d
        }, $.datepicker._base_formatDate = $.datepicker._formatDate, $.datepicker._formatDate = function (a, b, c, d) {
            var e = this._get(a, "timepicker");
            if (e) {
                e._updateDateTime(a);
                return e.$input.val()
            }
            return this._base_formatDate(a)
        }, $.datepicker._base_optionDatepicker = $.datepicker._optionDatepicker, $.datepicker._optionDatepicker = function (a, b, c) {
            var d = this._getInst(a), e;
            if (!d) return null;
            var f = this._get(d, "timepicker");
            if (f) {
                var g = null, h = null, i = null, j = f._defaults.evnts, k = {}, l;
                if (typeof b == "string") {
                    if (b === "minDate" || b === "minDateTime") g = c; else if (b === "maxDate" || b === "maxDateTime") h = c; else if (b === "onSelect") i = c; else if (j.hasOwnProperty(b)) {
                        if (typeof c == "undefined") return j[b];
                        k[b] = c, e = {}
                    }
                } else if (typeof b == "object") {
                    b.minDate ? g = b.minDate : b.minDateTime ? g = b.minDateTime : b.maxDate ? h = b.maxDate : b.maxDateTime && (h = b.maxDateTime);
                    for (l in j) j.hasOwnProperty(l) && b[l] && (k[l] = b[l])
                }
                for (l in k) k.hasOwnProperty(l) && (j[l] = k[l], e || (e = $.extend({}, b)), delete e[l]);
                if (e && isEmptyObject(e)) return;
                g ? (g === 0 ? g = new Date : g = new Date(g), f._defaults.minDate = g, f._defaults.minDateTime = g) : h ? (h === 0 ? h = new Date : h = new Date(h), f._defaults.maxDate = h, f._defaults.maxDateTime = h) : i && (f._defaults.onSelect = i)
            }
            if (c === undefined) return this._base_optionDatepicker.call($.datepicker, a, b);
            return this._base_optionDatepicker.call($.datepicker, a, e || b, c)
        };
        var isEmptyObject = function (a) {
            var b;
            for (b in a) if (a.hasOwnProperty(a)) return !1;
            return !0
        }, extendRemove = function (a, b) {
            $.extend(a, b);
            for (var c in b) if (b[c] === null || b[c] === undefined) a[c] = b[c];
            return a
        }, useAmpm = function (a) {
            return (a.indexOf("t") !== -1 || a.indexOf("T") !== -1) && a.indexOf("h") !== -1
        }, convert24to12 = function (a) {
            a > 12 && (a = a - 12), a === 0 && (a = 12);
            return String(a)
        }, splitDateTime = function (a, b, c, d) {
            try {
                var e = d && d.separator ? d.separator : $.timepicker._defaults.separator,
                    f = d && d.timeFormat ? d.timeFormat : $.timepicker._defaults.timeFormat, g = f.split(e),
                    h = g.length, i = b.split(e), j = i.length;
                if (j > 1) return [i.splice(0, j - h).join(e), i.splice(0, h).join(e)]
            } catch (k) {
                $.timepicker.log("Could not split the date from the time. Please check the following datetimepicker options\nthrown error: " + k + "\ndateTimeString" + b + "\ndateFormat = " + a + "\nseparator = " + d.separator + "\ntimeFormat = " + d.timeFormat);
                if (k.indexOf(":") >= 0) {
                    var l = b.length - (k.length - k.indexOf(":") - 2), m = b.substring(l);
                    return [$.trim(b.substring(0, l)), $.trim(b.substring(l))]
                }
                throw k
            }
            return [b, ""]
        }, parseDateTimeInternal = function (a, b, c, d, e) {
            var f, g = splitDateTime(a, c, d, e);
            f = $.datepicker._base_parseDate(a, g[0], d);
            if (g[1] !== "") {
                var h = g[1], i = $.datepicker.parseTime(b, h, e);
                if (i === null) throw"Wrong time format";
                return {date: f, timeObj: i}
            }
            return {date: f}
        }, selectLocalTimeZone = function (a, b) {
            if (a && a.timezone_select) {
                a._defaults.useLocalTimezone = !0;
                var c = typeof b != "undefined" ? b : new Date, d = $.timepicker.timeZoneOffsetString(c);
                a._defaults.timezoneIso8601 && (d = d.substring(0, 3) + ":" + d.substring(3)), a.timezone_select.val(d)
            }
        };
        $.timepicker = new Timepicker, $.timepicker.timeZoneOffsetString = function (a) {
            var b = a.getTimezoneOffset() * -1, c = b % 60, d = (b - c) / 60;
            return (b >= 0 ? "+" : "-") + ("0" + (d * 101).toString()).slice(-2) + ("0" + (c * 101).toString()).slice(-2)
        }, $.timepicker.timeRange = function (a, b, c) {
            return $.timepicker.handleRange("timepicker", a, b, c)
        }, $.timepicker.dateTimeRange = function (a, b, c) {
            $.timepicker.dateRange(a, b, c, "datetimepicker")
        }, $.timepicker.dateRange = function (a, b, c, d) {
            d = d || "datepicker", $.timepicker.handleRange(d, a, b, c)
        }, $.timepicker.handleRange = function (a, b, c, d) {
            function f(b, c, d) {
                if (!!$(b).val()) {
                    var e = $(b)[a].call($(b), "getDate");
                    e.getTime && $(c)[a].call($(c), "option", d, e)
                }
            }

            function e(a, d, e) {
                d.val() && new Date(b.val()) > new Date(c.val()) && d.val(e)
            }

            $.fn[a].call(b, $.extend({
                onClose: function (a, b) {
                    e(this, c, a)
                }, onSelect: function (a) {
                    f(this, c, "minDate")
                }
            }, d, d.start)), $.fn[a].call(c, $.extend({
                onClose: function (a, c) {
                    e(this, b, a)
                }, onSelect: function (a) {
                    f(this, b, "maxDate")
                }
            }, d, d.end)), a != "timepicker" && d.reformat && $([b, c]).each(function () {
                var b = $(this)[a].call($(this), "option", "dateFormat"), c = new Date($(this).val());
                $(this).val() && c && $(this).val($.datepicker.formatDate(b, c))
            }), e(b, c, b.val()), f(b, c, "minDate"), f(c, b, "maxDate");
            return $([b.get(0), c.get(0)])
        }, $.timepicker.log = function (a) {
            window.console && console.log(a)
        }, $.timepicker.version = "1.2.2"
    }
})(jQuery);

/* Simplified Chinese translation for the jQuery Timepicker Addon /
/ Written by Will Lu */
(function ($) {
    $.timepicker.regional['zh-CN'] = {
        timeOnlyTitle: '选择时间',
        timeText: '时间',
        hourText: '小时',
        minuteText: '分钟',
        secondText: '秒钟',
        millisecText: '微秒',
        microsecText: '微秒',
        timezoneText: '时区',
        currentText: '现在时间',
        closeText: '关闭',
        timeFormat: 'HH:mm',
        amNames: ['AM', 'A'],
        pmNames: ['PM', 'P'],
        isRTL: false
    };
    $.timepicker.setDefaults($.timepicker.regional['zh-CN']);
})(jQuery);

// datepicker locale zh-cn lang
jQuery(function (e) {
    e.datepicker.regional["zh-CN"] = {
        closeText: "关闭",
        prevText: "&#x3C;上月",
        nextText: "下月&#x3E;",
        currentText: "今天",
        monthNames: ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
        monthNamesShort: ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
        dayNames: ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
        dayNamesShort: ["周日", "周一", "周二", "周三", "周四", "周五", "周六"],
        dayNamesMin: ["日", "一", "二", "三", "四", "五", "六"],
        weekHeader: "周",
        dateFormat: "yy-mm-dd",
        firstDay: 1,
        isRTL: !1,
        showMonthAfterYear: !0,
        yearSuffix: "年"
    }, e.datepicker.setDefaults(e.datepicker.regional["zh-CN"])
});
/*! jQuery UI - v1.9.2 - 2015-03-29
* http://jqueryui.com
* Copyright 2015 jQuery Foundation and other contributors; Licensed MIT */

(function (e, t) {
    var i = 0, s = Array.prototype.slice, a = e.cleanData;
    e.cleanData = function (t) {
        for (var i, s = 0; null != (i = t[s]); s++) try {
            e(i).triggerHandler("remove")
        } catch (n) {
        }
        a(t)
    }, e.widget = function (i, s, a) {
        var n, o, r, h, l = i.split(".")[0];
        i = i.split(".")[1], n = l + "-" + i, a || (a = s, s = e.Widget), e.expr[":"][n.toLowerCase()] = function (t) {
            return !!e.data(t, n)
        }, e[l] = e[l] || {}, o = e[l][i], r = e[l][i] = function (e, i) {
            return this._createWidget ? (arguments.length && this._createWidget(e, i), t) : new r(e, i)
        }, e.extend(r, o, {
            version: a.version,
            _proto: e.extend({}, a),
            _childConstructors: []
        }), h = new s, h.options = e.widget.extend({}, h.options), e.each(a, function (t, i) {
            e.isFunction(i) && (a[t] = function () {
                var e = function () {
                    return s.prototype[t].apply(this, arguments)
                }, a = function (e) {
                    return s.prototype[t].apply(this, e)
                };
                return function () {
                    var t, s = this._super, n = this._superApply;
                    return this._super = e, this._superApply = a, t = i.apply(this, arguments), this._super = s, this._superApply = n, t
                }
            }())
        }), r.prototype = e.widget.extend(h, {widgetEventPrefix: o ? h.widgetEventPrefix : i}, a, {
            constructor: r,
            namespace: l,
            widgetName: i,
            widgetBaseClass: n,
            widgetFullName: n
        }), o ? (e.each(o._childConstructors, function (t, i) {
            var s = i.prototype;
            e.widget(s.namespace + "." + s.widgetName, r, i._proto)
        }), delete o._childConstructors) : s._childConstructors.push(r), e.widget.bridge(i, r)
    }, e.widget.extend = function (i) {
        for (var a, n, o = s.call(arguments, 1), r = 0, h = o.length; h > r; r++) for (a in o[r]) n = o[r][a], o[r].hasOwnProperty(a) && n !== t && (i[a] = e.isPlainObject(n) ? e.isPlainObject(i[a]) ? e.widget.extend({}, i[a], n) : e.widget.extend({}, n) : n);
        return i
    }, e.widget.bridge = function (i, a) {
        var n = a.prototype.widgetFullName || i;
        e.fn[i] = function (o) {
            var r = "string" == typeof o, h = s.call(arguments, 1), l = this;
            return o = !r && h.length ? e.widget.extend.apply(null, [o].concat(h)) : o, r ? this.each(function () {
                var s, a = e.data(this, n);
                return a ? e.isFunction(a[o]) && "_" !== o.charAt(0) ? (s = a[o].apply(a, h), s !== a && s !== t ? (l = s && s.jquery ? l.pushStack(s.get()) : s, !1) : t) : e.error("no such method '" + o + "' for " + i + " widget instance") : e.error("cannot call methods on " + i + " prior to initialization; " + "attempted to call method '" + o + "'")
            }) : this.each(function () {
                var t = e.data(this, n);
                t ? t.option(o || {})._init() : e.data(this, n, new a(o, this))
            }), l
        }
    }, e.Widget = function () {
    }, e.Widget._childConstructors = [], e.Widget.prototype = {
        widgetName: "widget",
        widgetEventPrefix: "",
        defaultElement: "<div>",
        options: {disabled: !1, create: null},
        _createWidget: function (t, s) {
            s = e(s || this.defaultElement || this)[0], this.element = e(s), this.uuid = i++, this.eventNamespace = "." + this.widgetName + this.uuid, this.options = e.widget.extend({}, this.options, this._getCreateOptions(), t), this.bindings = e(), this.hoverable = e(), this.focusable = e(), s !== this && (e.data(s, this.widgetName, this), e.data(s, this.widgetFullName, this), this._on(!0, this.element, {
                remove: function (e) {
                    e.target === s && this.destroy()
                }
            }), this.document = e(s.style ? s.ownerDocument : s.document || s), this.window = e(this.document[0].defaultView || this.document[0].parentWindow)), this._create(), this._trigger("create", null, this._getCreateEventData()), this._init()
        },
        _getCreateOptions: e.noop,
        _getCreateEventData: e.noop,
        _create: e.noop,
        _init: e.noop,
        destroy: function () {
            this._destroy(), this.element.unbind(this.eventNamespace).removeData(this.widgetName).removeData(this.widgetFullName).removeData(e.camelCase(this.widgetFullName)), this.widget().unbind(this.eventNamespace).removeAttr("aria-disabled").removeClass(this.widgetFullName + "-disabled " + "ui-state-disabled"), this.bindings.unbind(this.eventNamespace), this.hoverable.removeClass("ui-state-hover"), this.focusable.removeClass("ui-state-focus")
        },
        _destroy: e.noop,
        widget: function () {
            return this.element
        },
        option: function (i, s) {
            var a, n, o, r = i;
            if (0 === arguments.length) return e.widget.extend({}, this.options);
            if ("string" == typeof i) if (r = {}, a = i.split("."), i = a.shift(), a.length) {
                for (n = r[i] = e.widget.extend({}, this.options[i]), o = 0; a.length - 1 > o; o++) n[a[o]] = n[a[o]] || {}, n = n[a[o]];
                if (i = a.pop(), s === t) return n[i] === t ? null : n[i];
                n[i] = s
            } else {
                if (s === t) return this.options[i] === t ? null : this.options[i];
                r[i] = s
            }
            return this._setOptions(r), this
        },
        _setOptions: function (e) {
            var t;
            for (t in e) this._setOption(t, e[t]);
            return this
        },
        _setOption: function (e, t) {
            return this.options[e] = t, "disabled" === e && (this.widget().toggleClass(this.widgetFullName + "-disabled ui-state-disabled", !!t).attr("aria-disabled", t), this.hoverable.removeClass("ui-state-hover"), this.focusable.removeClass("ui-state-focus")), this
        },
        enable: function () {
            return this._setOption("disabled", !1)
        },
        disable: function () {
            return this._setOption("disabled", !0)
        },
        _on: function (i, s, a) {
            var n, o = this;
            "boolean" != typeof i && (a = s, s = i, i = !1), a ? (s = n = e(s), this.bindings = this.bindings.add(s)) : (a = s, s = this.element, n = this.widget()), e.each(a, function (a, r) {
                function h() {
                    return i || o.options.disabled !== !0 && !e(this).hasClass("ui-state-disabled") ? ("string" == typeof r ? o[r] : r).apply(o, arguments) : t
                }

                "string" != typeof r && (h.guid = r.guid = r.guid || h.guid || e.guid++);
                var l = a.match(/^(\w+)\s*(.*)$/), u = l[1] + o.eventNamespace, d = l[2];
                d ? n.delegate(d, u, h) : s.bind(u, h)
            })
        },
        _off: function (e, t) {
            t = (t || "").split(" ").join(this.eventNamespace + " ") + this.eventNamespace, e.unbind(t).undelegate(t)
        },
        _delay: function (e, t) {
            function i() {
                return ("string" == typeof e ? s[e] : e).apply(s, arguments)
            }

            var s = this;
            return setTimeout(i, t || 0)
        },
        _hoverable: function (t) {
            this.hoverable = this.hoverable.add(t), this._on(t, {
                mouseenter: function (t) {
                    e(t.currentTarget).addClass("ui-state-hover")
                }, mouseleave: function (t) {
                    e(t.currentTarget).removeClass("ui-state-hover")
                }
            })
        },
        _focusable: function (t) {
            this.focusable = this.focusable.add(t), this._on(t, {
                focusin: function (t) {
                    e(t.currentTarget).addClass("ui-state-focus")
                }, focusout: function (t) {
                    e(t.currentTarget).removeClass("ui-state-focus")
                }
            })
        },
        _trigger: function (t, i, s) {
            var a, n, o = this.options[t];
            if (s = s || {}, i = e.Event(i), i.type = (t === this.widgetEventPrefix ? t : this.widgetEventPrefix + t).toLowerCase(), i.target = this.element[0], n = i.originalEvent) for (a in n) a in i || (i[a] = n[a]);
            return this.element.trigger(i, s), !(e.isFunction(o) && o.apply(this.element[0], [i].concat(s)) === !1 || i.isDefaultPrevented())
        }
    }, e.each({show: "fadeIn", hide: "fadeOut"}, function (t, i) {
        e.Widget.prototype["_" + t] = function (s, a, n) {
            "string" == typeof a && (a = {effect: a});
            var o, r = a ? a === !0 || "number" == typeof a ? i : a.effect || i : t;
            a = a || {}, "number" == typeof a && (a = {duration: a}), o = !e.isEmptyObject(a), a.complete = n, a.delay && s.delay(a.delay), o && e.effects && (e.effects.effect[r] || e.uiBackCompat !== !1 && e.effects[r]) ? s[t](a) : r !== t && s[r] ? s[r](a.duration, a.easing, n) : s.queue(function (i) {
                e(this)[t](), n && n.call(s[0]), i()
            })
        }
    }), e.uiBackCompat !== !1 && (e.Widget.prototype._getCreateOptions = function () {
        return e.metadata && e.metadata.get(this.element[0])[this.widgetName]
    })
})(jQuery);
/*! jQuery UI - v1.9.2 - 2015-03-29
* http://jqueryui.com
* Copyright 2015 jQuery Foundation and other contributors; Licensed MIT */

(function (e) {
    var t = !1;
    e(document).mouseup(function () {
        t = !1
    }), e.widget("ui.mouse", {
        version: "1.9.2",
        options: {cancel: "input,textarea,button,select,option", distance: 1, delay: 0},
        _mouseInit: function () {
            var t = this;
            this.element.bind("mousedown." + this.widgetName, function (e) {
                return t._mouseDown(e)
            }).bind("click." + this.widgetName, function (i) {
                return !0 === e.data(i.target, t.widgetName + ".preventClickEvent") ? (e.removeData(i.target, t.widgetName + ".preventClickEvent"), i.stopImmediatePropagation(), !1) : undefined
            }), this.started = !1
        },
        _mouseDestroy: function () {
            this.element.unbind("." + this.widgetName), this._mouseMoveDelegate && e(document).unbind("mousemove." + this.widgetName, this._mouseMoveDelegate).unbind("mouseup." + this.widgetName, this._mouseUpDelegate)
        },
        _mouseDown: function (i) {
            if (!t) {
                this._mouseStarted && this._mouseUp(i), this._mouseDownEvent = i;
                var s = this, a = 1 === i.which,
                    n = "string" == typeof this.options.cancel && i.target.nodeName ? e(i.target).closest(this.options.cancel).length : !1;
                return a && !n && this._mouseCapture(i) ? (this.mouseDelayMet = !this.options.delay, this.mouseDelayMet || (this._mouseDelayTimer = setTimeout(function () {
                    s.mouseDelayMet = !0
                }, this.options.delay)), this._mouseDistanceMet(i) && this._mouseDelayMet(i) && (this._mouseStarted = this._mouseStart(i) !== !1, !this._mouseStarted) ? (i.preventDefault(), !0) : (!0 === e.data(i.target, this.widgetName + ".preventClickEvent") && e.removeData(i.target, this.widgetName + ".preventClickEvent"), this._mouseMoveDelegate = function (e) {
                    return s._mouseMove(e)
                }, this._mouseUpDelegate = function (e) {
                    return s._mouseUp(e)
                }, e(document).bind("mousemove." + this.widgetName, this._mouseMoveDelegate).bind("mouseup." + this.widgetName, this._mouseUpDelegate), i.preventDefault(), t = !0, !0)) : !0
            }
        },
        _mouseMove: function (t) {
            return !e.ui.ie || document.documentMode >= 9 || t.button ? this._mouseStarted ? (this._mouseDrag(t), t.preventDefault()) : (this._mouseDistanceMet(t) && this._mouseDelayMet(t) && (this._mouseStarted = this._mouseStart(this._mouseDownEvent, t) !== !1, this._mouseStarted ? this._mouseDrag(t) : this._mouseUp(t)), !this._mouseStarted) : this._mouseUp(t)
        },
        _mouseUp: function (t) {
            return e(document).unbind("mousemove." + this.widgetName, this._mouseMoveDelegate).unbind("mouseup." + this.widgetName, this._mouseUpDelegate), this._mouseStarted && (this._mouseStarted = !1, t.target === this._mouseDownEvent.target && e.data(t.target, this.widgetName + ".preventClickEvent", !0), this._mouseStop(t)), !1
        },
        _mouseDistanceMet: function (e) {
            return Math.max(Math.abs(this._mouseDownEvent.pageX - e.pageX), Math.abs(this._mouseDownEvent.pageY - e.pageY)) >= this.options.distance
        },
        _mouseDelayMet: function () {
            return this.mouseDelayMet
        },
        _mouseStart: function () {
        },
        _mouseDrag: function () {
        },
        _mouseStop: function () {
        },
        _mouseCapture: function () {
            return !0
        }
    })
})(jQuery);
/*! jQuery UI - v1.9.2 - 2015-03-29
* http://jqueryui.com
* Copyright 2015 jQuery Foundation and other contributors; Licensed MIT */

(function (e) {
    e.widget("ui.sortable", e.ui.mouse, {
        version: "1.9.2",
        widgetEventPrefix: "sort",
        ready: !1,
        options: {
            appendTo: "parent",
            axis: !1,
            connectWith: !1,
            containment: !1,
            cursor: "auto",
            cursorAt: !1,
            dropOnEmpty: !0,
            forcePlaceholderSize: !1,
            forceHelperSize: !1,
            grid: !1,
            handle: !1,
            helper: "original",
            items: "> *",
            opacity: !1,
            placeholder: !1,
            revert: !1,
            scroll: !0,
            scrollSensitivity: 20,
            scrollSpeed: 20,
            scope: "default",
            tolerance: "intersect",
            zIndex: 1e3
        },
        _create: function () {
            var e = this.options;
            this.containerCache = {}, this.element.addClass("ui-sortable"), this.refresh(), this.floating = this.items.length ? "x" === e.axis || /left|right/.test(this.items[0].item.css("float")) || /inline|table-cell/.test(this.items[0].item.css("display")) : !1, this.offset = this.element.offset(), this._mouseInit(), this.ready = !0
        },
        _destroy: function () {
            this.element.removeClass("ui-sortable ui-sortable-disabled"), this._mouseDestroy();
            for (var e = this.items.length - 1; e >= 0; e--) this.items[e].item.removeData(this.widgetName + "-item");
            return this
        },
        _setOption: function (t, i) {
            "disabled" === t ? (this.options[t] = i, this.widget().toggleClass("ui-sortable-disabled", !!i)) : e.Widget.prototype._setOption.apply(this, arguments)
        },
        _mouseCapture: function (t, i) {
            var s = this;
            if (this.reverting) return !1;
            if (this.options.disabled || "static" == this.options.type) return !1;
            this._refreshItems(t);
            var a = null;
            if (e(t.target).parents().each(function () {
                return e.data(this, s.widgetName + "-item") == s ? (a = e(this), !1) : undefined
            }), e.data(t.target, s.widgetName + "-item") == s && (a = e(t.target)), !a) return !1;
            if (this.options.handle && !i) {
                var n = !1;
                if (e(this.options.handle, a).find("*").andSelf().each(function () {
                    this == t.target && (n = !0)
                }), !n) return !1
            }
            return this.currentItem = a, this._removeCurrentsFromItems(), !0
        },
        _mouseStart: function (t, i, s) {
            var a = this.options;
            if (this.currentContainer = this, this.refreshPositions(), this.helper = this._createHelper(t), this._cacheHelperProportions(), this._cacheMargins(), this.scrollParent = this.helper.scrollParent(), this.offset = this.currentItem.offset(), this.offset = {
                top: this.offset.top - this.margins.top,
                left: this.offset.left - this.margins.left
            }, e.extend(this.offset, {
                click: {left: t.pageX - this.offset.left, top: t.pageY - this.offset.top},
                parent: this._getParentOffset(),
                relative: this._getRelativeOffset()
            }), this.helper.css("position", "absolute"), this.cssPosition = this.helper.css("position"), this.originalPosition = this._generatePosition(t), this.originalPageX = t.pageX, this.originalPageY = t.pageY, a.cursorAt && this._adjustOffsetFromHelper(a.cursorAt), this.domPosition = {
                prev: this.currentItem.prev()[0],
                parent: this.currentItem.parent()[0]
            }, this.helper[0] != this.currentItem[0] && this.currentItem.hide(), this._createPlaceholder(), a.containment && this._setContainment(), a.cursor && (e("body").css("cursor") && (this._storedCursor = e("body").css("cursor")), e("body").css("cursor", a.cursor)), a.opacity && (this.helper.css("opacity") && (this._storedOpacity = this.helper.css("opacity")), this.helper.css("opacity", a.opacity)), a.zIndex && (this.helper.css("zIndex") && (this._storedZIndex = this.helper.css("zIndex")), this.helper.css("zIndex", a.zIndex)), this.scrollParent[0] != document && "HTML" != this.scrollParent[0].tagName && (this.overflowOffset = this.scrollParent.offset()), this._trigger("start", t, this._uiHash()), this._preserveHelperProportions || this._cacheHelperProportions(), !s) for (var n = this.containers.length - 1; n >= 0; n--) this.containers[n]._trigger("activate", t, this._uiHash(this));
            return e.ui.ddmanager && (e.ui.ddmanager.current = this), e.ui.ddmanager && !a.dropBehaviour && e.ui.ddmanager.prepareOffsets(this, t), this.dragging = !0, this.helper.addClass("ui-sortable-helper"), this._mouseDrag(t), !0
        },
        _mouseDrag: function (t) {
            if (this.position = this._generatePosition(t), this.positionAbs = this._convertPositionTo("absolute"), this.lastPositionAbs || (this.lastPositionAbs = this.positionAbs), this.options.scroll) {
                var i = this.options, s = !1;
                this.scrollParent[0] != document && "HTML" != this.scrollParent[0].tagName ? (this.overflowOffset.top + this.scrollParent[0].offsetHeight - t.pageY < i.scrollSensitivity ? this.scrollParent[0].scrollTop = s = this.scrollParent[0].scrollTop + i.scrollSpeed : t.pageY - this.overflowOffset.top < i.scrollSensitivity && (this.scrollParent[0].scrollTop = s = this.scrollParent[0].scrollTop - i.scrollSpeed), this.overflowOffset.left + this.scrollParent[0].offsetWidth - t.pageX < i.scrollSensitivity ? this.scrollParent[0].scrollLeft = s = this.scrollParent[0].scrollLeft + i.scrollSpeed : t.pageX - this.overflowOffset.left < i.scrollSensitivity && (this.scrollParent[0].scrollLeft = s = this.scrollParent[0].scrollLeft - i.scrollSpeed)) : (t.pageY - e(document).scrollTop() < i.scrollSensitivity ? s = e(document).scrollTop(e(document).scrollTop() - i.scrollSpeed) : e(window).height() - (t.pageY - e(document).scrollTop()) < i.scrollSensitivity && (s = e(document).scrollTop(e(document).scrollTop() + i.scrollSpeed)), t.pageX - e(document).scrollLeft() < i.scrollSensitivity ? s = e(document).scrollLeft(e(document).scrollLeft() - i.scrollSpeed) : e(window).width() - (t.pageX - e(document).scrollLeft()) < i.scrollSensitivity && (s = e(document).scrollLeft(e(document).scrollLeft() + i.scrollSpeed))), s !== !1 && e.ui.ddmanager && !i.dropBehaviour && e.ui.ddmanager.prepareOffsets(this, t)
            }
            this.positionAbs = this._convertPositionTo("absolute"), this.options.axis && "y" == this.options.axis || (this.helper[0].style.left = this.position.left + "px"), this.options.axis && "x" == this.options.axis || (this.helper[0].style.top = this.position.top + "px");
            for (var a = this.items.length - 1; a >= 0; a--) {
                var n = this.items[a], r = n.item[0], o = this._intersectsWithPointer(n);
                if (o && n.instance === this.currentContainer && r != this.currentItem[0] && this.placeholder[1 == o ? "next" : "prev"]()[0] != r && !e.contains(this.placeholder[0], r) && ("semi-dynamic" == this.options.type ? !e.contains(this.element[0], r) : !0)) {
                    if (this.direction = 1 == o ? "down" : "up", "pointer" != this.options.tolerance && !this._intersectsWithSides(n)) break;
                    this._rearrange(t, n), this._trigger("change", t, this._uiHash());
                    break
                }
            }
            return this._contactContainers(t), e.ui.ddmanager && e.ui.ddmanager.drag(this, t), this._trigger("sort", t, this._uiHash()), this.lastPositionAbs = this.positionAbs, !1
        },
        _mouseStop: function (t, i) {
            if (t) {
                if (e.ui.ddmanager && !this.options.dropBehaviour && e.ui.ddmanager.drop(this, t), this.options.revert) {
                    var s = this, a = this.placeholder.offset();
                    this.reverting = !0, e(this.helper).animate({
                        left: a.left - this.offset.parent.left - this.margins.left + (this.offsetParent[0] == document.body ? 0 : this.offsetParent[0].scrollLeft),
                        top: a.top - this.offset.parent.top - this.margins.top + (this.offsetParent[0] == document.body ? 0 : this.offsetParent[0].scrollTop)
                    }, parseInt(this.options.revert, 10) || 500, function () {
                        s._clear(t)
                    })
                } else this._clear(t, i);
                return !1
            }
        },
        cancel: function () {
            if (this.dragging) {
                this._mouseUp({target: null}), "original" == this.options.helper ? this.currentItem.css(this._storedCSS).removeClass("ui-sortable-helper") : this.currentItem.show();
                for (var t = this.containers.length - 1; t >= 0; t--) this.containers[t]._trigger("deactivate", null, this._uiHash(this)), this.containers[t].containerCache.over && (this.containers[t]._trigger("out", null, this._uiHash(this)), this.containers[t].containerCache.over = 0)
            }
            return this.placeholder && (this.placeholder[0].parentNode && this.placeholder[0].parentNode.removeChild(this.placeholder[0]), "original" != this.options.helper && this.helper && this.helper[0].parentNode && this.helper.remove(), e.extend(this, {
                helper: null,
                dragging: !1,
                reverting: !1,
                _noFinalSort: null
            }), this.domPosition.prev ? e(this.domPosition.prev).after(this.currentItem) : e(this.domPosition.parent).prepend(this.currentItem)), this
        },
        serialize: function (t) {
            var i = this._getItemsAsjQuery(t && t.connected), s = [];
            return t = t || {}, e(i).each(function () {
                var i = (e(t.item || this).attr(t.attribute || "id") || "").match(t.expression || /(.+)[-=_](.+)/);
                i && s.push((t.key || i[1] + "[]") + "=" + (t.key && t.expression ? i[1] : i[2]))
            }), !s.length && t.key && s.push(t.key + "="), s.join("&")
        },
        toArray: function (t) {
            var i = this._getItemsAsjQuery(t && t.connected), s = [];
            return t = t || {}, i.each(function () {
                s.push(e(t.item || this).attr(t.attribute || "id") || "")
            }), s
        },
        _intersectsWith: function (e) {
            var t = this.positionAbs.left, i = t + this.helperProportions.width, s = this.positionAbs.top,
                a = s + this.helperProportions.height, n = e.left, r = n + e.width, o = e.top, h = o + e.height,
                l = this.offset.click.top, u = this.offset.click.left,
                d = s + l > o && h > s + l && t + u > n && r > t + u;
            return "pointer" == this.options.tolerance || this.options.forcePointerForContainers || "pointer" != this.options.tolerance && this.helperProportions[this.floating ? "width" : "height"] > e[this.floating ? "width" : "height"] ? d : t + this.helperProportions.width / 2 > n && r > i - this.helperProportions.width / 2 && s + this.helperProportions.height / 2 > o && h > a - this.helperProportions.height / 2
        },
        _intersectsWithPointer: function (t) {
            var i = "x" === this.options.axis || e.ui.isOverAxis(this.positionAbs.top + this.offset.click.top, t.top, t.height),
                s = "y" === this.options.axis || e.ui.isOverAxis(this.positionAbs.left + this.offset.click.left, t.left, t.width),
                a = i && s, n = this._getDragVerticalDirection(), r = this._getDragHorizontalDirection();
            return a ? this.floating ? r && "right" == r || "down" == n ? 2 : 1 : n && ("down" == n ? 2 : 1) : !1
        },
        _intersectsWithSides: function (t) {
            var i = e.ui.isOverAxis(this.positionAbs.top + this.offset.click.top, t.top + t.height / 2, t.height),
                s = e.ui.isOverAxis(this.positionAbs.left + this.offset.click.left, t.left + t.width / 2, t.width),
                a = this._getDragVerticalDirection(), n = this._getDragHorizontalDirection();
            return this.floating && n ? "right" == n && s || "left" == n && !s : a && ("down" == a && i || "up" == a && !i)
        },
        _getDragVerticalDirection: function () {
            var e = this.positionAbs.top - this.lastPositionAbs.top;
            return 0 != e && (e > 0 ? "down" : "up")
        },
        _getDragHorizontalDirection: function () {
            var e = this.positionAbs.left - this.lastPositionAbs.left;
            return 0 != e && (e > 0 ? "right" : "left")
        },
        refresh: function (e) {
            return this._refreshItems(e), this.refreshPositions(), this
        },
        _connectWith: function () {
            var e = this.options;
            return e.connectWith.constructor == String ? [e.connectWith] : e.connectWith
        },
        _getItemsAsjQuery: function (t) {
            var i = [], s = [], a = this._connectWith();
            if (a && t) for (var n = a.length - 1; n >= 0; n--) for (var r = e(a[n]), o = r.length - 1; o >= 0; o--) {
                var h = e.data(r[o], this.widgetName);
                h && h != this && !h.options.disabled && s.push([e.isFunction(h.options.items) ? h.options.items.call(h.element) : e(h.options.items, h.element).not(".ui-sortable-helper").not(".ui-sortable-placeholder"), h])
            }
            s.push([e.isFunction(this.options.items) ? this.options.items.call(this.element, null, {
                options: this.options,
                item: this.currentItem
            }) : e(this.options.items, this.element).not(".ui-sortable-helper").not(".ui-sortable-placeholder"), this]);
            for (var n = s.length - 1; n >= 0; n--) s[n][0].each(function () {
                i.push(this)
            });
            return e(i)
        },
        _removeCurrentsFromItems: function () {
            var t = this.currentItem.find(":data(" + this.widgetName + "-item)");
            this.items = e.grep(this.items, function (e) {
                for (var i = 0; t.length > i; i++) if (t[i] == e.item[0]) return !1;
                return !0
            })
        },
        _refreshItems: function (t) {
            this.items = [], this.containers = [this];
            var i = this.items,
                s = [[e.isFunction(this.options.items) ? this.options.items.call(this.element[0], t, {item: this.currentItem}) : e(this.options.items, this.element), this]],
                a = this._connectWith();
            if (a && this.ready) for (var n = a.length - 1; n >= 0; n--) for (var r = e(a[n]), o = r.length - 1; o >= 0; o--) {
                var h = e.data(r[o], this.widgetName);
                h && h != this && !h.options.disabled && (s.push([e.isFunction(h.options.items) ? h.options.items.call(h.element[0], t, {item: this.currentItem}) : e(h.options.items, h.element), h]), this.containers.push(h))
            }
            for (var n = s.length - 1; n >= 0; n--) for (var l = s[n][1], u = s[n][0], o = 0, d = u.length; d > o; o++) {
                var c = e(u[o]);
                c.data(this.widgetName + "-item", l), i.push({
                    item: c,
                    instance: l,
                    width: 0,
                    height: 0,
                    left: 0,
                    top: 0
                })
            }
        },
        refreshPositions: function (t) {
            this.offsetParent && this.helper && (this.offset.parent = this._getParentOffset());
            for (var i = this.items.length - 1; i >= 0; i--) {
                var s = this.items[i];
                if (s.instance == this.currentContainer || !this.currentContainer || s.item[0] == this.currentItem[0]) {
                    var a = this.options.toleranceElement ? e(this.options.toleranceElement, s.item) : s.item;
                    t || (s.width = a.outerWidth(), s.height = a.outerHeight());
                    var n = a.offset();
                    s.left = n.left, s.top = n.top
                }
            }
            if (this.options.custom && this.options.custom.refreshContainers) this.options.custom.refreshContainers.call(this); else for (var i = this.containers.length - 1; i >= 0; i--) {
                var n = this.containers[i].element.offset();
                this.containers[i].containerCache.left = n.left, this.containers[i].containerCache.top = n.top, this.containers[i].containerCache.width = this.containers[i].element.outerWidth(), this.containers[i].containerCache.height = this.containers[i].element.outerHeight()
            }
            return this
        },
        _createPlaceholder: function (t) {
            t = t || this;
            var i = t.options;
            if (!i.placeholder || i.placeholder.constructor == String) {
                var s = i.placeholder;
                i.placeholder = {
                    element: function () {
                        var i = e(document.createElement(t.currentItem[0].nodeName)).addClass(s || t.currentItem[0].className + " ui-sortable-placeholder").removeClass("ui-sortable-helper")[0];
                        return s || (i.style.visibility = "hidden"), i
                    }, update: function (e, a) {
                        (!s || i.forcePlaceholderSize) && (a.height() || a.height(t.currentItem.innerHeight() - parseInt(t.currentItem.css("paddingTop") || 0, 10) - parseInt(t.currentItem.css("paddingBottom") || 0, 10)), a.width() || a.width(t.currentItem.innerWidth() - parseInt(t.currentItem.css("paddingLeft") || 0, 10) - parseInt(t.currentItem.css("paddingRight") || 0, 10)))
                    }
                }
            }
            t.placeholder = e(i.placeholder.element.call(t.element, t.currentItem)), t.currentItem.after(t.placeholder), i.placeholder.update(t, t.placeholder)
        },
        _contactContainers: function (t) {
            for (var i = null, s = null, a = this.containers.length - 1; a >= 0; a--) if (!e.contains(this.currentItem[0], this.containers[a].element[0])) if (this._intersectsWith(this.containers[a].containerCache)) {
                if (i && e.contains(this.containers[a].element[0], i.element[0])) continue;
                i = this.containers[a], s = a
            } else this.containers[a].containerCache.over && (this.containers[a]._trigger("out", t, this._uiHash(this)), this.containers[a].containerCache.over = 0);
            if (i) if (1 === this.containers.length) this.containers[s]._trigger("over", t, this._uiHash(this)), this.containers[s].containerCache.over = 1; else {
                for (var n = 1e4, r = null, o = this.containers[s].floating ? "left" : "top", h = this.containers[s].floating ? "width" : "height", l = this.positionAbs[o] + this.offset.click[o], u = this.items.length - 1; u >= 0; u--) if (e.contains(this.containers[s].element[0], this.items[u].item[0]) && this.items[u].item[0] != this.currentItem[0]) {
                    var d = this.items[u].item.offset()[o], c = !1;
                    Math.abs(d - l) > Math.abs(d + this.items[u][h] - l) && (c = !0, d += this.items[u][h]), n > Math.abs(d - l) && (n = Math.abs(d - l), r = this.items[u], this.direction = c ? "up" : "down")
                }
                if (!r && !this.options.dropOnEmpty) return;
                this.currentContainer = this.containers[s], r ? this._rearrange(t, r, null, !0) : this._rearrange(t, null, this.containers[s].element, !0), this._trigger("change", t, this._uiHash()), this.containers[s]._trigger("change", t, this._uiHash(this)), this.options.placeholder.update(this.currentContainer, this.placeholder), this.containers[s]._trigger("over", t, this._uiHash(this)), this.containers[s].containerCache.over = 1
            }
        },
        _createHelper: function (t) {
            var i = this.options,
                s = e.isFunction(i.helper) ? e(i.helper.apply(this.element[0], [t, this.currentItem])) : "clone" == i.helper ? this.currentItem.clone() : this.currentItem;
            return s.parents("body").length || e("parent" != i.appendTo ? i.appendTo : this.currentItem[0].parentNode)[0].appendChild(s[0]), s[0] == this.currentItem[0] && (this._storedCSS = {
                width: this.currentItem[0].style.width,
                height: this.currentItem[0].style.height,
                position: this.currentItem.css("position"),
                top: this.currentItem.css("top"),
                left: this.currentItem.css("left")
            }), ("" == s[0].style.width || i.forceHelperSize) && s.width(this.currentItem.width()), ("" == s[0].style.height || i.forceHelperSize) && s.height(this.currentItem.height()), s
        },
        _adjustOffsetFromHelper: function (t) {
            "string" == typeof t && (t = t.split(" ")), e.isArray(t) && (t = {
                left: +t[0],
                top: +t[1] || 0
            }), "left" in t && (this.offset.click.left = t.left + this.margins.left), "right" in t && (this.offset.click.left = this.helperProportions.width - t.right + this.margins.left), "top" in t && (this.offset.click.top = t.top + this.margins.top), "bottom" in t && (this.offset.click.top = this.helperProportions.height - t.bottom + this.margins.top)
        },
        _getParentOffset: function () {
            this.offsetParent = this.helper.offsetParent();
            var t = this.offsetParent.offset();
            return "absolute" == this.cssPosition && this.scrollParent[0] != document && e.contains(this.scrollParent[0], this.offsetParent[0]) && (t.left += this.scrollParent.scrollLeft(), t.top += this.scrollParent.scrollTop()), (this.offsetParent[0] == document.body || this.offsetParent[0].tagName && "html" == this.offsetParent[0].tagName.toLowerCase() && e.ui.ie) && (t = {
                top: 0,
                left: 0
            }), {
                top: t.top + (parseInt(this.offsetParent.css("borderTopWidth"), 10) || 0),
                left: t.left + (parseInt(this.offsetParent.css("borderLeftWidth"), 10) || 0)
            }
        },
        _getRelativeOffset: function () {
            if ("relative" == this.cssPosition) {
                var e = this.currentItem.position();
                return {
                    top: e.top - (parseInt(this.helper.css("top"), 10) || 0) + this.scrollParent.scrollTop(),
                    left: e.left - (parseInt(this.helper.css("left"), 10) || 0) + this.scrollParent.scrollLeft()
                }
            }
            return {top: 0, left: 0}
        },
        _cacheMargins: function () {
            this.margins = {
                left: parseInt(this.currentItem.css("marginLeft"), 10) || 0,
                top: parseInt(this.currentItem.css("marginTop"), 10) || 0
            }
        },
        _cacheHelperProportions: function () {
            this.helperProportions = {width: this.helper.outerWidth(), height: this.helper.outerHeight()}
        },
        _setContainment: function () {
            var t = this.options;
            if ("parent" == t.containment && (t.containment = this.helper[0].parentNode), ("document" == t.containment || "window" == t.containment) && (this.containment = [0 - this.offset.relative.left - this.offset.parent.left, 0 - this.offset.relative.top - this.offset.parent.top, e("document" == t.containment ? document : window).width() - this.helperProportions.width - this.margins.left, (e("document" == t.containment ? document : window).height() || document.body.parentNode.scrollHeight) - this.helperProportions.height - this.margins.top]), !/^(document|window|parent)$/.test(t.containment)) {
                var i = e(t.containment)[0], s = e(t.containment).offset(), a = "hidden" != e(i).css("overflow");
                this.containment = [s.left + (parseInt(e(i).css("borderLeftWidth"), 10) || 0) + (parseInt(e(i).css("paddingLeft"), 10) || 0) - this.margins.left, s.top + (parseInt(e(i).css("borderTopWidth"), 10) || 0) + (parseInt(e(i).css("paddingTop"), 10) || 0) - this.margins.top, s.left + (a ? Math.max(i.scrollWidth, i.offsetWidth) : i.offsetWidth) - (parseInt(e(i).css("borderLeftWidth"), 10) || 0) - (parseInt(e(i).css("paddingRight"), 10) || 0) - this.helperProportions.width - this.margins.left, s.top + (a ? Math.max(i.scrollHeight, i.offsetHeight) : i.offsetHeight) - (parseInt(e(i).css("borderTopWidth"), 10) || 0) - (parseInt(e(i).css("paddingBottom"), 10) || 0) - this.helperProportions.height - this.margins.top]
            }
        },
        _convertPositionTo: function (t, i) {
            i || (i = this.position);
            var s = "absolute" == t ? 1 : -1,
                a = (this.options, "absolute" != this.cssPosition || this.scrollParent[0] != document && e.contains(this.scrollParent[0], this.offsetParent[0]) ? this.scrollParent : this.offsetParent),
                n = /(html|body)/i.test(a[0].tagName);
            return {
                top: i.top + this.offset.relative.top * s + this.offset.parent.top * s - ("fixed" == this.cssPosition ? -this.scrollParent.scrollTop() : n ? 0 : a.scrollTop()) * s,
                left: i.left + this.offset.relative.left * s + this.offset.parent.left * s - ("fixed" == this.cssPosition ? -this.scrollParent.scrollLeft() : n ? 0 : a.scrollLeft()) * s
            }
        },
        _generatePosition: function (t) {
            var i = this.options,
                s = "absolute" != this.cssPosition || this.scrollParent[0] != document && e.contains(this.scrollParent[0], this.offsetParent[0]) ? this.scrollParent : this.offsetParent,
                a = /(html|body)/i.test(s[0].tagName);
            "relative" != this.cssPosition || this.scrollParent[0] != document && this.scrollParent[0] != this.offsetParent[0] || (this.offset.relative = this._getRelativeOffset());
            var n = t.pageX, r = t.pageY;
            if (this.originalPosition && (this.containment && (t.pageX - this.offset.click.left < this.containment[0] && (n = this.containment[0] + this.offset.click.left), t.pageY - this.offset.click.top < this.containment[1] && (r = this.containment[1] + this.offset.click.top), t.pageX - this.offset.click.left > this.containment[2] && (n = this.containment[2] + this.offset.click.left), t.pageY - this.offset.click.top > this.containment[3] && (r = this.containment[3] + this.offset.click.top)), i.grid)) {
                var o = this.originalPageY + Math.round((r - this.originalPageY) / i.grid[1]) * i.grid[1];
                r = this.containment ? o - this.offset.click.top < this.containment[1] || o - this.offset.click.top > this.containment[3] ? o - this.offset.click.top < this.containment[1] ? o + i.grid[1] : o - i.grid[1] : o : o;
                var h = this.originalPageX + Math.round((n - this.originalPageX) / i.grid[0]) * i.grid[0];
                n = this.containment ? h - this.offset.click.left < this.containment[0] || h - this.offset.click.left > this.containment[2] ? h - this.offset.click.left < this.containment[0] ? h + i.grid[0] : h - i.grid[0] : h : h
            }
            return {
                top: r - this.offset.click.top - this.offset.relative.top - this.offset.parent.top + ("fixed" == this.cssPosition ? -this.scrollParent.scrollTop() : a ? 0 : s.scrollTop()),
                left: n - this.offset.click.left - this.offset.relative.left - this.offset.parent.left + ("fixed" == this.cssPosition ? -this.scrollParent.scrollLeft() : a ? 0 : s.scrollLeft())
            }
        },
        _rearrange: function (e, t, i, s) {
            i ? i[0].appendChild(this.placeholder[0]) : t.item[0].parentNode.insertBefore(this.placeholder[0], "down" == this.direction ? t.item[0] : t.item[0].nextSibling), this.counter = this.counter ? ++this.counter : 1;
            var a = this.counter;
            this._delay(function () {
                a == this.counter && this.refreshPositions(!s)
            })
        },
        _clear: function (t, i) {
            this.reverting = !1;
            var s = [];
            if (!this._noFinalSort && this.currentItem.parent().length && this.placeholder.before(this.currentItem), this._noFinalSort = null, this.helper[0] == this.currentItem[0]) {
                for (var a in this._storedCSS) ("auto" == this._storedCSS[a] || "static" == this._storedCSS[a]) && (this._storedCSS[a] = "");
                this.currentItem.css(this._storedCSS).removeClass("ui-sortable-helper")
            } else this.currentItem.show();
            this.fromOutside && !i && s.push(function (e) {
                this._trigger("receive", e, this._uiHash(this.fromOutside))
            }), !this.fromOutside && this.domPosition.prev == this.currentItem.prev().not(".ui-sortable-helper")[0] && this.domPosition.parent == this.currentItem.parent()[0] || i || s.push(function (e) {
                this._trigger("update", e, this._uiHash())
            }), this !== this.currentContainer && (i || (s.push(function (e) {
                this._trigger("remove", e, this._uiHash())
            }), s.push(function (e) {
                return function (t) {
                    e._trigger("receive", t, this._uiHash(this))
                }
            }.call(this, this.currentContainer)), s.push(function (e) {
                return function (t) {
                    e._trigger("update", t, this._uiHash(this))
                }
            }.call(this, this.currentContainer))));
            for (var a = this.containers.length - 1; a >= 0; a--) i || s.push(function (e) {
                return function (t) {
                    e._trigger("deactivate", t, this._uiHash(this))
                }
            }.call(this, this.containers[a])), this.containers[a].containerCache.over && (s.push(function (e) {
                return function (t) {
                    e._trigger("out", t, this._uiHash(this))
                }
            }.call(this, this.containers[a])), this.containers[a].containerCache.over = 0);
            if (this._storedCursor && e("body").css("cursor", this._storedCursor), this._storedOpacity && this.helper.css("opacity", this._storedOpacity), this._storedZIndex && this.helper.css("zIndex", "auto" == this._storedZIndex ? "" : this._storedZIndex), this.dragging = !1, this.cancelHelperRemoval) {
                if (!i) {
                    this._trigger("beforeStop", t, this._uiHash());
                    for (var a = 0; s.length > a; a++) s[a].call(this, t);
                    this._trigger("stop", t, this._uiHash())
                }
                return this.fromOutside = !1, !1
            }
            if (i || this._trigger("beforeStop", t, this._uiHash()), this.placeholder[0].parentNode.removeChild(this.placeholder[0]), this.helper[0] != this.currentItem[0] && this.helper.remove(), this.helper = null, !i) {
                for (var a = 0; s.length > a; a++) s[a].call(this, t);
                this._trigger("stop", t, this._uiHash())
            }
            return this.fromOutside = !1, !0
        },
        _trigger: function () {
            e.Widget.prototype._trigger.apply(this, arguments) === !1 && this.cancel()
        },
        _uiHash: function (t) {
            var i = t || this;
            return {
                helper: i.helper,
                placeholder: i.placeholder || e([]),
                position: i.position,
                originalPosition: i.originalPosition,
                offset: i.positionAbs,
                item: i.currentItem,
                sender: t ? t.element : null
            }
        }
    })
})(jQuery);
/*!
 * lhgcore Dialog Plugin v4.2.0
 * Date: 2012-04-19 10:55:11
 * http://code.google.com/p/lhgdialog/
 * Copyright 2009-2012 LiHuiGang
 */
(function (a, b, c) {
    var d = b.ActiveXObject && !b.XMLHttpRequest, e = function () {
        }, f = 0, g = /^url:/, h, i, j = b.document, k = "JDG" + +(new Date),
        l = '<table class="ui_border"><tbody><tr><td class="ui_lt"></td><td class="ui_t"></td><td class="ui_rt"></td></tr><tr><td class="ui_l"><div style="width: 13px;"></div></td><td class="ui_c"><div class="ui_inner"><table class="ui_dialog"><tbody><tr><td colspan="2"><div class="ui_title_bar"><div class="ui_title" unselectable="on"></div><div class="ui_title_buttons"><a class="ui_min" href="javascript:void(0);" title="最小化"><b class="ui_min_b"></b></a><a class="ui_max" href="javascript:void(0);" title="最大化"><b class="ui_max_b"></b></a><a class="ui_res" href="javascript:void(0);" title="还原"><b class="ui_res_b"></b><b class="ui_res_t"></b></a><a class="ui_close" href="javascript:void(0);" title="关闭(esc键)">×</a></div></div></td></tr><tr><td class="ui_icon"></td><td><div class="ui_main"><div class="ui_content"></div></div></td></tr><tr><td colspan="2"><div class="ui_buttons"></div></td></tr></tbody></table></div></td><td class="ui_r"><div style="width: 13px;"></div></td></tr><tr><td class="ui_lb"></td><td class="ui_b"></td><td class="ui_rb"></td></tr></tbody></table>',
        m, n = function (a, b, c) {
            var d = a.length;
            for (; b < d; b++) {
                c = !j.querySelector ? a[b].getAttribute("src", 4) : a[b].src;
                if (c.substr(c.lastIndexOf("/")).indexOf("lhgdialog") !== -1) break
            }
            c = c.split("?"), m = c[1];
            return c[0].substr(0, c[0].lastIndexOf("/") + 1)
        }(j.getElementsByTagName("script"), 0), o = function (a) {
            if (m) {
                var b = m.split("&"), c = 0, d = b.length, e;
                for (; c < d; c++) {
                    e = b[c].split("=");
                    if (a === e[0]) return e[1]
                }
            }
            return null
        }, p = o("skin") || "default", q, r = function (a) {
            q = a.document;
            return a
        }(b), s = q.documentElement, t = q.compatMode === "BackCompat";
    _$doc = a(q), _$top = a(r), _$html = a(q.getElementsByTagName("html")[0]);
    try {
        q.execCommand("BackgroundImageCache", !1, !0)
    } catch (u) {
    }
    (function (a) {
        if (!a) {
            var b = q.getElementsByTagName("head")[0], c = q.createElement("link");
            c.href = n + "skins/" + p + ".css", c.rel = "stylesheet", c.id = "lhgdialoglink", b.insertBefore(c, b.firstChild)
        }
    })(q.getElementById("lhgdialoglink")), d && function (a) {
        _$html.css(a) !== "fixed" && _$html.css({
            zoom: 1,
            backgroundImage: "url(about:blank)",
            backgroundAttachment: "fixed"
        })
    }("backgroundAttachment");
    var v = function (a) {
        a = a || {};
        var b, d = v.setting;
        for (var e in d) a[e] === c && (a[e] = d[e]);
        a.id = a.id || k + f, b = v.list[a.id];
        if (b) return b.zindex().focus();
        a.button = a.button || [], a.ok && a.button.push({
            id: "ok",
            name: a.okVal,
            callback: a.ok,
            focus: a.focus
        }), a.cancel && a.button.push({
            id: "cancel",
            name: a.cancelVal,
            callback: a.cancel
        }), v.setting.zIndex = a.zIndex, f++;
        return v.list[a.id] = h ? h._init(a) : new v.fn._init(a)
    };
    v.fn = v.prototype = {
        constructor: v, _init: function (a) {
            var c = this, d, e = a.content, f = g.test(e);
            c.opener = b, c.config = a, c.DOM = d = c.DOM || c._getDOM(), c.closed = !1, c.data = a.data, a.icon && !f ? (a.min = !1, a.max = !1, d.icon[0].style.display = "", d.icon[0].innerHTML = '<img src="' + a.path + "skins/icons/" + a.icon + '" class="ui_icon_bg"/>') : d.icon[0].style.display = "none", d.wrap.addClass(a.skin), d.rb[0].style.cursor = a.resize ? "se-resize" : "auto", d.title[0].style.cursor = a.drag ? "move" : "auto", d.max[0].style.display = a.max ? "inline-block" : "none", d.min[0].style.display = a.min ? "inline-block" : "none", d.close[0].style.display = a.cancel === !1 ? "none" : "inline-block", d.content[0].style.padding = a.padding, c.button.apply(c, a.button), c.title(a.title).content(e, !0, f).size(a.width, a.height).position(a.left, a.top).time(a.time)[a.show ? "show" : "hide"](!0).zindex(), a.focus && c.focus(), a.lock && c.lock(), c._ie6PngFix()._addEvent(), h = null, !f && a.init && a.init.call(c, b);
            return c
        }, button: function () {
            var b = this, c = b.DOM, d = c.buttons[0], e = "ui_state_highlight", f = b._listeners = b._listeners || {},
                g = [].slice.call(arguments), h = 0, i, j, l, m, n;
            for (; h < g.length; h++) i = g[h], j = i.name, l = i.id || j, m = !f[l], n = m ? q.createElement("input") : f[l].elem, n.type = "button", f[l] || (f[l] = {}), j && (n.value = j), i.callback && (f[l].callback = i.callback), i.focus && (b._focus && b._focus.removeClass(e), b._focus = a(n).addClass(e), b.focus()), n[k + "callback"] = l, n.disabled = !!i.disabled, m && (f[l].elem = n, d.appendChild(n));
            d.style.display = g.length ? "" : "none", b._ie6SelectFix();
            return b
        }, title: function (a) {
            if (a === c) return this;
            var b = this.DOM, d = b.border, e = b.title[0];
            a === !1 ? (e.style.display = "none", e.innerHTML = "", d.addClass("ui_state_tips")) : (e.style.display = "", e.innerHTML = a, d.removeClass("ui_state_tips"));
            return this
        }, content: function (a, b, d) {
            if (a === c) return this;
            var e = this, f = e.DOM, g = f.wrap[0], h = g.offsetWidth, i = g.offsetHeight, j = parseInt(g.style.left),
                k = parseInt(g.style.top), l = g.style.width, m = f.content, n = v.setting.content;
            d ? (m[0].innerHTML = n, e._iframe(a.split("url:")[1])) : m.html(a), b || (h = g.offsetWidth - h, i = g.offsetHeight - i, j = j - h / 2, k = k - i / 2, g.style.left = Math.max(j, 0) + "px", g.style.top = Math.max(k, 0) + "px", l && l !== "auto" && (g.style.width = g.offsetWidth + "px"), e._autoPositionType()), e._ie6SelectFix();
            return e
        }, size: function (a, b) {
            var c = this, d = c.DOM, e = d.wrap[0], f = d.main[0].style;
            e.style.width = "auto", typeof a == "number" && (a = a + "px"), typeof b == "number" && (b = b + "px"), f.width = a, f.height = b, a !== "auto" && (e.style.width = e.offsetWidth + "px"), c._ie6SelectFix();
            return c
        }, position: function (a, b) {
            var e = this, f = e.config, g = e.DOM.wrap[0], h = g.style, i = d ? !1 : f.fixed, j = d && f.fixed,
                k = _$top.scrollLeft(), l = _$top.scrollTop(), m = i ? 0 : k, n = i ? 0 : l, o = _$top.width(),
                p = _$top.height(), q = g.offsetWidth, r = g.offsetHeight;
            if (a || a === 0) e._left = a.toString().indexOf("%") !== -1 ? a : null, a = e._toNumber(a, o - q), typeof a == "number" && (a = j ? a += k : a + m, a = Math.max(a, m) + "px"), h.left = a;
            if (b || b === 0) e._top = b.toString().indexOf("%") !== -1 ? b : null, b = e._toNumber(b, p - r), typeof b == "number" && (b = j ? b += l : b + n, b = Math.max(b, n) + "px"), h.top = b;
            a !== c && b !== c && e._autoPositionType();
            return e
        }, time: function (a, b) {
            var c = this, d = c._timer;
            d && clearTimeout(d), b && b.call(c), a && (c._timer = setTimeout(function () {
                c._click("cancel")
            }, 1e3 * a));
            return c
        }, show: function (b) {
            this.DOM.wrap[0].style.visibility = "visible", this.DOM.border.addClass("ui_state_visible"), !b && this._lock && (a("#ldg_lockmask_" + this.config.id, q)[0].style.display = "");
            return this
        }, hide: function (b) {
            this.DOM.wrap[0].style.visibility = "hidden", this.DOM.border.removeClass("ui_state_visible"), !b && this._lock && (a("#ldg_lockmask_" + this.config.id, q)[0].style.display = "none");
            return this
        }, zindex: function () {
            var a = this, b = a.DOM, c = a._load, d = v.focus, e = v.setting.zIndex++;
            b.wrap[0].style.zIndex = e, d && d.DOM.border.removeClass("ui_state_focus"), v.focus = a, b.border.addClass("ui_state_focus"), c && c.style.zIndex && (c.style.display = "none"), d && d !== a && d.iframe && (d._load.style.display = "");
            return a
        }, focus: function () {
            try {
                elemFocus = this._focus && this._focus[0] || this.DOM.close[0], elemFocus && elemFocus.focus()
            } catch (a) {
            }
            return this
        }, lock: function () {
            var b = this, c, e = v.setting.zIndex - 1, f = b.config, g = a("#ldg_lockmask_" + f.id, q)[0],
                h = g ? g.style : "", i = d ? "absolute" : "fixed";
            g || (c = '<iframe src="javascript:\'\'" style="width:100%;height:100%;position:absolute;top:0;left:0;z-index:-1;filter:alpha(opacity=0)"></iframe>', g = q.createElement("div"), g.id = "ldg_lockmask_" + f.id, g.className = "ldg_lockmask", g.style.cssText = "position:" + i + ";left:0;top:0;width:100%;height:100%;overflow:hidden;", h = g.style, d && (g.innerHTML = c), q.body.appendChild(g)), i === "absolute" && (h.width = _$top.width(), h.height = _$top.height(), h.top = _$top.scrollTop(), h.left = _$top.scrollLeft(), b._setFixed(g)), h.zIndex = e, h.display = "", b.zindex(), b.DOM.border.addClass("ui_state_lock"), b._lock = !0;
            return b
        }, unlock: function () {
            var b = this, c = b.config, d = a("#ldg_lockmask_" + c.id, q)[0];
            if (d && b._lock) {
                if (c.parent && c.parent._lock) {
                    var e = c.parent.DOM.wrap[0].style.zIndex;
                    d.style.zIndex = parseInt(e, 10) - 1
                } else d.style.display = "none";
                b.DOM.border.removeClass("ui_state_lock")
            }
            b._lock = !1;
            return b
        }, close: function () {
            var c = this, d = c.DOM, e = d.wrap, f = v.list, g = c.config.close;
            c.time();
            if (c.iframe) {
                if (typeof g == "function" && g.call(c, c.iframe.contentWindow, b) === !1) return c;
                a(c.iframe).unbind("load", c._fmLoad).attr("src", "javascript:''").remove(), d.content.removeClass("ui_state_full"), c._frmTimer && clearTimeout(c._frmTimer)
            } else if (typeof g == "function" && g.call(c, b) === !1) return c;
            if (c.closed) return c;
            c.unlock(), c._maxState && (_$html.removeClass("ui_lock_scroll"), d.res[0].style.display = "none"), v.focus === c && (v.focus = null), c._removeEvent(), delete f[c.config.id];
            if (h) e.remove(); else {
                h = c, c._minState && (d.main[0].style.display = "", d.buttons[0].style.display = "", d.dialog[0].style.width = ""), d.wrap[0].style.cssText = "left:0;top:0;", d.wrap[0].className = "", d.border.removeClass("ui_state_focus"), d.title[0].innerHTML = "", d.content.html(""), d.icon[0].innerHTML = "", d.buttons[0].innerHTML = "", c.hide(!0)._setAbsolute();
                for (var i in c) c.hasOwnProperty(i) && i !== "DOM" && delete c[i]
            }
            c.closed = !0;
            return c
        }, max: function () {
            var a = this, b, c = a.DOM, e = c.wrap[0].style, f = c.main[0].style, g = c.rb[0].style,
                h = c.title[0].style, i = a.config, j = _$top.scrollTop(), k = _$top.scrollLeft();
            a._maxState ? (_$html.removeClass("ui_lock_scroll"), e.top = a._or.t, e.left = a._or.l, a.size(a._or.w, a._or.h)._autoPositionType(), i.drag = a._or.d, i.resize = a._or.r, g.cursor = a._or.rc, h.cursor = a._or.tc, c.res[0].style.display = "none", c.max[0].style.display = "inline-block", delete a._or, a._maxState = !1) : (_$html.addClass("ui_lock_scroll"), a._minState && a.min(), a._or = {
                t: e.top,
                l: e.left,
                w: f.width,
                h: f.height,
                d: i.drag,
                r: i.resize,
                rc: g.cursor,
                tc: h.cursor
            }, e.top = j + "px", e.left = k + "px", b = a._maxSize(), a.size(b.w, b.h)._setAbsolute(), d && t && (e.width = _$top.width() + "px"), i.drag = !1, i.resize = !1, g.cursor = "auto", h.cursor = "auto", c.max[0].style.display = "none", c.res[0].style.display = "inline-block", a._maxState = !0);
            return a
        }, min: function () {
            var a = this, b = a.DOM, c = b.main[0].style, d = b.buttons[0].style, e = b.dialog[0].style,
                f = b.rb[0].style.cursor, g = a.config.resize;
            a._minState ? (c.display = "", d.display = a._minRz.btn, e.width = "", g = a._minRz, f.cursor = a._minRz.rzs ? "se-resize" : "auto", delete a._minRz, a._minState = !1) : (a._maxState && a.max(), a._minRz = {
                rzs: g,
                btn: d.display
            }, c.display = "none", d.display = "none", e.width = c.width, f.cursor = "auto", g = !1, a._minState = !0), a._ie6SelectFix();
            return a
        }, get: function (a, b) {
            if (v.list[a]) return b === 1 ? v.list[a] : v.list[a].content || null;
            return null
        }, reload: function (c, d, e) {
            c = c || b;
            try {
                c.location.href = d ? d : c.location.href
            } catch (f) {
                d = this.iframe.src, a(this.iframe).attr("src", d)
            }
            e && e.call(this);
            return this
        }, _iframe: function (b) {
            var c = this, e, f, g, h, i, j, k, l = c.DOM.content, m = c.config, n = c._load = a(".ui_loading", l[0])[0],
                o = "position:absolute;left:-9999em;border:none 0;background:transparent",
                p = "width:100%;height:100%;border:none 0;";
            if (m.cache === !1) {
                var s = (new Date).getTime(), t = b.replace(/([?&])_=[^&]*/, "$1_=" + s);
                b = t + (t === b ? (/\?/.test(b) ? "&" : "?") + "_=" + s : "")
            }
            e = c.iframe = q.createElement("iframe"), e.name = m.id, e.style.cssText = o, e.setAttribute("frameborder", 0, 0), f = a(e), l[0].appendChild(e), c._frmTimer = setTimeout(function () {
                f.attr("src", b)
            }, 1);
            var u = c._fmLoad = function () {
                l.addClass("ui_state_full");
                var b = c.DOM, f, o = b.lt[0].offsetHeight, q = b.main[0].style;
                n.style.cssText = "display:none;position:absolute;background:#FFF;opacity:0;filter:alpha(opacity=0);z-index:1;width:" + q.width + ";height:" + q.height + ";";
                try {
                    g = c.content = e.contentWindow, h = a(g.document), i = a(g.document.body)
                } catch (s) {
                    e.style.cssText = p;
                    return
                }
                j = m.width === "auto" ? h.width() + (d ? 0 : parseInt(i.css("marginLeft"))) : m.width, k = m.height === "auto" ? h.height() : m.height, setTimeout(function () {
                    e.style.cssText = p
                }, 0), c._maxState || c.size(j, k).position(m.left, m.top), n.style.width = q.width, n.style.height = q.height, m.init && m.init.call(c, g, r)
            };
            c.iframe.api = c, f.bind("load", u)
        }, _getDOM: function () {
            var b = q.createElement("div"), c = q.body;
            b.style.cssText = "position:absolute;left:0;top:0;visibility:hidden;", b.innerHTML = l;
            var d, e = 0, f = {wrap: a(b)}, g = b.getElementsByTagName("*"), h = g.length;
            for (; e < h; e++) d = g[e].className.split("ui_")[1], d && (f[d] = a(g[e]));
            c.insertBefore(b, c.firstChild);
            return f
        }, _toNumber: function (a, b) {
            if (typeof a == "number") return a;
            a.indexOf("%") !== -1 && (a = parseInt(b * a.split("%")[0] / 100));
            return a
        }, _maxSize: function () {
            var a = this, b = a.DOM, c = b.wrap[0], d = b.main[0], e, f;
            e = _$top.width() - c.offsetWidth + d.offsetWidth, f = _$top.height() - c.offsetHeight + d.offsetHeight;
            return {w: e, h: f}
        }, _ie6PngFix: function () {
            if (d) {
                var a = 0, b, c, e, f, g = v.setting.path + "/skins/", h = this.DOM.wrap[0].getElementsByTagName("*");
                for (; a < h.length; a++) b = h[a], c = b.currentStyle.png, c && (e = g + c, f = b.runtimeStyle, f.backgroundImage = "none", f.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + e + "',sizingMethod='scale')")
            }
            return this
        }, _ie6SelectFix: d ? function () {
            var a = this.DOM.wrap, b = a[0], c = c + "iframeMask", d = a[c], e = b.offsetWidth, f = b.offsetHeight;
            e = e + "px", f = f + "px", d ? (d.style.width = e, d.style.height = f) : (d = b.appendChild(q.createElement("iframe")), a[c] = d, d.src = "javascript:''", d.style.cssText = "position:absolute;z-index:-1;left:0;top:0;filter:alpha(opacity=0);width:" + e + ";height:" + f)
        } : e, _autoPositionType: function () {
            this[this.config.fixed ? "_setFixed" : "_setAbsolute"]()
        }, _setFixed: function (a) {
            var b = a ? a.style : this.DOM.wrap[0].style;
            if (d) {
                var c = _$top.scrollLeft(), e = _$top.scrollTop(), f = parseInt(b.left) - c, g = parseInt(b.top) - e,
                    h = t ? "this.ownerDocument.body" : "this.ownerDocument.documentElement";
                this._setAbsolute(), b.setExpression("left", h + ".scrollLeft +" + f), b.setExpression("top", h + ".scrollTop +" + g)
            } else b.position = "fixed"
        }, _setAbsolute: function () {
            var a = this.DOM.wrap[0].style;
            d && (a.removeExpression("left"), a.removeExpression("top")), a.position = "absolute"
        }, _click: function (a) {
            var c = this, d = c._listeners[a] && c._listeners[a].callback;
            return typeof d != "function" || d.call(c, b) !== !1 ? c.close() : c
        }, _reset: function () {
            var c = !!b.ActiveXObject, e, f = this, g = _$top.width(), h = _$top.height(), i = f._winSize || g * h,
                j = f._lockDocW || g, k = f._left, l = f._top;
            if (c) {
                f._lock && d && a("#ldg_lockmask_" + config.id, q).css({
                    width: g + "px",
                    height: h + 17 + "px"
                }), newWidth = f._lockDocW = g, e = f._winSize = g * h;
                if (i === e) return
            }
            if (f._maxState) {
                var m = f._maxSize();
                f.size(m.w, m.h)
            }
            (!c || Math.abs(j - newWidth) !== 17) && (k || l) && f.position(k, l)
        }, _addEvent: function () {
            var a, b = this, c = b.config, d = b.DOM;
            b._winResize = function () {
                a && clearTimeout(a), a = setTimeout(function () {
                    b._reset()
                }, 140)
            }, _$top.bind("resize", b._winResize), d.wrap.bind("click", function (a) {
                var c = a.target, e;
                if (c.disabled) return !1;
                if (c === d.close[0]) {
                    b._click("cancel");
                    return !1
                }
                if (c === d.max[0] || c === d.res[0] || c === d.max_b[0] || c === d.res_b[0] || c === d.res_t[0]) {
                    b.max();
                    return !1
                }
                if (c === d.min[0] || c === d.min_b[0]) {
                    b.min();
                    return !1
                }
                e = c[k + "callback"], e && b._click(e)
            }).bind("mousedown", function (a) {
                b.zindex();
                var e = a.target;
                if (c.drag !== !1 && e === d.title[0] || c.resize !== !1 && e === d.rb[0]) {
                    w(a);
                    return !1
                }
            }), c.max && d.title.bind("dblclick", function () {
                b.max();
                return !1
            })
        }, _removeEvent: function () {
            var a = this, b = a.DOM;
            b.wrap.unbind(), b.title.unbind(), _$top.unbind("resize", a._winResize)
        }
    }, v.fn._init.prototype = v.fn, v.focus = null, v.list = {}, i = function (a) {
        var b = a.target, c = v.focus, d = a.keyCode;
        !!c && !!c.config.esc && c.config.cancel !== !1 && d === 27 && c._click(c.config.cancelVal)
    }, _$doc.bind("keydown", i), r != b && a(b).bind("unload", function () {
        var b = v.list;
        for (var c in b) b[c] && b[c].close();
        h && h.DOM.wrap.remove(), _$doc.unbind("keydown", i), a(".ldg_lockmask", q) && a(".ldg_lockmask", q).remove(), a("#ldg_dragmask", q) && a("#ldg_dragmask", q).remove()
    }), v.setting = {
        content: '<div class="ui_loading"><span>loading...</span></div>',
        title: "视窗 ",
        button: null,
        ok: null,
        cancel: null,
        init: null,
        close: null,
        okVal: "确定",
        cancelVal: "取消",
        skin: "",
        esc: !0,
        show: !0,
        width: "auto",
        height: "auto",
        icon: null,
        path: n,
        lock: !1,
        focus: !0,
        parent: null,
        padding: "10px",
        fixed: !1,
        left: "50%",
        top: "38.2%",
        max: !0,
        min: !0,
        zIndex: 1976,
        resize: !0,
        drag: !0,
        cache: !0,
        data: null,
        extendDrag: !1
    };
    var w, x = "setCapture" in s, y = "onlosecapture" in s;
    v.dragEvent = {
        onstart: e, start: function (a) {
            var b = v.dragEvent;
            _$doc.bind("mousemove", b.move).bind("mouseup", b.end), b._sClientX = a.clientX, b._sClientY = a.clientY, b.onstart(a.clientX, a.clientY);
            return !1
        }, onmove: e, move: function (a) {
            var b = v.dragEvent;
            b.onmove(a.clientX - b._sClientX, a.clientY - b._sClientY);
            return !1
        }, onend: e, end: function (a) {
            var b = v.dragEvent;
            _$doc.unbind("mousemove", b.move).unbind("mouseup", b.end), b.onend(a.clientX, a.clientY);
            return !1
        }
    }, w = function (b) {
        var c, e, f, g, h, i, j = v.focus, k = j.config, l = j.DOM, m = l.wrap[0], n = l.title, o = l.main[0],
            p = v.dragEvent, s = "getSelection" in r ? function () {
                r.getSelection().removeAllRanges()
            } : function () {
                try {
                    q.selection.empty()
                } catch (a) {
                }
            };
        p.onstart = function (a, b) {
            i ? (e = o.offsetWidth, f = o.offsetHeight) : (g = m.offsetLeft, h = m.offsetTop), _$doc.bind("dblclick", p.end), !d && y ? n.bind("losecapture", p.end) : _$top.bind("blur", p.end), x && n[0].setCapture(), l.border.addClass("ui_state_drag"), j.focus()
        }, p.onmove = function (b, d) {
            if (i) {
                var l = m.style, n = o.style, p = b + e, q = d + f;
                l.width = "auto", k.width = n.width = Math.max(0, p) + "px", l.width = m.offsetWidth + "px", k.height = n.height = Math.max(0, q) + "px", j._load && a(j._load).css({
                    width: n.width,
                    height: n.height
                })
            } else {
                var n = m.style, r = b + g, t = d + h;
                k.left = Math.max(c.minX, Math.min(c.maxX, r)), k.top = Math.max(c.minY, Math.min(c.maxY, t)), n.left = k.left + "px", n.top = k.top + "px"
            }
            s()
        }, p.onend = function (a, b) {
            _$doc.unbind("dblclick", p.end), !d && y ? n.unbind("losecapture", p.end) : _$top.unbind("blur", p.end), x && n[0].releaseCapture(), d && j._autoPositionType(), l.border.removeClass("ui_state_drag")
        }, i = b.target === l.rb[0] ? !0 : !1, c = function (a) {
            var b = m.offsetWidth, c = n[0].offsetHeight || 20, d = _$top.width(), e = _$top.height(),
                f = a ? 0 : _$top.scrollLeft(), g = a ? 0 : _$top.scrollTop();
            maxX = d - b + f, maxY = e - c + g;
            return {minX: f, minY: g, maxX: maxX, maxY: maxY}
        }(m.style.position === "fixed"), p.start(b)
    }, a(function () {
        setTimeout(function () {
            f || v({left: "-9999em", time: 9, fixed: !1, lock: !1, focus: !1})
        }, 150), v.setting.extendDrag && function (a) {
            var b = q.createElement("div"), c = b.style, e = d ? "absolute" : "fixed";
            b.id = "ldg_dragmask", c.cssText = "display:none;position:" + e + ";left:0;top:0;width:100%;height:100%;" + "cursor:move;filter:alpha(opacity=0);opacity:0;background:#FFF;pointer-events:none;", q.body.appendChild(b), a._start = a.start, a._end = a.end, a.start = function () {
                var b = v.focus, d = b.DOM.main[0], f = b.iframe;
                a._start.apply(this, arguments), c.display = "block", c.zIndex = v.setting.zIndex + 3, e === "absolute" && (c.width = _$top.width() + "px", c.height = _$top.height() + "px", c.left = _$doc.scrollLeft() + "px", c.top = _$doc.scrollTop() + "px"), f && d.offsetWidth * d.offsetHeight > 307200 && (d.style.visibility = "hidden")
            }, a.end = function () {
                var b = v.focus;
                a._end.apply(this, arguments), c.display = "none", b && (b.DOM.main[0].style.visibility = "visible")
            }
        }(v.dragEvent)
    }), a.fn.dialog = function () {
        var a = arguments;
        this.bind("click", function () {
            v.apply(this, a);
            return !1
        });
        return this
    }, b.lhgdialog = a.dialog = v
})(this.jQuery || this.lhgcore, this), function (a, b, c) {
    var d = function () {
        return b.setting.zIndex
    };
    b.alert = function (a, c, e) {
        return b({
            title: "",
            id: "Alert",
            zIndex: d(),
            icon: "alert.gif",
            fixed: !0,
            lock: !0,
            content: a,
            ok: !0,
            resize: !1,
            close: c,
            parent: e || null
        })
    }, b.confirm = function (a, c, e, f) {
        return b({
            title: "",
            id: "Confirm",
            zIndex: d(),
            icon: "confirm.gif",
            fixed: !0,
            lock: !0,
            content: a,
            resize: !1,
            parent: f || null,
            ok: function (a) {
                return c.call(this, a)
            },
            cancel: function (a) {
                return e && e.call(this, a)
            }
        })
    }, b.prompt = function (a, c, e, f) {
        e = e || "";
        var g;
        return b({
            title: "",
            id: "Prompt",
            zIndex: d(),
            icon: "prompt.gif",
            fixed: !0,
            lock: !0,
            parent: f || null,
            content: ['<div style="margin-bottom:5px;font-size:12px">', a, "</div>", "<div>", '<input value="', e, '" style="width:18em;padding:6px 4px" />', "</div>"].join(""),
            init: function () {
                g = this.DOM.content[0].getElementsByTagName("input")[0], g.select(), g.focus()
            },
            ok: function (a) {
                return c && c.call(this, g.value, a)
            },
            cancel: !0
        })
    }, b.tips = function (a, c, e, f) {
        var g = e ? function () {
            this.DOM.icon[0].innerHTML = '<img src="' + this.config.path + "skins/icons/" + e + '" class="ui_icon_bg"/>', this.DOM.icon[0].style.display = "", f && (this.config.close = f)
        } : function () {
            this.DOM.icon[0].style.display = "none", f && (this.config.close = f)
        };
        return b({
            id: "Tips",
            zIndex: d(),
            title: !1,
            cancel: !1,
            fixed: !0,
            lock: !1,
            resize: !1
        }).content(a).time(c || 1.5, g)
    }
}(this.jQuery || this.lhgcore, this.lhgdialog);
/*
SWFObject v2.2 <http://code.google.com/p/swfobject/>
is released under the MIT License <http://www.opensource.org/licenses/mit-license.php>
*/
var swfobject = function () {
    function V(b) {
        var c = /[\\\"<>\.;]/, d = c.exec(b) != null;
        return d && typeof encodeURIComponent != a ? encodeURIComponent(b) : b
    }

    function U(a, b) {
        if (!!x) {
            var c = b ? "visible" : "hidden";
            t && P(a) ? P(a).style.visibility = c : T("#" + a, "visibility:" + c)
        }
    }

    function T(c, d, e, f) {
        if (!y.ie || !y.mac) {
            var g = i.getElementsByTagName("head")[0];
            if (!g) return;
            var h = e && typeof e == "string" ? e : "screen";
            f && (v = null, w = null);
            if (!v || w != h) {
                var j = Q("style");
                j.setAttribute("type", "text/css"), j.setAttribute("media", h), v = g.appendChild(j), y.ie && y.win && typeof i.styleSheets != a && i.styleSheets.length > 0 && (v = i.styleSheets[i.styleSheets.length - 1]), w = h
            }
            y.ie && y.win ? v && typeof v.addRule == b && v.addRule(c, d) : v && typeof i.createTextNode != a && v.appendChild(i.createTextNode(c + " {" + d + "}"))
        }
    }

    function S(a) {
        var b = y.pv, c = a.split(".");
        c[0] = parseInt(c[0], 10), c[1] = parseInt(c[1], 10) || 0, c[2] = parseInt(c[2], 10) || 0;
        return b[0] > c[0] || b[0] == c[0] && b[1] > c[1] || b[0] == c[0] && b[1] == c[1] && b[2] >= c[2] ? !0 : !1
    }

    function R(a, b, c) {
        a.attachEvent(b, c), o[o.length] = [a, b, c]
    }

    function Q(a) {
        return i.createElement(a)
    }

    function P(a) {
        var b = null;
        try {
            b = i.getElementById(a)
        } catch (c) {
        }
        return b
    }

    function O(a) {
        var b = P(a);
        if (b) {
            for (var c in b) typeof b[c] == "function" && (b[c] = null);
            b.parentNode.removeChild(b)
        }
    }

    function N(a) {
        var b = P(a);
        b && b.nodeName == "OBJECT" && (y.ie && y.win ? (b.style.display = "none", function () {
            b.readyState == 4 ? O(a) : setTimeout(arguments.callee, 10)
        }()) : b.parentNode.removeChild(b))
    }

    function M(a, b, c) {
        var d = Q("param");
        d.setAttribute("name", b), d.setAttribute("value", c), a.appendChild(d)
    }

    function L(c, d, f) {
        var g, h = P(f);
        if (y.wk && y.wk < 312) return g;
        if (h) {
            typeof c.id == a && (c.id = f);
            if (y.ie && y.win) {
                var i = "";
                for (var j in c) c[j] != Object.prototype[j] && (j.toLowerCase() == "data" ? d.movie = c[j] : j.toLowerCase() == "styleclass" ? i += ' class="' + c[j] + '"' : j.toLowerCase() != "classid" && (i += " " + j + '="' + c[j] + '"'));
                var k = "";
                for (var l in d) d[l] != Object.prototype[l] && (k += '<param name="' + l + '" value="' + d[l] + '" />');
                h.outerHTML = '<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"' + i + ">" + k + "</object>", n[n.length] = c.id, g = P(c.id)
            } else {
                var m = Q(b);
                m.setAttribute("type", e);
                for (var o in c) c[o] != Object.prototype[o] && (o.toLowerCase() == "styleclass" ? m.setAttribute("class", c[o]) : o.toLowerCase() != "classid" && m.setAttribute(o, c[o]));
                for (var p in d) d[p] != Object.prototype[p] && p.toLowerCase() != "movie" && M(m, p, d[p]);
                h.parentNode.replaceChild(m, h), g = m
            }
        }
        return g
    }

    function K(a) {
        var c = Q("div");
        if (y.win && y.ie) c.innerHTML = a.innerHTML; else {
            var d = a.getElementsByTagName(b)[0];
            if (d) {
                var e = d.childNodes;
                if (e) {
                    var f = e.length;
                    for (var g = 0; g < f; g++) (e[g].nodeType != 1 || e[g].nodeName != "PARAM") && e[g].nodeType != 8 && c.appendChild(e[g].cloneNode(!0))
                }
            }
        }
        return c
    }

    function J(a) {
        if (y.ie && y.win && a.readyState != 4) {
            var b = Q("div");
            a.parentNode.insertBefore(b, a), b.parentNode.replaceChild(K(a), b), a.style.display = "none", function () {
                a.readyState == 4 ? a.parentNode.removeChild(a) : setTimeout(arguments.callee, 10)
            }()
        } else a.parentNode.replaceChild(K(a), a)
    }

    function I(b, c, d, e) {
        u = !0, r = e || null, s = {success: !1, id: d};
        var g = P(d);
        if (g) {
            g.nodeName == "OBJECT" ? (p = K(g), q = null) : (p = g, q = d), b.id = f;
            if (typeof b.width == a || !/%$/.test(b.width) && parseInt(b.width, 10) < 310) b.width = "310";
            if (typeof b.height == a || !/%$/.test(b.height) && parseInt(b.height, 10) < 137) b.height = "137";
            i.title = i.title.slice(0, 47) + " - Flash Player Installation";
            var j = y.ie && y.win ? "ActiveX" : "PlugIn",
                k = "MMredirectURL=" + h.location.toString().replace(/&/g, "%26") + "&MMplayerType=" + j + "&MMdoctitle=" + i.title;
            typeof c.flashvars != a ? c.flashvars += "&" + k : c.flashvars = k;
            if (y.ie && y.win && g.readyState != 4) {
                var l = Q("div");
                d += "SWFObjectNew", l.setAttribute("id", d), g.parentNode.insertBefore(l, g), g.style.display = "none", function () {
                    g.readyState == 4 ? g.parentNode.removeChild(g) : setTimeout(arguments.callee, 10)
                }()
            }
            L(b, c, d)
        }
    }

    function H() {
        return !u && S("6.0.65") && (y.win || y.mac) && !(y.wk && y.wk < 312)
    }

    function G(c) {
        var d = null, e = P(c);
        if (e && e.nodeName == "OBJECT") if (typeof e.SetVariable != a) d = e; else {
            var f = e.getElementsByTagName(b)[0];
            f && (d = f)
        }
        return d
    }

    function F() {
        var b = m.length;
        if (b > 0) for (var c = 0; c < b; c++) {
            var d = m[c].id, e = m[c].callbackFn, f = {success: !1, id: d};
            if (y.pv[0] > 0) {
                var g = P(d);
                if (g) if (S(m[c].swfVersion) && !(y.wk && y.wk < 312)) U(d, !0), e && (f.success = !0, f.ref = G(d), e(f)); else if (m[c].expressInstall && H()) {
                    var h = {};
                    h.data = m[c].expressInstall, h.width = g.getAttribute("width") || "0", h.height = g.getAttribute("height") || "0", g.getAttribute("class") && (h.styleclass = g.getAttribute("class")), g.getAttribute("align") && (h.align = g.getAttribute("align"));
                    var i = {}, j = g.getElementsByTagName("param"), k = j.length;
                    for (var l = 0; l < k; l++) j[l].getAttribute("name").toLowerCase() != "movie" && (i[j[l].getAttribute("name")] = j[l].getAttribute("value"));
                    I(h, i, d, e)
                } else J(g), e && e(f)
            } else {
                U(d, !0);
                if (e) {
                    var n = G(d);
                    n && typeof n.SetVariable != a && (f.success = !0, f.ref = n), e(f)
                }
            }
        }
    }

    function E() {
        var c = i.getElementsByTagName("body")[0], d = Q(b);
        d.setAttribute("type", e);
        var f = c.appendChild(d);
        if (f) {
            var g = 0;
            (function () {
                if (typeof f.GetVariable != a) {
                    var b = f.GetVariable("$version");
                    b && (b = b.split(" ")[1].split(","), y.pv = [parseInt(b[0], 10), parseInt(b[1], 10), parseInt(b[2], 10)])
                } else if (g < 10) {
                    g++, setTimeout(arguments.callee, 10);
                    return
                }
                c.removeChild(d), f = null, F()
            })()
        } else F()
    }

    function D() {
        k ? E() : F()
    }

    function C(b) {
        if (typeof h.addEventListener != a) h.addEventListener("load", b, !1); else if (typeof i.addEventListener != a) i.addEventListener("load", b, !1); else if (typeof h.attachEvent != a) R(h, "onload", b); else if (typeof h.onload == "function") {
            var c = h.onload;
            h.onload = function () {
                c(), b()
            }
        } else h.onload = b
    }

    function B(a) {
        t ? a() : l[l.length] = a
    }

    function A() {
        if (!t) {
            try {
                var a = i.getElementsByTagName("body")[0].appendChild(Q("span"));
                a.parentNode.removeChild(a)
            } catch (b) {
                return
            }
            t = !0;
            var c = l.length;
            for (var d = 0; d < c; d++) l[d]()
        }
    }

    var a = "undefined", b = "object", c = "Shockwave Flash", d = "ShockwaveFlash.ShockwaveFlash",
        e = "application/x-shockwave-flash", f = "SWFObjectExprInst", g = "onreadystatechange", h = window,
        i = document, j = navigator, k = !1, l = [D], m = [], n = [], o = [], p, q, r, s, t = !1, u = !1, v, w, x = !0,
        y = function () {
            var f = typeof i.getElementById != a && typeof i.getElementsByTagName != a && typeof i.createElement != a,
                g = j.userAgent.toLowerCase(), l = j.platform.toLowerCase(), m = l ? /win/.test(l) : /win/.test(g),
                n = l ? /mac/.test(l) : /mac/.test(g),
                o = /webkit/.test(g) ? parseFloat(g.replace(/^.*webkit\/(\d+(\.\d+)?).*$/, "$1")) : !1, p = !1,
                q = [0, 0, 0], r = null;
            if (typeof j.plugins != a && typeof j.plugins[c] == b) r = j.plugins[c].description, r && (typeof j.mimeTypes == a || !j.mimeTypes[e] || !!j.mimeTypes[e].enabledPlugin) && (k = !0, p = !1, r = r.replace(/^.*\s+(\S+\s+\S+$)/, "$1"), q[0] = parseInt(r.replace(/^(.*)\..*$/, "$1"), 10), q[1] = parseInt(r.replace(/^.*\.(.*)\s.*$/, "$1"), 10), q[2] = /[a-zA-Z]/.test(r) ? parseInt(r.replace(/^.*[a-zA-Z]+(.*)$/, "$1"), 10) : 0); else if (typeof h.ActiveXObject != a) try {
                var s = new ActiveXObject(d);
                s && (r = s.GetVariable("$version"), r && (p = !0, r = r.split(" ")[1].split(","), q = [parseInt(r[0], 10), parseInt(r[1], 10), parseInt(r[2], 10)]))
            } catch (t) {
            }
            return {w3: f, pv: q, wk: o, ie: p, win: m, mac: n}
        }(), z = function () {
            !y.w3 || ((typeof i.readyState != a && i.readyState == "complete" || typeof i.readyState == a && (i.getElementsByTagName("body")[0] || i.body)) && A(), t || (typeof i.addEventListener != a && i.addEventListener("DOMContentLoaded", A, !1), y.ie && y.win && (i.attachEvent(g, function () {
                i.readyState == "complete" && (i.detachEvent(g, arguments.callee), A())
            }), h == top && function () {
                if (!t) {
                    try {
                        i.documentElement.doScroll("left")
                    } catch (a) {
                        setTimeout(arguments.callee, 0);
                        return
                    }
                    A()
                }
            }()), y.wk && function () {
                if (!t) {
                    if (!/loaded|complete/.test(i.readyState)) {
                        setTimeout(arguments.callee, 0);
                        return
                    }
                    A()
                }
            }(), C(A)))
        }(), W = function () {
            y.ie && y.win && window.attachEvent("onunload", function () {
                var a = o.length;
                for (var b = 0; b < a; b++) o[b][0].detachEvent(o[b][1], o[b][2]);
                var c = n.length;
                for (var d = 0; d < c; d++) N(n[d]);
                for (var e in y) y[e] = null;
                y = null;
                for (var f in swfobject) swfobject[f] = null;
                swfobject = null
            })
        }();
    return {
        registerObject: function (a, b, c, d) {
            if (y.w3 && a && b) {
                var e = {};
                e.id = a, e.swfVersion = b, e.expressInstall = c, e.callbackFn = d, m[m.length] = e, U(a, !1)
            } else d && d({success: !1, id: a})
        }, getObjectById: function (a) {
            if (y.w3) return G(a)
        }, embedSWF: function (c, d, e, f, g, h, i, j, k, l) {
            var m = {success: !1, id: d};
            y.w3 && !(y.wk && y.wk < 312) && c && d && e && f && g ? (U(d, !1), B(function () {
                e += "", f += "";
                var n = {};
                if (k && typeof k === b) for (var o in k) n[o] = k[o];
                n.data = c, n.width = e, n.height = f;
                var p = {};
                if (j && typeof j === b) for (var q in j) p[q] = j[q];
                if (i && typeof i === b) for (var r in i) typeof p.flashvars != a ? p.flashvars += "&" + r + "=" + i[r] : p.flashvars = r + "=" + i[r];
                if (S(g)) {
                    var s = L(n, p, d);
                    n.id == d && U(d, !0), m.success = !0, m.ref = s
                } else {
                    if (h && H()) {
                        n.data = h, I(n, p, d, l);
                        return
                    }
                    U(d, !0)
                }
                l && l(m)
            })) : l && l(m)
        }, switchOffAutoHideShow: function () {
            x = !1
        }, ua: y, getFlashPlayerVersion: function () {
            return {major: y.pv[0], minor: y.pv[1], release: y.pv[2]}
        }, hasFlashPlayerVersion: S, createSWF: function (a, b, c) {
            return y.w3 ? L(a, b, c) : undefined
        }, showExpressInstall: function (a, b, c, d) {
            y.w3 && H() && I(a, b, c, d)
        }, removeSWF: function (a) {
            y.w3 && N(a)
        }, createCSS: function (a, b, c, d) {
            y.w3 && T(a, b, c, d)
        }, addDomLoadEvent: B, addLoadEvent: C, getQueryParamValue: function (a) {
            var b = i.location.search || i.location.hash;
            if (b) {
                /\?/.test(b) && (b = b.split("?")[1]);
                if (a == null) return V(b);
                var c = b.split("&");
                for (var d = 0; d < c.length; d++) if (c[d].substring(0, c[d].indexOf("=")) == a) return V(c[d].substring(c[d].indexOf("=") + 1))
            }
            return ""
        }, expressInstallCallback: function () {
            if (u) {
                var a = P(f);
                a && p && (a.parentNode.replaceChild(p, a), q && (U(q, !0), y.ie && y.win && (p.style.display = "block")), r && r(s)), u = !1
            }
        }
    }
}(), SWFUpload;
SWFUpload == undefined && (SWFUpload = function (a) {
    this.initSWFUpload(a)
}), SWFUpload.prototype.initSWFUpload = function (a) {
    try {
        this.customSettings = {}, this.settings = a, this.eventQueue = [], this.movieName = "SWFUpload_" + SWFUpload.movieCount++, this.movieElement = null, SWFUpload.instances[this.movieName] = this, this.initSettings(), this.loadFlash(), this.displayDebugInfo()
    } catch (b) {
        delete SWFUpload.instances[this.movieName];
        throw b
    }
}, SWFUpload.instances = {}, SWFUpload.movieCount = 0, SWFUpload.version = "2.2.0 2009-03-25", SWFUpload.QUEUE_ERROR = {
    QUEUE_LIMIT_EXCEEDED: -100,
    FILE_EXCEEDS_SIZE_LIMIT: -110,
    ZERO_BYTE_FILE: -120,
    INVALID_FILETYPE: -130
}, SWFUpload.UPLOAD_ERROR = {
    HTTP_ERROR: -200,
    MISSING_UPLOAD_URL: -210,
    IO_ERROR: -220,
    SECURITY_ERROR: -230,
    UPLOAD_LIMIT_EXCEEDED: -240,
    UPLOAD_FAILED: -250,
    SPECIFIED_FILE_ID_NOT_FOUND: -260,
    FILE_VALIDATION_FAILED: -270,
    FILE_CANCELLED: -280,
    UPLOAD_STOPPED: -290
}, SWFUpload.FILE_STATUS = {
    QUEUED: -1,
    IN_PROGRESS: -2,
    ERROR: -3,
    COMPLETE: -4,
    CANCELLED: -5
}, SWFUpload.BUTTON_ACTION = {SELECT_FILE: -100, SELECT_FILES: -110, START_UPLOAD: -120}, SWFUpload.CURSOR = {
    ARROW: -1,
    HAND: -2
}, SWFUpload.WINDOW_MODE = {
    WINDOW: "window",
    TRANSPARENT: "transparent",
    OPAQUE: "opaque"
}, SWFUpload.completeURL = function (a) {
    if (!a) return "";
    if (typeof a != "string" || a.match(/^https?:\/\//i) || a.match(/^\//)) return a;
    var b = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ":" + window.location.port : ""),
        c = window.location.pathname.lastIndexOf("/");
    c <= 0 ? path = "/" : path = window.location.pathname.substr(0, c) + "/";
    return path + a
}, SWFUpload.prototype.initSettings = function () {
    this.ensureDefault = function (a, b) {
        this.settings[a] = this.settings[a] == undefined ? b : this.settings[a]
    }, this.ensureDefault("upload_url", ""), this.ensureDefault("preserve_relative_urls", !1), this.ensureDefault("file_post_name", "Filedata"), this.ensureDefault("post_params", {}), this.ensureDefault("use_query_string", !1), this.ensureDefault("requeue_on_error", !1), this.ensureDefault("http_success", []), this.ensureDefault("assume_success_timeout", 0), this.ensureDefault("file_types", "*.*"), this.ensureDefault("file_types_description", "All Files"), this.ensureDefault("file_size_limit", 0), this.ensureDefault("file_upload_limit", 0), this.ensureDefault("file_queue_limit", 0), this.ensureDefault("flash_url", "swfupload.swf"), this.ensureDefault("prevent_swf_caching", !0), this.ensureDefault("button_image_url", ""), this.ensureDefault("button_width", 1), this.ensureDefault("button_height", 1), this.ensureDefault("button_text", ""), this.ensureDefault("button_text_style", "color: #000000; font-size: 16pt;"), this.ensureDefault("button_text_top_padding", 0), this.ensureDefault("button_text_left_padding", 0), this.ensureDefault("button_action", SWFUpload.BUTTON_ACTION.SELECT_FILES), this.ensureDefault("button_disabled", !1), this.ensureDefault("button_placeholder_id", ""), this.ensureDefault("button_placeholder", null), this.ensureDefault("button_cursor", SWFUpload.CURSOR.ARROW), this.ensureDefault("button_window_mode", SWFUpload.WINDOW_MODE.WINDOW), this.ensureDefault("debug", !1), this.settings.debug_enabled = this.settings.debug, this.settings.return_upload_start_handler = this.returnUploadStart, this.ensureDefault("swfupload_loaded_handler", null), this.ensureDefault("file_dialog_start_handler", null), this.ensureDefault("file_queued_handler", null), this.ensureDefault("file_queue_error_handler", null), this.ensureDefault("file_dialog_complete_handler", null), this.ensureDefault("upload_start_handler", null), this.ensureDefault("upload_progress_handler", null), this.ensureDefault("upload_error_handler", null), this.ensureDefault("upload_success_handler", null), this.ensureDefault("upload_complete_handler", null), this.ensureDefault("debug_handler", this.debugMessage), this.ensureDefault("custom_settings", {}), this.customSettings = this.settings.custom_settings, !this.settings.prevent_swf_caching || (this.settings.flash_url = this.settings.flash_url + (this.settings.flash_url.indexOf("?") < 0 ? "?" : "&") + "preventswfcaching=" + (new Date).getTime()), this.settings.preserve_relative_urls || (this.settings.upload_url = SWFUpload.completeURL(this.settings.upload_url), this.settings.button_image_url = SWFUpload.completeURL(this.settings.button_image_url)), delete this.ensureDefault
}, SWFUpload.prototype.loadFlash = function () {
    var a, b;
    if (document.getElementById(this.movieName) !== null) throw"ID " + this.movieName + " is already in use. The Flash Object could not be added";
    a = document.getElementById(this.settings.button_placeholder_id) || this.settings.button_placeholder;
    if (a == undefined) throw"Could not find the placeholder element: " + this.settings.button_placeholder_id;
    b = document.createElement("div"), b.innerHTML = this.getFlashHTML(), a.parentNode.replaceChild(b.firstChild, a), window[this.movieName] == undefined && (window[this.movieName] = this.getMovieElement())
}, SWFUpload.prototype._getFlashHTML = function () {
    return ['<object id="', this.movieName, '" type="application/x-shockwave-flash" data="', this.settings.flash_url, '" width="', this.settings.button_width, '" height="', this.settings.button_height, '" class="swfupload">', '<param name="wmode" value="', this.settings.button_window_mode, '" />', '<param name="movie" value="', this.settings.flash_url, '" />', '<param name="quality" value="high" />', '<param name="menu" value="false" />', '<param name="allowScriptAccess" value="always" />', '<param name="flashvars" value="' + this.getFlashVars() + '" />', "</object>"].join("")
}, SWFUpload.prototype.getFlashHTML = function () {
    return $.browser.msie ? ['<object classid="clsid:D27CDB6E-AE6D-11CF-96B8-444553540000" id="', this.movieName, '" type="application/x-shockwave-flash" data="', this.settings.flash_url, '" width="', this.settings.button_width, '" height="', this.settings.button_height, '" class="swfupload">', '<param name="wmode" value="', this.settings.button_window_mode, '" />', '<param name="movie" value="', this.settings.flash_url, '" />', '<param name="quality" value="high" />', '<param name="menu" value="false" />', '<param name="allowScriptAccess" value="always" />', '<param name="flashvars" value="' + this.getFlashVars() + '" />', "</object>"].join("") : this._getFlashHTML()
}, SWFUpload.prototype.getFlashVars = function () {
    var a = this.buildParamString(), b = this.settings.http_success.join(",");
    return ["movieName=", encodeURIComponent(this.movieName), "&amp;uploadURL=", encodeURIComponent(this.settings.upload_url), "&amp;useQueryString=", encodeURIComponent(this.settings.use_query_string), "&amp;requeueOnError=", encodeURIComponent(this.settings.requeue_on_error), "&amp;httpSuccess=", encodeURIComponent(b), "&amp;assumeSuccessTimeout=", encodeURIComponent(this.settings.assume_success_timeout), "&amp;params=", encodeURIComponent(a), "&amp;filePostName=", encodeURIComponent(this.settings.file_post_name), "&amp;fileTypes=", encodeURIComponent(this.settings.file_types), "&amp;fileTypesDescription=", encodeURIComponent(this.settings.file_types_description), "&amp;fileSizeLimit=", encodeURIComponent(this.settings.file_size_limit), "&amp;fileUploadLimit=", encodeURIComponent(this.settings.file_upload_limit), "&amp;fileQueueLimit=", encodeURIComponent(this.settings.file_queue_limit), "&amp;debugEnabled=", encodeURIComponent(this.settings.debug_enabled), "&amp;buttonImageURL=", encodeURIComponent(this.settings.button_image_url), "&amp;buttonWidth=", encodeURIComponent(this.settings.button_width), "&amp;buttonHeight=", encodeURIComponent(this.settings.button_height), "&amp;buttonText=", encodeURIComponent(this.settings.button_text), "&amp;buttonTextTopPadding=", encodeURIComponent(this.settings.button_text_top_padding), "&amp;buttonTextLeftPadding=", encodeURIComponent(this.settings.button_text_left_padding), "&amp;buttonTextStyle=", encodeURIComponent(this.settings.button_text_style), "&amp;buttonAction=", encodeURIComponent(this.settings.button_action), "&amp;buttonDisabled=", encodeURIComponent(this.settings.button_disabled), "&amp;buttonCursor=", encodeURIComponent(this.settings.button_cursor)].join("")
}, SWFUpload.prototype.getMovieElement = function () {
    this.movieElement == undefined && (this.movieElement = document.getElementById(this.movieName));
    if (this.movieElement === null) throw"Could not find Flash element";
    return this.movieElement
}, SWFUpload.prototype.buildParamString = function () {
    var a = this.settings.post_params, b = [];
    if (typeof a == "object") for (var c in a) a.hasOwnProperty(c) && b.push(encodeURIComponent(c.toString()) + "=" + encodeURIComponent(a[c].toString()));
    return b.join("&amp;")
}, SWFUpload.prototype.destroy = function () {
    try {
        this.cancelUpload(null, !1);
        var a = null;
        a = this.getMovieElement();
        if (a && typeof a.CallFunction == "unknown") {
            for (var b in a) try {
                typeof a[b] == "function" && (a[b] = null)
            } catch (c) {
            }
            try {
                a.parentNode.removeChild(a)
            } catch (d) {
            }
        }
        window[this.movieName] = null, SWFUpload.instances[this.movieName] = null, delete SWFUpload.instances[this.movieName], this.movieElement = null, this.settings = null, this.customSettings = null, this.eventQueue = null, this.movieName = null;
        return !0
    } catch (e) {
        return !1
    }
}, SWFUpload.prototype.displayDebugInfo = function () {
    this.debug(["---SWFUpload Instance Info---\n", "Version: ", SWFUpload.version, "\n", "Movie Name: ", this.movieName, "\n", "Settings:\n", "\t", "upload_url:               ", this.settings.upload_url, "\n", "\t", "flash_url:                ", this.settings.flash_url, "\n", "\t", "use_query_string:         ", this.settings.use_query_string.toString(), "\n", "\t", "requeue_on_error:         ", this.settings.requeue_on_error.toString(), "\n", "\t", "http_success:             ", this.settings.http_success.join(", "), "\n", "\t", "assume_success_timeout:   ", this.settings.assume_success_timeout, "\n", "\t", "file_post_name:           ", this.settings.file_post_name, "\n", "\t", "post_params:              ", this.settings.post_params.toString(), "\n", "\t", "file_types:               ", this.settings.file_types, "\n", "\t", "file_types_description:   ", this.settings.file_types_description, "\n", "\t", "file_size_limit:          ", this.settings.file_size_limit, "\n", "\t", "file_upload_limit:        ", this.settings.file_upload_limit, "\n", "\t", "file_queue_limit:         ", this.settings.file_queue_limit, "\n", "\t", "debug:                    ", this.settings.debug.toString(), "\n", "\t", "prevent_swf_caching:      ", this.settings.prevent_swf_caching.toString(), "\n", "\t", "button_placeholder_id:    ", this.settings.button_placeholder_id.toString(), "\n", "\t", "button_placeholder:       ", this.settings.button_placeholder ? "Set" : "Not Set", "\n", "\t", "button_image_url:         ", this.settings.button_image_url.toString(), "\n", "\t", "button_width:             ", this.settings.button_width.toString(), "\n", "\t", "button_height:            ", this.settings.button_height.toString(), "\n", "\t", "button_text:              ", this.settings.button_text.toString(), "\n", "\t", "button_text_style:        ", this.settings.button_text_style.toString
    (), "\n", "\t", "button_text_top_padding:  ", this.settings.button_text_top_padding.toString(), "\n", "\t", "button_text_left_padding: ", this.settings.button_text_left_padding.toString(), "\n", "\t", "button_action:            ", this.settings.button_action.toString(), "\n", "\t", "button_disabled:          ", this.settings.button_disabled.toString(), "\n", "\t", "custom_settings:          ", this.settings.custom_settings.toString(), "\n", "Event Handlers:\n", "\t", "swfupload_loaded_handler assigned:  ", (typeof this.settings.swfupload_loaded_handler == "function").toString(), "\n", "\t", "file_dialog_start_handler assigned: ", (typeof this.settings.file_dialog_start_handler == "function").toString(), "\n", "\t", "file_queued_handler assigned:       ", (typeof this.settings.file_queued_handler == "function").toString(), "\n", "\t", "file_queue_error_handler assigned:  ", (typeof this.settings.file_queue_error_handler == "function").toString(), "\n", "\t", "upload_start_handler assigned:      ", (typeof this.settings.upload_start_handler == "function").toString(), "\n", "\t", "upload_progress_handler assigned:   ", (typeof this.settings.upload_progress_handler == "function").toString(), "\n", "\t", "upload_error_handler assigned:      ", (typeof this.settings.upload_error_handler == "function").toString(), "\n", "\t", "upload_success_handler assigned:    ", (typeof this.settings.upload_success_handler == "function").toString(), "\n", "\t", "upload_complete_handler assigned:   ", (typeof this.settings.upload_complete_handler == "function").toString(), "\n", "\t", "debug_handler assigned:             ", (typeof this.settings.debug_handler == "function").toString(), "\n"].join(""))
}, SWFUpload.prototype.addSetting = function (a, b, c) {
    return b == undefined ? this.settings[a] = c : this.settings[a] = b
}, SWFUpload.prototype.getSetting = function (a) {
    if (this.settings[a] != undefined) return this.settings[a];
    return ""
}, SWFUpload.prototype.callFlash = function (functionName, argumentArray) {
    argumentArray = argumentArray || [];
    var movieElement = this.getMovieElement(), returnValue, returnString;
    try {
        returnString = movieElement.CallFunction('<invoke name="' + functionName + '" returntype="javascript">' + __flash__argumentsToXML(argumentArray, 0) + "</invoke>"), returnValue = eval(returnString)
    } catch (ex) {
        throw"Call to " + functionName + " failed"
    }
    returnValue != undefined && typeof returnValue.post == "object" && (returnValue = this.unescapeFilePostParams(returnValue));
    return returnValue
}, SWFUpload.prototype.selectFile = function () {
    this.callFlash("SelectFile")
}, SWFUpload.prototype.selectFiles = function () {
    this.callFlash("SelectFiles")
}, SWFUpload.prototype.startUpload = function (a) {
    this.callFlash("StartUpload", [a])
}, SWFUpload.prototype.cancelUpload = function (a, b) {
    b !== !1 && (b = !0), this.callFlash("CancelUpload", [a, b])
}, SWFUpload.prototype.stopUpload = function () {
    this.callFlash("StopUpload")
}, SWFUpload.prototype.getStats = function () {
    return this.callFlash("GetStats")
}, SWFUpload.prototype.setStats = function (a) {
    this.callFlash("SetStats", [a])
}, SWFUpload.prototype.getFile = function (a) {
    return typeof a == "number" ? this.callFlash("GetFileByIndex", [a]) : this.callFlash("GetFile", [a])
}, SWFUpload.prototype.addFileParam = function (a, b, c) {
    return this.callFlash("AddFileParam", [a, b, c])
}, SWFUpload.prototype.removeFileParam = function (a, b) {
    this.callFlash("RemoveFileParam", [a, b])
}, SWFUpload.prototype.setUploadURL = function (a) {
    this.settings.upload_url = a.toString(), this.callFlash("SetUploadURL", [a])
}, SWFUpload.prototype.setPostParams = function (a) {
    this.settings.post_params = a, this.callFlash("SetPostParams", [a])
}, SWFUpload.prototype.addPostParam = function (a, b) {
    this.settings.post_params[a] = b, this.callFlash("SetPostParams", [this.settings.post_params])
}, SWFUpload.prototype.removePostParam = function (a) {
    delete this.settings.post_params[a], this.callFlash("SetPostParams", [this.settings.post_params])
}, SWFUpload.prototype.setFileTypes = function (a, b) {
    this.settings.file_types = a, this.settings.file_types_description = b, this.callFlash("SetFileTypes", [a, b])
}, SWFUpload.prototype.setFileSizeLimit = function (a) {
    this.settings.file_size_limit = a, this.callFlash("SetFileSizeLimit", [a])
}, SWFUpload.prototype.setFileUploadLimit = function (a) {
    this.settings.file_upload_limit = a, this.callFlash("SetFileUploadLimit", [a])
}, SWFUpload.prototype.setFileQueueLimit = function (a) {
    this.settings.file_queue_limit = a, this.callFlash("SetFileQueueLimit", [a])
}, SWFUpload.prototype.setFilePostName = function (a) {
    this.settings.file_post_name = a, this.callFlash("SetFilePostName", [a])
}, SWFUpload.prototype.setUseQueryString = function (a) {
    this.settings.use_query_string = a, this.callFlash("SetUseQueryString", [a])
}, SWFUpload.prototype.setRequeueOnError = function (a) {
    this.settings.requeue_on_error = a, this.callFlash("SetRequeueOnError", [a])
}, SWFUpload.prototype.setHTTPSuccess = function (a) {
    typeof a == "string" && (a = a.replace(" ", "").split(",")), this.settings.http_success = a, this.callFlash("SetHTTPSuccess", [a])
}, SWFUpload.prototype.setAssumeSuccessTimeout = function (a) {
    this.settings.assume_success_timeout = a, this.callFlash("SetAssumeSuccessTimeout", [a])
}, SWFUpload.prototype.setDebugEnabled = function (a) {
    this.settings.debug_enabled = a, this.callFlash("SetDebugEnabled", [a])
}, SWFUpload.prototype.setButtonImageURL = function (a) {
    a == undefined && (a = ""), this.settings.button_image_url = a, this.callFlash("SetButtonImageURL", [a])
}, SWFUpload.prototype.setButtonDimensions = function (a, b) {
    this.settings.button_width = a, this.settings.button_height = b;
    var c = this.getMovieElement();
    c != undefined && (c.style.width = a + "px", c.style.height = b + "px"), this.callFlash("SetButtonDimensions", [a, b])
}, SWFUpload.prototype.setButtonText = function (a) {
    this.settings.button_text = a, this.callFlash("SetButtonText", [a])
}, SWFUpload.prototype.setButtonTextPadding = function (a, b) {
    this.settings.button_text_top_padding = b, this.settings.button_text_left_padding = a, this.callFlash("SetButtonTextPadding", [a, b])
}, SWFUpload.prototype.setButtonTextStyle = function (a) {
    this.settings.button_text_style = a, this.callFlash("SetButtonTextStyle", [a])
}, SWFUpload.prototype.setButtonDisabled = function (a) {
    this.settings.button_disabled = a, this.callFlash("SetButtonDisabled", [a])
}, SWFUpload.prototype.setButtonAction = function (a) {
    this.settings.button_action = a, this.callFlash("SetButtonAction", [a])
}, SWFUpload.prototype.setButtonCursor = function (a) {
    this.settings.button_cursor = a, this.callFlash("SetButtonCursor", [a])
}, SWFUpload.prototype.queueEvent = function (a, b) {
    b == undefined ? b = [] : b instanceof Array || (b = [b]);
    var c = this;
    if (typeof this.settings[a] == "function") this.eventQueue.push(function () {
        this.settings[a].apply(this, b)
    }), setTimeout(function () {
        c.executeNextEvent()
    }, 0); else if (this.settings[a] !== null) throw"Event handler " + a + " is unknown or is not a function"
}, SWFUpload.prototype.executeNextEvent = function () {
    var a = this.eventQueue ? this.eventQueue.shift() : null;
    typeof a == "function" && a.apply(this)
}, SWFUpload.prototype.unescapeFilePostParams = function (a) {
    var b = /[$]([0-9a-f]{4})/i, c = {}, d;
    if (a != undefined) {
        for (var e in a.post) if (a.post.hasOwnProperty(e)) {
            d = e;
            var f;
            while ((f = b.exec(d)) !== null) d = d.replace(f[0], String.fromCharCode(parseInt("0x" + f[1], 16)));
            c[d] = a.post[e]
        }
        a.post = c
    }
    return a
}, SWFUpload.prototype.testExternalInterface = function () {
    try {
        return this.callFlash("TestExternalInterface")
    } catch (a) {
        return !1
    }
}, SWFUpload.prototype.flashReady = function () {
    var a = this.getMovieElement();
    a ? (this.cleanUp(a), this.queueEvent("swfupload_loaded_handler")) : this.debug("Flash called back ready but the flash movie can't be found.")
}, SWFUpload.prototype.cleanUp = function (a) {
    try {
        if (this.movieElement && typeof a.CallFunction == "unknown") {
            this.debug("Removing Flash functions hooks (this should only run in IE and should prevent memory leaks)");
            for (var b in a) try {
                typeof a[b] == "function" && (a[b] = null)
            } catch (c) {
            }
        }
    } catch (d) {
    }
    window.__flash__removeCallback = function (a, b) {
        try {
            a && (a[b] = null)
        } catch (c) {
        }
    }
}, SWFUpload.prototype.fileDialogStart = function () {
    this.queueEvent("file_dialog_start_handler")
}, SWFUpload.prototype.fileQueued = function (a) {
    a = this.unescapeFilePostParams(a), this.queueEvent("file_queued_handler", a)
}, SWFUpload.prototype.fileQueueError = function (a, b, c) {
    a = this.unescapeFilePostParams(a), this.queueEvent("file_queue_error_handler", [a, b, c])
}, SWFUpload.prototype.fileDialogComplete = function (a, b, c) {
    this.queueEvent("file_dialog_complete_handler", [a, b, c])
}, SWFUpload.prototype.uploadStart = function (a) {
    a = this.unescapeFilePostParams(a), this.queueEvent("return_upload_start_handler", a)
}, SWFUpload.prototype.returnUploadStart = function (a) {
    var b;
    if (typeof this.settings.upload_start_handler == "function") a = this.unescapeFilePostParams(a), b = this.settings.upload_start_handler.call(this, a); else if (this.settings.upload_start_handler != undefined) throw"upload_start_handler must be a function";
    b === undefined && (b = !0), b = !!b, this.callFlash("ReturnUploadStart", [b])
}, SWFUpload.prototype.uploadProgress = function (a, b, c) {
    a = this.unescapeFilePostParams(a), this.queueEvent("upload_progress_handler", [a, b, c])
}, SWFUpload.prototype.uploadError = function (a, b, c) {
    a = this.unescapeFilePostParams(a), this.queueEvent("upload_error_handler", [a, b, c])
}, SWFUpload.prototype.uploadSuccess = function (a, b, c) {
    a = this.unescapeFilePostParams(a), this.queueEvent("upload_success_handler", [a, b, c])
}, SWFUpload.prototype.uploadComplete = function (a) {
    a = this.unescapeFilePostParams(a), this.queueEvent("upload_complete_handler", a)
}, SWFUpload.prototype.debug = function (a) {
    this.queueEvent("debug_handler", a)
}, SWFUpload.prototype.debugMessage = function (a) {
    if (this.settings.debug) {
        var b, c = [];
        if (typeof a == "object" && typeof a.name == "string" && typeof a.message == "string") {
            for (var d in a) a.hasOwnProperty(d) && c.push(d + ": " + a[d]);
            b = c.join("\n") || "", c = b.split("\n"), b = "EXCEPTION: " + c.join("\nEXCEPTION: "), SWFUpload.Console.writeLine(b)
        } else SWFUpload.Console.writeLine(a)
    }
}, SWFUpload.Console = {}, SWFUpload.Console.writeLine = function (a) {
    var b, c;
    try {
        b = document.getElementById("SWFUpload_Console"), b || (c = document.createElement("form"), document.getElementsByTagName("body")[0].appendChild(c), b = document.createElement("textarea"), b.id = "SWFUpload_Console", b.style.fontFamily = "monospace", b.setAttribute("wrap", "off"), b.wrap = "off", b.style.overflow = "auto", b.style.width = "700px", b.style.height = "350px", b.style.margin = "5px", c.appendChild(b)), b.value += a + "\n", b.scrollTop = b.scrollHeight - b.clientHeight
    } catch (d) {
        alert("发生异常：" + d.name + " 异常信息：" + d.message)
    }
}, function (a) {
    var b = {
        init: function (b, d) {
            return this.each(function () {
                var e = a(this), f = e.clone(), g = a.extend({
                    id: e.attr("id"),
                    swf: "uploadify.swf",
                    uploader: "uploadify.php",
                    auto: !0,
                    buttonClass: "",
                    buttonCursor: "hand",
                    buttonImage: null,
                    buttonText: "SELECT FILES",
                    checkExisting: !1,
                    debug: !1,
                    fileObjName: "Filedata",
                    fileSizeLimit: 0,
                    fileTypeDesc: "All Files",
                    fileTypeExts: "*.*",
                    height: 30,
                    itemTemplate: !1,
                    method: "post",
                    multi: !0,
                    formData: {},
                    preventCaching: !0,
                    progressData: "percentage",
                    queueID: !1,
                    queueSizeLimit: 999,
                    removeCompleted: !0,
                    removeTimeout: 3,
                    requeueErrors: !1,
                    successTimeout: 30,
                    uploadLimit: 0,
                    width: 120,
                    overrideEvents: []
                }, b), h = {
                    assume_success_timeout: g.successTimeout,
                    button_placeholder_id: g.id,
                    button_width: g.width,
                    button_height: g.height,
                    button_image_url: g.imageUrl,
                    button_text: null,
                    button_text_style: null,
                    button_text_top_padding: 0,
                    button_text_left_padding: 0,
                    button_action: g.multi ? SWFUpload.BUTTON_ACTION.SELECT_FILES : SWFUpload.BUTTON_ACTION.SELECT_FILE,
                    button_disabled: !1,
                    button_cursor: g.buttonCursor == "arrow" ? SWFUpload.CURSOR.ARROW : SWFUpload.CURSOR.HAND,
                    button_window_mode: SWFUpload.WINDOW_MODE.TRANSPARENT,
                    debug: g.debug,
                    requeue_on_error: g.requeueErrors,
                    file_post_name: g.fileObjName,
                    file_size_limit: g.fileSizeLimit,
                    file_types: g.fileTypeExts,
                    file_types_description: g.fileTypeDesc,
                    file_queue_limit: g.queueSizeLimit,
                    file_upload_limit: g.uploadLimit,
                    flash_url: g.swf,
                    prevent_swf_caching: g.preventCaching,
                    post_params: g.formData,
                    upload_url: g.uploader,
                    use_query_string: g.method == "get",
                    file_dialog_complete_handler: c.onDialogClose,
                    file_dialog_start_handler: c.onDialogOpen,
                    file_queued_handler: c.onSelect,
                    file_queue_error_handler: c.onSelectError,
                    swfupload_loaded_handler: g.onSWFReady,
                    upload_complete_handler: c.onUploadComplete,
                    upload_error_handler: c.onUploadError,
                    upload_progress_handler: c.onUploadProgress,
                    upload_start_handler: c.onUploadStart,
                    upload_success_handler: c.onUploadSuccess
                };
                d && (h = a.extend(h, d)), h = a.extend(h, g);
                var i = swfobject.getFlashPlayerVersion(), j = i.major >= 9;
                if (j) {
                    window["uploadify_" + g.id] = new SWFUpload(h);
                    var k = window["uploadify_" + g.id];
                    e.data("uploadify", k);
                    var l = a("<div />", {
                        id: g.id,
                        "class": "uploadify",
                        css: {height: g.height + "px", width: g.width + "px"}
                    });
                    a("#" + k.movieName).wrap(l), l = a("#" + g.id), l.data("uploadify", k), l.on("mousedown", function (b) {
                        a(document).triggerHandler("click")
                    });
                    var m = a("<div />", {id: g.id + "-button", "class": "uploadify-button " + g.buttonClass});
                    g.buttonImage && m.css({
                        "background-image": "url('" + g.buttonImage + "')",
                        "text-indent": "-9999px"
                    }), m.html('<span class="uploadify-button-text">' + g.buttonText + "</span>").css({
                        height: g.height + "px",
                        "line-height": g.height + "px",
                        width: g.width + "px"
                    }), l.append(m), a("#" + k.movieName).css({position: "absolute", "z-index": 1, left: 0});
                    if (!g.queueID) {
                        var n = a("<div />", {id: g.id + "-queue", "class": "uploadify-queue"});
                        l.after(n), k.settings.queueID = g.id + "-queue", k.settings.defaultQueue = !0
                    }
                    k.queueData = {
                        files: {},
                        filesSelected: 0,
                        filesQueued: 0,
                        filesReplaced: 0,
                        filesCancelled: 0,
                        filesErrored: 0,
                        uploadsSuccessful: 0,
                        uploadsErrored: 0,
                        averageSpeed: 0,
                        queueLength: 0,
                        queueSize: 0,
                        uploadSize: 0,
                        queueBytesUploaded: 0,
                        uploadQueue: [],
                        errorMsg: "部分文件无法加入上传队列:"
                    }, k.original = f, k.wrapper = l, k.button = m, k.queue = n, g.onInit && g.onInit.call(e, k)
                } else g.onFallback && g.onFallback.call(e)
            })
        }, cancel: function (b, c) {
            var d = arguments;
            this.each(function () {
                var b = a(this), c = b.data("uploadify"), e = c.settings, f = -1;
                if (d[0]) if (d[0] == "*") {
                    var g = c.queueData.queueLength;
                    a("#" + e.queueID).find(".uploadify-queue-item").each(function () {
                        f++, d[1] === !0 ? c.cancelUpload(a(this).attr("id"), !1) : c.cancelUpload(a(this).attr("id")), a(this).find(".data").removeClass("data").html(" - Cancelled"), a(this).find(".uploadify-progress-bar").remove(), a(this).delay(1e3 + 100 * f).fadeOut(500, function () {
                            a(this).remove()
                        })
                    }), c.queueData.queueSize = 0, c.queueData.queueLength = 0, e.onClearQueue && e.onClearQueue.call(b, g)
                } else for (var h = 0; h < d.length; h++) c.cancelUpload(d[h]), a("#" + d[h]).find(".data").removeClass("data").html(" - Cancelled"), a("#" + d[h]).find(".uploadify-progress-bar").remove(), a("#" + d[h]).delay(1e3 + 100 * h).fadeOut(500, function () {
                    a(this).remove()
                }); else {
                    var i = a("#" + e.queueID).find(".uploadify-queue-item").get(0);
                    $item = a(i), c.cancelUpload($item.attr("id")), $item.find(".data").removeClass("data").html(" - Cancelled"), $item.find(".uploadify-progress-bar").remove(), $item.delay(1e3).fadeOut(500, function () {
                        a(this).remove()
                    })
                }
            })
        }, destroy: function () {
            this.each(function () {
                var b = a(this), c = b.data("uploadify"), d = c.settings;
                c.destroy(), d.defaultQueue && a("#" + d.queueID).remove(), a("#" + d.id).replaceWith(c.original), d.onDestroy && d.onDestroy.call(this), delete c
            })
        }, disable: function (b) {
            this.each(function () {
                var c = a(this), d = c.data("uploadify"), e = d.settings;
                b ? (d.button.addClass("disabled"), e.onDisable && e.onDisable.call(this)) : (d.button.removeClass("disabled"), e.onEnable && e.onEnable.call(this)), d.setButtonDisabled(b)
            })
        }, settings: function (b, c, d) {
            var e = arguments, f = c;
            this.each(function () {
                var g = a(this), h = g.data("uploadify"), i = h.settings;
                if (typeof e[0] == "object") for (var j in c) setData(j, c[j]);
                if (e.length === 1) f = i[b]; else {
                    switch (b) {
                        case"uploader":
                            h.setUploadURL(c);
                            break;
                        case"formData":
                            d || (c = a.extend(i.formData, c)), h.setPostParams(i.formData);
                            break;
                        case"method":
                            c == "get" ? h.setUseQueryString(!0) : h.setUseQueryString(!1);
                            break;
                        case"fileObjName":
                            h.setFilePostName(c);
                            break;
                        case"fileTypeExts":
                            h.setFileTypes(c, i.fileTypeDesc);
                            break;
                        case"fileTypeDesc":
                            h.setFileTypes(i.fileTypeExts, c);
                            break;
                        case"fileSizeLimit":
                            h.setFileSizeLimit(c);
                            break;
                        case"uploadLimit":
                            h.setFileUploadLimit(c);
                            break;
                        case"queueSizeLimit":
                            h.setFileQueueLimit(c);
                            break;
                        case"buttonImage":
                            h.button.css("background-image", settingValue);
                            break;
                        case"buttonCursor":
                            c == "arrow" ? h.setButtonCursor(SWFUpload.CURSOR.ARROW) : h.setButtonCursor(SWFUpload.CURSOR.HAND);
                            break;
                        case"buttonText":
                            a("#" + i.id + "-button").find(".uploadify-button-text").html(c);
                            break;
                        case"width":
                            h.setButtonDimensions(c, i.height);
                            break;
                        case"height":
                            h.setButtonDimensions(i.width, c);
                            break;
                        case"multi":
                            c ? h.setButtonAction(SWFUpload.BUTTON_ACTION.SELECT_FILES) : h.setButtonAction(SWFUpload.BUTTON_ACTION.SELECT_FILE)
                    }
                    i[b] = c
                }
            });
            if (e.length === 1) return f
        }, stop: function () {
            this.each(function () {
                var b = a(this), c = b.data("uploadify");
                c.queueData.averageSpeed = 0, c.queueData.uploadSize = 0, c.queueData.bytesUploaded = 0, c.queueData.uploadQueue = [], c.stopUpload()
            })
        }, upload: function () {
            var b = arguments;
            this.each(function () {
                var c = a(this), d = c.data("uploadify");
                d.queueData.averageSpeed = 0, d.queueData.uploadSize = 0, d.queueData.bytesUploaded = 0, d.queueData.uploadQueue = [];
                if (b[0]) if (b[0] == "*") d.queueData.uploadSize = d.queueData.queueSize, d.queueData.uploadQueue.push("*"), d.startUpload(); else {
                    for (var e = 0; e < b.length; e++) d.queueData.uploadSize += d.queueData.files[b[e]].size, d.queueData.uploadQueue.push(b[e]);
                    d.startUpload(d.queueData.uploadQueue.shift())
                } else d.startUpload()
            })
        }
    }, c = {
        onDialogOpen: function () {
            var a = this.settings;
            this.queueData.errorMsg = "部分文件无法加入上传队列:", this.queueData.filesReplaced = 0, this.queueData.filesCancelled = 0, a.onDialogOpen && a.onDialogOpen.call(this)
        }, onDialogClose: function (b, c, d) {
            var e = this.settings;
            this.queueData.filesErrored = b - c, this.queueData.filesSelected = b, this.queueData.filesQueued = c - this.queueData.filesCancelled, this.queueData.queueLength = d, a.inArray("onDialogClose", e.overrideEvents) < 0 && this.queueData.filesErrored > 0 && alert(this.queueData.errorMsg), e.onDialogClose && e.onDialogClose.call(this, this.queueData), e.auto && a("#" + e.id).uploadify("upload", "*")
        }, onSelect: function (b) {
            var c = this.settings, d = {};
            for (var e in this.queueData.files) {
                d = this.queueData.files[e];
                if (d.uploaded != !0 && d.name == b.name) {
                    var f = confirm('文件名 "' + b.name + '" 已经在上传队列中，您是否要替换该文件？');
                    if (!f) {
                        this.cancelUpload(b.id), this.queueData.filesCancelled++;
                        return !1
                    }
                    a("#" + d.id).remove(), this.cancelUpload(d.id), this.queueData.filesReplaced++
                }
            }
            var g = Math.round(b.size / 1024), h = "KB";
            g > 1e3 && (g = Math.round(g / 1e3), h = "MB");
            var i = g.toString().split(".");
            g = i[0], i.length > 1 && (g += "." + i[1].substr(0, 2)), g += h;
            var j = b.name;
            j.length > 25 && (j = j.substr(0, 25) + "..."), itemData = {
                fileID: b.id,
                instanceID: c.id,
                fileName: j,
                fileSize: g
            }, c.itemTemplate == !1 && (c.itemTemplate = '<div id="${fileID}" class="uploadify-queue-item">\n\t\t\t\t\t<div class="cancel">\n\t\t\t\t\t\t<a href="javascript:$(\'#${instanceID}\').uploadify(\'cancel\', \'${fileID}\')">X</a>\n\t\t\t\t\t</div>\n\t\t\t\t\t<span class="fileName">${fileName} (${fileSize})</span><span class="data"></span>\n\t\t\t\t\t<div class="uploadify-progress">\n\t\t\t\t\t\t<div class="uploadify-progress-bar"><!--Progress Bar--></div>\n\t\t\t\t\t</div>\n\t\t\t\t</div>');
            if (a.inArray("onSelect", c.overrideEvents) < 0) {
                itemHTML = c.itemTemplate;
                for (var k in itemData) itemHTML = itemHTML.replace(new RegExp("\\$\\{" + k + "\\}", "g"), itemData[k]);
                a("#" + c.queueID).append(itemHTML)
            }
            this.queueData.queueSize += b.size, this.queueData.files[b.id] = b, c.onSelect && c.onSelect.apply(this, arguments)
        }, onSelectError: function (b, c, d) {
            var e = this.settings;
            if (a.inArray("onSelectError", e.overrideEvents) < 0) switch (c) {
                case SWFUpload.QUEUE_ERROR.QUEUE_LIMIT_EXCEEDED:
                    e.queueSizeLimit > d ? this.queueData.errorMsg += "\n上传文件超过剩余可上传文件个数 (" + d + ")." : this.queueData.errorMsg += "\n上传文件超过单次最大上传个数(" + e.queueSizeLimit + ").";
                    break;
                case SWFUpload.QUEUE_ERROR.FILE_EXCEEDS_SIZE_LIMIT:
                    this.queueData.errorMsg += '\n上传文件 "' + b.name + '"超过最大上传大小 (' + e.fileSizeLimit + "字节).";
                    break;
                case SWFUpload.QUEUE_ERROR.ZERO_BYTE_FILE:
                    this.queueData.errorMsg += '\n上传文件"' + b.name + '"为空.';
                    break;
                case SWFUpload.QUEUE_ERROR.FILE_EXCEEDS_SIZE_LIMIT:
                    this.queueData.errorMsg += '\n上传文件"' + b.name + '"格式不正确,目前支持格式 (' + e.fileTypeDesc + ")."
            }
            c != SWFUpload.QUEUE_ERROR.QUEUE_LIMIT_EXCEEDED && delete this.queueData.files[b.id], e.onSelectError && e.onSelectError.apply(this, arguments)
        }, onQueueComplete: function () {
            this.settings.onQueueComplete && this.settings.onQueueComplete.call(this, this.settings.queueData)
        }, onUploadComplete: function (b) {
            var c = this.settings, d = this, e = this.getStats();
            this.queueData.queueLength = e.files_queued, this.queueData.uploadQueue[0] == "*" ?
                this.queueData.queueLength > 0 ? this.startUpload() : (this.queueData.uploadQueue = [], c.onQueueComplete && c.onQueueComplete.call(this, this.queueData)) : this.queueData.uploadQueue.length > 0 ? this.startUpload(this.queueData.uploadQueue.shift()) : (this.queueData.uploadQueue = [], c.onQueueComplete && c.onQueueComplete.call(this, this.queueData));
            if (a.inArray("onUploadComplete", c.overrideEvents) < 0) if (c.removeCompleted) switch (b.filestatus) {
                case SWFUpload.FILE_STATUS.COMPLETE:
                    setTimeout(function () {
                        a("#" + b.id) && (d.queueData.queueSize -= b.size, d.queueData.queueLength -= 1, delete d.queueData.files[b.id], a("#" + b.id).fadeOut(500, function () {
                            a(this).remove()
                        }))
                    }, c.removeTimeout * 1e3);
                    break;
                case SWFUpload.FILE_STATUS.ERROR:
                    c.requeueErrors || setTimeout(function () {
                        a("#" + b.id) && (d.queueData.queueSize -= b.size, d.queueData.queueLength -= 1, delete d.queueData.files[b.id], a("#" + b.id).fadeOut(500, function () {
                            a(this).remove()
                        }))
                    }, c.removeTimeout * 1e3)
            } else b.uploaded = !0;
            c.onUploadComplete && c.onUploadComplete.call(this, b)
        }, onUploadError: function (b, c, d) {
            var e = this.settings, f = "Error";
            switch (c) {
                case SWFUpload.UPLOAD_ERROR.HTTP_ERROR:
                    f = "HTTP Error (" + d + ")";
                    break;
                case SWFUpload.UPLOAD_ERROR.MISSING_UPLOAD_URL:
                    f = "Missing Upload URL";
                    break;
                case SWFUpload.UPLOAD_ERROR.IO_ERROR:
                    f = "IO Error";
                    break;
                case SWFUpload.UPLOAD_ERROR.SECURITY_ERROR:
                    f = "Security Error";
                    break;
                case SWFUpload.UPLOAD_ERROR.UPLOAD_LIMIT_EXCEEDED:
                    alert("超过了上传限制 (" + d + ")。"), f = "Exceeds Upload Limit";
                    break;
                case SWFUpload.UPLOAD_ERROR.UPLOAD_FAILED:
                    f = "Failed";
                    break;
                case SWFUpload.UPLOAD_ERROR.SPECIFIED_FILE_ID_NOT_FOUND:
                    break;
                case SWFUpload.UPLOAD_ERROR.FILE_VALIDATION_FAILED:
                    f = "Validation Error";
                    break;
                case SWFUpload.UPLOAD_ERROR.FILE_CANCELLED:
                    f = "Cancelled", this.queueData.queueSize -= b.size, this.queueData.queueLength -= 1;
                    if (b.status == SWFUpload.FILE_STATUS.IN_PROGRESS || a.inArray(b.id, this.queueData.uploadQueue) >= 0) this.queueData.uploadSize -= b.size;
                    e.onCancel && e.onCancel.call(this, b), delete this.queueData.files[b.id];
                    break;
                case SWFUpload.UPLOAD_ERROR.UPLOAD_STOPPED:
                    f = "Stopped"
            }
            a.inArray("onUploadError", e.overrideEvents) < 0 && (c != SWFUpload.UPLOAD_ERROR.FILE_CANCELLED && c != SWFUpload.UPLOAD_ERROR.UPLOAD_STOPPED && a("#" + b.id).addClass("uploadify-error"), a("#" + b.id).find(".uploadify-progress-bar").css("width", "1px"), c != SWFUpload.UPLOAD_ERROR.SPECIFIED_FILE_ID_NOT_FOUND && b.status != SWFUpload.FILE_STATUS.COMPLETE && a("#" + b.id).find(".data").html(" - " + f));
            var g = this.getStats();
            this.queueData.uploadsErrored = g.upload_errors, e.onUploadError && e.onUploadError.call(this, b, c, d, f)
        }, onUploadProgress: function (b, c, d) {
            var e = this.settings, f = new Date, g = f.getTime(), h = g - this.timer;
            h > 500 && (this.timer = g);
            var i = c - this.bytesLoaded;
            this.bytesLoaded = c;
            var j = this.queueData.queueBytesUploaded + c, k = Math.round(c / d * 100), l = "KB/s", m = 0,
                n = i / 1024 / (h / 1e3);
            n = Math.floor(n * 10) / 10, this.queueData.averageSpeed > 0 ? this.queueData.averageSpeed = Math.floor((this.queueData.averageSpeed + n) / 2) : this.queueData.averageSpeed = Math.floor(n), n > 1e3 && (m = n * .001, this.queueData.averageSpeed = Math.floor(m), l = "MB/s"), a.inArray("onUploadProgress", e.overrideEvents) < 0 && (e.progressData == "percentage" ? a("#" + b.id).find(".data").html(" - " + k + "%") : e.progressData == "speed" && h > 500 && a("#" + b.id).find(".data").html(" - " + this.queueData.averageSpeed + l), a("#" + b.id).find(".uploadify-progress-bar").css("width", k + "%")), e.onUploadProgress && e.onUploadProgress.call(this, b, c, d, j, this.queueData.uploadSize)
        }, onUploadStart: function (b) {
            var c = this.settings, d = new Date;
            this.timer = d.getTime(), this.bytesLoaded = 0, this.queueData.uploadQueue.length == 0 && (this.queueData.uploadSize = b.size), c.checkExisting && a.ajax({
                type: "POST",
                async: !1,
                url: c.checkExisting,
                data: {filename: b.name},
                success: function (c) {
                    if (c == 1) {
                        var d = confirm('文件名 "' + b.name + '" 已经存在于服务端。您是否要覆盖？');
                        d || (this.cancelUpload(b.id), a("#" + b.id).remove(), this.queueData.uploadQueue.length > 0 && this.queueData.queueLength > 0 && (this.queueData.uploadQueue[0] == "*" ? this.startUpload() : this.startUpload(this.queueData.uploadQueue.shift())))
                    }
                }
            }), c.onUploadStart && c.onUploadStart.call(this, b)
        }, onUploadSuccess: function (b, c, d) {
            var e = this.settings, f = this.getStats();
            this.queueData.uploadsSuccessful = f.successful_uploads, this.queueData.queueBytesUploaded += b.size, a.inArray("onUploadSuccess", e.overrideEvents) < 0 && a("#" + b.id).find(".data").html(" - Complete"), e.onUploadSuccess && e.onUploadSuccess.call(this, b, c, d)
        }
    };
    a.fn.uploadify = function (c) {
        if (b[c]) return b[c].apply(this, Array.prototype.slice.call(arguments, 1));
        if (typeof c == "object" || !c) return b.init.apply(this, arguments);
        a.error("The method " + c + " does not exist in $.uploadify")
    }
}($)
;
/*
 AngularJS v1.2.23
 (c) 2010-2014 Google, Inc. http://angularjs.org
 License: MIT
*/
(function (Q, X, t) {
    'use strict';

    function x(b) {
        return function () {
            var a = arguments[0], c,
                a = "[" + (b ? b + ":" : "") + a + "] http://errors.angularjs.org/1.2.23/" + (b ? b + "/" : "") + a;
            for (c = 1; c < arguments.length; c++) a = a + (1 == c ? "?" : "&") + "p" + (c - 1) + "=" + encodeURIComponent("function" == typeof arguments[c] ? arguments[c].toString().replace(/ \{[\s\S]*$/, "") : "undefined" == typeof arguments[c] ? "undefined" : "string" != typeof arguments[c] ? JSON.stringify(arguments[c]) : arguments[c]);
            return Error(a)
        }
    }

    function fb(b) {
        if (null == b || Fa(b)) return !1;
        var a = b.length;
        return 1 === b.nodeType && a ? !0 : z(b) || H(b) || 0 === a || "number" === typeof a && 0 < a && a - 1 in b
    }

    function r(b, a, c) {
        var d;
        if (b) if (P(b)) for (d in b) "prototype" == d || ("length" == d || "name" == d || b.hasOwnProperty && !b.hasOwnProperty(d)) || a.call(c, b[d], d); else if (H(b) || fb(b)) for (d = 0; d < b.length; d++) a.call(c, b[d], d); else if (b.forEach && b.forEach !== r) b.forEach(a, c); else for (d in b) b.hasOwnProperty(d) && a.call(c, b[d], d);
        return b
    }

    function Zb(b) {
        var a = [], c;
        for (c in b) b.hasOwnProperty(c) && a.push(c);
        return a.sort()
    }

    function Tc(b,
                a, c) {
        for (var d = Zb(b), e = 0; e < d.length; e++) a.call(c, b[d[e]], d[e]);
        return d
    }

    function $b(b) {
        return function (a, c) {
            b(c, a)
        }
    }

    function gb() {
        for (var b = la.length, a; b;) {
            b--;
            a = la[b].charCodeAt(0);
            if (57 == a) return la[b] = "A", la.join("");
            if (90 == a) la[b] = "0"; else return la[b] = String.fromCharCode(a + 1), la.join("")
        }
        la.unshift("0");
        return la.join("")
    }

    function ac(b, a) {
        a ? b.$$hashKey = a : delete b.$$hashKey
    }

    function B(b) {
        var a = b.$$hashKey;
        r(arguments, function (a) {
            a !== b && r(a, function (a, c) {
                b[c] = a
            })
        });
        ac(b, a);
        return b
    }

    function Z(b) {
        return parseInt(b,
            10)
    }

    function bc(b, a) {
        return B(new (B(function () {
        }, {prototype: b})), a)
    }

    function y() {
    }

    function Ga(b) {
        return b
    }

    function $(b) {
        return function () {
            return b
        }
    }

    function D(b) {
        return "undefined" === typeof b
    }

    function A(b) {
        return "undefined" !== typeof b
    }

    function T(b) {
        return null != b && "object" === typeof b
    }

    function z(b) {
        return "string" === typeof b
    }

    function Ab(b) {
        return "number" === typeof b
    }

    function sa(b) {
        return "[object Date]" === ya.call(b)
    }

    function P(b) {
        return "function" === typeof b
    }

    function hb(b) {
        return "[object RegExp]" === ya.call(b)
    }

    function Fa(b) {
        return b && b.document && b.location && b.alert && b.setInterval
    }

    function Uc(b) {
        return !(!b || !(b.nodeName || b.prop && b.attr && b.find))
    }

    function Vc(b, a, c) {
        var d = [];
        r(b, function (b, f, g) {
            d.push(a.call(c, b, f, g))
        });
        return d
    }

    function Qa(b, a) {
        if (b.indexOf) return b.indexOf(a);
        for (var c = 0; c < b.length; c++) if (a === b[c]) return c;
        return -1
    }

    function Ra(b, a) {
        var c = Qa(b, a);
        0 <= c && b.splice(c, 1);
        return a
    }

    function Ha(b, a, c, d) {
        if (Fa(b) || b && b.$evalAsync && b.$watch) throw Sa("cpws");
        if (a) {
            if (b === a) throw Sa("cpi");
            c = c || [];
            d = d || [];
            if (T(b)) {
                var e = Qa(c, b);
                if (-1 !== e) return d[e];
                c.push(b);
                d.push(a)
            }
            if (H(b)) for (var f = a.length = 0; f < b.length; f++) e = Ha(b[f], null, c, d), T(b[f]) && (c.push(b[f]), d.push(e)), a.push(e); else {
                var g = a.$$hashKey;
                H(a) ? a.length = 0 : r(a, function (b, c) {
                    delete a[c]
                });
                for (f in b) e = Ha(b[f], null, c, d), T(b[f]) && (c.push(b[f]), d.push(e)), a[f] = e;
                ac(a, g)
            }
        } else if (a = b) H(b) ? a = Ha(b, [], c, d) : sa(b) ? a = new Date(b.getTime()) : hb(b) ? (a = RegExp(b.source, b.toString().match(/[^\/]*$/)[0]), a.lastIndex = b.lastIndex) : T(b) && (a = Ha(b, {}, c, d));
        return a
    }

    function ga(b, a) {
        if (H(b)) {
            a = a || [];
            for (var c = 0; c < b.length; c++) a[c] = b[c]
        } else if (T(b)) for (c in a = a || {}, b) !ib.call(b, c) || "$" === c.charAt(0) && "$" === c.charAt(1) || (a[c] = b[c]);
        return a || b
    }

    function za(b, a) {
        if (b === a) return !0;
        if (null === b || null === a) return !1;
        if (b !== b && a !== a) return !0;
        var c = typeof b, d;
        if (c == typeof a && "object" == c) if (H(b)) {
            if (!H(a)) return !1;
            if ((c = b.length) == a.length) {
                for (d = 0; d < c; d++) if (!za(b[d], a[d])) return !1;
                return !0
            }
        } else {
            if (sa(b)) return sa(a) ? isNaN(b.getTime()) && isNaN(a.getTime()) || b.getTime() ===
                a.getTime() : !1;
            if (hb(b) && hb(a)) return b.toString() == a.toString();
            if (b && b.$evalAsync && b.$watch || a && a.$evalAsync && a.$watch || Fa(b) || Fa(a) || H(a)) return !1;
            c = {};
            for (d in b) if ("$" !== d.charAt(0) && !P(b[d])) {
                if (!za(b[d], a[d])) return !1;
                c[d] = !0
            }
            for (d in a) if (!c.hasOwnProperty(d) && "$" !== d.charAt(0) && a[d] !== t && !P(a[d])) return !1;
            return !0
        }
        return !1
    }

    function Bb(b, a) {
        var c = 2 < arguments.length ? Aa.call(arguments, 2) : [];
        return !P(a) || a instanceof RegExp ? a : c.length ? function () {
            return arguments.length ? a.apply(b, c.concat(Aa.call(arguments,
                0))) : a.apply(b, c)
        } : function () {
            return arguments.length ? a.apply(b, arguments) : a.call(b)
        }
    }

    function Wc(b, a) {
        var c = a;
        "string" === typeof b && "$" === b.charAt(0) ? c = t : Fa(a) ? c = "$WINDOW" : a && X === a ? c = "$DOCUMENT" : a && (a.$evalAsync && a.$watch) && (c = "$SCOPE");
        return c
    }

    function ta(b, a) {
        return "undefined" === typeof b ? t : JSON.stringify(b, Wc, a ? "  " : null)
    }

    function cc(b) {
        return z(b) ? JSON.parse(b) : b
    }

    function Ta(b) {
        "function" === typeof b ? b = !0 : b && 0 !== b.length ? (b = N("" + b), b = !("f" == b || "0" == b || "false" == b || "no" == b || "n" == b || "[]" == b)) : b = !1;
        return b
    }

    function ha(b) {
        b = u(b).clone();
        try {
            b.empty()
        } catch (a) {
        }
        var c = u("<div>").append(b).html();
        try {
            return 3 === b[0].nodeType ? N(c) : c.match(/^(<[^>]+>)/)[1].replace(/^<([\w\-]+)/, function (a, b) {
                return "<" + N(b)
            })
        } catch (d) {
            return N(c)
        }
    }

    function dc(b) {
        try {
            return decodeURIComponent(b)
        } catch (a) {
        }
    }

    function ec(b) {
        var a = {}, c, d;
        r((b || "").split("&"), function (b) {
            b && (c = b.replace(/\+/g, "%20").split("="), d = dc(c[0]), A(d) && (b = A(c[1]) ? dc(c[1]) : !0, ib.call(a, d) ? H(a[d]) ? a[d].push(b) : a[d] = [a[d], b] : a[d] = b))
        });
        return a
    }

    function Cb(b) {
        var a =
            [];
        r(b, function (b, d) {
            H(b) ? r(b, function (b) {
                a.push(Ba(d, !0) + (!0 === b ? "" : "=" + Ba(b, !0)))
            }) : a.push(Ba(d, !0) + (!0 === b ? "" : "=" + Ba(b, !0)))
        });
        return a.length ? a.join("&") : ""
    }

    function jb(b) {
        return Ba(b, !0).replace(/%26/gi, "&").replace(/%3D/gi, "=").replace(/%2B/gi, "+")
    }

    function Ba(b, a) {
        return encodeURIComponent(b).replace(/%40/gi, "@").replace(/%3A/gi, ":").replace(/%24/g, "$").replace(/%2C/gi, ",").replace(/%20/g, a ? "%20" : "+")
    }

    function Xc(b, a) {
        function c(a) {
            a && d.push(a)
        }

        var d = [b], e, f, g = ["ng:app", "ng-app", "x-ng-app",
            "data-ng-app"], k = /\sng[:\-]app(:\s*([\w\d_]+);?)?\s/;
        r(g, function (a) {
            g[a] = !0;
            c(X.getElementById(a));
            a = a.replace(":", "\\:");
            b.querySelectorAll && (r(b.querySelectorAll("." + a), c), r(b.querySelectorAll("." + a + "\\:"), c), r(b.querySelectorAll("[" + a + "]"), c))
        });
        r(d, function (a) {
            if (!e) {
                var b = k.exec(" " + a.className + " ");
                b ? (e = a, f = (b[2] || "").replace(/\s+/g, ",")) : r(a.attributes, function (b) {
                    !e && g[b.name] && (e = a, f = b.value)
                })
            }
        });
        e && a(e, f ? [f] : [])
    }

    function fc(b, a) {
        var c = function () {
            b = u(b);
            if (b.injector()) {
                var c = b[0] === X ?
                    "document" : ha(b);
                throw Sa("btstrpd", c.replace(/</, "&lt;").replace(/>/, "&gt;"));
            }
            a = a || [];
            a.unshift(["$provide", function (a) {
                a.value("$rootElement", b)
            }]);
            a.unshift("ng");
            c = gc(a);
            c.invoke(["$rootScope", "$rootElement", "$compile", "$injector", "$animate", function (a, b, c, d, e) {
                a.$apply(function () {
                    b.data("$injector", d);
                    c(b)(a)
                })
            }]);
            return c
        }, d = /^NG_DEFER_BOOTSTRAP!/;
        if (Q && !d.test(Q.name)) return c();
        Q.name = Q.name.replace(d, "");
        Ua.resumeBootstrap = function (b) {
            r(b, function (b) {
                a.push(b)
            });
            c()
        }
    }

    function kb(b, a) {
        a =
            a || "_";
        return b.replace(Yc, function (b, d) {
            return (d ? a : "") + b.toLowerCase()
        })
    }

    function Db(b, a, c) {
        if (!b) throw Sa("areq", a || "?", c || "required");
        return b
    }

    function Va(b, a, c) {
        c && H(b) && (b = b[b.length - 1]);
        Db(P(b), a, "not a function, got " + (b && "object" === typeof b ? b.constructor.name || "Object" : typeof b));
        return b
    }

    function Ca(b, a) {
        if ("hasOwnProperty" === b) throw Sa("badname", a);
    }

    function hc(b, a, c) {
        if (!a) return b;
        a = a.split(".");
        for (var d, e = b, f = a.length, g = 0; g < f; g++) d = a[g], b && (b = (e = b)[d]);
        return !c && P(b) ? Bb(e, b) : b
    }

    function Eb(b) {
        var a =
            b[0];
        b = b[b.length - 1];
        if (a === b) return u(a);
        var c = [a];
        do {
            a = a.nextSibling;
            if (!a) break;
            c.push(a)
        } while (a !== b);
        return u(c)
    }

    function Zc(b) {
        var a = x("$injector"), c = x("ng");
        b = b.angular || (b.angular = {});
        b.$$minErr = b.$$minErr || x;
        return b.module || (b.module = function () {
            var b = {};
            return function (e, f, g) {
                if ("hasOwnProperty" === e) throw c("badname", "module");
                f && b.hasOwnProperty(e) && (b[e] = null);
                return b[e] || (b[e] = function () {
                    function b(a, d, e) {
                        return function () {
                            c[e || "push"]([a, d, arguments]);
                            return n
                        }
                    }

                    if (!f) throw a("nomod",
                        e);
                    var c = [], d = [], l = b("$injector", "invoke"), n = {
                        _invokeQueue: c,
                        _runBlocks: d,
                        requires: f,
                        name: e,
                        provider: b("$provide", "provider"),
                        factory: b("$provide", "factory"),
                        service: b("$provide", "service"),
                        value: b("$provide", "value"),
                        constant: b("$provide", "constant", "unshift"),
                        animation: b("$animateProvider", "register"),
                        filter: b("$filterProvider", "register"),
                        controller: b("$controllerProvider", "register"),
                        directive: b("$compileProvider", "directive"),
                        config: l,
                        run: function (a) {
                            d.push(a);
                            return this
                        }
                    };
                    g && l(g);
                    return n
                }())
            }
        }())
    }

    function $c(b) {
        B(b, {
            bootstrap: fc,
            copy: Ha,
            extend: B,
            equals: za,
            element: u,
            forEach: r,
            injector: gc,
            noop: y,
            bind: Bb,
            toJson: ta,
            fromJson: cc,
            identity: Ga,
            isUndefined: D,
            isDefined: A,
            isString: z,
            isFunction: P,
            isObject: T,
            isNumber: Ab,
            isElement: Uc,
            isArray: H,
            version: ad,
            isDate: sa,
            lowercase: N,
            uppercase: Ia,
            callbacks: {counter: 0},
            $$minErr: x,
            $$csp: Wa
        });
        Xa = Zc(Q);
        try {
            Xa("ngLocale")
        } catch (a) {
            Xa("ngLocale", []).provider("$locale", bd)
        }
        Xa("ng", ["ngLocale"], ["$provide", function (a) {
            a.provider({$$sanitizeUri: cd});
            a.provider("$compile",
                ic).directive({
                a: dd,
                input: jc,
                textarea: jc,
                form: ed,
                script: fd,
                select: gd,
                style: hd,
                option: id,
                ngBind: jd,
                ngBindHtml: kd,
                ngBindTemplate: ld,
                ngClass: md,
                ngClassEven: nd,
                ngClassOdd: od,
                ngCloak: pd,
                ngController: qd,
                ngForm: rd,
                ngHide: sd,
                ngIf: td,
                ngInclude: ud,
                ngInit: vd,
                ngNonBindable: wd,
                ngPluralize: xd,
                ngRepeat: yd,
                ngShow: zd,
                ngStyle: Ad,
                ngSwitch: Bd,
                ngSwitchWhen: Cd,
                ngSwitchDefault: Dd,
                ngOptions: Ed,
                ngTransclude: Fd,
                ngModel: Gd,
                ngList: Hd,
                ngChange: Id,
                required: kc,
                ngRequired: kc,
                ngValue: Jd
            }).directive({ngInclude: Kd}).directive(Fb).directive(lc);
            a.provider({
                $anchorScroll: Ld,
                $animate: Md,
                $browser: Nd,
                $cacheFactory: Od,
                $controller: Pd,
                $document: Qd,
                $exceptionHandler: Rd,
                $filter: mc,
                $interpolate: Sd,
                $interval: Td,
                $http: Ud,
                $httpBackend: Vd,
                $location: Wd,
                $log: Xd,
                $parse: Yd,
                $rootScope: Zd,
                $q: $d,
                $sce: ae,
                $sceDelegate: be,
                $sniffer: ce,
                $templateCache: de,
                $timeout: ee,
                $window: fe,
                $$rAF: ge,
                $$asyncCallback: he
            })
        }])
    }

    function Ya(b) {
        return b.replace(ie, function (a, b, d, e) {
            return e ? d.toUpperCase() : d
        }).replace(je, "Moz$1")
    }

    function Gb(b, a, c, d) {
        function e(b) {
            var e = c && b ? [this.filter(b)] :
                [this], m = a, h, l, n, p, q, s;
            if (!d || null != b) for (; e.length;) for (h = e.shift(), l = 0, n = h.length; l < n; l++) for (p = u(h[l]), m ? p.triggerHandler("$destroy") : m = !m, q = 0, p = (s = p.children()).length; q < p; q++) e.push(Da(s[q]));
            return f.apply(this, arguments)
        }

        var f = Da.fn[b], f = f.$original || f;
        e.$original = f;
        Da.fn[b] = e
    }

    function S(b) {
        if (b instanceof S) return b;
        z(b) && (b = aa(b));
        if (!(this instanceof S)) {
            if (z(b) && "<" != b.charAt(0)) throw Hb("nosel");
            return new S(b)
        }
        if (z(b)) {
            var a = b;
            b = X;
            var c;
            if (c = ke.exec(a)) b = [b.createElement(c[1])]; else {
                var d =
                    b, e;
                b = d.createDocumentFragment();
                c = [];
                if (Ib.test(a)) {
                    d = b.appendChild(d.createElement("div"));
                    e = (le.exec(a) || ["", ""])[1].toLowerCase();
                    e = ba[e] || ba._default;
                    d.innerHTML = "<div>&#160;</div>" + e[1] + a.replace(me, "<$1></$2>") + e[2];
                    d.removeChild(d.firstChild);
                    for (a = e[0]; a--;) d = d.lastChild;
                    a = 0;
                    for (e = d.childNodes.length; a < e; ++a) c.push(d.childNodes[a]);
                    d = b.firstChild;
                    d.textContent = ""
                } else c.push(d.createTextNode(a));
                b.textContent = "";
                b.innerHTML = "";
                b = c
            }
            Jb(this, b);
            u(X.createDocumentFragment()).append(this)
        } else Jb(this,
            b)
    }

    function Kb(b) {
        return b.cloneNode(!0)
    }

    function Ja(b) {
        Lb(b);
        var a = 0;
        for (b = b.childNodes || []; a < b.length; a++) Ja(b[a])
    }

    function nc(b, a, c, d) {
        if (A(d)) throw Hb("offargs");
        var e = ma(b, "events");
        ma(b, "handle") && (D(a) ? r(e, function (a, c) {
            Za(b, c, a);
            delete e[c]
        }) : r(a.split(" "), function (a) {
            D(c) ? (Za(b, a, e[a]), delete e[a]) : Ra(e[a] || [], c)
        }))
    }

    function Lb(b, a) {
        var c = b.ng339, d = $a[c];
        d && (a ? delete $a[c].data[a] : (d.handle && (d.events.$destroy && d.handle({}, "$destroy"), nc(b)), delete $a[c], b.ng339 = t))
    }

    function ma(b, a, c) {
        var d =
            b.ng339, d = $a[d || -1];
        if (A(c)) d || (b.ng339 = d = ++ne, d = $a[d] = {}), d[a] = c; else return d && d[a]
    }

    function Mb(b, a, c) {
        var d = ma(b, "data"), e = A(c), f = !e && A(a), g = f && !T(a);
        d || g || ma(b, "data", d = {});
        if (e) d[a] = c; else if (f) {
            if (g) return d && d[a];
            B(d, a)
        } else return d
    }

    function Nb(b, a) {
        return b.getAttribute ? -1 < (" " + (b.getAttribute("class") || "") + " ").replace(/[\n\t]/g, " ").indexOf(" " + a + " ") : !1
    }

    function lb(b, a) {
        a && b.setAttribute && r(a.split(" "), function (a) {
            b.setAttribute("class", aa((" " + (b.getAttribute("class") || "") + " ").replace(/[\n\t]/g,
                " ").replace(" " + aa(a) + " ", " ")))
        })
    }

    function mb(b, a) {
        if (a && b.setAttribute) {
            var c = (" " + (b.getAttribute("class") || "") + " ").replace(/[\n\t]/g, " ");
            r(a.split(" "), function (a) {
                a = aa(a);
                -1 === c.indexOf(" " + a + " ") && (c += a + " ")
            });
            b.setAttribute("class", aa(c))
        }
    }

    function Jb(b, a) {
        if (a) {
            a = a.nodeName || !A(a.length) || Fa(a) ? [a] : a;
            for (var c = 0; c < a.length; c++) b.push(a[c])
        }
    }

    function oc(b, a) {
        return nb(b, "$" + (a || "ngController") + "Controller")
    }

    function nb(b, a, c) {
        9 == b.nodeType && (b = b.documentElement);
        for (a = H(a) ? a : [a]; b;) {
            for (var d =
                0, e = a.length; d < e; d++) if ((c = u.data(b, a[d])) !== t) return c;
            b = b.parentNode || 11 === b.nodeType && b.host
        }
    }

    function pc(b) {
        for (var a = 0, c = b.childNodes; a < c.length; a++) Ja(c[a]);
        for (; b.firstChild;) b.removeChild(b.firstChild)
    }

    function qc(b, a) {
        var c = ob[a.toLowerCase()];
        return c && rc[b.nodeName] && c
    }

    function oe(b, a) {
        var c = function (c, e) {
            c.preventDefault || (c.preventDefault = function () {
                c.returnValue = !1
            });
            c.stopPropagation || (c.stopPropagation = function () {
                c.cancelBubble = !0
            });
            c.target || (c.target = c.srcElement || X);
            if (D(c.defaultPrevented)) {
                var f =
                    c.preventDefault;
                c.preventDefault = function () {
                    c.defaultPrevented = !0;
                    f.call(c)
                };
                c.defaultPrevented = !1
            }
            c.isDefaultPrevented = function () {
                return c.defaultPrevented || !1 === c.returnValue
            };
            var g = ga(a[e || c.type] || []);
            r(g, function (a) {
                a.call(b, c)
            });
            8 >= R ? (c.preventDefault = null, c.stopPropagation = null, c.isDefaultPrevented = null) : (delete c.preventDefault, delete c.stopPropagation, delete c.isDefaultPrevented)
        };
        c.elem = b;
        return c
    }

    function Ka(b, a) {
        var c = typeof b, d;
        "function" == c || "object" == c && null !== b ? "function" == typeof (d =
            b.$$hashKey) ? d = b.$$hashKey() : d === t && (d = b.$$hashKey = (a || gb)()) : d = b;
        return c + ":" + d
    }

    function ab(b, a) {
        if (a) {
            var c = 0;
            this.nextUid = function () {
                return ++c
            }
        }
        r(b, this.put, this)
    }

    function sc(b) {
        var a, c;
        "function" === typeof b ? (a = b.$inject) || (a = [], b.length && (c = b.toString().replace(pe, ""), c = c.match(qe), r(c[1].split(re), function (b) {
            b.replace(se, function (b, c, d) {
                a.push(d)
            })
        })), b.$inject = a) : H(b) ? (c = b.length - 1, Va(b[c], "fn"), a = b.slice(0, c)) : Va(b, "fn", !0);
        return a
    }

    function gc(b) {
        function a(a) {
            return function (b, c) {
                if (T(b)) r(b,
                    $b(a)); else return a(b, c)
            }
        }

        function c(a, b) {
            Ca(a, "service");
            if (P(b) || H(b)) b = n.instantiate(b);
            if (!b.$get) throw bb("pget", a);
            return l[a + k] = b
        }

        function d(a, b) {
            return c(a, {$get: b})
        }

        function e(a) {
            var b = [], c, d, f, k;
            r(a, function (a) {
                if (!h.get(a)) {
                    h.put(a, !0);
                    try {
                        if (z(a)) for (c = Xa(a), b = b.concat(e(c.requires)).concat(c._runBlocks), d = c._invokeQueue, f = 0, k = d.length; f < k; f++) {
                            var g = d[f], m = n.get(g[0]);
                            m[g[1]].apply(m, g[2])
                        } else P(a) ? b.push(n.invoke(a)) : H(a) ? b.push(n.invoke(a)) : Va(a, "module")
                    } catch (l) {
                        throw H(a) && (a =
                            a[a.length - 1]), l.message && (l.stack && -1 == l.stack.indexOf(l.message)) && (l = l.message + "\n" + l.stack), bb("modulerr", a, l.stack || l.message || l);
                    }
                }
            });
            return b
        }

        function f(a, b) {
            function c(d) {
                if (a.hasOwnProperty(d)) {
                    if (a[d] === g) throw bb("cdep", d + " <- " + m.join(" <- "));
                    return a[d]
                }
                try {
                    return m.unshift(d), a[d] = g, a[d] = b(d)
                } catch (e) {
                    throw a[d] === g && delete a[d], e;
                } finally {
                    m.shift()
                }
            }

            function d(a, b, e) {
                var f = [], k = sc(a), g, m, h;
                m = 0;
                for (g = k.length; m < g; m++) {
                    h = k[m];
                    if ("string" !== typeof h) throw bb("itkn", h);
                    f.push(e && e.hasOwnProperty(h) ?
                        e[h] : c(h))
                }
                H(a) && (a = a[g]);
                return a.apply(b, f)
            }

            return {
                invoke: d, instantiate: function (a, b) {
                    var c = function () {
                    }, e;
                    c.prototype = (H(a) ? a[a.length - 1] : a).prototype;
                    c = new c;
                    e = d(a, c, b);
                    return T(e) || P(e) ? e : c
                }, get: c, annotate: sc, has: function (b) {
                    return l.hasOwnProperty(b + k) || a.hasOwnProperty(b)
                }
            }
        }

        var g = {}, k = "Provider", m = [], h = new ab([], !0), l = {
            $provide: {
                provider: a(c), factory: a(d), service: a(function (a, b) {
                    return d(a, ["$injector", function (a) {
                        return a.instantiate(b)
                    }])
                }), value: a(function (a, b) {
                    return d(a, $(b))
                }), constant: a(function (a,
                                          b) {
                    Ca(a, "constant");
                    l[a] = b;
                    p[a] = b
                }), decorator: function (a, b) {
                    var c = n.get(a + k), d = c.$get;
                    c.$get = function () {
                        var a = q.invoke(d, c);
                        return q.invoke(b, null, {$delegate: a})
                    }
                }
            }
        }, n = l.$injector = f(l, function () {
            throw bb("unpr", m.join(" <- "));
        }), p = {}, q = p.$injector = f(p, function (a) {
            a = n.get(a + k);
            return q.invoke(a.$get, a)
        });
        r(e(b), function (a) {
            q.invoke(a || y)
        });
        return q
    }

    function Ld() {
        var b = !0;
        this.disableAutoScrolling = function () {
            b = !1
        };
        this.$get = ["$window", "$location", "$rootScope", function (a, c, d) {
            function e(a) {
                var b = null;
                r(a, function (a) {
                    b || "a" !== N(a.nodeName) || (b = a)
                });
                return b
            }

            function f() {
                var b = c.hash(), d;
                b ? (d = g.getElementById(b)) ? d.scrollIntoView() : (d = e(g.getElementsByName(b))) ? d.scrollIntoView() : "top" === b && a.scrollTo(0, 0) : a.scrollTo(0, 0)
            }

            var g = a.document;
            b && d.$watch(function () {
                return c.hash()
            }, function () {
                d.$evalAsync(f)
            });
            return f
        }]
    }

    function he() {
        this.$get = ["$$rAF", "$timeout", function (b, a) {
            return b.supported ? function (a) {
                return b(a)
            } : function (b) {
                return a(b, 0, !1)
            }
        }]
    }

    function te(b, a, c, d) {
        function e(a) {
            try {
                a.apply(null,
                    Aa.call(arguments, 1))
            } finally {
                if (s--, 0 === s) for (; L.length;) try {
                    L.pop()()
                } catch (b) {
                    c.error(b)
                }
            }
        }

        function f(a, b) {
            (function ca() {
                r(v, function (a) {
                    a()
                });
                C = b(ca, a)
            })()
        }

        function g() {
            w = null;
            O != k.url() && (O = k.url(), r(da, function (a) {
                a(k.url())
            }))
        }

        var k = this, m = a[0], h = b.location, l = b.history, n = b.setTimeout, p = b.clearTimeout, q = {};
        k.isMock = !1;
        var s = 0, L = [];
        k.$$completeOutstandingRequest = e;
        k.$$incOutstandingRequestCount = function () {
            s++
        };
        k.notifyWhenNoOutstandingRequests = function (a) {
            r(v, function (a) {
                a()
            });
            0 === s ? a() : L.push(a)
        };
        var v = [], C;
        k.addPollFn = function (a) {
            D(C) && f(100, n);
            v.push(a);
            return a
        };
        var O = h.href, I = a.find("base"), w = null;
        k.url = function (a, c) {
            h !== b.location && (h = b.location);
            l !== b.history && (l = b.history);
            if (a) {
                if (O != a) return O = a, d.history ? c ? l.replaceState(null, "", a) : (l.pushState(null, "", a), I.attr("href", I.attr("href"))) : (w = a, c ? h.replace(a) : h.href = a), k
            } else return w || h.href.replace(/%27/g, "'")
        };
        var da = [], K = !1;
        k.onUrlChange = function (a) {
            if (!K) {
                if (d.history) u(b).on("popstate", g);
                if (d.hashchange) u(b).on("hashchange", g);
                else k.addPollFn(g);
                K = !0
            }
            da.push(a);
            return a
        };
        k.baseHref = function () {
            var a = I.attr("href");
            return a ? a.replace(/^(https?\:)?\/\/[^\/]*/, "") : ""
        };
        var W = {}, ea = "", J = k.baseHref();
        k.cookies = function (a, b) {
            var d, e, f, k;
            if (a) b === t ? m.cookie = escape(a) + "=;path=" + J + ";expires=Thu, 01 Jan 1970 00:00:00 GMT" : z(b) && (d = (m.cookie = escape(a) + "=" + escape(b) + ";path=" + J).length + 1, 4096 < d && c.warn("Cookie '" + a + "' possibly not set or overflowed because it was too large (" + d + " > 4096 bytes)!")); else {
                if (m.cookie !== ea) for (ea = m.cookie,
                                              d = ea.split("; "), W = {}, f = 0; f < d.length; f++) e = d[f], k = e.indexOf("="), 0 < k && (a = unescape(e.substring(0, k)), W[a] === t && (W[a] = unescape(e.substring(k + 1))));
                return W
            }
        };
        k.defer = function (a, b) {
            var c;
            s++;
            c = n(function () {
                delete q[c];
                e(a)
            }, b || 0);
            q[c] = !0;
            return c
        };
        k.defer.cancel = function (a) {
            return q[a] ? (delete q[a], p(a), e(y), !0) : !1
        }
    }

    function Nd() {
        this.$get = ["$window", "$log", "$sniffer", "$document", function (b, a, c, d) {
            return new te(b, d, a, c)
        }]
    }

    function Od() {
        this.$get = function () {
            function b(b, d) {
                function e(a) {
                    a != n && (p ? p == a &&
                        (p = a.n) : p = a, f(a.n, a.p), f(a, n), n = a, n.n = null)
                }

                function f(a, b) {
                    a != b && (a && (a.p = b), b && (b.n = a))
                }

                if (b in a) throw x("$cacheFactory")("iid", b);
                var g = 0, k = B({}, d, {id: b}), m = {}, h = d && d.capacity || Number.MAX_VALUE, l = {}, n = null,
                    p = null;
                return a[b] = {
                    put: function (a, b) {
                        if (h < Number.MAX_VALUE) {
                            var c = l[a] || (l[a] = {key: a});
                            e(c)
                        }
                        if (!D(b)) return a in m || g++, m[a] = b, g > h && this.remove(p.key), b
                    }, get: function (a) {
                        if (h < Number.MAX_VALUE) {
                            var b = l[a];
                            if (!b) return;
                            e(b)
                        }
                        return m[a]
                    }, remove: function (a) {
                        if (h < Number.MAX_VALUE) {
                            var b = l[a];
                            if (!b) return;
                            b == n && (n = b.p);
                            b == p && (p = b.n);
                            f(b.n, b.p);
                            delete l[a]
                        }
                        delete m[a];
                        g--
                    }, removeAll: function () {
                        m = {};
                        g = 0;
                        l = {};
                        n = p = null
                    }, destroy: function () {
                        l = k = m = null;
                        delete a[b]
                    }, info: function () {
                        return B({}, k, {size: g})
                    }
                }
            }

            var a = {};
            b.info = function () {
                var b = {};
                r(a, function (a, e) {
                    b[e] = a.info()
                });
                return b
            };
            b.get = function (b) {
                return a[b]
            };
            return b
        }
    }

    function de() {
        this.$get = ["$cacheFactory", function (b) {
            return b("templates")
        }]
    }

    function ic(b, a) {
        var c = {}, d = "Directive", e = /^\s*directive\:\s*([\d\w_\-]+)\s+(.*)$/, f = /(([\d\w_\-]+)(?:\:([^;]+))?;?)/,
            g = /^(on[a-z]+|formaction)$/;
        this.directive = function m(a, e) {
            Ca(a, "directive");
            z(a) ? (Db(e, "directiveFactory"), c.hasOwnProperty(a) || (c[a] = [], b.factory(a + d, ["$injector", "$exceptionHandler", function (b, d) {
                var e = [];
                r(c[a], function (c, f) {
                    try {
                        var g = b.invoke(c);
                        P(g) ? g = {compile: $(g)} : !g.compile && g.link && (g.compile = $(g.link));
                        g.priority = g.priority || 0;
                        g.index = f;
                        g.name = g.name || a;
                        g.require = g.require || g.controller && g.name;
                        g.restrict = g.restrict || "A";
                        e.push(g)
                    } catch (m) {
                        d(m)
                    }
                });
                return e
            }])), c[a].push(e)) : r(a, $b(m));
            return this
        };
        this.aHrefSanitizationWhitelist = function (b) {
            return A(b) ? (a.aHrefSanitizationWhitelist(b), this) : a.aHrefSanitizationWhitelist()
        };
        this.imgSrcSanitizationWhitelist = function (b) {
            return A(b) ? (a.imgSrcSanitizationWhitelist(b), this) : a.imgSrcSanitizationWhitelist()
        };
        this.$get = ["$injector", "$interpolate", "$exceptionHandler", "$http", "$templateCache", "$parse", "$controller", "$rootScope", "$document", "$sce", "$animate", "$$sanitizeUri", function (a, b, l, n, p, q, s, L, v, C, O, I) {
            function w(a, b, c, d, e) {
                a instanceof
                u || (a = u(a));
                r(a, function (b, c) {
                    3 == b.nodeType && b.nodeValue.match(/\S+/) && (a[c] = u(b).wrap("<span></span>").parent()[0])
                });
                var f = K(a, b, a, c, d, e);
                da(a, "ng-scope");
                return function (b, c, d, e) {
                    Db(b, "scope");
                    var g = c ? La.clone.call(a) : a;
                    r(d, function (a, b) {
                        g.data("$" + b + "Controller", a)
                    });
                    d = 0;
                    for (var m = g.length; d < m; d++) {
                        var h = g[d].nodeType;
                        1 !== h && 9 !== h || g.eq(d).data("$scope", b)
                    }
                    c && c(g, b);
                    f && f(b, g, g, e);
                    return g
                }
            }

            function da(a, b) {
                try {
                    a.addClass(b)
                } catch (c) {
                }
            }

            function K(a, b, c, d, e, f) {
                function g(a, c, d, e) {
                    var f, h, l, q, n,
                        p, s;
                    f = c.length;
                    var M = Array(f);
                    for (q = 0; q < f; q++) M[q] = c[q];
                    p = q = 0;
                    for (n = m.length; q < n; p++) h = M[p], c = m[q++], f = m[q++], c ? (c.scope ? (l = a.$new(), u.data(h, "$scope", l)) : l = a, s = c.transcludeOnThisElement ? W(a, c.transclude, e) : !c.templateOnThisElement && e ? e : !e && b ? W(a, b) : null, c(f, l, h, d, s)) : f && f(a, h.childNodes, t, e)
                }

                for (var m = [], h, l, q, n, p = 0; p < a.length; p++) h = new Ob, l = ea(a[p], [], h, 0 === p ? d : t, e), (f = l.length ? F(l, a[p], h, b, c, null, [], [], f) : null) && f.scope && da(h.$$element, "ng-scope"), h = f && f.terminal || !(q = a[p].childNodes) || !q.length ?
                    null : K(q, f ? (f.transcludeOnThisElement || !f.templateOnThisElement) && f.transclude : b), m.push(f, h), n = n || f || h, f = null;
                return n ? g : null
            }

            function W(a, b, c) {
                return function (d, e, f) {
                    var g = !1;
                    d || (d = a.$new(), g = d.$$transcluded = !0);
                    e = b(d, e, f, c);
                    if (g) e.on("$destroy", function () {
                        d.$destroy()
                    });
                    return e
                }
            }

            function ea(a, b, c, d, g) {
                var h = c.$attr, m;
                switch (a.nodeType) {
                    case 1:
                        ca(b, na(Ma(a).toLowerCase()), "E", d, g);
                        for (var l, q, n, p = a.attributes, s = 0, L = p && p.length; s < L; s++) {
                            var C = !1, O = !1;
                            l = p[s];
                            if (!R || 8 <= R || l.specified) {
                                m = l.name;
                                q =
                                    aa(l.value);
                                l = na(m);
                                if (n = V.test(l)) m = kb(l.substr(6), "-");
                                var v = l.replace(/(Start|End)$/, "");
                                l === v + "Start" && (C = m, O = m.substr(0, m.length - 5) + "end", m = m.substr(0, m.length - 6));
                                l = na(m.toLowerCase());
                                h[l] = m;
                                if (n || !c.hasOwnProperty(l)) c[l] = q, qc(a, l) && (c[l] = !0);
                                Q(a, b, q, l);
                                ca(b, l, "A", d, g, C, O)
                            }
                        }
                        a = a.className;
                        if (z(a) && "" !== a) for (; m = f.exec(a);) l = na(m[2]), ca(b, l, "C", d, g) && (c[l] = aa(m[3])), a = a.substr(m.index + m[0].length);
                        break;
                    case 3:
                        x(b, a.nodeValue);
                        break;
                    case 8:
                        try {
                            if (m = e.exec(a.nodeValue)) l = na(m[1]), ca(b, l, "M",
                                d, g) && (c[l] = aa(m[2]))
                        } catch (w) {
                        }
                }
                b.sort(D);
                return b
            }

            function J(a, b, c) {
                var d = [], e = 0;
                if (b && a.hasAttribute && a.hasAttribute(b)) {
                    do {
                        if (!a) throw ia("uterdir", b, c);
                        1 == a.nodeType && (a.hasAttribute(b) && e++, a.hasAttribute(c) && e--);
                        d.push(a);
                        a = a.nextSibling
                    } while (0 < e)
                } else d.push(a);
                return u(d)
            }

            function E(a, b, c) {
                return function (d, e, f, g, m) {
                    e = J(e[0], b, c);
                    return a(d, e, f, g, m)
                }
            }

            function F(a, c, d, e, f, g, m, n, p) {
                function L(a, b, c, d) {
                    if (a) {
                        c && (a = E(a, c, d));
                        a.require = G.require;
                        a.directiveName = oa;
                        if (K === G || G.$$isolateScope) a =
                            tc(a, {isolateScope: !0});
                        m.push(a)
                    }
                    if (b) {
                        c && (b = E(b, c, d));
                        b.require = G.require;
                        b.directiveName = oa;
                        if (K === G || G.$$isolateScope) b = tc(b, {isolateScope: !0});
                        n.push(b)
                    }
                }

                function C(a, b, c, d) {
                    var e, f = "data", g = !1;
                    if (z(b)) {
                        for (; "^" == (e = b.charAt(0)) || "?" == e;) b = b.substr(1), "^" == e && (f = "inheritedData"), g = g || "?" == e;
                        e = null;
                        d && "data" === f && (e = d[b]);
                        e = e || c[f]("$" + b + "Controller");
                        if (!e && !g) throw ia("ctreq", b, a);
                    } else H(b) && (e = [], r(b, function (b) {
                        e.push(C(a, b, c, d))
                    }));
                    return e
                }

                function O(a, e, f, g, p) {
                    function L(a, b) {
                        var c;
                        2 >
                        arguments.length && (b = a, a = t);
                        Ea && (c = ea);
                        return p(a, b, c)
                    }

                    var v, M, w, I, E, J, ea = {}, qb;
                    v = c === f ? d : ga(d, new Ob(u(f), d.$attr));
                    M = v.$$element;
                    if (K) {
                        var Na = /^\s*([@=&])(\??)\s*(\w*)\s*$/;
                        J = e.$new(!0);
                        !F || F !== K && F !== K.$$originalDirective ? M.data("$isolateScopeNoTemplate", J) : M.data("$isolateScope", J);
                        da(M, "ng-isolate-scope");
                        r(K.scope, function (a, c) {
                            var d = a.match(Na) || [], f = d[3] || c, g = "?" == d[2], d = d[1], m, l, n, p;
                            J.$$isolateBindings[c] = d + f;
                            switch (d) {
                                case "@":
                                    v.$observe(f, function (a) {
                                        J[c] = a
                                    });
                                    v.$$observers[f].$$scope = e;
                                    v[f] && (J[c] = b(v[f])(e));
                                    break;
                                case "=":
                                    if (g && !v[f]) break;
                                    l = q(v[f]);
                                    p = l.literal ? za : function (a, b) {
                                        return a === b || a !== a && b !== b
                                    };
                                    n = l.assign || function () {
                                        m = J[c] = l(e);
                                        throw ia("nonassign", v[f], K.name);
                                    };
                                    m = J[c] = l(e);
                                    J.$watch(function () {
                                        var a = l(e);
                                        p(a, J[c]) || (p(a, m) ? n(e, a = J[c]) : J[c] = a);
                                        return m = a
                                    }, null, l.literal);
                                    break;
                                case "&":
                                    l = q(v[f]);
                                    J[c] = function (a) {
                                        return l(e, a)
                                    };
                                    break;
                                default:
                                    throw ia("iscp", K.name, c, a);
                            }
                        })
                    }
                    qb = p && L;
                    W && r(W, function (a) {
                        var b = {$scope: a === K || a.$$isolateScope ? J : e, $element: M, $attrs: v, $transclude: qb},
                            c;
                        E = a.controller;
                        "@" == E && (E = v[a.name]);
                        c = s(E, b);
                        ea[a.name] = c;
                        Ea || M.data("$" + a.name + "Controller", c);
                        a.controllerAs && (b.$scope[a.controllerAs] = c)
                    });
                    g = 0;
                    for (w = m.length; g < w; g++) try {
                        I = m[g], I(I.isolateScope ? J : e, M, v, I.require && C(I.directiveName, I.require, M, ea), qb)
                    } catch (ca) {
                        l(ca, ha(M))
                    }
                    g = e;
                    K && (K.template || null === K.templateUrl) && (g = J);
                    a && a(g, f.childNodes, t, p);
                    for (g = n.length - 1; 0 <= g; g--) try {
                        I = n[g], I(I.isolateScope ? J : e, M, v, I.require && C(I.directiveName, I.require, M, ea), qb)
                    } catch (pb) {
                        l(pb, ha(M))
                    }
                }

                p = p || {};
                for (var v =
                    -Number.MAX_VALUE, I, W = p.controllerDirectives, K = p.newIsolateScopeDirective, F = p.templateDirective, ca = p.nonTlbTranscludeDirective, D = !1, B = !1, Ea = p.hasElementTranscludeDirective, x = d.$$element = u(c), G, oa, U, S = e, R, Q = 0, pa = a.length; Q < pa; Q++) {
                    G = a[Q];
                    var V = G.$$start, Y = G.$$end;
                    V && (x = J(c, V, Y));
                    U = t;
                    if (v > G.priority) break;
                    if (U = G.scope) I = I || G, G.templateUrl || (N("new/isolated scope", K, G, x), T(U) && (K = G));
                    oa = G.name;
                    !G.templateUrl && G.controller && (U = G.controller, W = W || {}, N("'" + oa + "' controller", W[oa], G, x), W[oa] = G);
                    if (U = G.transclude) D =
                        !0, G.$$tlb || (N("transclusion", ca, G, x), ca = G), "element" == U ? (Ea = !0, v = G.priority, U = x, x = d.$$element = u(X.createComment(" " + oa + ": " + d[oa] + " ")), c = x[0], Na(f, Aa.call(U, 0), c), S = w(U, e, v, g && g.name, {nonTlbTranscludeDirective: ca})) : (U = u(Kb(c)).contents(), x.empty(), S = w(U, e));
                    if (G.template) if (B = !0, N("template", F, G, x), F = G, U = P(G.template) ? G.template(x, d) : G.template, U = Z(U), G.replace) {
                        g = G;
                        U = Ib.test(U) ? u(aa(U)) : [];
                        c = U[0];
                        if (1 != U.length || 1 !== c.nodeType) throw ia("tplrt", oa, "");
                        Na(f, x, c);
                        pa = {$attr: {}};
                        U = ea(c, [], pa);
                        var $ =
                            a.splice(Q + 1, a.length - (Q + 1));
                        K && pb(U);
                        a = a.concat(U).concat($);
                        A(d, pa);
                        pa = a.length
                    } else x.html(U);
                    if (G.templateUrl) B = !0, N("template", F, G, x), F = G, G.replace && (g = G), O = y(a.splice(Q, a.length - Q), x, d, f, D && S, m, n, {
                        controllerDirectives: W,
                        newIsolateScopeDirective: K,
                        templateDirective: F,
                        nonTlbTranscludeDirective: ca
                    }), pa = a.length; else if (G.compile) try {
                        R = G.compile(x, d, S), P(R) ? L(null, R, V, Y) : R && L(R.pre, R.post, V, Y)
                    } catch (ba) {
                        l(ba, ha(x))
                    }
                    G.terminal && (O.terminal = !0, v = Math.max(v, G.priority))
                }
                O.scope = I && !0 === I.scope;
                O.transcludeOnThisElement =
                    D;
                O.templateOnThisElement = B;
                O.transclude = S;
                p.hasElementTranscludeDirective = Ea;
                return O
            }

            function pb(a) {
                for (var b = 0, c = a.length; b < c; b++) a[b] = bc(a[b], {$$isolateScope: !0})
            }

            function ca(b, e, f, g, h, q, n) {
                if (e === h) return null;
                h = null;
                if (c.hasOwnProperty(e)) {
                    var p;
                    e = a.get(e + d);
                    for (var s = 0, v = e.length; s < v; s++) try {
                        p = e[s], (g === t || g > p.priority) && -1 != p.restrict.indexOf(f) && (q && (p = bc(p, {
                            $$start: q,
                            $$end: n
                        })), b.push(p), h = p)
                    } catch (L) {
                        l(L)
                    }
                }
                return h
            }

            function A(a, b) {
                var c = b.$attr, d = a.$attr, e = a.$$element;
                r(a, function (d, e) {
                    "$" !=
                    e.charAt(0) && (b[e] && b[e] !== d && (d += ("style" === e ? ";" : " ") + b[e]), a.$set(e, d, !0, c[e]))
                });
                r(b, function (b, f) {
                    "class" == f ? (da(e, b), a["class"] = (a["class"] ? a["class"] + " " : "") + b) : "style" == f ? (e.attr("style", e.attr("style") + ";" + b), a.style = (a.style ? a.style + ";" : "") + b) : "$" == f.charAt(0) || a.hasOwnProperty(f) || (a[f] = b, d[f] = c[f])
                })
            }

            function y(a, b, c, d, e, f, g, m) {
                var h = [], l, q, s = b[0], v = a.shift(),
                    L = B({}, v, {templateUrl: null, transclude: null, replace: null, $$originalDirective: v}),
                    O = P(v.templateUrl) ? v.templateUrl(b, c) : v.templateUrl;
                b.empty();
                n.get(C.getTrustedResourceUrl(O), {cache: p}).success(function (n) {
                    var p, C;
                    n = Z(n);
                    if (v.replace) {
                        n = Ib.test(n) ? u(aa(n)) : [];
                        p = n[0];
                        if (1 != n.length || 1 !== p.nodeType) throw ia("tplrt", v.name, O);
                        n = {$attr: {}};
                        Na(d, b, p);
                        var w = ea(p, [], n);
                        T(v.scope) && pb(w);
                        a = w.concat(a);
                        A(c, n)
                    } else p = s, b.html(n);
                    a.unshift(L);
                    l = F(a, p, c, e, b, v, f, g, m);
                    r(d, function (a, c) {
                        a == p && (d[c] = b[0])
                    });
                    for (q = K(b[0].childNodes, e); h.length;) {
                        n = h.shift();
                        C = h.shift();
                        var I = h.shift(), E = h.shift(), w = b[0];
                        if (C !== s) {
                            var J = C.className;
                            m.hasElementTranscludeDirective &&
                            v.replace || (w = Kb(p));
                            Na(I, u(C), w);
                            da(u(w), J)
                        }
                        C = l.transcludeOnThisElement ? W(n, l.transclude, E) : E;
                        l(q, n, w, d, C)
                    }
                    h = null
                }).error(function (a, b, c, d) {
                    throw ia("tpload", d.url);
                });
                return function (a, b, c, d, e) {
                    a = e;
                    h ? (h.push(b), h.push(c), h.push(d), h.push(a)) : (l.transcludeOnThisElement && (a = W(b, l.transclude, e)), l(q, b, c, d, a))
                }
            }

            function D(a, b) {
                var c = b.priority - a.priority;
                return 0 !== c ? c : a.name !== b.name ? a.name < b.name ? -1 : 1 : a.index - b.index
            }

            function N(a, b, c, d) {
                if (b) throw ia("multidir", b.name, c.name, a, ha(d));
            }

            function x(a,
                       c) {
                var d = b(c, !0);
                d && a.push({
                    priority: 0, compile: function (a) {
                        var b = a.parent().length;
                        b && da(a.parent(), "ng-binding");
                        return function (a, c) {
                            var e = c.parent(), f = e.data("$binding") || [];
                            f.push(d);
                            e.data("$binding", f);
                            b || da(e, "ng-binding");
                            a.$watch(d, function (a) {
                                c[0].nodeValue = a
                            })
                        }
                    }
                })
            }

            function S(a, b) {
                if ("srcdoc" == b) return C.HTML;
                var c = Ma(a);
                if ("xlinkHref" == b || "FORM" == c && "action" == b || "IMG" != c && ("src" == b || "ngSrc" == b)) return C.RESOURCE_URL
            }

            function Q(a, c, d, e) {
                var f = b(d, !0);
                if (f) {
                    if ("multiple" === e && "SELECT" ===
                        Ma(a)) throw ia("selmulti", ha(a));
                    c.push({
                        priority: 100, compile: function () {
                            return {
                                pre: function (c, d, m) {
                                    d = m.$$observers || (m.$$observers = {});
                                    if (g.test(e)) throw ia("nodomevents");
                                    if (f = b(m[e], !0, S(a, e))) m[e] = f(c), (d[e] || (d[e] = [])).$$inter = !0, (m.$$observers && m.$$observers[e].$$scope || c).$watch(f, function (a, b) {
                                        "class" === e && a != b ? m.$updateClass(a, b) : m.$set(e, a)
                                    })
                                }
                            }
                        }
                    })
                }
            }

            function Na(a, b, c) {
                var d = b[0], e = b.length, f = d.parentNode, g, m;
                if (a) for (g = 0, m = a.length; g < m; g++) if (a[g] == d) {
                    a[g++] = c;
                    m = g + e - 1;
                    for (var h = a.length; g <
                    h; g++, m++) m < h ? a[g] = a[m] : delete a[g];
                    a.length -= e - 1;
                    break
                }
                f && f.replaceChild(c, d);
                a = X.createDocumentFragment();
                a.appendChild(d);
                c[u.expando] = d[u.expando];
                d = 1;
                for (e = b.length; d < e; d++) f = b[d], u(f).remove(), a.appendChild(f), delete b[d];
                b[0] = c;
                b.length = 1
            }

            function tc(a, b) {
                return B(function () {
                    return a.apply(null, arguments)
                }, a, b)
            }

            var Ob = function (a, b) {
                this.$$element = a;
                this.$attr = b || {}
            };
            Ob.prototype = {
                $normalize: na, $addClass: function (a) {
                    a && 0 < a.length && O.addClass(this.$$element, a)
                }, $removeClass: function (a) {
                    a && 0 <
                    a.length && O.removeClass(this.$$element, a)
                }, $updateClass: function (a, b) {
                    var c = uc(a, b), d = uc(b, a);
                    0 === c.length ? O.removeClass(this.$$element, d) : 0 === d.length ? O.addClass(this.$$element, c) : O.setClass(this.$$element, c, d)
                }, $set: function (a, b, c, d) {
                    var e = qc(this.$$element[0], a);
                    e && (this.$$element.prop(a, b), d = e);
                    this[a] = b;
                    d ? this.$attr[a] = d : (d = this.$attr[a]) || (this.$attr[a] = d = kb(a, "-"));
                    e = Ma(this.$$element);
                    if ("A" === e && "href" === a || "IMG" === e && "src" === a) this[a] = b = I(b, "src" === a);
                    !1 !== c && (null === b || b === t ? this.$$element.removeAttr(d) :
                        this.$$element.attr(d, b));
                    (c = this.$$observers) && r(c[a], function (a) {
                        try {
                            a(b)
                        } catch (c) {
                            l(c)
                        }
                    })
                }, $observe: function (a, b) {
                    var c = this, d = c.$$observers || (c.$$observers = {}), e = d[a] || (d[a] = []);
                    e.push(b);
                    L.$evalAsync(function () {
                        e.$$inter || b(c[a])
                    });
                    return b
                }
            };
            var pa = b.startSymbol(), Ea = b.endSymbol(), Z = "{{" == pa || "}}" == Ea ? Ga : function (a) {
                return a.replace(/\{\{/g, pa).replace(/}}/g, Ea)
            }, V = /^ngAttr[A-Z]/;
            return w
        }]
    }

    function na(b) {
        return Ya(b.replace(ue, ""))
    }

    function uc(b, a) {
        var c = "", d = b.split(/\s+/), e = a.split(/\s+/),
            f = 0;
        a:for (; f < d.length; f++) {
            for (var g = d[f], k = 0; k < e.length; k++) if (g == e[k]) continue a;
            c += (0 < c.length ? " " : "") + g
        }
        return c
    }

    function Pd() {
        var b = {}, a = /^(\S+)(\s+as\s+(\w+))?$/;
        this.register = function (a, d) {
            Ca(a, "controller");
            T(a) ? B(b, a) : b[a] = d
        };
        this.$get = ["$injector", "$window", function (c, d) {
            return function (e, f) {
                var g, k, m;
                z(e) && (g = e.match(a), k = g[1], m = g[3], e = b.hasOwnProperty(k) ? b[k] : hc(f.$scope, k, !0) || hc(d, k, !0), Va(e, k, !0));
                g = c.instantiate(e, f);
                if (m) {
                    if (!f || "object" !== typeof f.$scope) throw x("$controller")("noscp",
                        k || e.name, m);
                    f.$scope[m] = g
                }
                return g
            }
        }]
    }

    function Qd() {
        this.$get = ["$window", function (b) {
            return u(b.document)
        }]
    }

    function Rd() {
        this.$get = ["$log", function (b) {
            return function (a, c) {
                b.error.apply(b, arguments)
            }
        }]
    }

    function vc(b) {
        var a = {}, c, d, e;
        if (!b) return a;
        r(b.split("\n"), function (b) {
            e = b.indexOf(":");
            c = N(aa(b.substr(0, e)));
            d = aa(b.substr(e + 1));
            c && (a[c] = a[c] ? a[c] + ", " + d : d)
        });
        return a
    }

    function wc(b) {
        var a = T(b) ? b : t;
        return function (c) {
            a || (a = vc(b));
            return c ? a[N(c)] || null : a
        }
    }

    function xc(b, a, c) {
        if (P(c)) return c(b,
            a);
        r(c, function (c) {
            b = c(b, a)
        });
        return b
    }

    function Ud() {
        var b = /^\s*(\[|\{[^\{])/, a = /[\}\]]\s*$/, c = /^\)\]\}',?\n/,
            d = {"Content-Type": "application/json;charset=utf-8"}, e = this.defaults = {
                transformResponse: [function (d) {
                    z(d) && (d = d.replace(c, ""), b.test(d) && a.test(d) && (d = cc(d)));
                    return d
                }],
                transformRequest: [function (a) {
                    return T(a) && "[object File]" !== ya.call(a) && "[object Blob]" !== ya.call(a) ? ta(a) : a
                }],
                headers: {common: {Accept: "application/json, text/plain, */*"}, post: ga(d), put: ga(d), patch: ga(d)},
                xsrfCookieName: "XSRF-TOKEN",
                xsrfHeaderName: "X-XSRF-TOKEN"
            }, f = this.interceptors = [], g = this.responseInterceptors = [];
        this.$get = ["$httpBackend", "$browser", "$cacheFactory", "$rootScope", "$q", "$injector", function (a, b, c, d, n, p) {
            function q(a) {
                function b(a) {
                    var d = B({}, a, {data: xc(a.data, a.headers, c.transformResponse)});
                    return 200 <= a.status && 300 > a.status ? d : n.reject(d)
                }

                var c = {method: "get", transformRequest: e.transformRequest, transformResponse: e.transformResponse},
                    d = function (a) {
                        var b = e.headers, c = B({}, a.headers), d, f, b = B({}, b.common, b[N(a.method)]);
                        a:for (d in b) {
                            a = N(d);
                            for (f in c) if (N(f) === a) continue a;
                            c[d] = b[d]
                        }
                        (function (a) {
                            var b;
                            r(a, function (c, d) {
                                P(c) && (b = c(), null != b ? a[d] = b : delete a[d])
                            })
                        })(c);
                        return c
                    }(a);
                B(c, a);
                c.headers = d;
                c.method = Ia(c.method);
                var f = [function (a) {
                    d = a.headers;
                    var c = xc(a.data, wc(d), a.transformRequest);
                    D(c) && r(d, function (a, b) {
                        "content-type" === N(b) && delete d[b]
                    });
                    D(a.withCredentials) && !D(e.withCredentials) && (a.withCredentials = e.withCredentials);
                    return s(a, c, d).then(b, b)
                }, t], g = n.when(c);
                for (r(C, function (a) {
                    (a.request || a.requestError) &&
                    f.unshift(a.request, a.requestError);
                    (a.response || a.responseError) && f.push(a.response, a.responseError)
                }); f.length;) {
                    a = f.shift();
                    var m = f.shift(), g = g.then(a, m)
                }
                g.success = function (a) {
                    g.then(function (b) {
                        a(b.data, b.status, b.headers, c)
                    });
                    return g
                };
                g.error = function (a) {
                    g.then(null, function (b) {
                        a(b.data, b.status, b.headers, c)
                    });
                    return g
                };
                return g
            }

            function s(c, f, g) {
                function h(a, b, c, e) {
                    E && (200 <= a && 300 > a ? E.put(u, [a, b, vc(c), e]) : E.remove(u));
                    p(b, a, c, e);
                    d.$$phase || d.$apply()
                }

                function p(a, b, d, e) {
                    b = Math.max(b, 0);
                    (200 <=
                    b && 300 > b ? C.resolve : C.reject)({data: a, status: b, headers: wc(d), config: c, statusText: e})
                }

                function s() {
                    var a = Qa(q.pendingRequests, c);
                    -1 !== a && q.pendingRequests.splice(a, 1)
                }

                var C = n.defer(), r = C.promise, E, F, u = L(c.url, c.params);
                q.pendingRequests.push(c);
                r.then(s, s);
                !c.cache && !e.cache || (!1 === c.cache || "GET" !== c.method && "JSONP" !== c.method) || (E = T(c.cache) ? c.cache : T(e.cache) ? e.cache : v);
                if (E) if (F = E.get(u), A(F)) {
                    if (F && P(F.then)) return F.then(s, s), F;
                    H(F) ? p(F[1], F[0], ga(F[2]), F[3]) : p(F, 200, {}, "OK")
                } else E.put(u, r);
                D(F) &&
                ((F = Pb(c.url) ? b.cookies()[c.xsrfCookieName || e.xsrfCookieName] : t) && (g[c.xsrfHeaderName || e.xsrfHeaderName] = F), a(c.method, u, f, h, g, c.timeout, c.withCredentials, c.responseType));
                return r
            }

            function L(a, b) {
                if (!b) return a;
                var c = [];
                Tc(b, function (a, b) {
                    null === a || D(a) || (H(a) || (a = [a]), r(a, function (a) {
                        T(a) && (sa(a) ? a = a.toISOString() : T(a) && (a = ta(a)));
                        c.push(Ba(b) + "=" + Ba(a))
                    }))
                });
                0 < c.length && (a += (-1 == a.indexOf("?") ? "?" : "&") + c.join("&"));
                return a
            }

            var v = c("$http"), C = [];
            r(f, function (a) {
                C.unshift(z(a) ? p.get(a) : p.invoke(a))
            });
            r(g, function (a, b) {
                var c = z(a) ? p.get(a) : p.invoke(a);
                C.splice(b, 0, {
                    response: function (a) {
                        return c(n.when(a))
                    }, responseError: function (a) {
                        return c(n.reject(a))
                    }
                })
            });
            q.pendingRequests = [];
            (function (a) {
                r(arguments, function (a) {
                    q[a] = function (b, c) {
                        return q(B(c || {}, {method: a, url: b}))
                    }
                })
            })("get", "delete", "head", "jsonp");
            (function (a) {
                r(arguments, function (a) {
                    q[a] = function (b, c, d) {
                        return q(B(d || {}, {method: a, url: b, data: c}))
                    }
                })
            })("post", "put");
            q.defaults = e;
            return q
        }]
    }

    function ve(b) {
        if (8 >= R && (!b.match(/^(get|post|head|put|delete|options)$/i) ||
            !Q.XMLHttpRequest)) return new Q.ActiveXObject("Microsoft.XMLHTTP");
        if (Q.XMLHttpRequest) return new Q.XMLHttpRequest;
        throw x("$httpBackend")("noxhr");
    }

    function Vd() {
        this.$get = ["$browser", "$window", "$document", function (b, a, c) {
            return we(b, ve, b.defer, a.angular.callbacks, c[0])
        }]
    }

    function we(b, a, c, d, e) {
        function f(a, b, c) {
            var f = e.createElement("script"), g = null;
            f.type = "text/javascript";
            f.src = a;
            f.async = !0;
            g = function (a) {
                Za(f, "load", g);
                Za(f, "error", g);
                e.body.removeChild(f);
                f = null;
                var k = -1, s = "unknown";
                a && ("load" !==
                a.type || d[b].called || (a = {type: "error"}), s = a.type, k = "error" === a.type ? 404 : 200);
                c && c(k, s)
            };
            rb(f, "load", g);
            rb(f, "error", g);
            8 >= R && (f.onreadystatechange = function () {
                z(f.readyState) && /loaded|complete/.test(f.readyState) && (f.onreadystatechange = null, g({type: "load"}))
            });
            e.body.appendChild(f);
            return g
        }

        var g = -1;
        return function (e, m, h, l, n, p, q, s) {
            function L() {
                C = g;
                I && I();
                w && w.abort()
            }

            function v(a, d, e, f, g) {
                K && c.cancel(K);
                I = w = null;
                0 === d && (d = e ? 200 : "file" == ua(m).protocol ? 404 : 0);
                a(1223 === d ? 204 : d, e, f, g || "");
                b.$$completeOutstandingRequest(y)
            }

            var C;
            b.$$incOutstandingRequestCount();
            m = m || b.url();
            if ("jsonp" == N(e)) {
                var O = "_" + (d.counter++).toString(36);
                d[O] = function (a) {
                    d[O].data = a;
                    d[O].called = !0
                };
                var I = f(m.replace("JSON_CALLBACK", "angular.callbacks." + O), O, function (a, b) {
                    v(l, a, d[O].data, "", b);
                    d[O] = y
                })
            } else {
                var w = a(e);
                w.open(e, m, !0);
                r(n, function (a, b) {
                    A(a) && w.setRequestHeader(b, a)
                });
                w.onreadystatechange = function () {
                    if (w && 4 == w.readyState) {
                        var a = null, b = null, c = "";
                        C !== g && (a = w.getAllResponseHeaders(), b = "response" in w ? w.response : w.responseText);
                        C === g &&
                        10 > R || (c = w.statusText);
                        v(l, C || w.status, b, a, c)
                    }
                };
                q && (w.withCredentials = !0);
                if (s) try {
                    w.responseType = s
                } catch (da) {
                    if ("json" !== s) throw da;
                }
                w.send(h || null)
            }
            if (0 < p) var K = c(L, p); else p && P(p.then) && p.then(L)
        }
    }

    function Sd() {
        var b = "{{", a = "}}";
        this.startSymbol = function (a) {
            return a ? (b = a, this) : b
        };
        this.endSymbol = function (b) {
            return b ? (a = b, this) : a
        };
        this.$get = ["$parse", "$exceptionHandler", "$sce", function (c, d, e) {
            function f(f, h, l) {
                for (var n, p, q = 0, s = [], L = f.length, v = !1, C = []; q < L;) -1 != (n = f.indexOf(b, q)) && -1 != (p = f.indexOf(a,
                    n + g)) ? (q != n && s.push(f.substring(q, n)), s.push(q = c(v = f.substring(n + g, p))), q.exp = v, q = p + k, v = !0) : (q != L && s.push(f.substring(q)), q = L);
                (L = s.length) || (s.push(""), L = 1);
                if (l && 1 < s.length) throw yc("noconcat", f);
                if (!h || v) return C.length = L, q = function (a) {
                    try {
                        for (var b = 0, c = L, g; b < c; b++) {
                            if ("function" == typeof (g = s[b])) if (g = g(a), g = l ? e.getTrusted(l, g) : e.valueOf(g), null == g) g = ""; else switch (typeof g) {
                                case "string":
                                    break;
                                case "number":
                                    g = "" + g;
                                    break;
                                default:
                                    g = ta(g)
                            }
                            C[b] = g
                        }
                        return C.join("")
                    } catch (k) {
                        a = yc("interr", f, k.toString()),
                            d(a)
                    }
                }, q.exp = f, q.parts = s, q
            }

            var g = b.length, k = a.length;
            f.startSymbol = function () {
                return b
            };
            f.endSymbol = function () {
                return a
            };
            return f
        }]
    }

    function Td() {
        this.$get = ["$rootScope", "$window", "$q", function (b, a, c) {
            function d(d, g, k, m) {
                var h = a.setInterval, l = a.clearInterval, n = c.defer(), p = n.promise, q = 0, s = A(m) && !m;
                k = A(k) ? k : 0;
                p.then(null, null, d);
                p.$$intervalId = h(function () {
                    n.notify(q++);
                    0 < k && q >= k && (n.resolve(q), l(p.$$intervalId), delete e[p.$$intervalId]);
                    s || b.$apply()
                }, g);
                e[p.$$intervalId] = n;
                return p
            }

            var e = {};
            d.cancel =
                function (b) {
                    return b && b.$$intervalId in e ? (e[b.$$intervalId].reject("canceled"), a.clearInterval(b.$$intervalId), delete e[b.$$intervalId], !0) : !1
                };
            return d
        }]
    }

    function bd() {
        this.$get = function () {
            return {
                id: "en-us",
                NUMBER_FORMATS: {
                    DECIMAL_SEP: ".",
                    GROUP_SEP: ",",
                    PATTERNS: [{
                        minInt: 1,
                        minFrac: 0,
                        maxFrac: 3,
                        posPre: "",
                        posSuf: "",
                        negPre: "-",
                        negSuf: "",
                        gSize: 3,
                        lgSize: 3
                    }, {
                        minInt: 1,
                        minFrac: 2,
                        maxFrac: 2,
                        posPre: "\u00a4",
                        posSuf: "",
                        negPre: "(\u00a4",
                        negSuf: ")",
                        gSize: 3,
                        lgSize: 3
                    }],
                    CURRENCY_SYM: "$"
                },
                DATETIME_FORMATS: {
                    MONTH: "January February March April May June July August September October November December".split(" "),
                    SHORTMONTH: "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split(" "),
                    DAY: "Sunday Monday Tuesday Wednesday Thursday Friday Saturday".split(" "),
                    SHORTDAY: "Sun Mon Tue Wed Thu Fri Sat".split(" "),
                    AMPMS: ["AM", "PM"],
                    medium: "MMM d, y h:mm:ss a",
                    "short": "M/d/yy h:mm a",
                    fullDate: "EEEE, MMMM d, y",
                    longDate: "MMMM d, y",
                    mediumDate: "MMM d, y",
                    shortDate: "M/d/yy",
                    mediumTime: "h:mm:ss a",
                    shortTime: "h:mm a"
                },
                pluralCat: function (b) {
                    return 1 === b ? "one" : "other"
                }
            }
        }
    }

    function Qb(b) {
        b = b.split("/");
        for (var a = b.length; a--;) b[a] =
            jb(b[a]);
        return b.join("/")
    }

    function zc(b, a, c) {
        b = ua(b, c);
        a.$$protocol = b.protocol;
        a.$$host = b.hostname;
        a.$$port = Z(b.port) || xe[b.protocol] || null
    }

    function Ac(b, a, c) {
        var d = "/" !== b.charAt(0);
        d && (b = "/" + b);
        b = ua(b, c);
        a.$$path = decodeURIComponent(d && "/" === b.pathname.charAt(0) ? b.pathname.substring(1) : b.pathname);
        a.$$search = ec(b.search);
        a.$$hash = decodeURIComponent(b.hash);
        a.$$path && "/" != a.$$path.charAt(0) && (a.$$path = "/" + a.$$path)
    }

    function qa(b, a) {
        if (0 === a.indexOf(b)) return a.substr(b.length)
    }

    function cb(b) {
        var a =
            b.indexOf("#");
        return -1 == a ? b : b.substr(0, a)
    }

    function Rb(b) {
        return b.substr(0, cb(b).lastIndexOf("/") + 1)
    }

    function Bc(b, a) {
        this.$$html5 = !0;
        a = a || "";
        var c = Rb(b);
        zc(b, this, b);
        this.$$parse = function (a) {
            var e = qa(c, a);
            if (!z(e)) throw Sb("ipthprfx", a, c);
            Ac(e, this, b);
            this.$$path || (this.$$path = "/");
            this.$$compose()
        };
        this.$$compose = function () {
            var a = Cb(this.$$search), b = this.$$hash ? "#" + jb(this.$$hash) : "";
            this.$$url = Qb(this.$$path) + (a ? "?" + a : "") + b;
            this.$$absUrl = c + this.$$url.substr(1)
        };
        this.$$rewrite = function (d) {
            var e;
            if ((e = qa(b, d)) !== t) return d = e, (e = qa(a, e)) !== t ? c + (qa("/", e) || e) : b + d;
            if ((e = qa(c, d)) !== t) return c + e;
            if (c == d + "/") return c
        }
    }

    function Tb(b, a) {
        var c = Rb(b);
        zc(b, this, b);
        this.$$parse = function (d) {
            var e = qa(b, d) || qa(c, d), e = "#" == e.charAt(0) ? qa(a, e) : this.$$html5 ? e : "";
            if (!z(e)) throw Sb("ihshprfx", d, a);
            Ac(e, this, b);
            d = this.$$path;
            var f = /^\/[A-Z]:(\/.*)/;
            0 === e.indexOf(b) && (e = e.replace(b, ""));
            f.exec(e) || (d = (e = f.exec(d)) ? e[1] : d);
            this.$$path = d;
            this.$$compose()
        };
        this.$$compose = function () {
            var c = Cb(this.$$search), e = this.$$hash ?
                "#" + jb(this.$$hash) : "";
            this.$$url = Qb(this.$$path) + (c ? "?" + c : "") + e;
            this.$$absUrl = b + (this.$$url ? a + this.$$url : "")
        };
        this.$$rewrite = function (a) {
            if (cb(b) == cb(a)) return a
        }
    }

    function Ub(b, a) {
        this.$$html5 = !0;
        Tb.apply(this, arguments);
        var c = Rb(b);
        this.$$rewrite = function (d) {
            var e;
            if (b == cb(d)) return d;
            if (e = qa(c, d)) return b + a + e;
            if (c === d + "/") return c
        };
        this.$$compose = function () {
            var c = Cb(this.$$search), e = this.$$hash ? "#" + jb(this.$$hash) : "";
            this.$$url = Qb(this.$$path) + (c ? "?" + c : "") + e;
            this.$$absUrl = b + a + this.$$url
        }
    }

    function sb(b) {
        return function () {
            return this[b]
        }
    }

    function Cc(b, a) {
        return function (c) {
            if (D(c)) return this[b];
            this[b] = a(c);
            this.$$compose();
            return this
        }
    }

    function Wd() {
        var b = "", a = !1;
        this.hashPrefix = function (a) {
            return A(a) ? (b = a, this) : b
        };
        this.html5Mode = function (b) {
            return A(b) ? (a = b, this) : a
        };
        this.$get = ["$rootScope", "$browser", "$sniffer", "$rootElement", function (c, d, e, f) {
            function g(a) {
                c.$broadcast("$locationChangeSuccess", k.absUrl(), a)
            }

            var k, m, h = d.baseHref(), l = d.url(), n;
            a ? (n = l.substring(0, l.indexOf("/", l.indexOf("//") + 2)) + (h || "/"), m = e.history ? Bc : Ub) : (n =
                cb(l), m = Tb);
            k = new m(n, "#" + b);
            k.$$parse(k.$$rewrite(l));
            var p = /^\s*(javascript|mailto):/i;
            f.on("click", function (a) {
                if (!a.ctrlKey && !a.metaKey && 2 != a.which) {
                    for (var e = u(a.target); "a" !== N(e[0].nodeName);) if (e[0] === f[0] || !(e = e.parent())[0]) return;
                    var g = e.prop("href");
                    T(g) && "[object SVGAnimatedString]" === g.toString() && (g = ua(g.animVal).href);
                    if (!p.test(g)) {
                        if (m === Ub) {
                            var h = e.attr("href") || e.attr("xlink:href");
                            if (h && 0 > h.indexOf("://")) if (g = "#" + b, "/" == h[0]) g = n + g + h; else if ("#" == h[0]) g = n + g + (k.path() || "/") + h;
                            else {
                                var l = k.path().split("/"), h = h.split("/");
                                2 !== l.length || l[1] || (l.length = 1);
                                for (var q = 0; q < h.length; q++) "." != h[q] && (".." == h[q] ? l.pop() : h[q].length && l.push(h[q]));
                                g = n + g + l.join("/")
                            }
                        }
                        l = k.$$rewrite(g);
                        g && (!e.attr("target") && l && !a.isDefaultPrevented()) && (a.preventDefault(), l != d.url() && (k.$$parse(l), c.$apply(), Q.angular["ff-684208-preventDefault"] = !0))
                    }
                }
            });
            k.absUrl() != l && d.url(k.absUrl(), !0);
            d.onUrlChange(function (a) {
                k.absUrl() != a && (c.$evalAsync(function () {
                    var b = k.absUrl();
                    k.$$parse(a);
                    c.$broadcast("$locationChangeStart",
                        a, b).defaultPrevented ? (k.$$parse(b), d.url(b)) : g(b)
                }), c.$$phase || c.$digest())
            });
            var q = 0;
            c.$watch(function () {
                var a = d.url(), b = k.$$replace;
                q && a == k.absUrl() || (q++, c.$evalAsync(function () {
                    c.$broadcast("$locationChangeStart", k.absUrl(), a).defaultPrevented ? k.$$parse(a) : (d.url(k.absUrl(), b), g(a))
                }));
                k.$$replace = !1;
                return q
            });
            return k
        }]
    }

    function Xd() {
        var b = !0, a = this;
        this.debugEnabled = function (a) {
            return A(a) ? (b = a, this) : b
        };
        this.$get = ["$window", function (c) {
            function d(a) {
                a instanceof Error && (a.stack ? a = a.message &&
                -1 === a.stack.indexOf(a.message) ? "Error: " + a.message + "\n" + a.stack : a.stack : a.sourceURL && (a = a.message + "\n" + a.sourceURL + ":" + a.line));
                return a
            }

            function e(a) {
                var b = c.console || {}, e = b[a] || b.log || y;
                a = !1;
                try {
                    a = !!e.apply
                } catch (m) {
                }
                return a ? function () {
                    var a = [];
                    r(arguments, function (b) {
                        a.push(d(b))
                    });
                    return e.apply(b, a)
                } : function (a, b) {
                    e(a, null == b ? "" : b)
                }
            }

            return {
                log: e("log"), info: e("info"), warn: e("warn"), error: e("error"), debug: function () {
                    var c = e("debug");
                    return function () {
                        b && c.apply(a, arguments)
                    }
                }()
            }
        }]
    }

    function ja(b,
                a) {
        if ("__defineGetter__" === b || "__defineSetter__" === b || "__lookupGetter__" === b || "__lookupSetter__" === b || "__proto__" === b) throw ka("isecfld", a);
        return b
    }

    function Oa(b, a) {
        if (b) {
            if (b.constructor === b) throw ka("isecfn", a);
            if (b.document && b.location && b.alert && b.setInterval) throw ka("isecwindow", a);
            if (b.children && (b.nodeName || b.prop && b.attr && b.find)) throw ka("isecdom", a);
            if (b === Object) throw ka("isecobj", a);
        }
        return b
    }

    function tb(b, a, c, d, e) {
        e = e || {};
        a = a.split(".");
        for (var f, g = 0; 1 < a.length; g++) {
            f = ja(a.shift(), d);
            var k = b[f];
            k || (k = {}, b[f] = k);
            b = k;
            b.then && e.unwrapPromises && (va(d), "$$v" in b || function (a) {
                a.then(function (b) {
                    a.$$v = b
                })
            }(b), b.$$v === t && (b.$$v = {}), b = b.$$v)
        }
        f = ja(a.shift(), d);
        Oa(b, d);
        Oa(b[f], d);
        return b[f] = c
    }

    function Dc(b, a, c, d, e, f, g) {
        ja(b, f);
        ja(a, f);
        ja(c, f);
        ja(d, f);
        ja(e, f);
        return g.unwrapPromises ? function (g, m) {
            var h = m && m.hasOwnProperty(b) ? m : g, l;
            if (null == h) return h;
            (h = h[b]) && h.then && (va(f), "$$v" in h || (l = h, l.$$v = t, l.then(function (a) {
                l.$$v = a
            })), h = h.$$v);
            if (!a) return h;
            if (null == h) return t;
            (h = h[a]) && h.then &&
            (va(f), "$$v" in h || (l = h, l.$$v = t, l.then(function (a) {
                l.$$v = a
            })), h = h.$$v);
            if (!c) return h;
            if (null == h) return t;
            (h = h[c]) && h.then && (va(f), "$$v" in h || (l = h, l.$$v = t, l.then(function (a) {
                l.$$v = a
            })), h = h.$$v);
            if (!d) return h;
            if (null == h) return t;
            (h = h[d]) && h.then && (va(f), "$$v" in h || (l = h, l.$$v = t, l.then(function (a) {
                l.$$v = a
            })), h = h.$$v);
            if (!e) return h;
            if (null == h) return t;
            (h = h[e]) && h.then && (va(f), "$$v" in h || (l = h, l.$$v = t, l.then(function (a) {
                l.$$v = a
            })), h = h.$$v);
            return h
        } : function (f, g) {
            var h = g && g.hasOwnProperty(b) ? g : f;
            if (null ==
                h) return h;
            h = h[b];
            if (!a) return h;
            if (null == h) return t;
            h = h[a];
            if (!c) return h;
            if (null == h) return t;
            h = h[c];
            if (!d) return h;
            if (null == h) return t;
            h = h[d];
            return e ? null == h ? t : h = h[e] : h
        }
    }

    function Ec(b, a, c) {
        if (Vb.hasOwnProperty(b)) return Vb[b];
        var d = b.split("."), e = d.length, f;
        if (a.csp) f = 6 > e ? Dc(d[0], d[1], d[2], d[3], d[4], c, a) : function (b, f) {
            var g = 0, k;
            do k = Dc(d[g++], d[g++], d[g++], d[g++], d[g++], c, a)(b, f), f = t, b = k; while (g < e);
            return k
        }; else {
            var g = "var p;\n";
            r(d, function (b, d) {
                ja(b, c);
                g += "if(s == null) return undefined;\ns=" +
                    (d ? "s" : '((k&&k.hasOwnProperty("' + b + '"))?k:s)') + '["' + b + '"];\n' + (a.unwrapPromises ? 'if (s && s.then) {\n pw("' + c.replace(/(["\r\n])/g, "\\$1") + '");\n if (!("$$v" in s)) {\n p=s;\n p.$$v = undefined;\n p.then(function(v) {p.$$v=v;});\n}\n s=s.$$v\n}\n' : "")
            });
            var g = g + "return s;", k = new Function("s", "k", "pw", g);
            k.toString = $(g);
            f = a.unwrapPromises ? function (a, b) {
                return k(a, b, va)
            } : k
        }
        "hasOwnProperty" !== b && (Vb[b] = f);
        return f
    }

    function Yd() {
        var b = {}, a = {csp: !1, unwrapPromises: !1, logPromiseWarnings: !0};
        this.unwrapPromises =
            function (b) {
                return A(b) ? (a.unwrapPromises = !!b, this) : a.unwrapPromises
            };
        this.logPromiseWarnings = function (b) {
            return A(b) ? (a.logPromiseWarnings = b, this) : a.logPromiseWarnings
        };
        this.$get = ["$filter", "$sniffer", "$log", function (c, d, e) {
            a.csp = d.csp;
            va = function (b) {
                a.logPromiseWarnings && !Fc.hasOwnProperty(b) && (Fc[b] = !0, e.warn("[$parse] Promise found in the expression `" + b + "`. Automatic unwrapping of promises in Angular expressions is deprecated."))
            };
            return function (d) {
                var e;
                switch (typeof d) {
                    case "string":
                        if (b.hasOwnProperty(d)) return b[d];
                        e = new Wb(a);
                        e = (new db(e, c, a)).parse(d);
                        "hasOwnProperty" !== d && (b[d] = e);
                        return e;
                    case "function":
                        return d;
                    default:
                        return y
                }
            }
        }]
    }

    function $d() {
        this.$get = ["$rootScope", "$exceptionHandler", function (b, a) {
            return ye(function (a) {
                b.$evalAsync(a)
            }, a)
        }]
    }

    function ye(b, a) {
        function c(a) {
            return a
        }

        function d(a) {
            return g(a)
        }

        var e = function () {
            var g = [], h, l;
            return l = {
                resolve: function (a) {
                    if (g) {
                        var c = g;
                        g = t;
                        h = f(a);
                        c.length && b(function () {
                            for (var a, b = 0, d = c.length; b < d; b++) a = c[b], h.then(a[0], a[1], a[2])
                        })
                    }
                }, reject: function (a) {
                    l.resolve(k(a))
                },
                notify: function (a) {
                    if (g) {
                        var c = g;
                        g.length && b(function () {
                            for (var b, d = 0, e = c.length; d < e; d++) b = c[d], b[2](a)
                        })
                    }
                }, promise: {
                    then: function (b, f, k) {
                        var l = e(), L = function (d) {
                            try {
                                l.resolve((P(b) ? b : c)(d))
                            } catch (e) {
                                l.reject(e), a(e)
                            }
                        }, v = function (b) {
                            try {
                                l.resolve((P(f) ? f : d)(b))
                            } catch (c) {
                                l.reject(c), a(c)
                            }
                        }, C = function (b) {
                            try {
                                l.notify((P(k) ? k : c)(b))
                            } catch (d) {
                                a(d)
                            }
                        };
                        g ? g.push([L, v, C]) : h.then(L, v, C);
                        return l.promise
                    }, "catch": function (a) {
                        return this.then(null, a)
                    }, "finally": function (a) {
                        function b(a, c) {
                            var d = e();
                            c ? d.resolve(a) :
                                d.reject(a);
                            return d.promise
                        }

                        function d(e, f) {
                            var g = null;
                            try {
                                g = (a || c)()
                            } catch (k) {
                                return b(k, !1)
                            }
                            return g && P(g.then) ? g.then(function () {
                                return b(e, f)
                            }, function (a) {
                                return b(a, !1)
                            }) : b(e, f)
                        }

                        return this.then(function (a) {
                            return d(a, !0)
                        }, function (a) {
                            return d(a, !1)
                        })
                    }
                }
            }
        }, f = function (a) {
            return a && P(a.then) ? a : {
                then: function (c) {
                    var d = e();
                    b(function () {
                        d.resolve(c(a))
                    });
                    return d.promise
                }
            }
        }, g = function (a) {
            var b = e();
            b.reject(a);
            return b.promise
        }, k = function (c) {
            return {
                then: function (f, g) {
                    var k = e();
                    b(function () {
                        try {
                            k.resolve((P(g) ?
                                g : d)(c))
                        } catch (b) {
                            k.reject(b), a(b)
                        }
                    });
                    return k.promise
                }
            }
        };
        return {
            defer: e, reject: g, when: function (k, h, l, n) {
                var p = e(), q, s = function (b) {
                    try {
                        return (P(h) ? h : c)(b)
                    } catch (d) {
                        return a(d), g(d)
                    }
                }, L = function (b) {
                    try {
                        return (P(l) ? l : d)(b)
                    } catch (c) {
                        return a(c), g(c)
                    }
                }, v = function (b) {
                    try {
                        return (P(n) ? n : c)(b)
                    } catch (d) {
                        a(d)
                    }
                };
                b(function () {
                    f(k).then(function (a) {
                        q || (q = !0, p.resolve(f(a).then(s, L, v)))
                    }, function (a) {
                        q || (q = !0, p.resolve(L(a)))
                    }, function (a) {
                        q || p.notify(v(a))
                    })
                });
                return p.promise
            }, all: function (a) {
                var b = e(), c = 0, d = H(a) ?
                    [] : {};
                r(a, function (a, e) {
                    c++;
                    f(a).then(function (a) {
                        d.hasOwnProperty(e) || (d[e] = a, --c || b.resolve(d))
                    }, function (a) {
                        d.hasOwnProperty(e) || b.reject(a)
                    })
                });
                0 === c && b.resolve(d);
                return b.promise
            }
        }
    }

    function ge() {
        this.$get = ["$window", "$timeout", function (b, a) {
            var c = b.requestAnimationFrame || b.webkitRequestAnimationFrame || b.mozRequestAnimationFrame,
                d = b.cancelAnimationFrame || b.webkitCancelAnimationFrame || b.mozCancelAnimationFrame || b.webkitCancelRequestAnimationFrame,
                e = !!c, f = e ? function (a) {
                        var b = c(a);
                        return function () {
                            d(b)
                        }
                    } :
                    function (b) {
                        var c = a(b, 16.66, !1);
                        return function () {
                            a.cancel(c)
                        }
                    };
            f.supported = e;
            return f
        }]
    }

    function Zd() {
        var b = 10, a = x("$rootScope"), c = null;
        this.digestTtl = function (a) {
            arguments.length && (b = a);
            return b
        };
        this.$get = ["$injector", "$exceptionHandler", "$parse", "$browser", function (d, e, f, g) {
            function k() {
                this.$id = gb();
                this.$$phase = this.$parent = this.$$watchers = this.$$nextSibling = this.$$prevSibling = this.$$childHead = this.$$childTail = null;
                this["this"] = this.$root = this;
                this.$$destroyed = !1;
                this.$$asyncQueue = [];
                this.$$postDigestQueue =
                    [];
                this.$$listeners = {};
                this.$$listenerCount = {};
                this.$$isolateBindings = {}
            }

            function m(b) {
                if (p.$$phase) throw a("inprog", p.$$phase);
                p.$$phase = b
            }

            function h(a, b) {
                var c = f(a);
                Va(c, b);
                return c
            }

            function l(a, b, c) {
                do a.$$listenerCount[c] -= b, 0 === a.$$listenerCount[c] && delete a.$$listenerCount[c]; while (a = a.$parent)
            }

            function n() {
            }

            k.prototype = {
                constructor: k, $new: function (a) {
                    a ? (a = new k, a.$root = this.$root, a.$$asyncQueue = this.$$asyncQueue, a.$$postDigestQueue = this.$$postDigestQueue) : (this.$$childScopeClass || (this.$$childScopeClass =
                        function () {
                            this.$$watchers = this.$$nextSibling = this.$$childHead = this.$$childTail = null;
                            this.$$listeners = {};
                            this.$$listenerCount = {};
                            this.$id = gb();
                            this.$$childScopeClass = null
                        }, this.$$childScopeClass.prototype = this), a = new this.$$childScopeClass);
                    a["this"] = a;
                    a.$parent = this;
                    a.$$prevSibling = this.$$childTail;
                    this.$$childHead ? this.$$childTail = this.$$childTail.$$nextSibling = a : this.$$childHead = this.$$childTail = a;
                    return a
                }, $watch: function (a, b, d) {
                    var e = h(a, "watch"), f = this.$$watchers, g = {
                        fn: b, last: n, get: e, exp: a,
                        eq: !!d
                    };
                    c = null;
                    if (!P(b)) {
                        var k = h(b || y, "listener");
                        g.fn = function (a, b, c) {
                            k(c)
                        }
                    }
                    if ("string" == typeof a && e.constant) {
                        var m = g.fn;
                        g.fn = function (a, b, c) {
                            m.call(this, a, b, c);
                            Ra(f, g)
                        }
                    }
                    f || (f = this.$$watchers = []);
                    f.unshift(g);
                    return function () {
                        Ra(f, g);
                        c = null
                    }
                }, $watchCollection: function (a, b) {
                    var c = this, d, e, g, k = 1 < b.length, h = 0, m = f(a), l = [], p = {}, n = !0, r = 0;
                    return this.$watch(function () {
                        d = m(c);
                        var a, b, f;
                        if (T(d)) if (fb(d)) for (e !== l && (e = l, r = e.length = 0, h++), a = d.length, r !== a && (h++, e.length = r = a), b = 0; b < a; b++) f = e[b] !== e[b] && d[b] !==
                            d[b], f || e[b] === d[b] || (h++, e[b] = d[b]); else {
                            e !== p && (e = p = {}, r = 0, h++);
                            a = 0;
                            for (b in d) d.hasOwnProperty(b) && (a++, e.hasOwnProperty(b) ? (f = e[b] !== e[b] && d[b] !== d[b], f || e[b] === d[b] || (h++, e[b] = d[b])) : (r++, e[b] = d[b], h++));
                            if (r > a) for (b in h++, e) e.hasOwnProperty(b) && !d.hasOwnProperty(b) && (r--, delete e[b])
                        } else e !== d && (e = d, h++);
                        return h
                    }, function () {
                        n ? (n = !1, b(d, d, c)) : b(d, g, c);
                        if (k) if (T(d)) if (fb(d)) {
                            g = Array(d.length);
                            for (var a = 0; a < d.length; a++) g[a] = d[a]
                        } else for (a in g = {}, d) ib.call(d, a) && (g[a] = d[a]); else g = d
                    })
                }, $digest: function () {
                    var d,
                        f, g, k, h = this.$$asyncQueue, l = this.$$postDigestQueue, r, w, t = b, K, W = [], u, J, E;
                    m("$digest");
                    c = null;
                    do {
                        w = !1;
                        for (K = this; h.length;) {
                            try {
                                E = h.shift(), E.scope.$eval(E.expression)
                            } catch (F) {
                                p.$$phase = null, e(F)
                            }
                            c = null
                        }
                        a:do {
                            if (k = K.$$watchers) for (r = k.length; r--;) try {
                                if (d = k[r]) if ((f = d.get(K)) !== (g = d.last) && !(d.eq ? za(f, g) : "number" === typeof f && "number" === typeof g && isNaN(f) && isNaN(g))) w = !0, c = d, d.last = d.eq ? Ha(f, null) : f, d.fn(f, g === n ? f : g, K), 5 > t && (u = 4 - t, W[u] || (W[u] = []), J = P(d.exp) ? "fn: " + (d.exp.name || d.exp.toString()) : d.exp,
                                    J += "; newVal: " + ta(f) + "; oldVal: " + ta(g), W[u].push(J)); else if (d === c) {
                                    w = !1;
                                    break a
                                }
                            } catch (A) {
                                p.$$phase = null, e(A)
                            }
                            if (!(k = K.$$childHead || K !== this && K.$$nextSibling)) for (; K !== this && !(k = K.$$nextSibling);) K = K.$parent
                        } while (K = k);
                        if ((w || h.length) && !t--) throw p.$$phase = null, a("infdig", b, ta(W));
                    } while (w || h.length);
                    for (p.$$phase = null; l.length;) try {
                        l.shift()()
                    } catch (x) {
                        e(x)
                    }
                }, $destroy: function () {
                    if (!this.$$destroyed) {
                        var a = this.$parent;
                        this.$broadcast("$destroy");
                        this.$$destroyed = !0;
                        this !== p && (r(this.$$listenerCount,
                            Bb(null, l, this)), a.$$childHead == this && (a.$$childHead = this.$$nextSibling), a.$$childTail == this && (a.$$childTail = this.$$prevSibling), this.$$prevSibling && (this.$$prevSibling.$$nextSibling = this.$$nextSibling), this.$$nextSibling && (this.$$nextSibling.$$prevSibling = this.$$prevSibling), this.$parent = this.$$nextSibling = this.$$prevSibling = this.$$childHead = this.$$childTail = this.$root = null, this.$$listeners = {}, this.$$watchers = this.$$asyncQueue = this.$$postDigestQueue = [], this.$destroy = this.$digest = this.$apply = y, this.$on =
                            this.$watch = function () {
                                return y
                            })
                    }
                }, $eval: function (a, b) {
                    return f(a)(this, b)
                }, $evalAsync: function (a) {
                    p.$$phase || p.$$asyncQueue.length || g.defer(function () {
                        p.$$asyncQueue.length && p.$digest()
                    });
                    this.$$asyncQueue.push({scope: this, expression: a})
                }, $$postDigest: function (a) {
                    this.$$postDigestQueue.push(a)
                }, $apply: function (a) {
                    try {
                        return m("$apply"), this.$eval(a)
                    } catch (b) {
                        e(b)
                    } finally {
                        p.$$phase = null;
                        try {
                            p.$digest()
                        } catch (c) {
                            throw e(c), c;
                        }
                    }
                }, $on: function (a, b) {
                    var c = this.$$listeners[a];
                    c || (this.$$listeners[a] =
                        c = []);
                    c.push(b);
                    var d = this;
                    do d.$$listenerCount[a] || (d.$$listenerCount[a] = 0), d.$$listenerCount[a]++; while (d = d.$parent);
                    var e = this;
                    return function () {
                        c[Qa(c, b)] = null;
                        l(e, 1, a)
                    }
                }, $emit: function (a, b) {
                    var c = [], d, f = this, g = !1, k = {
                        name: a, targetScope: f, stopPropagation: function () {
                            g = !0
                        }, preventDefault: function () {
                            k.defaultPrevented = !0
                        }, defaultPrevented: !1
                    }, h = [k].concat(Aa.call(arguments, 1)), m, l;
                    do {
                        d = f.$$listeners[a] || c;
                        k.currentScope = f;
                        m = 0;
                        for (l = d.length; m < l; m++) if (d[m]) try {
                            d[m].apply(null, h)
                        } catch (p) {
                            e(p)
                        } else d.splice(m,
                            1), m--, l--;
                        if (g) break;
                        f = f.$parent
                    } while (f);
                    return k
                }, $broadcast: function (a, b) {
                    for (var c = this, d = this, f = {
                        name: a, targetScope: this, preventDefault: function () {
                            f.defaultPrevented = !0
                        }, defaultPrevented: !1
                    }, g = [f].concat(Aa.call(arguments, 1)), k, h; c = d;) {
                        f.currentScope = c;
                        d = c.$$listeners[a] || [];
                        k = 0;
                        for (h = d.length; k < h; k++) if (d[k]) try {
                            d[k].apply(null, g)
                        } catch (m) {
                            e(m)
                        } else d.splice(k, 1), k--, h--;
                        if (!(d = c.$$listenerCount[a] && c.$$childHead || c !== this && c.$$nextSibling)) for (; c !== this && !(d = c.$$nextSibling);) c = c.$parent
                    }
                    return f
                }
            };
            var p = new k;
            return p
        }]
    }

    function cd() {
        var b = /^\s*(https?|ftp|mailto|tel|file):/, a = /^\s*((https?|ftp|file):|data:image\/)/;
        this.aHrefSanitizationWhitelist = function (a) {
            return A(a) ? (b = a, this) : b
        };
        this.imgSrcSanitizationWhitelist = function (b) {
            return A(b) ? (a = b, this) : a
        };
        this.$get = function () {
            return function (c, d) {
                var e = d ? a : b, f;
                if (!R || 8 <= R) if (f = ua(c).href, "" !== f && !f.match(e)) return "unsafe:" + f;
                return c
            }
        }
    }

    function ze(b) {
        if ("self" === b) return b;
        if (z(b)) {
            if (-1 < b.indexOf("***")) throw wa("iwcard", b);
            b = b.replace(/([-()\[\]{}+?*.$\^|,:#<!\\])/g,
                "\\$1").replace(/\x08/g, "\\x08").replace("\\*\\*", ".*").replace("\\*", "[^:/.?&;]*");
            return RegExp("^" + b + "$")
        }
        if (hb(b)) return RegExp("^" + b.source + "$");
        throw wa("imatcher");
    }

    function Gc(b) {
        var a = [];
        A(b) && r(b, function (b) {
            a.push(ze(b))
        });
        return a
    }

    function be() {
        this.SCE_CONTEXTS = fa;
        var b = ["self"], a = [];
        this.resourceUrlWhitelist = function (a) {
            arguments.length && (b = Gc(a));
            return b
        };
        this.resourceUrlBlacklist = function (b) {
            arguments.length && (a = Gc(b));
            return a
        };
        this.$get = ["$injector", function (c) {
            function d(a) {
                var b =
                    function (a) {
                        this.$$unwrapTrustedValue = function () {
                            return a
                        }
                    };
                a && (b.prototype = new a);
                b.prototype.valueOf = function () {
                    return this.$$unwrapTrustedValue()
                };
                b.prototype.toString = function () {
                    return this.$$unwrapTrustedValue().toString()
                };
                return b
            }

            var e = function (a) {
                throw wa("unsafe");
            };
            c.has("$sanitize") && (e = c.get("$sanitize"));
            var f = d(), g = {};
            g[fa.HTML] = d(f);
            g[fa.CSS] = d(f);
            g[fa.URL] = d(f);
            g[fa.JS] = d(f);
            g[fa.RESOURCE_URL] = d(g[fa.URL]);
            return {
                trustAs: function (a, b) {
                    var c = g.hasOwnProperty(a) ? g[a] : null;
                    if (!c) throw wa("icontext",
                        a, b);
                    if (null === b || b === t || "" === b) return b;
                    if ("string" !== typeof b) throw wa("itype", a);
                    return new c(b)
                }, getTrusted: function (c, d) {
                    if (null === d || d === t || "" === d) return d;
                    var f = g.hasOwnProperty(c) ? g[c] : null;
                    if (f && d instanceof f) return d.$$unwrapTrustedValue();
                    if (c === fa.RESOURCE_URL) {
                        var f = ua(d.toString()), l, n, p = !1;
                        l = 0;
                        for (n = b.length; l < n; l++) if ("self" === b[l] ? Pb(f) : b[l].exec(f.href)) {
                            p = !0;
                            break
                        }
                        if (p) for (l = 0, n = a.length; l < n; l++) if ("self" === a[l] ? Pb(f) : a[l].exec(f.href)) {
                            p = !1;
                            break
                        }
                        if (p) return d;
                        throw wa("insecurl",
                            d.toString());
                    }
                    if (c === fa.HTML) return e(d);
                    throw wa("unsafe");
                }, valueOf: function (a) {
                    return a instanceof f ? a.$$unwrapTrustedValue() : a
                }
            }
        }]
    }

    function ae() {
        var b = !0;
        this.enabled = function (a) {
            arguments.length && (b = !!a);
            return b
        };
        this.$get = ["$parse", "$sniffer", "$sceDelegate", function (a, c, d) {
            if (b && c.msie && 8 > c.msieDocumentMode) throw wa("iequirks");
            var e = ga(fa);
            e.isEnabled = function () {
                return b
            };
            e.trustAs = d.trustAs;
            e.getTrusted = d.getTrusted;
            e.valueOf = d.valueOf;
            b || (e.trustAs = e.getTrusted = function (a, b) {
                return b
            },
                e.valueOf = Ga);
            e.parseAs = function (b, c) {
                var d = a(c);
                return d.literal && d.constant ? d : function (a, c) {
                    return e.getTrusted(b, d(a, c))
                }
            };
            var f = e.parseAs, g = e.getTrusted, k = e.trustAs;
            r(fa, function (a, b) {
                var c = N(b);
                e[Ya("parse_as_" + c)] = function (b) {
                    return f(a, b)
                };
                e[Ya("get_trusted_" + c)] = function (b) {
                    return g(a, b)
                };
                e[Ya("trust_as_" + c)] = function (b) {
                    return k(a, b)
                }
            });
            return e
        }]
    }

    function ce() {
        this.$get = ["$window", "$document", function (b, a) {
            var c = {}, d = Z((/android (\d+)/.exec(N((b.navigator || {}).userAgent)) || [])[1]),
                e = /Boxee/i.test((b.navigator ||
                    {}).userAgent), f = a[0] || {}, g = f.documentMode, k, m = /^(Moz|webkit|O|ms)(?=[A-Z])/,
                h = f.body && f.body.style, l = !1, n = !1;
            if (h) {
                for (var p in h) if (l = m.exec(p)) {
                    k = l[0];
                    k = k.substr(0, 1).toUpperCase() + k.substr(1);
                    break
                }
                k || (k = "WebkitOpacity" in h && "webkit");
                l = !!("transition" in h || k + "Transition" in h);
                n = !!("animation" in h || k + "Animation" in h);
                !d || l && n || (l = z(f.body.style.webkitTransition), n = z(f.body.style.webkitAnimation))
            }
            return {
                history: !(!b.history || !b.history.pushState || 4 > d || e),
                hashchange: "onhashchange" in b && (!g || 7 <
                    g),
                hasEvent: function (a) {
                    if ("input" == a && 9 == R) return !1;
                    if (D(c[a])) {
                        var b = f.createElement("div");
                        c[a] = "on" + a in b
                    }
                    return c[a]
                },
                csp: Wa(),
                vendorPrefix: k,
                transitions: l,
                animations: n,
                android: d,
                msie: R,
                msieDocumentMode: g
            }
        }]
    }

    function ee() {
        this.$get = ["$rootScope", "$browser", "$q", "$exceptionHandler", function (b, a, c, d) {
            function e(e, k, m) {
                var h = c.defer(), l = h.promise, n = A(m) && !m;
                k = a.defer(function () {
                    try {
                        h.resolve(e())
                    } catch (a) {
                        h.reject(a), d(a)
                    } finally {
                        delete f[l.$$timeoutId]
                    }
                    n || b.$apply()
                }, k);
                l.$$timeoutId = k;
                f[k] = h;
                return l
            }

            var f = {};
            e.cancel = function (b) {
                return b && b.$$timeoutId in f ? (f[b.$$timeoutId].reject("canceled"), delete f[b.$$timeoutId], a.defer.cancel(b.$$timeoutId)) : !1
            };
            return e
        }]
    }

    function ua(b, a) {
        var c = b;
        R && (V.setAttribute("href", c), c = V.href);
        V.setAttribute("href", c);
        return {
            href: V.href,
            protocol: V.protocol ? V.protocol.replace(/:$/, "") : "",
            host: V.host,
            search: V.search ? V.search.replace(/^\?/, "") : "",
            hash: V.hash ? V.hash.replace(/^#/, "") : "",
            hostname: V.hostname,
            port: V.port,
            pathname: "/" === V.pathname.charAt(0) ? V.pathname :
                "/" + V.pathname
        }
    }

    function Pb(b) {
        b = z(b) ? ua(b) : b;
        return b.protocol === Hc.protocol && b.host === Hc.host
    }

    function fe() {
        this.$get = $(Q)
    }

    function mc(b) {
        function a(d, e) {
            if (T(d)) {
                var f = {};
                r(d, function (b, c) {
                    f[c] = a(c, b)
                });
                return f
            }
            return b.factory(d + c, e)
        }

        var c = "Filter";
        this.register = a;
        this.$get = ["$injector", function (a) {
            return function (b) {
                return a.get(b + c)
            }
        }];
        a("currency", Ic);
        a("date", Jc);
        a("filter", Ae);
        a("json", Be);
        a("limitTo", Ce);
        a("lowercase", De);
        a("number", Kc);
        a("orderBy", Lc);
        a("uppercase", Ee)
    }

    function Ae() {
        return function (b,
                         a, c) {
            if (!H(b)) return b;
            var d = typeof c, e = [];
            e.check = function (a) {
                for (var b = 0; b < e.length; b++) if (!e[b](a)) return !1;
                return !0
            };
            "function" !== d && (c = "boolean" === d && c ? function (a, b) {
                return Ua.equals(a, b)
            } : function (a, b) {
                if (a && b && "object" === typeof a && "object" === typeof b) {
                    for (var d in a) if ("$" !== d.charAt(0) && ib.call(a, d) && c(a[d], b[d])) return !0;
                    return !1
                }
                b = ("" + b).toLowerCase();
                return -1 < ("" + a).toLowerCase().indexOf(b)
            });
            var f = function (a, b) {
                if ("string" == typeof b && "!" === b.charAt(0)) return !f(a, b.substr(1));
                switch (typeof a) {
                    case "boolean":
                    case "number":
                    case "string":
                        return c(a,
                            b);
                    case "object":
                        switch (typeof b) {
                            case "object":
                                return c(a, b);
                            default:
                                for (var d in a) if ("$" !== d.charAt(0) && f(a[d], b)) return !0
                        }
                        return !1;
                    case "array":
                        for (d = 0; d < a.length; d++) if (f(a[d], b)) return !0;
                        return !1;
                    default:
                        return !1
                }
            };
            switch (typeof a) {
                case "boolean":
                case "number":
                case "string":
                    a = {$: a};
                case "object":
                    for (var g in a) (function (b) {
                        "undefined" !== typeof a[b] && e.push(function (c) {
                            return f("$" == b ? c : c && c[b], a[b])
                        })
                    })(g);
                    break;
                case "function":
                    e.push(a);
                    break;
                default:
                    return b
            }
            d = [];
            for (g = 0; g < b.length; g++) {
                var k =
                    b[g];
                e.check(k) && d.push(k)
            }
            return d
        }
    }

    function Ic(b) {
        var a = b.NUMBER_FORMATS;
        return function (b, d) {
            D(d) && (d = a.CURRENCY_SYM);
            return Mc(b, a.PATTERNS[1], a.GROUP_SEP, a.DECIMAL_SEP, 2).replace(/\u00A4/g, d)
        }
    }

    function Kc(b) {
        var a = b.NUMBER_FORMATS;
        return function (b, d) {
            return Mc(b, a.PATTERNS[0], a.GROUP_SEP, a.DECIMAL_SEP, d)
        }
    }

    function Mc(b, a, c, d, e) {
        if (null == b || !isFinite(b) || T(b)) return "";
        var f = 0 > b;
        b = Math.abs(b);
        var g = b + "", k = "", m = [], h = !1;
        if (-1 !== g.indexOf("e")) {
            var l = g.match(/([\d\.]+)e(-?)(\d+)/);
            l && "-" == l[2] &&
            l[3] > e + 1 ? (g = "0", b = 0) : (k = g, h = !0)
        }
        if (h) 0 < e && (-1 < b && 1 > b) && (k = b.toFixed(e)); else {
            g = (g.split(Nc)[1] || "").length;
            D(e) && (e = Math.min(Math.max(a.minFrac, g), a.maxFrac));
            b = +(Math.round(+(b.toString() + "e" + e)).toString() + "e" + -e);
            b = ("" + b).split(Nc);
            g = b[0];
            b = b[1] || "";
            var l = 0, n = a.lgSize, p = a.gSize;
            if (g.length >= n + p) for (l = g.length - n, h = 0; h < l; h++) 0 === (l - h) % p && 0 !== h && (k += c), k += g.charAt(h);
            for (h = l; h < g.length; h++) 0 === (g.length - h) % n && 0 !== h && (k += c), k += g.charAt(h);
            for (; b.length < e;) b += "0";
            e && "0" !== e && (k += d + b.substr(0, e))
        }
        m.push(f ?
            a.negPre : a.posPre);
        m.push(k);
        m.push(f ? a.negSuf : a.posSuf);
        return m.join("")
    }

    function Xb(b, a, c) {
        var d = "";
        0 > b && (d = "-", b = -b);
        for (b = "" + b; b.length < a;) b = "0" + b;
        c && (b = b.substr(b.length - a));
        return d + b
    }

    function Y(b, a, c, d) {
        c = c || 0;
        return function (e) {
            e = e["get" + b]();
            if (0 < c || e > -c) e += c;
            0 === e && -12 == c && (e = 12);
            return Xb(e, a, d)
        }
    }

    function ub(b, a) {
        return function (c, d) {
            var e = c["get" + b](), f = Ia(a ? "SHORT" + b : b);
            return d[f][e]
        }
    }

    function Jc(b) {
        function a(a) {
            var b;
            if (b = a.match(c)) {
                a = new Date(0);
                var f = 0, g = 0, k = b[8] ? a.setUTCFullYear :
                    a.setFullYear, m = b[8] ? a.setUTCHours : a.setHours;
                b[9] && (f = Z(b[9] + b[10]), g = Z(b[9] + b[11]));
                k.call(a, Z(b[1]), Z(b[2]) - 1, Z(b[3]));
                f = Z(b[4] || 0) - f;
                g = Z(b[5] || 0) - g;
                k = Z(b[6] || 0);
                b = Math.round(1E3 * parseFloat("0." + (b[7] || 0)));
                m.call(a, f, g, k, b)
            }
            return a
        }

        var c = /^(\d{4})-?(\d\d)-?(\d\d)(?:T(\d\d)(?::?(\d\d)(?::?(\d\d)(?:\.(\d+))?)?)?(Z|([+-])(\d\d):?(\d\d))?)?$/;
        return function (c, e) {
            var f = "", g = [], k, m;
            e = e || "mediumDate";
            e = b.DATETIME_FORMATS[e] || e;
            z(c) && (c = Fe.test(c) ? Z(c) : a(c));
            Ab(c) && (c = new Date(c));
            if (!sa(c)) return c;
            for (; e;) (m = Ge.exec(e)) ? (g = g.concat(Aa.call(m, 1)), e = g.pop()) : (g.push(e), e = null);
            r(g, function (a) {
                k = He[a];
                f += k ? k(c, b.DATETIME_FORMATS) : a.replace(/(^'|'$)/g, "").replace(/''/g, "'")
            });
            return f
        }
    }

    function Be() {
        return function (b) {
            return ta(b, !0)
        }
    }

    function Ce() {
        return function (b, a) {
            if (!H(b) && !z(b)) return b;
            a = Infinity === Math.abs(Number(a)) ? Number(a) : Z(a);
            if (z(b)) return a ? 0 <= a ? b.slice(0, a) : b.slice(a, b.length) : "";
            var c = [], d, e;
            a > b.length ? a = b.length : a < -b.length && (a = -b.length);
            0 < a ? (d = 0, e = a) : (d = b.length + a, e = b.length);
            for (; d < e; d++) c.push(b[d]);
            return c
        }
    }

    function Lc(b) {
        return function (a, c, d) {
            function e(a, b) {
                return Ta(b) ? function (b, c) {
                    return a(c, b)
                } : a
            }

            function f(a, b) {
                var c = typeof a, d = typeof b;
                return c == d ? (sa(a) && sa(b) && (a = a.valueOf(), b = b.valueOf()), "string" == c && (a = a.toLowerCase(), b = b.toLowerCase()), a === b ? 0 : a < b ? -1 : 1) : c < d ? -1 : 1
            }

            if (!H(a) || !c) return a;
            c = H(c) ? c : [c];
            c = Vc(c, function (a) {
                var c = !1, d = a || Ga;
                if (z(a)) {
                    if ("+" == a.charAt(0) || "-" == a.charAt(0)) c = "-" == a.charAt(0), a = a.substring(1);
                    d = b(a);
                    if (d.constant) {
                        var g = d();
                        return e(function (a,
                                           b) {
                            return f(a[g], b[g])
                        }, c)
                    }
                }
                return e(function (a, b) {
                    return f(d(a), d(b))
                }, c)
            });
            for (var g = [], k = 0; k < a.length; k++) g.push(a[k]);
            return g.sort(e(function (a, b) {
                for (var d = 0; d < c.length; d++) {
                    var e = c[d](a, b);
                    if (0 !== e) return e
                }
                return 0
            }, d))
        }
    }

    function xa(b) {
        P(b) && (b = {link: b});
        b.restrict = b.restrict || "AC";
        return $(b)
    }

    function Oc(b, a, c, d) {
        function e(a, c) {
            c = c ? "-" + kb(c, "-") : "";
            d.removeClass(b, (a ? vb : wb) + c);
            d.addClass(b, (a ? wb : vb) + c)
        }

        var f = this, g = b.parent().controller("form") || xb, k = 0, m = f.$error = {}, h = [];
        f.$name = a.name ||
            a.ngForm;
        f.$dirty = !1;
        f.$pristine = !0;
        f.$valid = !0;
        f.$invalid = !1;
        g.$addControl(f);
        b.addClass(Pa);
        e(!0);
        f.$addControl = function (a) {
            Ca(a.$name, "input");
            h.push(a);
            a.$name && (f[a.$name] = a)
        };
        f.$removeControl = function (a) {
            a.$name && f[a.$name] === a && delete f[a.$name];
            r(m, function (b, c) {
                f.$setValidity(c, !0, a)
            });
            Ra(h, a)
        };
        f.$setValidity = function (a, b, c) {
            var d = m[a];
            if (b) d && (Ra(d, c), d.length || (k--, k || (e(b), f.$valid = !0, f.$invalid = !1), m[a] = !1, e(!0, a), g.$setValidity(a, !0, f))); else {
                k || e(b);
                if (d) {
                    if (-1 != Qa(d, c)) return
                } else m[a] =
                    d = [], k++, e(!1, a), g.$setValidity(a, !1, f);
                d.push(c);
                f.$valid = !1;
                f.$invalid = !0
            }
        };
        f.$setDirty = function () {
            d.removeClass(b, Pa);
            d.addClass(b, yb);
            f.$dirty = !0;
            f.$pristine = !1;
            g.$setDirty()
        };
        f.$setPristine = function () {
            d.removeClass(b, yb);
            d.addClass(b, Pa);
            f.$dirty = !1;
            f.$pristine = !0;
            r(h, function (a) {
                a.$setPristine()
            })
        }
    }

    function ra(b, a, c, d) {
        b.$setValidity(a, c);
        return c ? d : t
    }

    function Pc(b, a) {
        var c, d;
        if (a) for (c = 0; c < a.length; ++c) if (d = a[c], b[d]) return !0;
        return !1
    }

    function Ie(b, a, c, d, e) {
        T(e) && (b.$$hasNativeValidators = !0,
            b.$parsers.push(function (f) {
                if (b.$error[a] || Pc(e, d) || !Pc(e, c)) return f;
                b.$setValidity(a, !1)
            }))
    }

    function zb(b, a, c, d, e, f) {
        var g = a.prop(Je), k = a[0].placeholder, m = {}, h = N(a[0].type);
        d.$$validityState = g;
        if (!e.android) {
            var l = !1;
            a.on("compositionstart", function (a) {
                l = !0
            });
            a.on("compositionend", function () {
                l = !1;
                n()
            })
        }
        var n = function (e) {
            if (!l) {
                var f = a.val();
                if (R && "input" === (e || m).type && a[0].placeholder !== k) k = a[0].placeholder; else if ("password" !== h && Ta(c.ngTrim || "T") && (f = aa(f)), e = g && d.$$hasNativeValidators, d.$viewValue !==
                f || "" === f && e) b.$$phase ? d.$setViewValue(f) : b.$apply(function () {
                    d.$setViewValue(f)
                })
            }
        };
        if (e.hasEvent("input")) a.on("input", n); else {
            var p, q = function () {
                p || (p = f.defer(function () {
                    n();
                    p = null
                }))
            };
            a.on("keydown", function (a) {
                a = a.keyCode;
                91 === a || (15 < a && 19 > a || 37 <= a && 40 >= a) || q()
            });
            if (e.hasEvent("paste")) a.on("paste cut", q)
        }
        a.on("change", n);
        d.$render = function () {
            a.val(d.$isEmpty(d.$viewValue) ? "" : d.$viewValue)
        };
        var s = c.ngPattern;
        s && ((e = s.match(/^\/(.*)\/([gim]*)$/)) ? (s = RegExp(e[1], e[2]), e = function (a) {
            return ra(d,
                "pattern", d.$isEmpty(a) || s.test(a), a)
        }) : e = function (c) {
            var e = b.$eval(s);
            if (!e || !e.test) throw x("ngPattern")("noregexp", s, e, ha(a));
            return ra(d, "pattern", d.$isEmpty(c) || e.test(c), c)
        }, d.$formatters.push(e), d.$parsers.push(e));
        if (c.ngMinlength) {
            var r = Z(c.ngMinlength);
            e = function (a) {
                return ra(d, "minlength", d.$isEmpty(a) || a.length >= r, a)
            };
            d.$parsers.push(e);
            d.$formatters.push(e)
        }
        if (c.ngMaxlength) {
            var v = Z(c.ngMaxlength);
            e = function (a) {
                return ra(d, "maxlength", d.$isEmpty(a) || a.length <= v, a)
            };
            d.$parsers.push(e);
            d.$formatters.push(e)
        }
    }

    function Yb(b, a) {
        b = "ngClass" + b;
        return ["$animate", function (c) {
            function d(a, b) {
                var c = [], d = 0;
                a:for (; d < a.length; d++) {
                    for (var e = a[d], l = 0; l < b.length; l++) if (e == b[l]) continue a;
                    c.push(e)
                }
                return c
            }

            function e(a) {
                if (!H(a)) {
                    if (z(a)) return a.split(" ");
                    if (T(a)) {
                        var b = [];
                        r(a, function (a, c) {
                            a && (b = b.concat(c.split(" ")))
                        });
                        return b
                    }
                }
                return a
            }

            return {
                restrict: "AC", link: function (f, g, k) {
                    function m(a, b) {
                        var c = g.data("$classCounts") || {}, d = [];
                        r(a, function (a) {
                            if (0 < b || c[a]) c[a] = (c[a] || 0) + b, c[a] === +(0 <
                                b) && d.push(a)
                        });
                        g.data("$classCounts", c);
                        return d.join(" ")
                    }

                    function h(b) {
                        if (!0 === a || f.$index % 2 === a) {
                            var h = e(b || []);
                            if (!l) {
                                var q = m(h, 1);
                                k.$addClass(q)
                            } else if (!za(b, l)) {
                                var s = e(l), q = d(h, s), h = d(s, h), h = m(h, -1), q = m(q, 1);
                                0 === q.length ? c.removeClass(g, h) : 0 === h.length ? c.addClass(g, q) : c.setClass(g, q, h)
                            }
                        }
                        l = ga(b)
                    }

                    var l;
                    f.$watch(k[b], h, !0);
                    k.$observe("class", function (a) {
                        h(f.$eval(k[b]))
                    });
                    "ngClass" !== b && f.$watch("$index", function (c, d) {
                        var g = c & 1;
                        if (g !== (d & 1)) {
                            var h = e(f.$eval(k[b]));
                            g === a ? (g = m(h, 1), k.$addClass(g)) :
                                (g = m(h, -1), k.$removeClass(g))
                        }
                    })
                }
            }
        }]
    }

    var Je = "validity", N = function (b) {
            return z(b) ? b.toLowerCase() : b
        }, ib = Object.prototype.hasOwnProperty, Ia = function (b) {
            return z(b) ? b.toUpperCase() : b
        }, R, u, Da, Aa = [].slice, Ke = [].push, ya = Object.prototype.toString, Sa = x("ng"),
        Ua = Q.angular || (Q.angular = {}), Xa, Ma, la = ["0", "0", "0"];
    R = Z((/msie (\d+)/.exec(N(navigator.userAgent)) || [])[1]);
    isNaN(R) && (R = Z((/trident\/.*; rv:(\d+)/.exec(N(navigator.userAgent)) || [])[1]));
    y.$inject = [];
    Ga.$inject = [];
    var H = function () {
        return P(Array.isArray) ?
            Array.isArray : function (b) {
                return "[object Array]" === ya.call(b)
            }
    }(), aa = function () {
        return String.prototype.trim ? function (b) {
            return z(b) ? b.trim() : b
        } : function (b) {
            return z(b) ? b.replace(/^\s\s*/, "").replace(/\s\s*$/, "") : b
        }
    }();
    Ma = 9 > R ? function (b) {
        b = b.nodeName ? b : b[0];
        return b.scopeName && "HTML" != b.scopeName ? Ia(b.scopeName + ":" + b.nodeName) : b.nodeName
    } : function (b) {
        return b.nodeName ? b.nodeName : b[0].nodeName
    };
    var Wa = function () {
        if (A(Wa.isActive_)) return Wa.isActive_;
        var b = !(!X.querySelector("[ng-csp]") && !X.querySelector("[data-ng-csp]"));
        if (!b) try {
            new Function("")
        } catch (a) {
            b = !0
        }
        return Wa.isActive_ = b
    }, Yc = /[A-Z]/g, ad = {full: "1.2.23", major: 1, minor: 2, dot: 23, codeName: "superficial-malady"};
    S.expando = "ng339";
    var $a = S.cache = {}, ne = 1, rb = Q.document.addEventListener ? function (b, a, c) {
        b.addEventListener(a, c, !1)
    } : function (b, a, c) {
        b.attachEvent("on" + a, c)
    }, Za = Q.document.removeEventListener ? function (b, a, c) {
        b.removeEventListener(a, c, !1)
    } : function (b, a, c) {
        b.detachEvent("on" + a, c)
    };
    S._data = function (b) {
        return this.cache[b[this.expando]] || {}
    };
    var ie = /([\:\-\_]+(.))/g,
        je = /^moz([A-Z])/, Hb = x("jqLite"), ke = /^<(\w+)\s*\/?>(?:<\/\1>|)$/, Ib = /<|&#?\w+;/, le = /<([\w:]+)/,
        me = /<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/gi, ba = {
            option: [1, '<select multiple="multiple">', "</select>"],
            thead: [1, "<table>", "</table>"],
            col: [2, "<table><colgroup>", "</colgroup></table>"],
            tr: [2, "<table><tbody>", "</tbody></table>"],
            td: [3, "<table><tbody><tr>", "</tr></tbody></table>"],
            _default: [0, "", ""]
        };
    ba.optgroup = ba.option;
    ba.tbody = ba.tfoot = ba.colgroup = ba.caption = ba.thead;
    ba.th =
        ba.td;
    var La = S.prototype = {
        ready: function (b) {
            function a() {
                c || (c = !0, b())
            }

            var c = !1;
            "complete" === X.readyState ? setTimeout(a) : (this.on("DOMContentLoaded", a), S(Q).on("load", a))
        }, toString: function () {
            var b = [];
            r(this, function (a) {
                b.push("" + a)
            });
            return "[" + b.join(", ") + "]"
        }, eq: function (b) {
            return 0 <= b ? u(this[b]) : u(this[this.length + b])
        }, length: 0, push: Ke, sort: [].sort, splice: [].splice
    }, ob = {};
    r("multiple selected checked disabled readOnly required open".split(" "), function (b) {
        ob[N(b)] = b
    });
    var rc = {};
    r("input select option textarea button form details".split(" "),
        function (b) {
            rc[Ia(b)] = !0
        });
    r({data: Mb, removeData: Lb}, function (b, a) {
        S[a] = b
    });
    r({
        data: Mb, inheritedData: nb, scope: function (b) {
            return u.data(b, "$scope") || nb(b.parentNode || b, ["$isolateScope", "$scope"])
        }, isolateScope: function (b) {
            return u.data(b, "$isolateScope") || u.data(b, "$isolateScopeNoTemplate")
        }, controller: oc, injector: function (b) {
            return nb(b, "$injector")
        }, removeAttr: function (b, a) {
            b.removeAttribute(a)
        }, hasClass: Nb, css: function (b, a, c) {
            a = Ya(a);
            if (A(c)) b.style[a] = c; else {
                var d;
                8 >= R && (d = b.currentStyle && b.currentStyle[a],
                "" === d && (d = "auto"));
                d = d || b.style[a];
                8 >= R && (d = "" === d ? t : d);
                return d
            }
        }, attr: function (b, a, c) {
            var d = N(a);
            if (ob[d]) if (A(c)) c ? (b[a] = !0, b.setAttribute(a, d)) : (b[a] = !1, b.removeAttribute(d)); else return b[a] || (b.attributes.getNamedItem(a) || y).specified ? d : t; else if (A(c)) b.setAttribute(a, c); else if (b.getAttribute) return b = b.getAttribute(a, 2), null === b ? t : b
        }, prop: function (b, a, c) {
            if (A(c)) b[a] = c; else return b[a]
        }, text: function () {
            function b(b, d) {
                var e = a[b.nodeType];
                if (D(d)) return e ? b[e] : "";
                b[e] = d
            }

            var a = [];
            9 > R ? (a[1] =
                "innerText", a[3] = "nodeValue") : a[1] = a[3] = "textContent";
            b.$dv = "";
            return b
        }(), val: function (b, a) {
            if (D(a)) {
                if ("SELECT" === Ma(b) && b.multiple) {
                    var c = [];
                    r(b.options, function (a) {
                        a.selected && c.push(a.value || a.text)
                    });
                    return 0 === c.length ? null : c
                }
                return b.value
            }
            b.value = a
        }, html: function (b, a) {
            if (D(a)) return b.innerHTML;
            for (var c = 0, d = b.childNodes; c < d.length; c++) Ja(d[c]);
            b.innerHTML = a
        }, empty: pc
    }, function (b, a) {
        S.prototype[a] = function (a, d) {
            var e, f, g = this.length;
            if (b !== pc && (2 == b.length && b !== Nb && b !== oc ? a : d) === t) {
                if (T(a)) {
                    for (e =
                             0; e < g; e++) if (b === Mb) b(this[e], a); else for (f in a) b(this[e], f, a[f]);
                    return this
                }
                e = b.$dv;
                g = e === t ? Math.min(g, 1) : g;
                for (f = 0; f < g; f++) {
                    var k = b(this[f], a, d);
                    e = e ? e + k : k
                }
                return e
            }
            for (e = 0; e < g; e++) b(this[e], a, d);
            return this
        }
    });
    r({
        removeData: Lb, dealoc: Ja, on: function a(c, d, e, f) {
            if (A(f)) throw Hb("onargs");
            var g = ma(c, "events"), k = ma(c, "handle");
            g || ma(c, "events", g = {});
            k || ma(c, "handle", k = oe(c, g));
            r(d.split(" "), function (d) {
                var f = g[d];
                if (!f) {
                    if ("mouseenter" == d || "mouseleave" == d) {
                        var l = X.body.contains || X.body.compareDocumentPosition ?
                            function (a, c) {
                                var d = 9 === a.nodeType ? a.documentElement : a, e = c && c.parentNode;
                                return a === e || !!(e && 1 === e.nodeType && (d.contains ? d.contains(e) : a.compareDocumentPosition && a.compareDocumentPosition(e) & 16))
                            } : function (a, c) {
                                if (c) for (; c = c.parentNode;) if (c === a) return !0;
                                return !1
                            };
                        g[d] = [];
                        a(c, {mouseleave: "mouseout", mouseenter: "mouseover"}[d], function (a) {
                            var c = a.relatedTarget;
                            c && (c === this || l(this, c)) || k(a, d)
                        })
                    } else rb(c, d, k), g[d] = [];
                    f = g[d]
                }
                f.push(e)
            })
        }, off: nc, one: function (a, c, d) {
            a = u(a);
            a.on(c, function f() {
                a.off(c,
                    d);
                a.off(c, f)
            });
            a.on(c, d)
        }, replaceWith: function (a, c) {
            var d, e = a.parentNode;
            Ja(a);
            r(new S(c), function (c) {
                d ? e.insertBefore(c, d.nextSibling) : e.replaceChild(c, a);
                d = c
            })
        }, children: function (a) {
            var c = [];
            r(a.childNodes, function (a) {
                1 === a.nodeType && c.push(a)
            });
            return c
        }, contents: function (a) {
            return a.contentDocument || a.childNodes || []
        }, append: function (a, c) {
            r(new S(c), function (c) {
                1 !== a.nodeType && 11 !== a.nodeType || a.appendChild(c)
            })
        }, prepend: function (a, c) {
            if (1 === a.nodeType) {
                var d = a.firstChild;
                r(new S(c), function (c) {
                    a.insertBefore(c,
                        d)
                })
            }
        }, wrap: function (a, c) {
            c = u(c)[0];
            var d = a.parentNode;
            d && d.replaceChild(c, a);
            c.appendChild(a)
        }, remove: function (a) {
            Ja(a);
            var c = a.parentNode;
            c && c.removeChild(a)
        }, after: function (a, c) {
            var d = a, e = a.parentNode;
            r(new S(c), function (a) {
                e.insertBefore(a, d.nextSibling);
                d = a
            })
        }, addClass: mb, removeClass: lb, toggleClass: function (a, c, d) {
            c && r(c.split(" "), function (c) {
                var f = d;
                D(f) && (f = !Nb(a, c));
                (f ? mb : lb)(a, c)
            })
        }, parent: function (a) {
            return (a = a.parentNode) && 11 !== a.nodeType ? a : null
        }, next: function (a) {
            if (a.nextElementSibling) return a.nextElementSibling;
            for (a = a.nextSibling; null != a && 1 !== a.nodeType;) a = a.nextSibling;
            return a
        }, find: function (a, c) {
            return a.getElementsByTagName ? a.getElementsByTagName(c) : []
        }, clone: Kb, triggerHandler: function (a, c, d) {
            var e, f;
            e = c.type || c;
            var g = (ma(a, "events") || {})[e];
            g && (e = {
                preventDefault: function () {
                    this.defaultPrevented = !0
                }, isDefaultPrevented: function () {
                    return !0 === this.defaultPrevented
                }, stopPropagation: y, type: e, target: a
            }, c.type && (e = B(e, c)), c = ga(g), f = d ? [e].concat(d) : [e], r(c, function (c) {
                c.apply(a, f)
            }))
        }
    }, function (a, c) {
        S.prototype[c] =
            function (c, e, f) {
                for (var g, k = 0; k < this.length; k++) D(g) ? (g = a(this[k], c, e, f), A(g) && (g = u(g))) : Jb(g, a(this[k], c, e, f));
                return A(g) ? g : this
            };
        S.prototype.bind = S.prototype.on;
        S.prototype.unbind = S.prototype.off
    });
    ab.prototype = {
        put: function (a, c) {
            this[Ka(a, this.nextUid)] = c
        }, get: function (a) {
            return this[Ka(a, this.nextUid)]
        }, remove: function (a) {
            var c = this[a = Ka(a, this.nextUid)];
            delete this[a];
            return c
        }
    };
    var qe = /^function\s*[^\(]*\(\s*([^\)]*)\)/m, re = /,/, se = /^\s*(_?)(\S+?)\1\s*$/,
        pe = /((\/\/.*$)|(\/\*[\s\S]*?\*\/))/mg,
        bb = x("$injector"), Le = x("$animate"), Md = ["$provide", function (a) {
            this.$$selectors = {};
            this.register = function (c, d) {
                var e = c + "-animation";
                if (c && "." != c.charAt(0)) throw Le("notcsel", c);
                this.$$selectors[c.substr(1)] = e;
                a.factory(e, d)
            };
            this.classNameFilter = function (a) {
                1 === arguments.length && (this.$$classNameFilter = a instanceof RegExp ? a : null);
                return this.$$classNameFilter
            };
            this.$get = ["$timeout", "$$asyncCallback", function (a, d) {
                return {
                    enter: function (a, c, g, k) {
                        g ? g.after(a) : (c && c[0] || (c = g.parent()), c.append(a));
                        k &&
                        d(k)
                    }, leave: function (a, c) {
                        a.remove();
                        c && d(c)
                    }, move: function (a, c, d, k) {
                        this.enter(a, c, d, k)
                    }, addClass: function (a, c, g) {
                        c = z(c) ? c : H(c) ? c.join(" ") : "";
                        r(a, function (a) {
                            mb(a, c)
                        });
                        g && d(g)
                    }, removeClass: function (a, c, g) {
                        c = z(c) ? c : H(c) ? c.join(" ") : "";
                        r(a, function (a) {
                            lb(a, c)
                        });
                        g && d(g)
                    }, setClass: function (a, c, g, k) {
                        r(a, function (a) {
                            mb(a, c);
                            lb(a, g)
                        });
                        k && d(k)
                    }, enabled: y
                }
            }]
        }], ia = x("$compile");
    ic.$inject = ["$provide", "$$sanitizeUriProvider"];
    var ue = /^(x[\:\-_]|data[\:\-_])/i, yc = x("$interpolate"), Me = /^([^\?#]*)(\?([^#]*))?(#(.*))?$/,
        xe = {http: 80, https: 443, ftp: 21}, Sb = x("$location");
    Ub.prototype = Tb.prototype = Bc.prototype = {
        $$html5: !1, $$replace: !1, absUrl: sb("$$absUrl"), url: function (a, c) {
            if (D(a)) return this.$$url;
            var d = Me.exec(a);
            d[1] && this.path(decodeURIComponent(d[1]));
            (d[2] || d[1]) && this.search(d[3] || "");
            this.hash(d[5] || "", c);
            return this
        }, protocol: sb("$$protocol"), host: sb("$$host"), port: sb("$$port"), path: Cc("$$path", function (a) {
            return "/" == a.charAt(0) ? a : "/" + a
        }), search: function (a, c) {
            switch (arguments.length) {
                case 0:
                    return this.$$search;
                case 1:
                    if (z(a)) this.$$search = ec(a); else if (T(a)) r(a, function (c, e) {
                        null == c && delete a[e]
                    }), this.$$search = a; else throw Sb("isrcharg");
                    break;
                default:
                    D(c) || null === c ? delete this.$$search[a] : this.$$search[a] = c
            }
            this.$$compose();
            return this
        }, hash: Cc("$$hash", Ga), replace: function () {
            this.$$replace = !0;
            return this
        }
    };
    var ka = x("$parse"), Fc = {}, va, Ne = Function.prototype.call, Oe = Function.prototype.apply,
        Qc = Function.prototype.bind, eb = {
            "null": function () {
                return null
            }, "true": function () {
                return !0
            }, "false": function () {
                return !1
            },
            undefined: y, "+": function (a, c, d, e) {
                d = d(a, c);
                e = e(a, c);
                return A(d) ? A(e) ? d + e : d : A(e) ? e : t
            }, "-": function (a, c, d, e) {
                d = d(a, c);
                e = e(a, c);
                return (A(d) ? d : 0) - (A(e) ? e : 0)
            }, "*": function (a, c, d, e) {
                return d(a, c) * e(a, c)
            }, "/": function (a, c, d, e) {
                return d(a, c) / e(a, c)
            }, "%": function (a, c, d, e) {
                return d(a, c) % e(a, c)
            }, "^": function (a, c, d, e) {
                return d(a, c) ^ e(a, c)
            }, "=": y, "===": function (a, c, d, e) {
                return d(a, c) === e(a, c)
            }, "!==": function (a, c, d, e) {
                return d(a, c) !== e(a, c)
            }, "==": function (a, c, d, e) {
                return d(a, c) == e(a, c)
            }, "!=": function (a, c, d, e) {
                return d(a,
                    c) != e(a, c)
            }, "<": function (a, c, d, e) {
                return d(a, c) < e(a, c)
            }, ">": function (a, c, d, e) {
                return d(a, c) > e(a, c)
            }, "<=": function (a, c, d, e) {
                return d(a, c) <= e(a, c)
            }, ">=": function (a, c, d, e) {
                return d(a, c) >= e(a, c)
            }, "&&": function (a, c, d, e) {
                return d(a, c) && e(a, c)
            }, "||": function (a, c, d, e) {
                return d(a, c) || e(a, c)
            }, "&": function (a, c, d, e) {
                return d(a, c) & e(a, c)
            }, "|": function (a, c, d, e) {
                return e(a, c)(a, c, d(a, c))
            }, "!": function (a, c, d) {
                return !d(a, c)
            }
        }, Pe = {n: "\n", f: "\f", r: "\r", t: "\t", v: "\v", "'": "'", '"': '"'}, Wb = function (a) {
            this.options = a
        };
    Wb.prototype =
        {
            constructor: Wb, lex: function (a) {
                this.text = a;
                this.index = 0;
                this.ch = t;
                this.lastCh = ":";
                for (this.tokens = []; this.index < this.text.length;) {
                    this.ch = this.text.charAt(this.index);
                    if (this.is("\"'")) this.readString(this.ch); else if (this.isNumber(this.ch) || this.is(".") && this.isNumber(this.peek())) this.readNumber(); else if (this.isIdent(this.ch)) this.readIdent(); else if (this.is("(){}[].,;:?")) this.tokens.push({
                        index: this.index,
                        text: this.ch
                    }), this.index++; else if (this.isWhitespace(this.ch)) {
                        this.index++;
                        continue
                    } else {
                        a =
                            this.ch + this.peek();
                        var c = a + this.peek(2), d = eb[this.ch], e = eb[a], f = eb[c];
                        f ? (this.tokens.push({
                            index: this.index,
                            text: c,
                            fn: f
                        }), this.index += 3) : e ? (this.tokens.push({
                            index: this.index,
                            text: a,
                            fn: e
                        }), this.index += 2) : d ? (this.tokens.push({
                            index: this.index,
                            text: this.ch,
                            fn: d
                        }), this.index += 1) : this.throwError("Unexpected next character ", this.index, this.index + 1)
                    }
                    this.lastCh = this.ch
                }
                return this.tokens
            }, is: function (a) {
                return -1 !== a.indexOf(this.ch)
            }, was: function (a) {
                return -1 !== a.indexOf(this.lastCh)
            }, peek: function (a) {
                a =
                    a || 1;
                return this.index + a < this.text.length ? this.text.charAt(this.index + a) : !1
            }, isNumber: function (a) {
                return "0" <= a && "9" >= a
            }, isWhitespace: function (a) {
                return " " === a || "\r" === a || "\t" === a || "\n" === a || "\v" === a || "\u00a0" === a
            }, isIdent: function (a) {
                return "a" <= a && "z" >= a || "A" <= a && "Z" >= a || "_" === a || "$" === a
            }, isExpOperator: function (a) {
                return "-" === a || "+" === a || this.isNumber(a)
            }, throwError: function (a, c, d) {
                d = d || this.index;
                c = A(c) ? "s " + c + "-" + this.index + " [" + this.text.substring(c, d) + "]" : " " + d;
                throw ka("lexerr", a, c, this.text);
            }, readNumber: function () {
                for (var a = "", c = this.index; this.index < this.text.length;) {
                    var d = N(this.text.charAt(this.index));
                    if ("." == d || this.isNumber(d)) a += d; else {
                        var e = this.peek();
                        if ("e" == d && this.isExpOperator(e)) a += d; else if (this.isExpOperator(d) && e && this.isNumber(e) && "e" == a.charAt(a.length - 1)) a += d; else if (!this.isExpOperator(d) || e && this.isNumber(e) || "e" != a.charAt(a.length - 1)) break; else this.throwError("Invalid exponent")
                    }
                    this.index++
                }
                a *= 1;
                this.tokens.push({
                    index: c, text: a, literal: !0, constant: !0, fn: function () {
                        return a
                    }
                })
            },
            readIdent: function () {
                for (var a = this, c = "", d = this.index, e, f, g, k; this.index < this.text.length;) {
                    k = this.text.charAt(this.index);
                    if ("." === k || this.isIdent(k) || this.isNumber(k)) "." === k && (e = this.index), c += k; else break;
                    this.index++
                }
                if (e) for (f = this.index; f < this.text.length;) {
                    k = this.text.charAt(f);
                    if ("(" === k) {
                        g = c.substr(e - d + 1);
                        c = c.substr(0, e - d);
                        this.index = f;
                        break
                    }
                    if (this.isWhitespace(k)) f++; else break
                }
                d = {index: d, text: c};
                if (eb.hasOwnProperty(c)) d.fn = eb[c], d.literal = !0, d.constant = !0; else {
                    var m = Ec(c, this.options,
                        this.text);
                    d.fn = B(function (a, c) {
                        return m(a, c)
                    }, {
                        assign: function (d, e) {
                            return tb(d, c, e, a.text, a.options)
                        }
                    })
                }
                this.tokens.push(d);
                g && (this.tokens.push({index: e, text: "."}), this.tokens.push({index: e + 1, text: g}))
            }, readString: function (a) {
                var c = this.index;
                this.index++;
                for (var d = "", e = a, f = !1; this.index < this.text.length;) {
                    var g = this.text.charAt(this.index), e = e + g;
                    if (f) "u" === g ? (f = this.text.substring(this.index + 1, this.index + 5), f.match(/[\da-f]{4}/i) || this.throwError("Invalid unicode escape [\\u" + f + "]"), this.index +=
                        4, d += String.fromCharCode(parseInt(f, 16))) : d += Pe[g] || g, f = !1; else if ("\\" === g) f = !0; else {
                        if (g === a) {
                            this.index++;
                            this.tokens.push({
                                index: c, text: e, string: d, literal: !0, constant: !0, fn: function () {
                                    return d
                                }
                            });
                            return
                        }
                        d += g
                    }
                    this.index++
                }
                this.throwError("Unterminated quote", c)
            }
        };
    var db = function (a, c, d) {
        this.lexer = a;
        this.$filter = c;
        this.options = d
    };
    db.ZERO = B(function () {
        return 0
    }, {constant: !0});
    db.prototype = {
        constructor: db, parse: function (a) {
            this.text = a;
            this.tokens = this.lexer.lex(a);
            a = this.statements();
            0 !== this.tokens.length &&
            this.throwError("is an unexpected token", this.tokens[0]);
            a.literal = !!a.literal;
            a.constant = !!a.constant;
            return a
        }, primary: function () {
            var a;
            if (this.expect("(")) a = this.filterChain(), this.consume(")"); else if (this.expect("[")) a = this.arrayDeclaration(); else if (this.expect("{")) a = this.object(); else {
                var c = this.expect();
                (a = c.fn) || this.throwError("not a primary expression", c);
                a.literal = !!c.literal;
                a.constant = !!c.constant
            }
            for (var d; c = this.expect("(", "[", ".");) "(" === c.text ? (a = this.functionCall(a, d), d = null) :
                "[" === c.text ? (d = a, a = this.objectIndex(a)) : "." === c.text ? (d = a, a = this.fieldAccess(a)) : this.throwError("IMPOSSIBLE");
            return a
        }, throwError: function (a, c) {
            throw ka("syntax", c.text, a, c.index + 1, this.text, this.text.substring(c.index));
        }, peekToken: function () {
            if (0 === this.tokens.length) throw ka("ueoe", this.text);
            return this.tokens[0]
        }, peek: function (a, c, d, e) {
            if (0 < this.tokens.length) {
                var f = this.tokens[0], g = f.text;
                if (g === a || g === c || g === d || g === e || !(a || c || d || e)) return f
            }
            return !1
        }, expect: function (a, c, d, e) {
            return (a = this.peek(a,
                c, d, e)) ? (this.tokens.shift(), a) : !1
        }, consume: function (a) {
            this.expect(a) || this.throwError("is unexpected, expecting [" + a + "]", this.peek())
        }, unaryFn: function (a, c) {
            return B(function (d, e) {
                return a(d, e, c)
            }, {constant: c.constant})
        }, ternaryFn: function (a, c, d) {
            return B(function (e, f) {
                return a(e, f) ? c(e, f) : d(e, f)
            }, {constant: a.constant && c.constant && d.constant})
        }, binaryFn: function (a, c, d) {
            return B(function (e, f) {
                return c(e, f, a, d)
            }, {constant: a.constant && d.constant})
        }, statements: function () {
            for (var a = []; ;) if (0 < this.tokens.length &&
            !this.peek("}", ")", ";", "]") && a.push(this.filterChain()), !this.expect(";")) return 1 === a.length ? a[0] : function (c, d) {
                for (var e, f = 0; f < a.length; f++) {
                    var g = a[f];
                    g && (e = g(c, d))
                }
                return e
            }
        }, filterChain: function () {
            for (var a = this.expression(), c; ;) if (c = this.expect("|")) a = this.binaryFn(a, c.fn, this.filter()); else return a
        }, filter: function () {
            for (var a = this.expect(), c = this.$filter(a.text), d = []; ;) if (a = this.expect(":")) d.push(this.expression()); else {
                var e = function (a, e, k) {
                    k = [k];
                    for (var m = 0; m < d.length; m++) k.push(d[m](a,
                        e));
                    return c.apply(a, k)
                };
                return function () {
                    return e
                }
            }
        }, expression: function () {
            return this.assignment()
        }, assignment: function () {
            var a = this.ternary(), c, d;
            return (d = this.expect("=")) ? (a.assign || this.throwError("implies assignment but [" + this.text.substring(0, d.index) + "] can not be assigned to", d), c = this.ternary(), function (d, f) {
                return a.assign(d, c(d, f), f)
            }) : a
        }, ternary: function () {
            var a = this.logicalOR(), c, d;
            if (this.expect("?")) {
                c = this.assignment();
                if (d = this.expect(":")) return this.ternaryFn(a, c, this.assignment());
                this.throwError("expected :", d)
            } else return a
        }, logicalOR: function () {
            for (var a = this.logicalAND(), c; ;) if (c = this.expect("||")) a = this.binaryFn(a, c.fn, this.logicalAND()); else return a
        }, logicalAND: function () {
            var a = this.equality(), c;
            if (c = this.expect("&&")) a = this.binaryFn(a, c.fn, this.logicalAND());
            return a
        }, equality: function () {
            var a = this.relational(), c;
            if (c = this.expect("==", "!=", "===", "!==")) a = this.binaryFn(a, c.fn, this.equality());
            return a
        }, relational: function () {
            var a = this.additive(), c;
            if (c = this.expect("<",
                ">", "<=", ">=")) a = this.binaryFn(a, c.fn, this.relational());
            return a
        }, additive: function () {
            for (var a = this.multiplicative(), c; c = this.expect("+", "-");) a = this.binaryFn(a, c.fn, this.multiplicative());
            return a
        }, multiplicative: function () {
            for (var a = this.unary(), c; c = this.expect("*", "/", "%");) a = this.binaryFn(a, c.fn, this.unary());
            return a
        }, unary: function () {
            var a;
            return this.expect("+") ? this.primary() : (a = this.expect("-")) ? this.binaryFn(db.ZERO, a.fn, this.unary()) : (a = this.expect("!")) ? this.unaryFn(a.fn, this.unary()) :
                this.primary()
        }, fieldAccess: function (a) {
            var c = this, d = this.expect().text, e = Ec(d, this.options, this.text);
            return B(function (c, d, k) {
                return e(k || a(c, d))
            }, {
                assign: function (e, g, k) {
                    (k = a(e, k)) || a.assign(e, k = {});
                    return tb(k, d, g, c.text, c.options)
                }
            })
        }, objectIndex: function (a) {
            var c = this, d = this.expression();
            this.consume("]");
            return B(function (e, f) {
                var g = a(e, f), k = d(e, f), m;
                ja(k, c.text);
                if (!g) return t;
                (g = Oa(g[k], c.text)) && (g.then && c.options.unwrapPromises) && (m = g, "$$v" in g || (m.$$v = t, m.then(function (a) {
                    m.$$v = a
                })), g =
                    g.$$v);
                return g
            }, {
                assign: function (e, f, g) {
                    var k = ja(d(e, g), c.text);
                    (g = Oa(a(e, g), c.text)) || a.assign(e, g = {});
                    return g[k] = f
                }
            })
        }, functionCall: function (a, c) {
            var d = [];
            if (")" !== this.peekToken().text) {
                do d.push(this.expression()); while (this.expect(","))
            }
            this.consume(")");
            var e = this;
            return function (f, g) {
                for (var k = [], m = c ? c(f, g) : f, h = 0; h < d.length; h++) k.push(d[h](f, g));
                h = a(f, g, m) || y;
                Oa(m, e.text);
                var l = e.text;
                if (h) {
                    if (h.constructor === h) throw ka("isecfn", l);
                    if (h === Ne || h === Oe || Qc && h === Qc) throw ka("isecff", l);
                }
                k = h.apply ?
                    h.apply(m, k) : h(k[0], k[1], k[2], k[3], k[4]);
                return Oa(k, e.text)
            }
        }, arrayDeclaration: function () {
            var a = [], c = !0;
            if ("]" !== this.peekToken().text) {
                do {
                    if (this.peek("]")) break;
                    var d = this.expression();
                    a.push(d);
                    d.constant || (c = !1)
                } while (this.expect(","))
            }
            this.consume("]");
            return B(function (c, d) {
                for (var g = [], k = 0; k < a.length; k++) g.push(a[k](c, d));
                return g
            }, {literal: !0, constant: c})
        }, object: function () {
            var a = [], c = !0;
            if ("}" !== this.peekToken().text) {
                do {
                    if (this.peek("}")) break;
                    var d = this.expect(), d = d.string || d.text;
                    this.consume(":");
                    var e = this.expression();
                    a.push({key: d, value: e});
                    e.constant || (c = !1)
                } while (this.expect(","))
            }
            this.consume("}");
            return B(function (c, d) {
                for (var e = {}, m = 0; m < a.length; m++) {
                    var h = a[m];
                    e[h.key] = h.value(c, d)
                }
                return e
            }, {literal: !0, constant: c})
        }
    };
    var Vb = {}, wa = x("$sce"), fa = {HTML: "html", CSS: "css", URL: "url", RESOURCE_URL: "resourceUrl", JS: "js"},
        V = X.createElement("a"), Hc = ua(Q.location.href, !0);
    mc.$inject = ["$provide"];
    Ic.$inject = ["$locale"];
    Kc.$inject = ["$locale"];
    var Nc = ".", He = {
        yyyy: Y("FullYear", 4),
        yy: Y("FullYear",
            2, 0, !0),
        y: Y("FullYear", 1),
        MMMM: ub("Month"),
        MMM: ub("Month", !0),
        MM: Y("Month", 2, 1),
        M: Y("Month", 1, 1),
        dd: Y("Date", 2),
        d: Y("Date", 1),
        HH: Y("Hours", 2),
        H: Y("Hours", 1),
        hh: Y("Hours", 2, -12),
        h: Y("Hours", 1, -12),
        mm: Y("Minutes", 2),
        m: Y("Minutes", 1),
        ss: Y("Seconds", 2),
        s: Y("Seconds", 1),
        sss: Y("Milliseconds", 3),
        EEEE: ub("Day"),
        EEE: ub("Day", !0),
        a: function (a, c) {
            return 12 > a.getHours() ? c.AMPMS[0] : c.AMPMS[1]
        },
        Z: function (a) {
            a = -1 * a.getTimezoneOffset();
            return a = (0 <= a ? "+" : "") + (Xb(Math[0 < a ? "floor" : "ceil"](a / 60), 2) + Xb(Math.abs(a % 60),
                2))
        }
    }, Ge = /((?:[^yMdHhmsaZE']+)|(?:'(?:[^']|'')*')|(?:E+|y+|M+|d+|H+|h+|m+|s+|a|Z))(.*)/, Fe = /^\-?\d+$/;
    Jc.$inject = ["$locale"];
    var De = $(N), Ee = $(Ia);
    Lc.$inject = ["$parse"];
    var dd = $({
        restrict: "E", compile: function (a, c) {
            8 >= R && (c.href || c.name || c.$set("href", ""), a.append(X.createComment("IE fix")));
            if (!c.href && !c.xlinkHref && !c.name) return function (a, c) {
                var f = "[object SVGAnimatedString]" === ya.call(c.prop("href")) ? "xlink:href" : "href";
                c.on("click", function (a) {
                    c.attr(f) || a.preventDefault()
                })
            }
        }
    }), Fb = {};
    r(ob, function (a,
                    c) {
        if ("multiple" != a) {
            var d = na("ng-" + c);
            Fb[d] = function () {
                return {
                    priority: 100, link: function (a, f, g) {
                        a.$watch(g[d], function (a) {
                            g.$set(c, !!a)
                        })
                    }
                }
            }
        }
    });
    r(["src", "srcset", "href"], function (a) {
        var c = na("ng-" + a);
        Fb[c] = function () {
            return {
                priority: 99, link: function (d, e, f) {
                    var g = a, k = a;
                    "href" === a && "[object SVGAnimatedString]" === ya.call(e.prop("href")) && (k = "xlinkHref", f.$attr[k] = "xlink:href", g = null);
                    f.$observe(c, function (c) {
                        c ? (f.$set(k, c), R && g && e.prop(g, f[k])) : "href" === a && f.$set(k, null)
                    })
                }
            }
        }
    });
    var xb = {
        $addControl: y,
        $removeControl: y, $setValidity: y, $setDirty: y, $setPristine: y
    };
    Oc.$inject = ["$element", "$attrs", "$scope", "$animate"];
    var Rc = function (a) {
            return ["$timeout", function (c) {
                return {
                    name: "form", restrict: a ? "EAC" : "E", controller: Oc, compile: function () {
                        return {
                            pre: function (a, e, f, g) {
                                if (!f.action) {
                                    var k = function (a) {
                                        a.preventDefault ? a.preventDefault() : a.returnValue = !1
                                    };
                                    rb(e[0], "submit", k);
                                    e.on("$destroy", function () {
                                        c(function () {
                                            Za(e[0], "submit", k)
                                        }, 0, !1)
                                    })
                                }
                                var m = e.parent().controller("form"), h = f.name || f.ngForm;
                                h && tb(a,
                                    h, g, h);
                                if (m) e.on("$destroy", function () {
                                    m.$removeControl(g);
                                    h && tb(a, h, t, h);
                                    B(g, xb)
                                })
                            }
                        }
                    }
                }
            }]
        }, ed = Rc(), rd = Rc(!0),
        Qe = /^(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?$/,
        Re = /^[a-z0-9!#$%&'*+\/=?^_`{|}~.-]+@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$/i,
        Se = /^\s*(\-|\+)?(\d+|(\d*(\.\d*)))\s*$/, Sc = {
            text: zb, number: function (a, c, d, e, f, g) {
                zb(a, c, d, e, f, g);
                e.$parsers.push(function (a) {
                    var c = e.$isEmpty(a);
                    if (c || Se.test(a)) return e.$setValidity("number", !0), "" ===
                    a ? null : c ? a : parseFloat(a);
                    e.$setValidity("number", !1);
                    return t
                });
                Ie(e, "number", Te, null, e.$$validityState);
                e.$formatters.push(function (a) {
                    return e.$isEmpty(a) ? "" : "" + a
                });
                d.min && (a = function (a) {
                    var c = parseFloat(d.min);
                    return ra(e, "min", e.$isEmpty(a) || a >= c, a)
                }, e.$parsers.push(a), e.$formatters.push(a));
                d.max && (a = function (a) {
                    var c = parseFloat(d.max);
                    return ra(e, "max", e.$isEmpty(a) || a <= c, a)
                }, e.$parsers.push(a), e.$formatters.push(a));
                e.$formatters.push(function (a) {
                    return ra(e, "number", e.$isEmpty(a) || Ab(a), a)
                })
            },
            url: function (a, c, d, e, f, g) {
                zb(a, c, d, e, f, g);
                a = function (a) {
                    return ra(e, "url", e.$isEmpty(a) || Qe.test(a), a)
                };
                e.$formatters.push(a);
                e.$parsers.push(a)
            }, email: function (a, c, d, e, f, g) {
                zb(a, c, d, e, f, g);
                a = function (a) {
                    return ra(e, "email", e.$isEmpty(a) || Re.test(a), a)
                };
                e.$formatters.push(a);
                e.$parsers.push(a)
            }, radio: function (a, c, d, e) {
                D(d.name) && c.attr("name", gb());
                c.on("click", function () {
                    c[0].checked && a.$apply(function () {
                        e.$setViewValue(d.value)
                    })
                });
                e.$render = function () {
                    c[0].checked = d.value == e.$viewValue
                };
                d.$observe("value",
                    e.$render)
            }, checkbox: function (a, c, d, e) {
                var f = d.ngTrueValue, g = d.ngFalseValue;
                z(f) || (f = !0);
                z(g) || (g = !1);
                c.on("click", function () {
                    a.$apply(function () {
                        e.$setViewValue(c[0].checked)
                    })
                });
                e.$render = function () {
                    c[0].checked = e.$viewValue
                };
                e.$isEmpty = function (a) {
                    return a !== f
                };
                e.$formatters.push(function (a) {
                    return a === f
                });
                e.$parsers.push(function (a) {
                    return a ? f : g
                })
            }, hidden: y, button: y, submit: y, reset: y, file: y
        }, Te = ["badInput"], jc = ["$browser", "$sniffer", function (a, c) {
            return {
                restrict: "E", require: "?ngModel", link: function (d,
                                                                    e, f, g) {
                    g && (Sc[N(f.type)] || Sc.text)(d, e, f, g, c, a)
                }
            }
        }], wb = "ng-valid", vb = "ng-invalid", Pa = "ng-pristine", yb = "ng-dirty",
        Ue = ["$scope", "$exceptionHandler", "$attrs", "$element", "$parse", "$animate", function (a, c, d, e, f, g) {
            function k(a, c) {
                c = c ? "-" + kb(c, "-") : "";
                g.removeClass(e, (a ? vb : wb) + c);
                g.addClass(e, (a ? wb : vb) + c)
            }

            this.$modelValue = this.$viewValue = Number.NaN;
            this.$parsers = [];
            this.$formatters = [];
            this.$viewChangeListeners = [];
            this.$pristine = !0;
            this.$dirty = !1;
            this.$valid = !0;
            this.$invalid = !1;
            this.$name = d.name;
            var m = f(d.ngModel),
                h = m.assign;
            if (!h) throw x("ngModel")("nonassign", d.ngModel, ha(e));
            this.$render = y;
            this.$isEmpty = function (a) {
                return D(a) || "" === a || null === a || a !== a
            };
            var l = e.inheritedData("$formController") || xb, n = 0, p = this.$error = {};
            e.addClass(Pa);
            k(!0);
            this.$setValidity = function (a, c) {
                p[a] !== !c && (c ? (p[a] && n--, n || (k(!0), this.$valid = !0, this.$invalid = !1)) : (k(!1), this.$invalid = !0, this.$valid = !1, n++), p[a] = !c, k(c, a), l.$setValidity(a, c, this))
            };
            this.$setPristine = function () {
                this.$dirty = !1;
                this.$pristine = !0;
                g.removeClass(e, yb);
                g.addClass(e,
                    Pa)
            };
            this.$setViewValue = function (d) {
                this.$viewValue = d;
                this.$pristine && (this.$dirty = !0, this.$pristine = !1, g.removeClass(e, Pa), g.addClass(e, yb), l.$setDirty());
                r(this.$parsers, function (a) {
                    d = a(d)
                });
                this.$modelValue !== d && (this.$modelValue = d, h(a, d), r(this.$viewChangeListeners, function (a) {
                    try {
                        a()
                    } catch (d) {
                        c(d)
                    }
                }))
            };
            var q = this;
            a.$watch(function () {
                var c = m(a);
                if (q.$modelValue !== c) {
                    var d = q.$formatters, e = d.length;
                    for (q.$modelValue = c; e--;) c = d[e](c);
                    q.$viewValue !== c && (q.$viewValue = c, q.$render())
                }
                return c
            })
        }], Gd =
            function () {
                return {
                    require: ["ngModel", "^?form"], controller: Ue, link: function (a, c, d, e) {
                        var f = e[0], g = e[1] || xb;
                        g.$addControl(f);
                        a.$on("$destroy", function () {
                            g.$removeControl(f)
                        })
                    }
                }
            }, Id = $({
            require: "ngModel", link: function (a, c, d, e) {
                e.$viewChangeListeners.push(function () {
                    a.$eval(d.ngChange)
                })
            }
        }), kc = function () {
            return {
                require: "?ngModel", link: function (a, c, d, e) {
                    if (e) {
                        d.required = !0;
                        var f = function (a) {
                            if (d.required && e.$isEmpty(a)) e.$setValidity("required", !1); else return e.$setValidity("required", !0), a
                        };
                        e.$formatters.push(f);
                        e.$parsers.unshift(f);
                        d.$observe("required", function () {
                            f(e.$viewValue)
                        })
                    }
                }
            }
        }, Hd = function () {
            return {
                require: "ngModel", link: function (a, c, d, e) {
                    var f = (a = /\/(.*)\//.exec(d.ngList)) && RegExp(a[1]) || d.ngList || ",";
                    e.$parsers.push(function (a) {
                        if (!D(a)) {
                            var c = [];
                            a && r(a.split(f), function (a) {
                                a && c.push(aa(a))
                            });
                            return c
                        }
                    });
                    e.$formatters.push(function (a) {
                        return H(a) ? a.join(", ") : t
                    });
                    e.$isEmpty = function (a) {
                        return !a || !a.length
                    }
                }
            }
        }, Ve = /^(true|false|\d+)$/, Jd = function () {
            return {
                priority: 100, compile: function (a, c) {
                    return Ve.test(c.ngValue) ?
                        function (a, c, f) {
                            f.$set("value", a.$eval(f.ngValue))
                        } : function (a, c, f) {
                            a.$watch(f.ngValue, function (a) {
                                f.$set("value", a)
                            })
                        }
                }
            }
        }, jd = xa({
            compile: function (a) {
                a.addClass("ng-binding");
                return function (a, d, e) {
                    d.data("$binding", e.ngBind);
                    a.$watch(e.ngBind, function (a) {
                        d.text(a == t ? "" : a)
                    })
                }
            }
        }), ld = ["$interpolate", function (a) {
            return function (c, d, e) {
                c = a(d.attr(e.$attr.ngBindTemplate));
                d.addClass("ng-binding").data("$binding", c);
                e.$observe("ngBindTemplate", function (a) {
                    d.text(a)
                })
            }
        }], kd = ["$sce", "$parse", function (a, c) {
            return {
                compile: function (d) {
                    d.addClass("ng-binding");
                    return function (d, f, g) {
                        f.data("$binding", g.ngBindHtml);
                        var k = c(g.ngBindHtml);
                        d.$watch(function () {
                            return (k(d) || "").toString()
                        }, function (c) {
                            f.html(a.getTrustedHtml(k(d)) || "")
                        })
                    }
                }
            }
        }], md = Yb("", !0), od = Yb("Odd", 0), nd = Yb("Even", 1), pd = xa({
            compile: function (a, c) {
                c.$set("ngCloak", t);
                a.removeClass("ng-cloak")
            }
        }), qd = [function () {
            return {scope: !0, controller: "@", priority: 500}
        }], lc = {};
    r("click dblclick mousedown mouseup mouseover mouseout mousemove mouseenter mouseleave keydown keyup keypress submit focus blur copy cut paste".split(" "),
        function (a) {
            var c = na("ng-" + a);
            lc[c] = ["$parse", function (d) {
                return {
                    compile: function (e, f) {
                        var g = d(f[c]);
                        return function (c, d) {
                            d.on(N(a), function (a) {
                                c.$apply(function () {
                                    g(c, {$event: a})
                                })
                            })
                        }
                    }
                }
            }]
        });
    var td = ["$animate", function (a) {
        return {
            transclude: "element",
            priority: 600,
            terminal: !0,
            restrict: "A",
            $$tlb: !0,
            link: function (c, d, e, f, g) {
                var k, m, h;
                c.$watch(e.ngIf, function (f) {
                    Ta(f) ? m || (m = c.$new(), g(m, function (c) {
                        c[c.length++] = X.createComment(" end ngIf: " + e.ngIf + " ");
                        k = {clone: c};
                        a.enter(c, d.parent(), d)
                    })) : (h && (h.remove(),
                        h = null), m && (m.$destroy(), m = null), k && (h = Eb(k.clone), a.leave(h, function () {
                        h = null
                    }), k = null))
                })
            }
        }
    }], ud = ["$http", "$templateCache", "$anchorScroll", "$animate", "$sce", function (a, c, d, e, f) {
        return {
            restrict: "ECA",
            priority: 400,
            terminal: !0,
            transclude: "element",
            controller: Ua.noop,
            compile: function (g, k) {
                var m = k.ngInclude || k.src, h = k.onload || "", l = k.autoscroll;
                return function (g, k, q, r, L) {
                    var v = 0, t, u, I, w = function () {
                        u && (u.remove(), u = null);
                        t && (t.$destroy(), t = null);
                        I && (e.leave(I, function () {
                            u = null
                        }), u = I, I = null)
                    };
                    g.$watch(f.parseAsResourceUrl(m),
                        function (f) {
                            var m = function () {
                                !A(l) || l && !g.$eval(l) || d()
                            }, q = ++v;
                            f ? (a.get(f, {cache: c}).success(function (a) {
                                if (q === v) {
                                    var c = g.$new();
                                    r.template = a;
                                    a = L(c, function (a) {
                                        w();
                                        e.enter(a, null, k, m)
                                    });
                                    t = c;
                                    I = a;
                                    t.$emit("$includeContentLoaded");
                                    g.$eval(h)
                                }
                            }).error(function () {
                                q === v && w()
                            }), g.$emit("$includeContentRequested")) : (w(), r.template = null)
                        })
                }
            }
        }
    }], Kd = ["$compile", function (a) {
        return {
            restrict: "ECA", priority: -400, require: "ngInclude", link: function (c, d, e, f) {
                d.html(f.template);
                a(d.contents())(c)
            }
        }
    }], vd = xa({
        priority: 450,
        compile: function () {
            return {
                pre: function (a, c, d) {
                    a.$eval(d.ngInit)
                }
            }
        }
    }), wd = xa({terminal: !0, priority: 1E3}), xd = ["$locale", "$interpolate", function (a, c) {
        var d = /{}/g;
        return {
            restrict: "EA", link: function (e, f, g) {
                var k = g.count, m = g.$attr.when && f.attr(g.$attr.when), h = g.offset || 0, l = e.$eval(m) || {},
                    n = {}, p = c.startSymbol(), q = c.endSymbol(), s = /^when(Minus)?(.+)$/;
                r(g, function (a, c) {
                    s.test(c) && (l[N(c.replace("when", "").replace("Minus", "-"))] = f.attr(g.$attr[c]))
                });
                r(l, function (a, e) {
                    n[e] = c(a.replace(d, p + k + "-" + h + q))
                });
                e.$watch(function () {
                    var c =
                        parseFloat(e.$eval(k));
                    if (isNaN(c)) return "";
                    c in l || (c = a.pluralCat(c - h));
                    return n[c](e, f, !0)
                }, function (a) {
                    f.text(a)
                })
            }
        }
    }], yd = ["$parse", "$animate", function (a, c) {
        var d = x("ngRepeat");
        return {
            transclude: "element", priority: 1E3, terminal: !0, $$tlb: !0, link: function (e, f, g, k, m) {
                var h = g.ngRepeat, l = h.match(/^\s*([\s\S]+?)\s+in\s+([\s\S]+?)(?:\s+track\s+by\s+([\s\S]+?))?\s*$/),
                    n, p, q, s, t, v, C = {$id: Ka};
                if (!l) throw d("iexp", h);
                g = l[1];
                k = l[2];
                (l = l[3]) ? (n = a(l), p = function (a, c, d) {
                    v && (C[v] = a);
                    C[t] = c;
                    C.$index = d;
                    return n(e,
                        C)
                }) : (q = function (a, c) {
                    return Ka(c)
                }, s = function (a) {
                    return a
                });
                l = g.match(/^(?:([\$\w]+)|\(([\$\w]+)\s*,\s*([\$\w]+)\))$/);
                if (!l) throw d("iidexp", g);
                t = l[3] || l[1];
                v = l[2];
                var A = {};
                e.$watchCollection(k, function (a) {
                    var g, k, l = f[0], n, C = {}, J, E, F, x, z, y, H = [];
                    if (fb(a)) z = a, n = p || q; else {
                        n = p || s;
                        z = [];
                        for (F in a) a.hasOwnProperty(F) && "$" != F.charAt(0) && z.push(F);
                        z.sort()
                    }
                    J = z.length;
                    k = H.length = z.length;
                    for (g = 0; g < k; g++) if (F = a === z ? g : z[g], x = a[F], x = n(F, x, g), Ca(x, "`track by` id"), A.hasOwnProperty(x)) y = A[x], delete A[x], C[x] =
                        y, H[g] = y; else {
                        if (C.hasOwnProperty(x)) throw r(H, function (a) {
                            a && a.scope && (A[a.id] = a)
                        }), d("dupes", h, x);
                        H[g] = {id: x};
                        C[x] = !1
                    }
                    for (F in A) A.hasOwnProperty(F) && (y = A[F], g = Eb(y.clone), c.leave(g), r(g, function (a) {
                        a.$$NG_REMOVED = !0
                    }), y.scope.$destroy());
                    g = 0;
                    for (k = z.length; g < k; g++) {
                        F = a === z ? g : z[g];
                        x = a[F];
                        y = H[g];
                        H[g - 1] && (l = H[g - 1].clone[H[g - 1].clone.length - 1]);
                        if (y.scope) {
                            E = y.scope;
                            n = l;
                            do n = n.nextSibling; while (n && n.$$NG_REMOVED);
                            y.clone[0] != n && c.move(Eb(y.clone), null, u(l));
                            l = y.clone[y.clone.length - 1]
                        } else E = e.$new();
                        E[t] = x;
                        v && (E[v] = F);
                        E.$index = g;
                        E.$first = 0 === g;
                        E.$last = g === J - 1;
                        E.$middle = !(E.$first || E.$last);
                        E.$odd = !(E.$even = 0 === (g & 1));
                        y.scope || m(E, function (a) {
                            a[a.length++] = X.createComment(" end ngRepeat: " + h + " ");
                            c.enter(a, null, u(l));
                            l = a;
                            y.scope = E;
                            y.clone = a;
                            C[y.id] = y
                        })
                    }
                    A = C
                })
            }
        }
    }], zd = ["$animate", function (a) {
        return function (c, d, e) {
            c.$watch(e.ngShow, function (c) {
                a[Ta(c) ? "removeClass" : "addClass"](d, "ng-hide")
            })
        }
    }], sd = ["$animate", function (a) {
        return function (c, d, e) {
            c.$watch(e.ngHide, function (c) {
                a[Ta(c) ? "addClass" : "removeClass"](d,
                    "ng-hide")
            })
        }
    }], Ad = xa(function (a, c, d) {
        a.$watch(d.ngStyle, function (a, d) {
            d && a !== d && r(d, function (a, d) {
                c.css(d, "")
            });
            a && c.css(a)
        }, !0)
    }), Bd = ["$animate", function (a) {
        return {
            restrict: "EA", require: "ngSwitch", controller: ["$scope", function () {
                this.cases = {}
            }], link: function (c, d, e, f) {
                var g = [], k = [], m = [], h = [];
                c.$watch(e.ngSwitch || e.on, function (d) {
                    var n, p;
                    n = 0;
                    for (p = m.length; n < p; ++n) m[n].remove();
                    n = m.length = 0;
                    for (p = h.length; n < p; ++n) {
                        var q = k[n];
                        h[n].$destroy();
                        m[n] = q;
                        a.leave(q, function () {
                            m.splice(n, 1)
                        })
                    }
                    k.length = 0;
                    h.length =
                        0;
                    if (g = f.cases["!" + d] || f.cases["?"]) c.$eval(e.change), r(g, function (d) {
                        var e = c.$new();
                        h.push(e);
                        d.transclude(e, function (c) {
                            var e = d.element;
                            k.push(c);
                            a.enter(c, e.parent(), e)
                        })
                    })
                })
            }
        }
    }], Cd = xa({
        transclude: "element", priority: 800, require: "^ngSwitch", link: function (a, c, d, e, f) {
            e.cases["!" + d.ngSwitchWhen] = e.cases["!" + d.ngSwitchWhen] || [];
            e.cases["!" + d.ngSwitchWhen].push({transclude: f, element: c})
        }
    }), Dd = xa({
        transclude: "element", priority: 800, require: "^ngSwitch", link: function (a, c, d, e, f) {
            e.cases["?"] = e.cases["?"] ||
                [];
            e.cases["?"].push({transclude: f, element: c})
        }
    }), Fd = xa({
        link: function (a, c, d, e, f) {
            if (!f) throw x("ngTransclude")("orphan", ha(c));
            f(function (a) {
                c.empty();
                c.append(a)
            })
        }
    }), fd = ["$templateCache", function (a) {
        return {
            restrict: "E", terminal: !0, compile: function (c, d) {
                "text/ng-template" == d.type && a.put(d.id, c[0].text)
            }
        }
    }], We = x("ngOptions"), Ed = $({terminal: !0}), gd = ["$compile", "$parse", function (a, c) {
        var d = /^\s*([\s\S]+?)(?:\s+as\s+([\s\S]+?))?(?:\s+group\s+by\s+([\s\S]+?))?\s+for\s+(?:([\$\w][\$\w]*)|(?:\(\s*([\$\w][\$\w]*)\s*,\s*([\$\w][\$\w]*)\s*\)))\s+in\s+([\s\S]+?)(?:\s+track\s+by\s+([\s\S]+?))?$/,
            e = {$setViewValue: y};
        return {
            restrict: "E",
            require: ["select", "?ngModel"],
            controller: ["$element", "$scope", "$attrs", function (a, c, d) {
                var m = this, h = {}, l = e, n;
                m.databound = d.ngModel;
                m.init = function (a, c, d) {
                    l = a;
                    n = d
                };
                m.addOption = function (c) {
                    Ca(c, '"option value"');
                    h[c] = !0;
                    l.$viewValue == c && (a.val(c), n.parent() && n.remove())
                };
                m.removeOption = function (a) {
                    this.hasOption(a) && (delete h[a], l.$viewValue == a && this.renderUnknownOption(a))
                };
                m.renderUnknownOption = function (c) {
                    c = "? " + Ka(c) + " ?";
                    n.val(c);
                    a.prepend(n);
                    a.val(c);
                    n.prop("selected",
                        !0)
                };
                m.hasOption = function (a) {
                    return h.hasOwnProperty(a)
                };
                c.$on("$destroy", function () {
                    m.renderUnknownOption = y
                })
            }],
            link: function (e, g, k, m) {
                function h(a, c, d, e) {
                    d.$render = function () {
                        var a = d.$viewValue;
                        e.hasOption(a) ? (z.parent() && z.remove(), c.val(a), "" === a && v.prop("selected", !0)) : D(a) && v ? c.val("") : e.renderUnknownOption(a)
                    };
                    c.on("change", function () {
                        a.$apply(function () {
                            z.parent() && z.remove();
                            d.$setViewValue(c.val())
                        })
                    })
                }

                function l(a, c, d) {
                    var e;
                    d.$render = function () {
                        var a = new ab(d.$viewValue);
                        r(c.find("option"),
                            function (c) {
                                c.selected = A(a.get(c.value))
                            })
                    };
                    a.$watch(function () {
                        za(e, d.$viewValue) || (e = ga(d.$viewValue), d.$render())
                    });
                    c.on("change", function () {
                        a.$apply(function () {
                            var a = [];
                            r(c.find("option"), function (c) {
                                c.selected && a.push(c.value)
                            });
                            d.$setViewValue(a)
                        })
                    })
                }

                function n(e, f, g) {
                    function k() {
                        var a = {"": []}, c = [""], d, h, s, t, w;
                        s = g.$modelValue;
                        t = v(e) || [];
                        var E = n ? Zb(t) : t, I, M, B;
                        M = {};
                        B = !1;
                        if (q) if (h = g.$modelValue, u && H(h)) for (B = new ab([]), d = {}, w = 0; w < h.length; w++) d[m] = h[w], B.put(u(e, d), h[w]); else B = new ab(h);
                        w = B;
                        var D, J;
                        for (B = 0; I = E.length, B < I; B++) {
                            h = B;
                            if (n) {
                                h = E[B];
                                if ("$" === h.charAt(0)) continue;
                                M[n] = h
                            }
                            M[m] = t[h];
                            d = p(e, M) || "";
                            (h = a[d]) || (h = a[d] = [], c.push(d));
                            q ? d = A(w.remove(u ? u(e, M) : r(e, M))) : (u ? (d = {}, d[m] = s, d = u(e, d) === u(e, M)) : d = s === r(e, M), w = w || d);
                            D = l(e, M);
                            D = A(D) ? D : "";
                            h.push({id: u ? u(e, M) : n ? E[B] : B, label: D, selected: d})
                        }
                        q || (x || null === s ? a[""].unshift({
                            id: "",
                            label: "",
                            selected: !w
                        }) : w || a[""].unshift({id: "?", label: "", selected: !0}));
                        M = 0;
                        for (E = c.length; M < E; M++) {
                            d = c[M];
                            h = a[d];
                            z.length <= M ? (s = {
                                element: y.clone().attr("label",
                                    d), label: h.label
                            }, t = [s], z.push(t), f.append(s.element)) : (t = z[M], s = t[0], s.label != d && s.element.attr("label", s.label = d));
                            D = null;
                            B = 0;
                            for (I = h.length; B < I; B++) d = h[B], (w = t[B + 1]) ? (D = w.element, w.label !== d.label && D.text(w.label = d.label), w.id !== d.id && D.val(w.id = d.id), D[0].selected !== d.selected && (D.prop("selected", w.selected = d.selected), R && D.prop("selected", w.selected))) : ("" === d.id && x ? J = x : (J = C.clone()).val(d.id).prop("selected", d.selected).attr("selected", d.selected).text(d.label), t.push({
                                element: J, label: d.label,
                                id: d.id, selected: d.selected
                            }), D ? D.after(J) : s.element.append(J), D = J);
                            for (B++; t.length > B;) t.pop().element.remove()
                        }
                        for (; z.length > M;) z.pop()[0].element.remove()
                    }

                    var h;
                    if (!(h = s.match(d))) throw We("iexp", s, ha(f));
                    var l = c(h[2] || h[1]), m = h[4] || h[6], n = h[5], p = c(h[3] || ""), r = c(h[2] ? h[1] : m),
                        v = c(h[7]), u = h[8] ? c(h[8]) : null, z = [[{element: f, label: ""}]];
                    x && (a(x)(e), x.removeClass("ng-scope"), x.remove());
                    f.empty();
                    f.on("change", function () {
                        e.$apply(function () {
                            var a, c = v(e) || [], d = {}, h, l, p, s, w, x, y;
                            if (q) for (l = [], s = 0, x = z.length; s <
                            x; s++) for (a = z[s], p = 1, w = a.length; p < w; p++) {
                                if ((h = a[p].element)[0].selected) {
                                    h = h.val();
                                    n && (d[n] = h);
                                    if (u) for (y = 0; y < c.length && (d[m] = c[y], u(e, d) != h); y++) ; else d[m] = c[h];
                                    l.push(r(e, d))
                                }
                            } else if (h = f.val(), "?" == h) l = t; else if ("" === h) l = null; else if (u) for (y = 0; y < c.length; y++) {
                                if (d[m] = c[y], u(e, d) == h) {
                                    l = r(e, d);
                                    break
                                }
                            } else d[m] = c[h], n && (d[n] = h), l = r(e, d);
                            g.$setViewValue(l);
                            k()
                        })
                    });
                    g.$render = k;
                    e.$watchCollection(v, k);
                    q && e.$watchCollection(function () {
                        return g.$modelValue
                    }, k)
                }

                if (m[1]) {
                    var p = m[0];
                    m = m[1];
                    var q = k.multiple,
                        s = k.ngOptions, x = !1, v, C = u(X.createElement("option")),
                        y = u(X.createElement("optgroup")), z = C.clone();
                    k = 0;
                    for (var w = g.children(), B = w.length; k < B; k++) if ("" === w[k].value) {
                        v = x = w.eq(k);
                        break
                    }
                    p.init(m, x, z);
                    q && (m.$isEmpty = function (a) {
                        return !a || 0 === a.length
                    });
                    s ? n(e, g, m) : q ? l(e, g, m) : h(e, g, m, p)
                }
            }
        }
    }], id = ["$interpolate", function (a) {
        var c = {addOption: y, removeOption: y};
        return {
            restrict: "E", priority: 100, compile: function (d, e) {
                if (D(e.value)) {
                    var f = a(d.text(), !0);
                    f || e.$set("value", d.text())
                }
                return function (a, d, e) {
                    var h = d.parent(),
                        l = h.data("$selectController") || h.parent().data("$selectController");
                    l && l.databound ? d.prop("selected", !1) : l = c;
                    f ? a.$watch(f, function (a, c) {
                        e.$set("value", a);
                        a !== c && l.removeOption(c);
                        l.addOption(a)
                    }) : l.addOption(e.value);
                    d.on("$destroy", function () {
                        l.removeOption(e.value)
                    })
                }
            }
        }
    }], hd = $({restrict: "E", terminal: !0});
    Q.angular.bootstrap ? console.log("WARNING: Tried to load angular more than once.") : ((Da = Q.jQuery) && Da.fn.on ? (u = Da, B(Da.fn, {
        scope: La.scope, isolateScope: La.isolateScope, controller: La.controller, injector: La.injector,
        inheritedData: La.inheritedData
    }), Gb("remove", !0, !0, !1), Gb("empty", !1, !1, !1), Gb("html", !1, !1, !0)) : u = S, Ua.element = u, $c(Ua), u(X).ready(function () {
        Xc(X, fc)
    }))
})(window, document);
!window.angular.$$csp() && window.angular.element(document).find("head").prepend('<style type="text/css">@charset "UTF-8";[ng\\:cloak],[ng-cloak],[data-ng-cloak],[x-ng-cloak],.ng-cloak,.x-ng-cloak,.ng-hide{display:none !important;}ng\\:form{display:block;}.ng-animate-block-transitions{transition:0s all!important;-webkit-transition:0s all!important;}.ng-hide-add-active,.ng-hide-remove{display:block!important;}</style>');
