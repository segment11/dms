package script

import com.alibaba.fastjson.JSONObject
import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(this.getClass())

JSONObject jo = super.binding.getProperty('jo') as JSONObject

//def envList = jo.getJSONArray('envList')
def dnsServer = jo.getString('dnsServer')

if (dnsServer) {
    final String filePath = '/etc/resolv.conf'
    def lines = new File(filePath).text.readLines().reverse()

    def arr = dnsServer.split(',')
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



