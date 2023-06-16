package ctrl

import auth.User
import common.Conf
import model.EventDTO
import org.segment.d.Ds
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.get('/manage/conf/reload') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    def c = Conf.instance.load()
    c.params
}.get('/manage/conf/view') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    Conf.instance.params
}.get('/manage/conf/set') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    def key = req.param('key')
    def value = req.param('value')
    assert key && value

    def c = Conf.instance
    c.put(key, value)
    c.params
}.post('/manage/test/dns/put/hz') { req, resp ->
    String hostname = req.param('hostname')
    String ip = req.param('ip')
    String ttl = req.param('ttl')
    assert hostname && ip && ttl

    // add what you want
    // this is a just dns server mocker, usually it's a core dns update kv in etcd
    log.info 'dns update - {}/{}/{}', hostname, ip, ttl
    'ok'
}.get('/manage/sql/exe') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    String sql = req.param('sql')
    new EventDTO().useD().exeUpdate(sql)
    'ok'
}.get('/manage/sql/set/view') { req, resp ->
    User u = req.session('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    def sqlHashMap = Ds.one('dms_server_ds').sqlHashHolder()
    def sb = new StringBuilder()
    sb << '<html><body><ul>'
    sqlHashMap.each { k, v ->
        sb << '<li>'
        sb << k
        sb << ': '
        sb << v
        sb << "</li>"
    }
    sb << '</ul></body></html>'
    resp.end sb.toString()
}