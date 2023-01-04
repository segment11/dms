package ha

import com.alibaba.fastjson.JSONObject
import com.github.kevinsawicki.http.HttpRequest
import common.Conf
import common.Utils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class EtcdLeaderChecker implements LeaderChecker {
    static final String KEY = 'dms_leader_ip'

    private String etcdAddr

    private long ttl

    EtcdLeaderChecker(String etcdAddr, long ttl) {
        this.etcdAddr = etcdAddr
        this.ttl = ttl
    }

    private String leaderAddr

    private void setTimeout(HttpRequest req) {
        def c = Conf.instance
        def connectTimeout = c.getInt('leader.check.connectTimeout', 500)
        def readTimeout = c.getInt('leader.check.readTimeout', 2000)
        req.connectTimeout(connectTimeout).readTimeout(readTimeout)
    }

    private boolean isAddrLeader(String addr) {
        try {
            def req = HttpRequest.get(addr + '/v2/stats/self')
            setTimeout(req)
            def body = req.body()

            def r = JSONObject.parseObject(body)
            return 'StateLeader' == r.getString('state')
        } catch (Exception e) {
            log.error('visit etcd error', e)
            return false
        }
    }

    private boolean updateKeyValue() {
        if (!leaderAddr) {
            return false
        }

        try {
            Map params = [:]
            params.ttl = ttl
            params.value = Utils.localIp()
            def reqPut = HttpRequest.put(leaderAddr + '/v2/keys/' + KEY + '?prevExist=false', params, true)
            setTimeout(reqPut)
            def body = reqPut.body()

            /*
{
"action": "create",
"node": {
"key": "/name",
"value": "kerry",
"expiration": "2022-11-27T11:35:06.665439053Z",
"ttl": 10,
"modifiedIndex": 46,
"createdIndex": 46
}
}
       */
            log.info body

            def r = JSONObject.parseObject(body)

            Integer errorCode = r.getInteger('errorCode')
            // 105 -> Key already exists
            return 105 != errorCode
        } catch (Exception e) {
            log.error('visit etcd error', e)
            return false
        }
    }

    @Override
    boolean isLeader() {
        if (!etcdAddr) {
            return false
        }
        if (leaderAddr && isAddrLeader(leaderAddr)) {
            return updateKeyValue()
        }

        for (addr in etcdAddr.split(',')) {
            if (isAddrLeader(addr)) {
                leaderAddr = addr
                break
            }
        }
        updateKeyValue()
    }

    private void updateAgain() {
        if (!leaderAddr) {
            throw new IllegalStateException('leader addr not found')
        }

        try {
            Map params = [:]
            params.ttl = ttl
            params.value = Utils.localIp()
            def reqPut = HttpRequest.put(leaderAddr + '/v2/keys/' + KEY, params, true)
            setTimeout(reqPut)
            def code = reqPut.code()
            if (200 != code) {
                log.warn 'continue leader failed - ' + reqPut.body()
            }
        } catch (Exception e) {
            log.error('visit etcd error', e)
        }
    }

    @Override
    void continueLeader() {
        if (!etcdAddr) {
            return
        }

        if (leaderAddr && isAddrLeader(leaderAddr)) {
            updateAgain()
            return
        }

        for (addr in etcdAddr.split(',')) {
            if (isAddrLeader(addr)) {
                leaderAddr = addr
                break
            }
        }

        updateAgain()
    }
}
