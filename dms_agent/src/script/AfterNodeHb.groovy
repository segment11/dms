package script

import com.alibaba.fastjson.JSONObject
import model.json.GlobalEnvConf
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())

JSONObject jo = super.binding.getProperty('jo') as JSONObject

//def envList = jo.getJSONArray('envList')
def dnsInfo = jo.getObject('dnsInfo', GlobalEnvConf.DnsInfo)

if (dnsInfo && dnsInfo.nameservers) {
    final String filePath = '/etc/resolv.conf'
    def lines = new File(filePath).text.readLines().reverse()

    def arr = dnsInfo.nameservers.split(',')
    boolean needRewrite = false
    for (s in arr) {
        def ns = 'nameserver ' + s
        if (ns !in lines) {
            lines << ns
            needRewrite = true
        }
    }

    if (needRewrite) {
        new File(filePath).text = lines.reverse().join('\r\n')
        log.info 'done rewrite file: {}', filePath
    }
}

// void
return



